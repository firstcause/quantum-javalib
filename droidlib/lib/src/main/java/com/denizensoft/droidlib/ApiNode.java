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
	static public enum ReplyCode
	{
		CRITICAL_ERROR,
		SUCCESS_REQUEST,
		WARNING_MESSAGE,
		WARNING_NOTFOUND,
		USER_CANCELLED
	}

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

	public void invokeMethod(String stMethod, JSONObject jsRequest, JSONObject jsReply)
	{
		throw new FatalException("ApiNode: error! Undefined invocation handler!");
	}

	public void updateRequestNode(String stTag, String stToken, Bundle bundle)
	{
		throw new FatalException("ApiNode: error! Undefined update handler!");
	}

	///////////////////////////////////////////////////////////////////////////////////////////
	//
	// public API
	//
	public void attachTo(Requester requester)
	{
		mRequester = requester;
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
