package com.denizensoft.droidlib;

import android.os.Bundle;

import java.util.regex.Pattern;

/**
 * Created by sjm on 11/23/15.
 */
public class TokenNode extends TargetNode
{
	private Pattern rxCategorySpec = null;

	public void tokenHandler(String stToken, Bundle args)
	{
		// do nothing...
	}

	public Pattern tokenSpecification()
	{
		return rxCategorySpec;
	}

	public TokenNode(Object owner)
	{
		super(owner);
		this.rxCategorySpec = Pattern.compile("((.*))");
	}

	public TokenNode(Object owner,Pattern rxCategorySpec)
	{
		super(owner);
		this.rxCategorySpec = rxCategorySpec;
	}

	public TokenNode(Object owner,String stCategorySpec)
	{
		super(owner);
		rxCategorySpec = Pattern.compile(stCategorySpec);
	}
}
