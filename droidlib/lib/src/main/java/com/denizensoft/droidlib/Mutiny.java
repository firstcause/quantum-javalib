
package com.denizensoft.droidlib;

import android.os.Looper;
import android.util.Log;
import com.denizensoft.droidlib.*;
import com.denizensoft.jlib.FatalException;
import com.denizensoft.jlib.LibException;
import org.json.JSONException;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Logger;

public class Mutiny extends JsShell implements Runnable
{
	private Requester mMutinyTarget = null;

	private Context mJsContext = null;

	private Scriptable mScope = null;

	private final static Logger LOGGER = Logger.getLogger(Mutiny.class.getName());

	public void execJS(String stFileSpec) throws JSONException, LibException
	{
		InputStream inputStream = osApi().openInputStream(stFileSpec);

		InputStreamReader reader = new InputStreamReader(inputStream);

		int nIndex = stFileSpec.lastIndexOf('/');

		String stSourceName;

		if(nIndex >= 0)
			stSourceName = stFileSpec.substring(nIndex + 1);
		else
			stSourceName = stFileSpec;

		logDebug(String.format("execJS: file spec: %s", stFileSpec));

		try
		{
			mJsContext.evaluateReader(mScope, reader, stSourceName, 1, null);

			reader.close();

			osApi().closeInputStream(inputStream);
		}
		catch(IOException e)
		{
			logError(String.format("IOException while loading: %s", stFileSpec));
			logError(String.format("Message: %s", e.getMessage()));
			throw new FatalException(e.getMessage());
		}

		logDebug(String.format("execJS: '%s' loaded ok!", stFileSpec));
	}

	@Override
	public void run()
	{
		Log.d("Mutiny", String.format("%s: run start...", osApi().appName()));

		Looper.prepare();

		Looper.myLooper();

		mMutinyTarget = new Requester(this);

		mMutinyTarget.attachApiNode("Mutiny", new ApiNode(this)
			.attachApiMethod("exec", new ApiMethod()
			{
				@Override
				public void func(ApiContext ac) throws JSONException, LibException
				{
					if(!ac.request().has("$args"))
						throw new HandlerException("Mutiny: request has no args?!");

					String stLine = ac.request().getJSONArray("$args").getString(0);


					Object obj = mJsContext.evaluateString(mScope, stLine, osApi().appName(), 1, null);

					if(obj instanceof NativeJavaObject)
					{
						NativeJavaObject njo = (NativeJavaObject) obj;

						Object o2 = njo.unwrap();

						if(o2 instanceof String)
						{
							ac.replySuccessComplete((String) o2);
						}
						else
						{
							ac.replySuccessComplete(null);
						}
					}
					else if(obj instanceof Integer)
					{
						ac.replySuccessComplete(obj.toString());
					}
					else
					{
						ac.replySuccessComplete(null);
					}
				}
			})
			.attachApiMethod("execJS", new ApiMethod()
			{
				@Override
				public void func(ApiContext ac) throws JSONException, LibException
				{
					if(!ac.request().has("$args"))
						throw new HandlerException("Mutiny: request has no args?!");

					String stFileSpec = ac.request().getJSONArray("$args").getString(0);

					try
					{
						execJS(stFileSpec);
						ac.replySuccessComplete(null);
						logDebug(String.format("execJS: '%s' loaded ok!", stFileSpec));
					}
					catch(LibException e)
					{
						logError(String.format("IOException while loading: %s", stFileSpec));
						logError(String.format("Message: %s", e.getMessage()));
						ac.replyCommit(ApiContext.ReplyCode.CRITICAL_ERROR,e.getMessage());
					}
				}
			})
		);

		mJsContext = Context.enter();

		mJsContext.setOptimizationLevel(-1);

		mScope = mJsContext.initStandardObjects();

//		ScriptableObject.putProperty(mScope, "out", Context.javaToJS(System.out, mScope));
		ScriptableObject.putProperty(mScope, "Shell", Context.javaToJS(this, mScope));

//		mJsContext.evaluateString(mScope, "out.println('Howdy! From Mutiny!');", mAppName, 1, null);
		mJsContext.evaluateString(mScope, "Shell.logInfo('Mutiny jsShell: thread context start...');", osApi().appName(), 1, null);

		postNotification(String.format("%s:ready", osApi().appName()));

		Looper.loop();

		LOGGER.info(String.format("Mutiny: %s: exiting context thread...", osApi().appName()));
	}

	public Requester mutinyShell()
	{
		return mMutinyTarget;
	}

	public Mutiny(JsShell.OsApi osApi)
	{
		super(osApi);

		Log.d("Mutiny", "Instantiating....");
	}
}
