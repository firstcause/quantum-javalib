package com.denizensoft.droidlib;

import com.denizensoft.droidlib.Requester;

import org.apache.commons.lang3.StringUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.*;
import java.util.Stack;
import java.util.TreeMap;
import java.util.logging.Logger;

/**
 * Created by sjm on 9/18/15.
 */
public class XmlLoader
{
	protected static int nNonameSequence = 0;

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

		public void pushParser(InputStream inputStream)
		{
			XmlPullParser parser = null;

			try
			{
				parser = XmlPullParserFactory.newInstance().newPullParser();

				parser.setInput(new InputStreamReader(inputStream));

				if(parser.getEventType() != XmlPullParser.START_DOCUMENT)
					throw new RuntimeException("Mutiny: Invalid document start? malformed file?", null);

				mParserStack.push(parser);
			}
			catch(XmlPullParserException e)
			{
				throw new RuntimeException("Mutiny: parser exception!", e);
			}
		}
	}

	protected Logger LOGGER = Logger.getLogger(MutinyElement.class.getName());

	protected State mState = new State();

	protected MutinyElement allocateElement(String stTag) throws FileNotFoundException, XmlPullParserException
	{
		MutinyElement element = null;

		switch(stTag)
		{
			case "FolderDef":
			{
				element = new FolderElement(parseAttributes());
			}
			break;

			case "MenuItem":
			{
				element = new MenuItemElement(parseAttributes());
			}
			break;

			case "StackMenuDef":
			{
				element = new StackMenuElement(parseAttributes());
			}
			break;

			default:
			{
				throw new RuntimeException(String.format("XmlLoader: unknown element tag: %s",stTag));
			}
		}
		return element;
	}

	protected void logInfo(String stLogEntry, int nTabDepth)
	{
		LOGGER.info(String.format("%s%s\n", StringUtils.repeat("\t", nTabDepth), stLogEntry));
	}

	protected TreeMap<String,String> parseAttributes()
	{
		XmlPullParser parser = mState.parser();

		TreeMap<String,String> attributeMap = new TreeMap<>();

		int n = parser.getAttributeCount();

		for(int i = 0; i < n; ++i)
		{
			String s1 = parser.getAttributeName(i), s2 = parser.getAttributeValue(i); //, s3 = parser.getAttributePrefix(i);

			attributeMap.put(s1,s2);
		}

		if(!attributeMap.containsKey("name"))
			attributeMap.put("name", String.format("__noname_%04X", nNonameSequence++));

		return attributeMap;
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

		String stTag;

		try
		{
			while(mState.parser() != null)
			{
				nEventType = mState.parser().nextToken();

				switch(nEventType)
				{
					case XmlPullParser.CDSECT:
					{
						String s1 = mState.parser().getText().trim();

						if(!s1.isEmpty())
							mState.element().mCDATA=s1;
					}
					break;

					case XmlPullParser.END_DOCUMENT :
					{
						XmlPullParser parser = mState.popParser();

//						logInfo(String.format("Mutiny: Closing document: %s", parser.g.getProperty("mutiny_filespec")),
//								mState.parser().getDepth());
					}
					break;

					case XmlPullParser.START_TAG :
					{
						MutinyElement element = allocateElement(mState.parser().getName());

						element.attachToParent(mState.element());
						mState.pushElement(element);
					}
					break;

					case XmlPullParser.END_TAG :
					{
						stTag = mState.parser().getName();

						if(stTag.equals("LinkSpecDef"))
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
			throw new RuntimeException("XmlLoader.Element - parser exception in parseElement()", e);
		}
		catch(IOException e)
		{
			throw new RuntimeException("XmlLoader.Element - IO exception in parseElement()", e);
		}
	}

	public MainContainer loadDocument(InputStream inputStream, Requester hostTarget)
	{
		// Lets start things off...
		//
		MainContainer mainContainer = new MainContainer(hostTarget);

		mState.pushElement(mainContainer);

		mState.pushParser(inputStream);

		loadMembers();

		return mainContainer;
	}

	public XmlLoader()
	{
	}
}
