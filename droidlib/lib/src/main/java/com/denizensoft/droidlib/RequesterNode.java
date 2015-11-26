package com.denizensoft.droidlib;

/**
 * Created by sjm on 11/22/15.
 */
abstract public class RequesterNode
{
	private final Object mOwner;

	private final String nNodeTag;

	///////////////////////////////////////////////////////////////////////////////////////////
	//
	// public API
	//
	public String nodeTag()
	{
		return nNodeTag;
	}

	public Object nodeOwner()
	{
		return mOwner;
	}

	public RequesterNode(Object owner, String stClass)
	{
		mOwner = owner;
		nNodeTag = stClass;
	}
}

