package com.denizensoft.droidlib;

import java.util.TreeMap;

/**
 * Created by sjm on 10/24/15.
 */
public class StackMenuElement extends MutinyElement
{
	public void attachToParent(MutinyElement parentElement)
	{
		if(!( parentElement instanceof FolderElement))
		{
			throw new RuntimeException(String.format("Mutiny: Invalid parent class, for %s: %s",
					getClass().getSimpleName(),attribute("name")));
		}

		super.attachToParent(parentElement);
	}

	public StackMenuElement(TreeMap<String,String> attributeMap)
	{
		super(attributeMap);
	}
}
