package com.denizensoft.mutinyxml;

import org.apache.commons.lang3.StringUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by sjm on 9/18/15.
 */
public class MutinyElement
{
	int nUnNamedSequence = 0;

	protected String mTag = null, mText = null;

	protected MutinyElement mParent = null;

	protected TreeMap<String, String> mAttributeMap = null;

	protected TreeMap<String,TreeMap<String,MutinyElement>> mChildMap = null;

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
		MutinyElement element = null;

		if(stPathSpec.startsWith("/") == true)
		{
			if(stPathSpec.equals("/"))
				return rootElement();

			return rootElement().getElement(stTag,stPathSpec.substring(1));
		}
		else
		{
			TreeMap<String,MutinyElement> tagMap = mChildMap.get(stTag);

			if(tagMap != null)
			{
				Pattern pattern = Pattern.compile("(\\w+)/?(.*)");

				Matcher matcher = pattern.matcher(stPathSpec);

				if(matcher.matches())
				{
					element = tagMap.get(matcher.group(1));

					if(!matcher.group(2).isEmpty())
						return element.getElement(stTag,matcher.group(2));
				}
			}
		}
		return element;
	}

	public String tag()
	{
		return mTag;
	}

	public String attribute(String stTag)
	{
		if(mAttributeMap != null && mAttributeMap.containsKey(stTag))
			return mAttributeMap.get(stTag);

		return null;
	}

	public Map<String, String> attributes()
	{
		return mAttributeMap;
	}

	protected void addAttribute(String stTag,String stValue)
	{
		if(mAttributeMap == null)
			mAttributeMap = new TreeMap<String,String>();

		mAttributeMap.put(stTag, stValue);
	}

	protected void parseAttributes(XmlPullParser parser)
	{
		int n = parser.getAttributeCount();

		for(int i = 0; i < n; ++i)
		{
			String s1 = parser.getAttributeName(i), s2 = parser.getAttributeValue(i); //, s3 = parser.getAttributePrefix(i);

			addAttribute(s1, s2);
		}
	}

	protected String parseAttributeValue(XmlPullParser parser,String stName)
	{
		int n = parser.getAttributeCount();

		for(int i = 0; i < n; ++i)
		{
			String s1 = parser.getAttributeName(i), s2 = parser.getAttributeValue(i); //, s3 = parser.getAttributePrefix(i);

			if(s1.equals(stName))
				return s2;
		}
		return null;
	}

	public MutinyElement(MutinyElement parentElement, XmlPullParser parser)
	{
		mParent = parentElement;

		try
		{
			int nEventType;

			if(parser.getEventType() != XmlPullParser.START_TAG)
				throw new RuntimeException("MutinyXml.Element - parser state error, expected SOT", null);

			mTag = parser.getName();

			if(mTag.equals("LinkSpec") && mParent == null)
				throw new RuntimeException("Mutiny: Bad start, cannot begin with a LinkSpec!!");

			logInfo(String.format(Locale.getDefault(), "Start: <%s>", mTag), parser.getDepth());

			parseAttributes(parser);

			while(( nEventType = parser.next()) != XmlPullParser.END_TAG)
			{
				switch(nEventType)
				{
					case XmlPullParser.START_TAG:
					{
						MutinyElement element = null;

						if(parser.getName().equals("LinkSpec"))
						{
							String stFileSpec = parseAttributeValue(parser,"filespec");

							if(stFileSpec == null)
								throw new RuntimeException("Mutiny: LinkSpec source file not specified!");

							logInfo(String.format("Mutiny: Loading linked file: %s",stFileSpec),parser.getDepth());

							element = MutinyXml.parseFile(stFileSpec);
							element.mParent = this;
						}
						else
						{
							element = new MutinyElement(this, parser);

							if(element.attribute("name") == null)
								element.addAttribute("name", String.format("_no_name_%04X", nUnNamedSequence++));
						}

						if(mChildMap == null)
							mChildMap = new TreeMap<String,TreeMap<String, MutinyElement>>();

						TreeMap<String, MutinyElement> tagMap = mChildMap.get(element.tag());

						if(tagMap == null)
						{
							tagMap = new TreeMap<String,MutinyElement>();

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
			}

//			for(Map.Entry<String, String> entry : mAttributeMap.entrySet())
//			{
//				logInfo(String.format("%s - %s\n", entry.getKey(), entry.getValue()), parser.getDepth() + 1);
//			}

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
}
