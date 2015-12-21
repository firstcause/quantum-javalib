package com.denizensoft.droidlib;

import android.util.Log;
import com.denizensoft.jlib.FatalException;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class ApiInvoker extends ApiTask
{
	final protected JSONObject mJsParm;

	@Override
	final protected void invokeTask()
	{
		String stClassSpec = null;

		try
		{
			try
			{
				if(!mJsParm.has("$classpec"))
				{
					throw new HandlerException("ApiInvoker: missing $classspec...");
				}

				stClassSpec = mJsParm.getJSONArray("$classpec").getString(0);

				Log.d("ApiInvoker", String.format("Loading class: %s", stClassSpec ));

				Class mutinyClass = Class.forName(stClassSpec);

				Constructor constructor = mutinyClass.getConstructor(Requester.class);

				Object obj = constructor.newInstance(requester());

				Class apiNodeClass = Class.forName("ApiNode");

				if(apiNodeClass.isInstance(obj))
				{
					ApiNode apiNode = (ApiNode)obj;

					if(!requester().hasApi(apiNode.nodeTag()))
						requester().addApiNode((ApiNode)obj);
				}
			}
			catch(JSONException e)
			{
				throw new FatalException("ApiInvoker: JSON exception!");
			}
		}
		catch(InstantiationException e)
		{
			throw new FatalException(String.format("ApiInvoker: Couldn't instantiate class: %s",stClassSpec),e);
		}
		catch(InvocationTargetException e)
		{
			throw new FatalException(String.format("ApiInvoker: Couldn't invoke constructor: %s",stClassSpec),e);
		}
		catch(NoSuchMethodException e)
		{
			throw new FatalException(String.format("ApiInvoker: Constructor not found? : %s",stClassSpec),e);
		}
		catch(IllegalAccessException e)
		{
			throw new FatalException(String.format("ApiInvoker: Constructor not public? : %s",stClassSpec),e);
		}
		catch(ClassNotFoundException e)
		{
			throw new FatalException(String.format("ApiInvoker: Class not found? : %s",stClassSpec),e);
		}
	}

	public ApiInvoker(Requester requester, JSONObject jsParm)
	{
		super(requester);
		mJsParm = jsParm;
	}
}
