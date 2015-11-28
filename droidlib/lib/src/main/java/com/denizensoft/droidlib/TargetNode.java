package com.denizensoft.droidlib;

/**
 * Created by sjm on 11/22/15.
 */
abstract public class TargetNode
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

	public TargetNode(Object owner, String stClass)
	{
		mOwner = owner;
		nNodeTag = stClass;
	}
}

