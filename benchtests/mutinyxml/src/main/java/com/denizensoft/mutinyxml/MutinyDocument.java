package com.denizensoft.mutinyxml;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by sjm on 10/5/15.
 */
public class MutinyDocument extends MutinyElement
{
	MutinyXml.AppInterface mAppInterface = null;

	String mFileSpec;

	Pattern
			mMatchCommandPattern = Pattern.compile("([\\w\\d]+)\\((.*)\\);"),
			mMatchMutinyParms = Pattern.compile("(\\w+)\\:(.*)");

	protected void invokeSwitch(MutinyElement element)
	{
		if(!element.tag().equals("TokenSwitchDef"))
			throw new RuntimeException(String.format("Mutiny: state error, element is not a switch: %s",element.elementPath()),null);

	}

	protected void invokeCommand(String stElementSpec,String stCommand)
	{
		Matcher m1 = mMatchCommandPattern.matcher(stCommand);

		if(!m1.matches())
			throw new RuntimeException(String.format("Mutiny: malformed command? could not parse: %s",stCommand),null);

		System.out.printf("Action: %s Parameter: %s\n", m1.group(1), m1.group(2));

		if(m1.group(1).equals("mutiny"))
		{
			Matcher m2 = mMatchMutinyParms.matcher(m1.group(2));

			switch(m2.group(1))
			{
				case "switch" :
				{
					MutinyElement e1 = getElement(stElementSpec).getElement(m2.group(2));

					if(e1 != null)
					{
						invokeSwitch(e1);
					}
				}
				break;
			}

		}
	}

	public int exec(String args[])
	{
		String stEntryPoint = attribute("/main:action");

		Matcher m1 = mMatchCommandPattern.matcher(stEntryPoint);

		if(m1.matches())
		{
			System.out.printf("Action: %s Parameter: %s\n", m1.group(1), m1.group(2));

			if(m1.group(1).equals("mutiny"))
			{
				Matcher m2 = Pattern.compile("(\\w+)\\:(.*)").matcher(m1.group(2));

				switch(m2.group(1))
				{
					case "switch" :
					{
						MutinyElement e3 = getElement("/main").getElement(m2.group(2));

						if(e3 != null)
						{
							invokeSwitch(e3);
						}
					}
					break;
				}

			}
		}
		return 0;
	}

	public MutinyDocument(MutinyXml.AppInterface appInterface,String stFileSpec)
	{
		super();

		mAppInterface = appInterface;

		mFileSpec = stFileSpec;
	}
}
