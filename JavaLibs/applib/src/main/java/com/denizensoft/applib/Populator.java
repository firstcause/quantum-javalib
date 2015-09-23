package com.denizensoft.applib;

/**
 * Created by sjm on 3/27/2015.
 */
public abstract class Populator implements Runnable
{
	private String mParameter = null;

	private AppFragment mAppFragment = null;

	protected AppFragment fragment()
	{
		return mAppFragment;
	}

	protected String parameterString()
	{
		return mParameter;
	}

	public Populator(String stParameter)
	{
		mParameter = stParameter;
	}

	public Populator(AppFragment appFragment)
	{
		mAppFragment = appFragment;
	}

	public Populator(AppFragment appFragment,String stParameter)
	{
		mParameter = stParameter;
		mAppFragment = appFragment;
	}
}
