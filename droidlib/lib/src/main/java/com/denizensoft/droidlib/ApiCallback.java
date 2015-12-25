package com.denizensoft.droidlib;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by sjm on 12/9/15.
 */
public interface ApiCallback
{
	void fnCallback(int nRC, String stReply, JSONObject jsReply) throws JSONException;
}
