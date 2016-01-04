package com.denizensoft.droidlib;

import android.content.Intent;
import com.denizensoft.jlib.FatalException;
import com.denizensoft.jlib.LibException;

import java.util.HashMap;


/**
 * Created by sjm on 11/22/15.
 */
public class ApiNode extends TargetNode implements ResultListener
{
	HashMap<String,ApiMethod> mMethodMap = null;

	private Requester mRequester = null;

	///////////////////////////////////////////////////////////////////////////////////////////
	//
	// These methods require override in instances or subclasses
	//
	@Override
	public boolean onActivityResultHook(int requestCode, int resultCode, Intent data)
	{
		return false;
	}

	public ApiMethod getMethod(String stMethod)
	{
		ApiMethod apiMethod = null;

		try
		{
			if(mMethodMap != null && mMethodMap.containsKey(stMethod))
			{
				apiMethod = mMethodMap.get(stMethod);
			}
		}
		catch(LibException e)
		{
			throw new FatalException(String.format("ApiNode:%s.%s error: %s",nodeTag(),stMethod,e.getMessage()));
		}
		return apiMethod;
	}

	///////////////////////////////////////////////////////////////////////////////////////////
	//
	// public API
	//
	public void attachedTo(Requester requester,String stApiTag)
	{
		if(stApiTag != null)
			setTag(stApiTag);

		mRequester = requester;
	}

	public ApiNode attachApiMethod(String stMethodTag, ApiMethod apiMethod)
	{
		if(mMethodMap == null)
			mMethodMap = new HashMap<>();

		mMethodMap.put(stMethodTag,apiMethod);

		return this;
	}

	public Requester requester()
	{
		return mRequester;
	}

	public ApiNode()
	{
	}

	public ApiNode(Object owner)
	{
		super(owner);
	}

}
