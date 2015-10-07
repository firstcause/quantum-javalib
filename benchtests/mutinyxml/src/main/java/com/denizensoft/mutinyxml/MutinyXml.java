package com.denizensoft.mutinyxml;

import org.apache.commons.lang3.StringUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Stack;
import java.util.logging.Logger;

/**
 * Created by sjm on 9/18/15.
 */
public class MutinyXml
{
	public interface AppInterface
	{
		public String invokeRequest(String stRequest);
	}

	private class State
	{
		Stack<XmlPullParser> mParserStack = new Stack<>();

		Stack<MutinyElement> mElementStack = new Stack<>();

		Stack<MutinyElement> mLinkStack = new Stack<>();

		protected MutinyElement element()
		{
			if(!mElementStack.empty())
				return mElementStack.peek();

			return null;
		}

		protected MutinyElement linkSpec()
		{
			if(!mLinkStack.empty())
				return mLinkStack.peek();

			return null;
		}

		protected XmlPullParser parser()
		{
			if(!mParserStack.empty())
				return mParserStack.peek();

			return null;
		}

		public MutinyElement popElement()
		{
			MutinyElement element = mElementStack.pop();

			return element;
		}

		public MutinyElement popLink()
		{
			MutinyElement element = mLinkStack.pop();

			return element;
		}

		public XmlPullParser popParser()
		{
			return mParserStack.pop();
		}

		public void pushElement(MutinyElement element)
		{
			mElementStack.push(element);
		}

		public void pushLink(MutinyElement element)
		{
			mLinkStack.push(element);
		}

		public void pushParser(String stFileSpec) throws FileNotFoundException
		{
			Reader reader = new FileReader(stFileSpec);

			XmlPullParser parser = null;

			try
			{
				parser = XmlPullParserFactory.newInstance().newPullParser();

				parser.setInput(reader);

				if(parser.getEventType() != XmlPullParser.START_DOCUMENT)
					throw new RuntimeException("Mutiny: Invalid document start? malformed file?", null);

				mParserStack.push(parser);
			}
			catch(XmlPullParserException e)
			{
				throw new RuntimeException("Mutiny: parser exception!", e);
			}
		}
	};

	protected Logger LOGGER = Logger.getLogger(MutinyElement.class.getName());

	protected State mState = new State();

	protected void logInfo(String stLogEntry, int nTabDepth)
	{
		LOGGER.info(String.format("%s%s\n", StringUtils.repeat("\t", nTabDepth), stLogEntry));
	}

	protected String parseAttributeValue(String stName)
	{
		int n = mState.parser().getAttributeCount();

		for(int i = 0; i < n; ++i)
		{
			String s1 = mState.parser().getAttributeName(i), s2 = mState.parser().getAttributeValue(i); //, s3 = parser.getAttributePrefix(i);

			if(s1.equals(stName))
				return s2;
		}
		return null;
	}

	public void loadMembers()
	{
		int nEventType = 0;

		String stFileSpec, stTag;

		try
		{
			while(mState.parser() != null)
			{
				nEventType = mState.parser().next();

				switch(nEventType)
				{
					case XmlPullParser.END_DOCUMENT :
					{
						XmlPullParser parser = mState.mParserStack.pop();

//						logInfo(String.format("Mutiny: Closing document: %s", parser.g.getProperty("mutiny_filespec")),
//								mState.parser().getDepth());
					}
					break;

					case XmlPullParser.START_TAG :
					{
						MutinyElement element = null;

						stTag = mState.parser().getName();

						if(stTag.equals("LinkSpec"))
						{
							element = new MutinyElement(null,stTag);
							element.parseAttributes(mState.parser());

							stFileSpec = element.attribute("filespec");

							if(stFileSpec == null)
								throw new RuntimeException("Mutiny: LinkSpec source file not specified!");

							mState.pushParser(stFileSpec);
							mState.pushLink(element);

							if(mState.parser().getEventType() != XmlPullParser.START_DOCUMENT)
								throw new RuntimeException("Mutiny: parser state error, expected document start!", null);

							logInfo(String.format("Mutiny: Loading linked file: %s", stFileSpec), mState.parser().getDepth());
						}
						else
						{
							element = new MutinyElement(mState.element(),stTag);
							element.parseAttributes(mState.parser());
							mState.element().addElement(element);
							mState.pushElement(element);
						}
					}
					break;

					case XmlPullParser.END_TAG :
					{
						stTag = mState.parser().getName();

						if(stTag.equals("LinkSpec"))
						{
							MutinyElement linkSpec = mState.popLink();

							logInfo(String.format("Mutiny: Closing link: %s", linkSpec.attribute("filespec")), mState.parser().getDepth());
						}
						else
						{
							MutinyElement element = mState.popElement();

							logInfo(String.format("Mutiny: Closing tag: %s", element.tag()), mState.parser().getDepth());
						}
					}
					break;

					case XmlPullParser.TEXT:
					{
						String s1 = mState.parser().getText().trim();

						if(!s1.isEmpty())
							mState.element().mText+=s1;
					}
					break;
				}
			}
		}
		catch(XmlPullParserException e)
		{
			throw new RuntimeException("MutinyXml.Element - parser exception in parseElement()", e);
		}
		catch(IOException e)
		{
			throw new RuntimeException("MutinyXml.Element - IO exception in parseElement()", e);
		}
	}

	public MutinyDocument loadDocument(MutinyXml.AppInterface appInterface,String stFileSpec)
	{
		MutinyDocument mutinyDocument = null;

		try
		{
			// Lets start things off...
			//
			mutinyDocument = new MutinyDocument(appInterface,stFileSpec);

			mState.pushElement(mutinyDocument);

			mState.pushParser(stFileSpec);

			loadMembers();
		}
		catch(IOException e)
		{
			throw new RuntimeException("MutinyXml.Element - IO exception in parseElement()", e);
		}
		return mutinyDocument;
	}

	public MutinyXml()
	{
	}
}
