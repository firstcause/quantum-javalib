package com.denizensoft.mutinyxml;

import org.apache.commons.lang3.StringUtils;
import org.xmlpull.v1.XmlPullParser;

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
	int nNonameSequence = 0;

	protected String mTag = null, mText = "";

	protected MutinyElement mParent = null;

	protected TreeMap<String, String> mAttributeMap = null;

	protected TreeMap<String,TreeMap<String,MutinyElement>> mTagMap = null;

	protected Logger LOGGER = Logger.getLogger(MutinyElement.class.getName());

	protected void addAttribute(String stTag,String stValue)
	{
		if(mAttributeMap == null)
			mAttributeMap = new TreeMap<String,String>();

		mAttributeMap.put(stTag, stValue);
	}

	protected void addElement(MutinyElement element)
	{
		if(mTagMap == null)
			mTagMap = new TreeMap<String,TreeMap<String, MutinyElement>>();

		TreeMap<String, MutinyElement> elementMap = mTagMap.get(element.tag());

		if(elementMap == null)
		{
			elementMap = new TreeMap<String,MutinyElement>();

			mTagMap.put(element.tag(), elementMap);
		}

		String stName = element.attribute("name");

		if(stName == null)
		{
			stName = String.format("__noname_%04X", nNonameSequence);
			element.addAttribute("name", stName);
		}

		elementMap.put(stName, element);
	}

	protected void logInfo(String stLogEntry, int nTabDepth)
	{
		LOGGER.info(String.format("%s%s\n", StringUtils.repeat("\t", nTabDepth), stLogEntry));
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

	public Map<String,MutinyElement> childrenByTag(String stTag)
	{
		Map<String,MutinyElement> map = null;

		if(mTagMap != null)
			map = mTagMap.get(stTag);

		return map;
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
			TreeMap<String,MutinyElement> elementMap = mTagMap.get(stTag);

			if(elementMap != null)
			{
				Pattern pattern = Pattern.compile("(\\w+)/?(.*)");

				Matcher matcher = pattern.matcher(stPathSpec);

				if(matcher.matches())
				{
					element = elementMap.get(matcher.group(1));

					if(!matcher.group(2).isEmpty())
						return element.getElement(stTag,matcher.group(2));
				}
			}
		}
		return element;
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

	public String tag()
	{
		return mTag;
	}

	public MutinyElement()
	{
	}

	public MutinyElement(MutinyElement parentElement, String stTag)
	{
		mParent = parentElement;
		mTag = stTag;
	}
}
