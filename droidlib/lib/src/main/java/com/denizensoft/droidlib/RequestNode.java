package com.denizensoft.droidlib;

import android.os.Bundle;
import com.denizensoft.jlib.FatalException;
import org.json.JSONObject;


/**
 * Created by sjm on 11/22/15.
 */
public class RequestNode extends TargetNode
{
	private Requester mRequester = null;

	private String mInvokeMethod = null;

	///////////////////////////////////////////////////////////////////////////////////////////
	//
	// These methods require override in instances or subclasses
	//
	public void invokeMethod(String stMethod, JSONObject jsRequest, JSONObject jsReply)
	{
		throw new FatalException("RequestNode: error! Undefined invocation handler!");
	}

	public void updateRequestNode(String stTag, String stToken, Bundle bundle)
	{
		throw new FatalException(String.format("RequestNode: error! Undefined update handler!"));
	}

	///////////////////////////////////////////////////////////////////////////////////////////
	//
	// public API
	//
	public void attachTo(Requester requester)
	{
		mRequester = requester;
	}

	public void replyCommit(Requester.ReplyCode replyCode, String stMessage)
	{
		mRequester.replyCommit(replyCode,stMessage);
	}

	public String invokeMethod()
	{
		return mInvokeMethod;
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

	public void replyCriticalError(String stReply)
	{
		replyCommit(Requester.ReplyCode.CRITICAL_ERROR,stReply);
	}

	public void replySuccessComplete(String stReply)
	{
		replyCommit(Requester.ReplyCode.SUCCESS_REQUEST,stReply);
	}

	final public void startRequest(String stMethod) throws FatalException
	{
		mInvokeMethod = stMethod;

		invokeMethod(stMethod, mRequester.pendingRequest(),mRequester.pendingReply());
	}

	public RequestNode(Object owner, String stClass)
	{
		super(owner,stClass);
	}
}
