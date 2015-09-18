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
public class MutinyElement extends HashMap<String, HashMap<String, MutinyElement>>
{
	protected String mTag = null;

	protected MutinyElement mParent = null;

	protected Map<String, String> mAttributeMap = null;

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
			HashMap<String,MutinyElement> tagMap = get(stTag);

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

			for(Entry<String, String> entry : mAttributeMap.entrySet())
			{
				logInfo(String.format("%s - %s\n", entry.getKey(), entry.getValue()), parser.getDepth() + 1);
			}
		}
	}

	protected void parseTags(XmlPullParser parser)
	{
		try
		{
			int nEventType = 0;

			while((nEventType = parser.getEventType()) != XmlPullParser.END_DOCUMENT)
			{
				System.out.printf("%s", StringUtils.repeat("\t", parser.getDepth()));

				switch(nEventType)
				{
					case XmlPullParser.START_DOCUMENT:
					{
						LOGGER.info("----> SOD <----\n");
					}
					break;

					case XmlPullParser.START_TAG:
					{
						MutinyElement element = new MutinyElement(parser);

						if(element.attributes() != null && element.attributes().containsKey("name"))
						{
							HashMap<String, MutinyElement> tagMap = get(element.tag());

							if(tagMap == null)
							{
								tagMap = new HashMap<>();

								put(element.tag(), tagMap);
							}

							tagMap.put(element.attribute("name"), element);
						}
					}
					break;

					case XmlPullParser.END_TAG:
					{
						if(mAttributeMap.containsKey("name"))
						{
							logInfo(String.format(Locale.getDefault(), "End: <%s:%s>",
									mTag, mAttributeMap.get("name")), parser.getDepth());
						}
						else
						{
							logInfo(String.format(Locale.getDefault(), "End: <%s:(noname)>",
									mTag), parser.getDepth());
						}
					}
					return;

					case XmlPullParser.TEXT:
					{
						logInfo(String.format(Locale.getDefault(), "TEXT: %s", parser.getText()), parser.getDepth());
					}
					break;

				}

				parser.next();
			}

			LOGGER.info("----> EOD <----\n");

		}
		catch(XmlPullParserException e)
		{
			throw new RuntimeException("MutinyXml.Element - parser exception in parseTags()", e);
		}
		catch(IOException e)
		{
			throw new RuntimeException("MutinyXml.Element - io exception in parseTags()", e);
		}
	}

	protected void parseElement(XmlPullParser parser)
	{
		try
		{
			if(parser.getEventType() != XmlPullParser.START_TAG)
				throw new RuntimeException("MutinyXml.Element - parser state error in constructor(parser)", null);

			mTag = parser.getName();

			logInfo(String.format(Locale.getDefault(), "Start: <%s>", mTag), parser.getDepth());

			parseAttributes(parser);

			if(mAttributeMap != null && mAttributeMap.containsKey("name"))
			{
				logInfo(String.format(Locale.getDefault(), "Name: %s", mAttributeMap.get("name")), parser.getDepth());

				parser.next();

				parseTags(parser);
			}
			else
			{
				logInfo(String.format(Locale.getDefault(), "Start: <%s:(noname)>",mTag), parser.getDepth());
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

	public MutinyElement(XmlPullParser parser)
	{
		parseElement(parser);
	}

	public MutinyElement(Reader reader)
	{
		MXParser parser = new MXParser();

		try
		{
			parser.setInput(reader);

			if(parser.getEventType() != XmlPullParser.START_DOCUMENT)
				throw new RuntimeException("MutinyXml.Element - parser state error in constructor(reader)", null);

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
