package com.denizensoft.droidlib;

import org.json.JSONException;

public class ApiInvoker extends ApiTask
{
	@Override
	protected void taskFunc() throws JSONException
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
