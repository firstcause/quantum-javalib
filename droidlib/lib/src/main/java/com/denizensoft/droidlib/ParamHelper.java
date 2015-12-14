package com.denizensoft.droidlib;

/**
 * Created by sjm on 12/13/15.
 */
abstract public class ParamHelper<T> implements Runnable
{
	protected T mParam = null;

	public T param()
	{
		return mParam;
	}

	ParamHelper(T param)
	{
		mParam = param;
	}
}
