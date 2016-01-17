package com.denizensoft.droidlib;

import android.os.Handler;
import android.util.Log;
import com.denizensoft.jlib.CriticalException;
import com.denizensoft.jlib.FatalException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Created by sjm on 12/26/15.
 */
public class ApiContext
{
	static final public int N_RC_ERROR				= -1;
	static final public int N_RC_OK					= 0;
	static final public int N_RC_WARNING			= 1;
	static final public int N_RC_WARNING_NOTFOUND	= 2;
	static final public int N_RC_USER_CANCELLED		= 3;

	static public enum ReplyCode
	{
		CRITICAL_ERROR,
		SUCCESS_REQUEST,
		WARNING_MESSAGE,
		WARNING_NOTFOUND,
		USER_CANCELLED
	}

	public final long mSequenceNo;

	protected int mRC = N_RC_OK, nRetries = 0;

	protected String mState = "new";

	protected ApiNode mApiNode = null;

	protected String mMethod = null;

	protected JSONObject mJsRequest = null, mJsReply = null;

	protected Handler mReplyTo = null;

	protected ApiCallback mResultHandler = null;

	public JSONArray args()
	{
		JSONArray args = null;

		if(mJsRequest.has("$args"))
		{
			try
			{
				args = mJsRequest.getJSONArray("$args");
			}
			catch(JSONException e)
			{
				throw new FatalException("ApiContext: request has no args!");
			}
		}
		return args;
	}

	public void invokeNodeMethod()
	{
		ApiMethod method = mApiNode.getMethod(mMethod);

		if(method != null)
		{
			try
			{
				method.func(this);
			}
			catch(JSONException e)
			{
				throw new FatalException(String.format("ApiContext:%s.%s: JSON error: %s",
						mApiNode.nodeTag(), mMethod, e.getMessage()));
			}
		}

	}

	protected ApiTask loadApiTask(String stTaskSpec)
	{
		Log.d("ApiContext", String.format("Looking for TASK: %s", stTaskSpec));

		try
		{
			Class specClass = Class.forName(stTaskSpec);

			Constructor constructor = specClass.getConstructor(Requester.class);

			Object obj = constructor.newInstance(requester());

			if(!ApiTask.class.isInstance(obj))
			{
				throw new CriticalException(String.format("Class: %s, is not an extension of API task...",stTaskSpec));
			}
			return(ApiTask)obj;
		}
		catch(ClassNotFoundException e)
		{
			throw new CriticalException(String.format("Class not found: %s",e.getMessage()));
		}
		catch(NoSuchMethodException e)
		{
			throw new CriticalException(String.format("Constructor not found: %s",e.getMessage()));
		}
		catch(IllegalAccessException e)
		{
			throw new CriticalException(String.format("Constructor not public? : %s",e.getMessage()));
		}
		catch(InstantiationException e)
		{
			throw new CriticalException(String.format("Constructor/Class not available? : %s",e.getMessage()));
		}
		catch(InvocationTargetException e)
		{
			throw new CriticalException(String.format("Invocation target error? : %s",e.getMessage()));
		}
	}

	public String method()
	{
		return mMethod;
	}

	public ApiNode node()
	{
		return mApiNode;
	}

	public JSONObject reply()
	{
		return mJsReply;
	}

	public void replyCommit(ReplyCode replyCode, String stReply) throws HandlerException
	{
		try
		{
			String s1 = "";

			synchronized(this)
			{
				switch(replyCode)
				{
					case CRITICAL_ERROR:
					{
						Log.d("Requester","Committing reply with ERROR");

						mRC = N_RC_ERROR;

						s1 = "Unknown error!";
					}
					break;

					case SUCCESS_REQUEST:
					{
						Log.d("Requester","Committing reply with SUCCESS");

						mRC = N_RC_OK;

						s1 = "Unspecified SUCCESS!";
					}
					break;

					case WARNING_MESSAGE:
					{
						Log.d("Requester","Committing reply with WARNING");

						mRC = N_RC_WARNING;

						s1 = "Unspecified WARNING!";
					}
					break;

					case WARNING_NOTFOUND:
					{
						Log.d("Requester","Committing reply with NOTFOUND");

						mRC = N_RC_WARNING_NOTFOUND;

						s1 = "Unknown NOTFOUND warning!";
					}
					break;

					case USER_CANCELLED:
					{
						Log.d("Requester","Committing reply with USERCANCEL");

						mRC = N_RC_USER_CANCELLED;

						s1 = "Unknown,was cancelled by user!";
					}
					break;

				}

				mJsReply.put("$rc",mRC);

				if(stReply == null)
					stReply = s1;

				if(!mJsReply.has("$reply"))
					mJsReply.put("$reply",stReply);

				mState = "done";

				requester().popApiContext();

				notify();
			};
		}
		catch(JSONException e)
		{
			throw new HandlerException(String.format("JSON exception: %s",e.getMessage()));
		}
	}

	public void replyCriticalError(String stReply)
	{
		replyCommit(ReplyCode.CRITICAL_ERROR,stReply);
	}

	public void replySuccessComplete(String stReply)
	{
		replyCommit(ReplyCode.SUCCESS_REQUEST,stReply);
	}

	public JSONObject request()
	{
		return mJsRequest;
	}

	public Requester requester()
	{
		return mApiNode.requester();
	}

	public ApiContext(Handler replyTo, ApiNode apiNode, String stMethod, JSONObject jsRequest, JSONObject jsReply,
					  ApiCallback resultHandler)
	{
		mSequenceNo = ++Requester.nRequestSequence;
		mReplyTo = replyTo;
		mApiNode = apiNode;
		mMethod = stMethod;
		mJsRequest = jsRequest;
		mJsReply = (jsReply != null ? jsReply : new JSONObject());
		mResultHandler = resultHandler;
	}
}
