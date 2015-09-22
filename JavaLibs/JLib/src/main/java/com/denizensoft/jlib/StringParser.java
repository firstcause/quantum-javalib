package com.denizensoft.jlib;

import java.util.ArrayList;

/**
 * Created by sjm on 12/15/2014.
 */
public class StringParser
{
	static public class ParseResult
	{
		public int nParseIndex = 0;

		public char chTerminator = 0;

		public String stToken = "";

		public ParseResult(int nParseIndex)
		{
			this.nParseIndex = nParseIndex;
		}
	}

	static public String parseStripQuotes(String stLine)
			throws ParserException
	{
		char ch, cQuote = 0;

		String stString = "";

		stLine.trim();

		cQuote = stLine.charAt(0);

		if(cQuote!='\"' && cQuote != '\'')
			throw new ParserException(String.format("Expected quoted string: %s",stLine));

		for( int i=1 ;i < stLine.length(); ++i)
		{
			ch = stLine.charAt(i);

			if(cQuote != 0)
			{
				if(ch == cQuote)
				{
					cQuote = 0;
					continue;
				}
			}
			else
			{
				throw new ParserException(String.format("Malformed quoted string: %s",stLine));
			}

			stString += ch;
		}

		if(cQuote != 0)
			throw new ParserException(String.format("Unterminated string in: %s",stLine));

		return(stString);
	}

	static public ParseResult parseDelimitedTokenAt(int nStartIndex,String stLine,String stDelims,boolean bStripQuotes)
			throws ParserException
	{
		char ch, cQuote = 0;

		ParseResult result = new ParseResult(nStartIndex);

		for( ;result.nParseIndex < stLine.length(); ++result.nParseIndex)
		{
			ch = stLine.charAt(result.nParseIndex);

			if(cQuote != 0)
			{
				if(ch == cQuote)
				{
					cQuote = 0;

					if(bStripQuotes)
						continue;
				}
			}
			else if(!Character.isWhitespace(ch))
			{
				if(ch == '\"' || ch == '\'')
				{
					cQuote = ch;

					if(bStripQuotes)
						continue;
				}
				else if(stDelims.indexOf(ch) >= 0)
				{
					result.chTerminator = ch;

					return result;
				}
			}
			result.stToken += ch;
		}

		if(cQuote != 0)
			throw new ParserException(String.format("Unterminated string in: %s",stLine));

		return(result);
	}

	static public String[] parseDelimitedStrings(String stLine,String stDelims,boolean bStripQuotes)
			throws ParserException
	{
		int nParseIndex = 0;

		ParseResult result;

		ArrayList<String> stList = new ArrayList<String>();

		do
		{
			result = parseDelimitedTokenAt(nParseIndex,stLine,stDelims,bStripQuotes);

			if(result.stToken.length() == 0 )
			{
				if(result.chTerminator != 0)
					throw new ParserException(String.format("Comma with missing argument in: %s", stLine));
			}
			else
			{
				stList.add(result.stToken);

				nParseIndex = ( result.nParseIndex + 1 );
			}

		} while(result.chTerminator != 0);

		return stList.toArray(new String[stList.size()]);
	}
}
