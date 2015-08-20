package com.denizensoft.insurgent;

import com.denizensoft.jlib.Function;
import com.denizensoft.jlib.LibException;
import com.denizensoft.jlib.NotFoundException;
import com.denizensoft.jlib.ParserException;
import com.denizensoft.jlib.ScopeContext;
import com.denizensoft.jlib.Variable;

import java.util.ArrayList;

public class InsurgentParser
{
	public interface ParserContext
	{
		public ScopeContext scopeContext();

		public void startOfLine();

		public String variableNotFoundHook(String stTag) throws NotFoundException;

		public void output(String string);

		public void endOfLine();

		public void error(Exception e);

	}

	protected ParserContext mParserContext = null;

	abstract public class Token
	{
		final protected String stToken;

		abstract public String invokeNode(ParserContext parserContext) throws LibException;

		public Token(String stToken)
		{
			this.stToken = stToken;
		}
	}

	public class FunctionRef extends Token
	{
		final protected Token[] mArgTokens;

		@Override
		public String invokeNode(ParserContext parserContext) throws LibException
		{
			char ch;

			String s1 = "";

			// Have to push scope in case local vars are created
			//
			parserContext.scopeContext().pushScope();

			String[] stArgs = new String[mArgTokens.length];

			try
			{
				Function f1 = parserContext.scopeContext().scope().findFunction(stToken);

				int i = 0;

				for(Token token: mArgTokens)
				{
					s1 = token.invokeNode(parserContext);
					stArgs[i++] = s1;
				}

				s1 = f1.invoke(parserContext.scopeContext().scope(),stArgs);
			}
			catch(NotFoundException e)
			{
				s1 = "!ERRNOTFOUND!";
			}

			parserContext.scopeContext().popScope();

			return s1;
		}

		public FunctionRef(String stToken, Token[] ArgTokens)
		{
			super(stToken);
			this.mArgTokens = ArgTokens;
		}
	}

	public class LiteralRef extends Token
	{
		@Override
		public String invokeNode(ParserContext parserContext) throws LibException
		{
			return stToken;
		}

		public LiteralRef(String stToken)
		{
			super(stToken);
		}
	}

	public class CapsuleRef extends Token
	{
		final protected int nStartIndex, nEndIndex;

		private Token mTokenRef;

		@Override
		public String invokeNode(ParserContext parserContext) throws LibException
		{
			return mTokenRef.invokeNode(parserContext);
		}

		public CapsuleRef(int nStartIndex, int nEndIndex, Token token)
		{
			super(null);

			this.nStartIndex = nStartIndex;
			this.nEndIndex = nEndIndex;

			mTokenRef = token;
		}
	}

	public class VariableRef extends Token
	{
		@Override
		public String invokeNode(ParserContext parserContext) throws LibException
		{
			try
			{
				Variable v1 = parserContext.scopeContext().scope().findVariable(stToken);

				return v1.invoke(parserContext.scopeContext().scope(), null);
			}
			catch(NotFoundException e)
			{
				// We'll try the hook below...
				//
			}
			return parserContext.variableNotFoundHook(stToken);
		}

		public VariableRef(String stToken)
		{
			super(stToken);
		}
	}

	public class State
	{
		public int
			nLine = 0,
			nParseIndex=0;

		public String stParserLine = null;

		public void bumpIndex()
		{
			if(nParseIndex < stParserLine.length())
				++nParseIndex;
		}

		public void rewindIndex()
		{
			if(nParseIndex > 0)
				--nParseIndex;
		}

		public char popNextChar()
		{
			if(nParseIndex < stParserLine.length())
				return stParserLine.charAt(nParseIndex++);

			return 0;
		}

		protected char peekNextChar()
		{
			if(nParseIndex < stParserLine.length())
				return stParserLine.charAt(nParseIndex);

			return 0;
		}

		public void resetCurrentLine(String stLine)
		{
			nParseIndex=0;

			stParserLine = stLine;
		}

		public void pushNextLine(String stLine)
		{
			resetCurrentLine(stLine);

			++nLine;
		}

		public State()
		{
		}
	}

	private State state = new State();

	protected char popNextChar(boolean bEatWhitespace)
	{
		if(bEatWhitespace)
			parseEatWhitespace();

		return state().popNextChar();
	}

	protected char peekNextChar(boolean bEatWhitespace)
	{
		if(bEatWhitespace)
			parseEatWhitespace();

		return state().peekNextChar();
	}

	protected Token parseLiteralString() throws ParserException
	{
		char ch, cQuote = popNextChar(false);

		String s1 = "";

		if(cQuote != '\'' && cQuote != '\"')
			parserError("Expecting a quote here??");

		while(( ch = popNextChar(false)) != 0 && ch != cQuote)
		{
			s1 += ch;
		}

		if(ch == 0)
			parserError("Unterminated quoted string.");

		return new LiteralRef(s1);
	}

	protected void parseEatWhitespace()
	{
		while(Character.isWhitespace(state().peekNextChar()))
			state().bumpIndex();
	}

	protected String parseTag() throws ParserException
	{
		String s1 = "";

		// System.out.println(String.format("Expecting  tag: %s",state().stParserLine.substring(state().nParseIndex)));

		char ch = popNextChar(true);

		if(ch != '_' && !Character.isAlphabetic(ch))
			parserError("Tags must begin with a letter or underscore.");

		do
		{
			if(ch == '_' || Character.isAlphabetic(ch) || Character.isDigit(ch))
			{
				s1 += ch;
			}
			else
			{
				state().rewindIndex();
				break;
			}

		} while((ch = popNextChar(false)) != 0);

		return s1;
	}

	protected void parserError(String stMessage) throws ParserException
	{
		throw new ParserException(String.format("Parser: Line %d:%d - %s",state().nLine,state().nParseIndex,stMessage));
	}

	protected Token[] parseArgsScopeChunk() throws ParserException
	{
		char ch = 0;

		int nStartIndex = state().nParseIndex;

		boolean bCommaExpected = false;

		ArrayList<Token> ArgTokens = new ArrayList<Token>();

		// System.out.println(String.format("Expecting args: %s",state().stParserLine.substring(state().nParseIndex)));

		if(peekNextChar(true)!='(')
			parserError("Missing an open parenthesis?");

		state().bumpIndex();

		while(( ch = peekNextChar(true)) != ')')
		{
			if(ch == ',')
			{
				if(!bCommaExpected)
					parserError("Unexpected comma in argument scope?");

				state().bumpIndex();
			}
			else if(bCommaExpected)
			{
				parserError("Missing a comma in argument scope?");
			}

			Token token = parseUnknownArgChunk();

			ArgTokens.add(token);

			bCommaExpected = true;
		}

		state().bumpIndex();

		return( ArgTokens.toArray(new Token[ArgTokens.size()]));
	}

	protected Token parseUnknownArgChunk() throws ParserException
	{
		char ch = 0;

		int nStartIndex = state().nParseIndex;

		ch = peekNextChar(true);

		Token token=null;

		// System.out.println(String.format("Expecting  unk: %s",state().stParserLine.substring(state().nParseIndex)));

		if(Character.isAlphabetic(ch))
		{
			token = parseNextToken();
		}
		else if(ch == '\'' || ch == '\"')
		{
			token = parseLiteralString();
		}
		else
		{
			parserError("Argument starts with invalid character?");
		}
		return token;
	}

	public Token parseNextToken() throws ParserException
	{
		String stTag = parseTag();

		if(stTag.length() == 0)
			parserError("Missing tag name was expected.");

		if(peekNextChar(true) != '(')
			return new VariableRef(stTag);

		// The tag _may_ be a function reference...
		//
		// An arg scope is expected here
		//
		Token[] ArgTokens = parseArgsScopeChunk();

		return new FunctionRef(stTag,ArgTokens);
	}

	public CapsuleRef parseNextCapsule() throws ParserException
	{
		char ch;

		int nStartIndex = 0;

		CapsuleRef capsuleRef = null;

		while((ch = popNextChar(true))!=0)
		{
			if(ch != '$')
				continue;

			if( peekNextChar(false) != '(')
				continue;

			nStartIndex = ( state().nParseIndex - 1 );

			state().bumpIndex();

			Token token = parseNextToken();

			if( peekNextChar(true) != ')')
				parserError("Missing a closing parenthesis?");

			capsuleRef = new CapsuleRef(nStartIndex,state().nParseIndex,token);

			state().bumpIndex();

			break;
		}
		return capsuleRef;
	}

	public int processInputLine(String stLine)
	{
		int nCount = 0, nStartIndex = 0;

		CapsuleRef capsuleRef=null;

		if(stLine == null)
			return 0;

		state().pushNextLine(stLine);

		mParserContext.startOfLine();

		try
		{
			while(( capsuleRef=parseNextCapsule()) != null)
			{
				++nCount;

				if(nStartIndex < capsuleRef.nStartIndex)
					mParserContext.output(stLine.substring(nStartIndex, capsuleRef.nStartIndex));

				nStartIndex = ( capsuleRef.nEndIndex + 1 );

				mParserContext.output(capsuleRef.invokeNode(mParserContext));
			}

			if(nCount != 0)
			{
				if(nStartIndex < stLine.length())
					mParserContext.output(stLine.substring(nStartIndex));
			}
			else
			{
				mParserContext.output(stLine);
			}

			mParserContext.endOfLine();
		}
		catch(NotFoundException e)
		{
			mParserContext.error(e);
		}
		catch(ParserException e)
		{
			mParserContext.error(e);
		}
		catch(LibException e)
		{
			e.printStackTrace();
		}
		return nCount;
	}

	public State state()
	{
		return state;
	}

	public InsurgentParser(ParserContext parserContext)
	{
		this.mParserContext = parserContext;
	}
}
