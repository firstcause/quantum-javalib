package com.denizensoft.droidlib;

/**
 * Created by sjm on 11/22/15.
 */
abstract public class RequesterNode
{
	private final Object mOwner;

	private final String mNodeName;

	///////////////////////////////////////////////////////////////////////////////////////////
	//
	// public API
	//
	public String nodeName()
	{
		return mNodeName;
	}

	public Object nodeOwner()
	{
		return mOwner;
	}

	public RequesterNode(Object owner, String stName)
	{
		mOwner = owner;
		mNodeName = stName;
	}
}

