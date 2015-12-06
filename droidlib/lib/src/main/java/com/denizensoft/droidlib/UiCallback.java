package com.denizensoft.droidlib;

import android.util.Log;
import org.json.JSONObject;

/**
 * Created by sjm on 12/4/15.
 */
public class UiCallback implements UiEventListener
{
	@Override
	public void uiEvent(JSONObject jsEvent)
	{
		Log.d("UiCallback", "Handler not defined!");
	}
}
