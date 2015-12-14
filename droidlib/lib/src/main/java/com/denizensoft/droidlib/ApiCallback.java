package com.denizensoft.droidlib;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by sjm on 12/9/15.
 */
abstract public class ApiCallback implements ApiResultHandler
{
	@Override
	abstract public void fnCallback(int nRC, String stReply, JSONObject jsReply) throws JSONException;
}
