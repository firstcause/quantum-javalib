package com.denizensoft.mutinyxml;

import org.apache.commons.lang3.StringUtils;
import org.xmlpull.mxp1.MXParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by sjm on 9/18/15.
 */
public class MutinyElement
{
	protected String mTag = null, mText = null;

	protected MutinyElement mParent = null;

	protected Map<String, String> mAttributeMap = null;

	protected HashMap<String,HashMap<String,MutinyElement>> mChildMap = null;

	protected Logger LOGGER = Logger.getLogger(MutinyElement.class.getName());

	protected void logInfo(String stLogEntry, int nTabDepth)
	{
		LOGGER.info(String.format("%s%s\n", StringUtils.repeat("\t", nTabDepth), stLogEntry));
	}

	public MutinyElement parent()
	{
		return mParent;
	}

	public MutinyElement rootElement()
	{
		if(mParent != null)
			return mParent.rootElement();

		return this;
	}

	public MutinyElement getElement(String stTag,String stPathSpec)
	{
		if(stPathSpec.charAt(0) == '/')
		{
			if(stPathSpec.equals("/"))
				return rootElement();

			return rootElement().getElement(stTag,stPathSpec.substring(1));
		}
		else
		{
			HashMap<String,MutinyElement> tagMap = mChildMap.get(stTag);

			if(tagMap != null)
			{
				Pattern pattern = Pattern.compile("(\\w+)/?(.*)");

				Matcher matcher = pattern.matcher(stPathSpec);

				if(matcher.matches())
				{
					MutinyElement element = tagMap.get(matcher.group(1));

					return element;
				}
			}
		}
		return null;
	}

	public String tag()
	{
		return mTag;
	}

	public String attribute(String stTag)
	{
		if(mAttributeMap.containsKey(stTag))
			return mAttributeMap.get(stTag);

		return null;
	}

	public Map<String, String> attributes()
	{
		return mAttributeMap;
	}

	protected void parseAttributes(XmlPullParser parser)
	{
		int n = parser.getAttributeCount();

		if(n > 0)
		{
			mAttributeMap = new HashMap<String, String>();

			for(int i = 0; i < n; ++i)
			{
				String s1 = parser.getAttributeName(i), s2 = parser.getAttributeValue(i), s3 = parser.getAttributePrefix(i);

				mAttributeMap.put(s1, s2);
			}
		}
	}

	protected void parseElement(XmlPullParser parser)
	{
		int nEventType;

		try
		{
			if(parser.getEventType() != XmlPullParser.START_TAG)
				throw new RuntimeException("MutinyXml.Element - parser state error, expected SOT", null);

			mTag = parser.getName();

			logInfo(String.format(Locale.getDefault(), "Start: <%s>", mTag), parser.getDepth());

			parseAttributes(parser);

			do
			{
				nEventType = parser.next();

				switch(nEventType)
				{
					case XmlPullParser.START_TAG:
					{
						MutinyElement element = new MutinyElement(this,parser);

						if(element.attributes() == null || !element.attributes().containsKey("name"))
						{
							logInfo("-------------------> Discarding an unnamed element! <------------------",
									parser.getDepth());

							return;
						}

						if(mChildMap == null)
							mChildMap = new HashMap<String,HashMap<String, MutinyElement>>();

						HashMap<String, MutinyElement> tagMap = mChildMap.get(element.tag());

						if(tagMap == null)
						{
							tagMap = new HashMap<>();

							mChildMap.put(element.tag(), tagMap);
						}

						tagMap.put(element.attribute("name"), element);
					}
					break;

					case XmlPullParser.TEXT:
					{
						String s1 = parser.getText().trim();

						if(s1.length() != 0)
							mText = s1;

						logInfo(String.format(Locale.getDefault(), "TEXT: %s", mText), parser.getDepth());
					}
					break;

				}

			} while(nEventType != XmlPullParser.END_TAG);

			for(Map.Entry<String, String> entry : mAttributeMap.entrySet())
			{
				logInfo(String.format("%s - %s\n", entry.getKey(), entry.getValue()), parser.getDepth() + 1);
			}

			logInfo(String.format(Locale.getDefault(), "End: </%s>", mTag), parser.getDepth());

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

	public MutinyElement(MutinyElement parentElement, XmlPullParser parser)
	{
		mParent = parentElement;

		parseElement(parser);
	}

	public MutinyElement(MutinyElement parentElement,Reader reader)
	{
		mParent = parentElement;

		MXParser parser = new MXParser();

		try
		{
			parser.setInput(reader);

			if(parser.getEventType() != XmlPullParser.START_DOCUMENT)
				throw new RuntimeException("MutinyXml.Element - Expected SOD in constructor(reader)", null);

			parser.next();

			parseElement(parser);
		}
		catch(XmlPullParserException e)
		{
			throw new RuntimeException("MutinyXml.Element parser exception!", e);
		}
		catch(IOException e)
		{
			throw new RuntimeException("MutinyXml.Element io exception!", e);
		}
	}
}
