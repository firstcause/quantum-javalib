package com.denizensoft.droidlib;

import android.content.Intent;
import com.denizensoft.jlib.FatalException;
import com.denizensoft.jlib.LibException;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;


/**
 * Created by sjm on 11/22/15.
 */
public class ApiNode extends TargetNode implements ResultListener
{
	static public enum ReplyCode
	{
		CRITICAL_ERROR,
		SUCCESS_REQUEST,
		WARNING_MESSAGE,
		WARNING_NOTFOUND,
		USER_CANCELLED
	}

	HashMap<String,ApiMethod> mMethodMap = null;

	private Requester mRequester = null;

	///////////////////////////////////////////////////////////////////////////////////////////
	//
	// These methods require override in instances or subclasses
	//
	@Override
	public boolean onActivityResultHook(int requestCode, int resultCode, Intent data)
	{
		return false;
	}

	public void builtins(String stMethod, JSONObject jsRequest, JSONObject jsReply) throws JSONException, LibException
	{
		throw new HandlerException("ApiNode: undefined builtins handler function!");
	}

	final public void invokeMethod(String stMethod, JSONObject jsRequest, JSONObject jsReply)
	{
		try
		{
			if(mMethodMap != null && mMethodMap.containsKey(stMethod))
			{
				mMethodMap.get(stMethod).callback(this,jsRequest,jsReply);
			}
			else
			{
				builtins(stMethod,jsRequest,jsReply);
			}
		}
		catch(LibException e)
		{
			throw new FatalException(String.format("ApiNode:%s.%s error: %s",nodeTag(),stMethod,e.getMessage()));
		}
		catch(JSONException e)
		{
			throw new FatalException(String.format("ApiNode:%s.%s: JSON error: %s",nodeTag(),stMethod,e.getMessage()));
		}
	}

	///////////////////////////////////////////////////////////////////////////////////////////
	//
	// public API
	//
	public void attachTo(Requester requester)
	{
		mRequester = requester;
	}

	public void attachApiMethod(ApiMethod apiMethod)
	{
		if(mMethodMap == null)
			mMethodMap = new HashMap<>();

		mMethodMap.put(apiMethod.methodTag(),apiMethod);
	}

	public String method()
	{
		return mRequester.apiContext().mMethod;
	}

	public JSONObject reply()
	{
		return mRequester.apiContext().mJsReply;
	}

	public void replyCommit(ReplyCode replyCode, String stReply) throws HandlerException
	{
		mRequester.replyCommit(replyCode,stReply);
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
		return mRequester.apiContext().mJsRequest;
	}

	public Requester requester()
	{
		return mRequester;
	}

	public ApiNode(Object owner, String stClass)
	{
		super(owner,stClass);
	}

}
