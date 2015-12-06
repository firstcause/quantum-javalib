package com.denizensoft.droidlib;

public class ApiInvoker
{
	final private Requester mRequester;

	public String initAPI()
	{
		return null;
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
