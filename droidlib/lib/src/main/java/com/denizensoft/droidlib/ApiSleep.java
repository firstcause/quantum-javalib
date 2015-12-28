package com.denizensoft.droidlib;

import android.util.Log;
import org.json.JSONException;

/**
 * Created by sjm on 12/27/15.
 */
public class ApiSleep extends ApiTask
{
	@Override
	protected void taskFunc() throws JSONException
	{
		try
		{
			int nInterval = requester().apiContext().request().getInt("$interval");

			Log.d("ApiSleep",String.format("Sleep interval: %d",nInterval));

			Thread.sleep(nInterval,0);

			requester().apiContext().replySuccessComplete(null);
		}
		catch(JSONException e)
		{
			throw new HandlerException(String.format("ApiSleep: JSON exception: %s",e.getMessage()));
		}
		catch(InterruptedException e)
		{
			Log.d("ApiSleep", "thread sleep was interrupted!");
		}
	}

	public ApiSleep(Requester requester)
	{
		super(requester);
	}

}
