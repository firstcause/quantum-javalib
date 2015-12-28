package com.denizensoft.droidlib;

import android.util.Log;
import org.json.JSONException;

/**
 * Created by sjm on 12/17/15.
 */
public abstract class ApiTask implements Runnable
{
	private long mTaskThreadId = 0;

	final protected Requester mRequester;

	abstract protected void taskFunc() throws JSONException;

	public long taskThreadId()
	{
		return mTaskThreadId;
	}

	@Override
	final public void run()
	{
		mTaskThreadId = Thread.currentThread().getId();

		Log.d("ApiTask",String.format("invoked on thread: %s",Thread.currentThread().getName()));

		try
		{
			taskFunc();
		}
		catch(JSONException e)
		{
			throw new HandlerException(String.format("ApiTask: JSON exception: %s",e.getMessage()));
		}
	}

	protected Requester requester()
	{
		return mRequester;
	}

	public ApiTask(Requester requester)
	{
		mRequester=requester;
	}
}
