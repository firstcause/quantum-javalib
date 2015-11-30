package com.denizensoft.droidlib;

public class ApiInvoker implements Runnable
{
	final private Requester mRequester;

	public void invokeMutiny()
	{
	}

	protected Requester requester()
	{
		return mRequester;
	}

	@Override
	public void run()
	{
		invokeMutiny();
	}

	public ApiInvoker(Requester requester)
	{
		mRequester=requester;
	}
}
