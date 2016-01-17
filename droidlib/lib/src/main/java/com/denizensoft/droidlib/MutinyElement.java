package com.denizensoft.droidlib;

import com.denizensoft.droidlib.Requester;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
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
	protected MutinyElement mParent = null;

	protected String mText = "", mCDATA = "";

	protected TreeMap<String, String> mAttributeMap = null;

	protected TreeMap<String,MutinyElement> mElementMap = null;

	protected TreeMap<String,ArrayList<MutinyElement>> mTypeMap = null;

	protected Logger LOGGER = Logger.getLogger(MutinyElement.class.getName());

	protected static final Pattern
			mAttributePathPattern = Pattern.compile("^([^:]+)\\:(\\w+)$"),
			mPathTypePattern = Pattern.compile("(\\/|\\@\\/|\\$\\/)(.*)");

	private void attachChild(MutinyElement element)
	{
		String stName = element.attribute("name");

		if(mTypeMap == null)
			mTypeMap = new TreeMap<String,ArrayList<MutinyElement>>();

		ArrayList<MutinyElement> tagArray = mTypeMap.get(tag());

		if(tagArray == null)
		{
			tagArray = new ArrayList<MutinyElement>();

			mTypeMap.put(element.tag(),tagArray);
		}

		if(mElementMap == null)
			mElementMap = new TreeMap<>();

		if(mElementMap.containsKey(stName))
			throw new RuntimeException(String.format("Mutiny: element name collision at: %s/%s",elementPath(),stName),null);

		tagArray.add(element);

		mElementMap.put(stName, element);

		element.mParent = this;
	}

	protected void addAttribute(String stTag,String stValue)
	{
		if(mAttributeMap == null)
			mAttributeMap = new TreeMap<String,String>();

		mAttributeMap.put(stTag, stValue);
	}

	public void attachToParent(MutinyElement parentElement)
	{
		parentElement.attachChild(this);
	}

	public void postResult(String stToken)
	{
		getElement("/").postResult(stToken);
	}

	protected void logInfo(String stLogEntry, int nTabDepth)
	{
		LOGGER.info(String.format("%s%s\n", StringUtils.repeat("\t", nTabDepth), stLogEntry));
	}

	public MutinyElement currentFolder()
	{
		if(this instanceof FolderElement)
			return this;

		if(mParent != null)
			return mParent.currentFolder();

		return null;
	}

	public String attribute(String stTagSpec)
	{
		Matcher matcher = mAttributePathPattern.matcher(stTagSpec);

		if(matcher.matches())
		{
			String
					stPathSpec = matcher.group(1),
					stAttribute = matcher.group(2);

			MutinyElement element = getElement(matcher.group(1));

			if(element != null)
				return element.attribute(matcher.group(2));
		}
		else
		{
			if(mAttributeMap != null && mAttributeMap.containsKey(stTagSpec))
				return mAttributeMap.get(stTagSpec);
		}
		return null;
	}

	public Map<String, String> attributes()
	{
		return mAttributeMap;
	}

	public ArrayList<MutinyElement> childrenByTag(String stTag)
	{
		ArrayList<MutinyElement> arrayList = null;

		if(mTypeMap != null)
			arrayList = mTypeMap.get(stTag);

		return arrayList;
	}

	public String elementPath()
	{
		String stName = attribute("name");

		MutinyElement parent = parent();

		if(parent != null)
			return String.format("%s/%s",parent.elementPath(),stName);

		return stName;
	}

	public MutinyElement getElement(String stPathSpec)
	{
		MutinyElement e2 = currentFolder();

		if( this instanceof FolderElement)
		{
			Matcher m1 = mPathTypePattern.matcher(stPathSpec);

			if(m1.matches())
			{
				String s2 = m1.group(2);

				switch(m1.group(1))
				{
					case "/" :
					{
						e2 = rootElement();
					}
					break;

					case "@/" :
					{
					}
					break;

				}
				return e2.getElement(s2);
			}
			else
			{
				Pattern pattern = Pattern.compile("([^/]+)/(.*)");

				Matcher m2 = pattern.matcher(stPathSpec);

				if(m2.matches())
				{
					// Still dealing with a path here...
					//
					if(mElementMap.containsKey(m2.group(1)))
						return mElementMap.get(m2.group(2)).getElement(m2.group(2));
				}
				else
				{
					if(mElementMap.containsKey(stPathSpec))
						return mElementMap.get(stPathSpec);
				}
				return null;
			}
		}
		return e2.getElement(stPathSpec);
	}

	public Requester requester()
	{
		if(mParent != null)
			return mParent.requester();

		return null;
	}

	public String invokeElement(String stArgs[])
	{

		return null;
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
		return getClass().getSimpleName();
	}

	public MutinyElement()
	{
	}

	public MutinyElement(TreeMap<String,String> attributeMap)
	{
		mAttributeMap = attributeMap;
	}
}
