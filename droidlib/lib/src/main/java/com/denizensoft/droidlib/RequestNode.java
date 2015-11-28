package com.denizensoft.droidlib;

import android.os.Bundle;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by sjm on 11/22/15.
 */
public class RequestNode extends TargetNode
{
	private Requester mRequester = null;

	///////////////////////////////////////////////////////////////////////////////////////////
	//
	// These methods require override in instances or subclasses
	//
	public void invokeMethod(String stMethod, JSONObject jsRequest, JSONObject jsReply) throws JSONException
	{
		throw new HandlerException("RequestNode: error! Undefined invocation handler!");
	}

	public void updateRequestNode(String stTag, String stToken, Bundle bundle)
	{
		throw new RuntimeException(String.format("RequestNode: error! Undefined update handler!"));
	}

	///////////////////////////////////////////////////////////////////////////////////////////
	//
	// public API
	//
	public void attachTo(Requester requester)
	{
		mRequester = requester;
	}

	public void commitReply(Requester.ReplyCode replyCode, String stMessage)
	{
		mRequester.commitReply(replyCode,stMessage);
	}

	public JSONObject reply()
	{
		return mRequester.pendingReply();
	}

	public Requester requester()
	{
		return mRequester;
	}

	public JSONObject request()
	{
		return mRequester.pendingRequest();
	}

	public void replyCriticalError(String stMessage)
	{
		commitReply(Requester.ReplyCode.CRITICAL_ERROR,stMessage);
	}

	public void replySuccessComplete(String stMessage)
	{
		commitReply(Requester.ReplyCode.SUCCESS_REQUEST,stMessage);
	}

	final public void startRequest(String stMethod)
	{
		try
		{
			invokeMethod(stMethod, mRequester.pendingRequest(),mRequester.pendingReply());
		}
		catch(JSONException e)
		{
			throw new HandlerException("RequestNode: JSON exception during invoke!");
		}
	}

	public RequestNode(Object owner, String stClass)
	{
		super(owner,stClass);
	}
}
