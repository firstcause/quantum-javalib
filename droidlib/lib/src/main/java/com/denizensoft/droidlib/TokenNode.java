package com.denizensoft.droidlib;

import android.os.Bundle;

import java.util.regex.Pattern;

/**
 * Created by sjm on 11/23/15.
 */
public class TokenNode
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

	public TokenNode()
	{
		this.rxCategorySpec = Pattern.compile(".*");
	}

	public TokenNode(Pattern rxCategorySpec)
	{
		this.rxCategorySpec = rxCategorySpec;
	}

	public TokenNode(String stCategorySpec)
	{
		rxCategorySpec = Pattern.compile(stCategorySpec);
	}
}
