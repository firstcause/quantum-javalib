package com.denizensoft.droidlib;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by sjm on 11/22/15.
 */
public class TargetNode
{
	private final String mNodeName;

	protected Requester mRequester = null;

	///////////////////////////////////////////////////////////////////////////////////////////
	//
	public void attachTo(Requester requester)
	{
		mRequester = requester;
	}

	public void invokeRequest(String stAction, JSONObject jsRequest, JSONObject jsReply) throws JSONException
	{
		throw new HandlerException("Undefined invocation handler!");
	}

	public void updatePendingReply(String stTag, String stValue)
	{
		try
		{
			mRequester.pendingReply().put(stTag,stValue);
		}
		catch(JSONException e)
		{
			throw new RuntimeException(String.format("JSON exception during update: %s",e.getMessage()));
		}
	}

	public void startPendingRequest(String stAction)
	{
		try
		{
			invokeRequest(stAction, mRequester.pendingRequest(),mRequester.pendingReply());
		}
		catch(JSONException e)
		{
			throw new HandlerException("TargetNode: JSON exception during invoke!");
		}
	}

	///////////////////////////////////////////////////////////////////////////////////////////
	//
	// public API
	//
	public String nodeName()
	{
		return mNodeName;
	}

	public Requester requester()
	{
		return mRequester;
	}

	public JSONObject reply()
	{
		return mRequester.pendingReply();
	}

	public JSONObject request()
	{
		return mRequester.pendingRequest();
	}

	public void commitReply(Requester.ReplyCode replyCode, String stMessage)
	{
		mRequester.commitReply(replyCode,stMessage);
	}

	public void replyCriticalError(String stMessage)
	{
		commitReply(Requester.ReplyCode.CRITICAL_ERROR,stMessage);
	}

	public void replySuccessComplete(String stMessage)
	{
		commitReply(Requester.ReplyCode.SUCCESS_REQUEST,stMessage);
	}

	public TargetNode(String stName)
	{
		mNodeName = stName;
	}
}

