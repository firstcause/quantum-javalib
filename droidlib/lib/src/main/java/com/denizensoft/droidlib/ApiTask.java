package com.denizensoft.droidlib;

/**
 * Created by sjm on 12/17/15.
 */
public abstract class ApiTask implements Runnable
{
	private long mOwnerThreadId = 0;

	final protected Requester mRequester;

	abstract protected void invokeTask();

	public long ownerThreadId()
	{
		return mOwnerThreadId;
	}

	@Override
	final public void run()
	{
		mOwnerThreadId = Thread.currentThread().getId();
		invokeTask();
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
