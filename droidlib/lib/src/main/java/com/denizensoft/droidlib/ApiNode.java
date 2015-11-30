package com.denizensoft.droidlib;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.denizensoft.jlib.FatalException;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * Created by sjm on 11/22/15.
 */
public class ApiNode extends TargetNode implements ResultListener
{
	static public enum ReplyCode
	{
		CRITICAL_ERROR,
		COMMIT_PENDING,
		SUCCESS_REQUEST,
		WARNING_MESSAGE,
		WARNING_NOTFOUND,
		USER_CANCELLED
	}

	static final public int N_RC_ERROR				= -1;
	static final public int N_RC_OK					= 0;
	static final public int N_RC_WARNING			= 1;
	static final public int N_RC_WARNING_NOTFOUND	= 2;
	static final public int N_RC_USER_CANCELLED		= 3;

	private Requester mRequester = null;

	private String mInvokeMethod = null;

	private JSONObject mJsRequest = null, mJsReply = null;

	///////////////////////////////////////////////////////////////////////////////////////////
	//
	// These methods require override in instances or subclasses
	//
	@Override
	public boolean onActivityResultHook(int requestCode, int resultCode, Intent data)
	{
		return false;
	}

	public void invokeMethod(String stMethod, JSONObject jsRequest, JSONObject jsReply)
	{
		throw new FatalException("ApiNode: error! Undefined invocation handler!");
	}

	public void updateRequestNode(String stTag, String stToken, Bundle bundle)
	{
		throw new FatalException(String.format("ApiNode: error! Undefined update handler!"));
	}

	///////////////////////////////////////////////////////////////////////////////////////////
	//
	// public API
	//
	public void attachTo(Requester requester)
	{
		mRequester = requester;
	}

	public String invokeMethod()
	{
		return mInvokeMethod;
	}

	public JSONObject reply()
	{
		return mJsReply;
	}

	public void replyCommit(ReplyCode replyCode, String stReply) throws HandlerException
	{
		try
		{
			synchronized(mJsReply)
			{
				switch(replyCode)
				{
					case CRITICAL_ERROR:
					{
						Log.d("Requester","Committing reply with ERROR");

						mJsReply.put("$rc",N_RC_ERROR);

						if(stReply == null)
							stReply = "Unknown error!";
					}
					break;

					case SUCCESS_REQUEST:
					{
						Log.d("Requester","Committing reply with SUCCESS");

						mJsReply.put("$rc",N_RC_OK);

						if(stReply == null)
							stReply = "Unspecified SUCCESS!";
					}
					break;

					case WARNING_MESSAGE:
					{
						Log.d("Requester","Committing reply with WARNING");

						mJsReply.put("$rc",N_RC_WARNING);

						if(stReply == null)
							stReply = "Unspecified WARNING!";
					}
					break;

					case WARNING_NOTFOUND:
					{
						Log.d("Requester","Committing reply with NOTFOUND");

						mJsReply.put("$rc", N_RC_WARNING_NOTFOUND);

						if(stReply == null)
							stReply = "Unknown NOTFOUND warning!";
					}
					break;

					case USER_CANCELLED:
					{
						Log.d("Requester","Committing reply with USERCANCEL");

						mJsReply.put("$rc", N_RC_USER_CANCELLED);

						if(stReply == null)
							stReply = "Unknown,was cancelled by user!";
					}
					break;

				}

				if(!mJsReply.has("$reply"))
					mJsReply.put("$reply",stReply);

				JSONObject jso = mJsReply;

				mJsRequest = null;
				mJsReply = null;
				jso.notify();
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
		return mRequester;
	}

	final public void startRequest(String stMethod,JSONObject jsRequest,JSONObject jsReply) throws FatalException
	{
		mInvokeMethod = stMethod;
		mJsRequest = jsRequest;
		mJsReply = jsReply;

		invokeMethod(stMethod,jsRequest,jsReply);
	}

	public ApiNode(Object owner, String stClass)
	{
		super(owner,stClass);
	}

}
