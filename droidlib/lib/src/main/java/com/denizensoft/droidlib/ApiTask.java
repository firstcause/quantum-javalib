package com.denizensoft.droidlib;

/**
 * Created by sjm on 12/17/15.
 */
public abstract class ApiTask implements Runnable
{
	private long mTaskThreadId = 0;

	final protected Requester mRequester;

	abstract protected void taskFunc();

	public long taskThreadId()
	{
		return mTaskThreadId;
	}

	@Override
	final public void run()
	{
		mTaskThreadId = Thread.currentThread().getId();
		taskFunc();
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
