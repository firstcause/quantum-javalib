package com.denizensoft.droidlib;

public class ApiInvoker
{
	final private Requester mRequester;

	public void invokeApi()
	{
	}

	protected Requester requester()
	{
		return mRequester;
	}

	public ApiInvoker(Requester requester)
	{
		mRequester=requester;
	}
}
