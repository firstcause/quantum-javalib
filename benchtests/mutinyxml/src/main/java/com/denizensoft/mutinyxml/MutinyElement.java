package com.denizensoft.mutinyxml;

import org.apache.commons.lang3.StringUtils;
import org.xmlpull.v1.XmlPullParser;

import java.io.Reader;
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
	int nNonameSequence = 0;

	protected String mTag = null, mText = "", mCDATA = "";

	protected MutinyElement mParent = null;

	protected TreeMap<String, String> mAttributeMap = null;

	protected TreeMap<String,MutinyElement> mElementMap = null;

	protected TreeMap<String,ArrayList<MutinyElement>> mTagMap = null;

	protected Logger LOGGER = Logger.getLogger(MutinyElement.class.getName());

	protected Pattern
			mAttributePathPattern = Pattern.compile("^([^:]+)\\:(\\w+)$"),
			mMatchCommandPattern = Pattern.compile("([\\w\\d]+)\\((.*)\\);"),
			mMatchMutinyParms = Pattern.compile("(\\w+)\\:(.*)");

	public interface Requester
	{
		public Reader openFileReader(String stFileSpec);

		public String invokeRequest(MutinyElement element,String stRequest);
	}

	protected void addAttribute(String stTag,String stValue)
	{
		if(mAttributeMap == null)
			mAttributeMap = new TreeMap<String,String>();

		mAttributeMap.put(stTag, stValue);
	}

	protected void addElement(MutinyElement element)
	{
		String stName = element.attribute("name");

		if(stName == null)
		{
			stName = String.format("__noname_%04X", nNonameSequence++);
			element.addAttribute("name", stName);
		}

		if(mTagMap == null)
			mTagMap = new TreeMap<String,ArrayList<MutinyElement>>();

		ArrayList<MutinyElement> tagArray = mTagMap.get(element.tag());

		if(tagArray == null)
		{
			tagArray = new ArrayList<MutinyElement>();

			mTagMap.put(element.tag(), tagArray);
		}

		if(mElementMap == null)
			mElementMap = new TreeMap<>();

		if(mElementMap.containsKey(stName))
			throw new RuntimeException(String.format("Mutiny: element name collision at: %s/%s",elementPath(),stName),null);

		tagArray.add(element);

		mElementMap.put(stName,element);
	}

	protected void invokeSwitch(Requester requester)
	{
		if(!mTag.equals("TokenSwitchDef"))
			throw new RuntimeException(String.format("Mutiny: state error, this element is not a switch: %s",elementPath()),null);

		String stToken = invokeCommand(requester,attribute("token"));

	}

	protected String invokeCommand(Requester requester,String stCommand)
	{
		String s1 = null;

		Matcher m1 = mMatchCommandPattern.matcher(stCommand);

		if(!m1.matches())
			throw new RuntimeException(String.format("Mutiny: malformed command? could not parse: %s",stCommand),null);

		System.out.printf("Action: %s Parameter: %s\n", m1.group(1), m1.group(2));

		switch(m1.group(1))
		{
			case "mutiny" :
			{
				Matcher m2 = mMatchMutinyParms.matcher(m1.group(2));

				if(!m2.matches())
					throw new RuntimeException(String.format("Mutiny: action parameters not matched in: %s",m1.group(2)));

				switch(m2.group(1))
				{
					case "switch" :
					{
						MutinyElement e1 = getElement(m2.group(2));

						if(e1 != null)
							e1.invokeSwitch(requester);
					}
					break;

					case "show_menu" :
					{
						MutinyElement e1 = getElement(m2.group(2));

						if(e1 != null)
							requester.invokeRequest(e1,"show_menu");
					}
					break;
				}
			}
			break;

			case "request" :
			{
				s1 = requester.invokeRequest(this,m1.group(2));
			}
			break;


		}
		return s1;
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

		if(mTagMap != null)
			arrayList = mTagMap.get(stTag);

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
		if(stPathSpec.startsWith("/") == true)
		{
			if(stPathSpec.equals("/"))
				return rootElement();

			return rootElement().getElement(stPathSpec.substring(1));
		}
		else
		{
			Pattern pattern = Pattern.compile("([^/]+)/(.*)");

			Matcher matcher = pattern.matcher(stPathSpec);

			if(matcher.matches())
			{
				// Still dealing with a path here...
				//
				if(mElementMap.containsKey(matcher.group(1)))
					return mElementMap.get(matcher.group(2)).getElement(matcher.group(2));
			}
			else
			{
				if(mElementMap.containsKey(stPathSpec))
					return mElementMap.get(stPathSpec);
			}
		}
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
