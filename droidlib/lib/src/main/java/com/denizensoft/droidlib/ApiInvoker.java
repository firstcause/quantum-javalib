package com.denizensoft.droidlib;

public class ApiInvoker extends ApiTask
{
	@Override
	protected void taskFunc()
	{
		// complete the request...
		//
		requester().apiContext().replySuccessComplete(null);
	}

	public ApiInvoker(Requester requester)
	{
		super(requester);
	}
}
