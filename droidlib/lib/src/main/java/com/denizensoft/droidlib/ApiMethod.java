package com.denizensoft.droidlib;

import com.denizensoft.jlib.LibException;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by sjm on 12/5/15.
 */
public class ApiMethod
{
	final private String mMethodTag;

	public void callback(ApiNode apiNode, JSONObject jsRequest, JSONObject jsReply)
			throws JSONException, LibException
	{
		throw new HandlerException("ApiMethod: undefined handler function!");
	}

	final public String methodTag()
	{
		return mMethodTag;
	}

	public ApiMethod(String stMethodTag)
	{
		this.mMethodTag = stMethodTag;
	}
}

