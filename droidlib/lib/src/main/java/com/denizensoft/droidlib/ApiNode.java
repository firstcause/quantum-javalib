package com.denizensoft.droidlib;

import android.content.Intent;
import android.os.Bundle;
import com.denizensoft.jlib.FatalException;
import org.json.JSONObject;


/**
 * Created by sjm on 11/22/15.
 */
public class ApiNode extends TargetNode implements ResultListener
{
	private Requester mRequester = null;

	private String mInvokeMethod = null;

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
		return mRequester.pendingReply();
	}

	public void replyCommit(Requester.ReplyCode replyCode, String stMessage)
	{
		mRequester.replyCommit(replyCode,stMessage);
	}

	public void replyCriticalError(String stReply)
	{
		replyCommit(Requester.ReplyCode.CRITICAL_ERROR,stReply);
	}

	public void replySuccessComplete(String stReply)
	{
		replyCommit(Requester.ReplyCode.SUCCESS_REQUEST,stReply);
	}

	public JSONObject request()
	{
		return mRequester.pendingRequest();
	}

	public Requester requester()
	{
		return mRequester;
	}

	final public void startRequest(String stMethod) throws FatalException
	{
		mInvokeMethod = stMethod;

		invokeMethod(stMethod, mRequester.pendingRequest(),mRequester.pendingReply());
	}

	public ApiNode(Object owner, String stClass)
	{
		super(owner,stClass);
	}

}
