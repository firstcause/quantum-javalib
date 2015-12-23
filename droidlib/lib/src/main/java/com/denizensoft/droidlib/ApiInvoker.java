package com.denizensoft.droidlib;

public class ApiInvoker extends ApiTask
{
	@Override
	protected void invokeTask()
	{
		// complete the request...
		//
		requester().apiNode().replySuccessComplete(null);
	}

	public ApiInvoker(Requester requester)
	{
		super(requester);
	}
}
