package com.denizensoft.droidlib;

import java.util.TreeMap;

/**
 * Created by sjm on 10/24/15.
 */
public class MenuItemElement extends MutinyElement
{
	@Override
	public void attachToParent(MutinyElement parentElement)
	{
		if(!( parentElement instanceof StackMenuElement))
		{
			throw new RuntimeException(String.format("Mutiny: Invalid parent class, for %s: %s",
					getClass().getSimpleName(),attribute("name")));
		}

		super.attachToParent(parentElement);
	}

	public MenuItemElement(TreeMap<String,String> attributeMap)
	{
		super(attributeMap);
	}
}
