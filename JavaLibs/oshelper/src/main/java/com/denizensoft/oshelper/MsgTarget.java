package com.denizensoft.oshelper;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class MsgTarget extends Handler
{
	static final public int N_APP_MESSAGE			= 0xDEADBEEF;
	static final public int N_MSG_COMMAND 			= ( N_APP_MESSAGE + 0 );
	static final public int N_MSG_REQUEST 			= ( N_APP_MESSAGE + 1 );
	static final public int N_MSG_NOTIFY 			= ( N_APP_MESSAGE + 2 );
	static final public int N_MSG_TOKEN 			= ( N_APP_MESSAGE + 3 );

	static final public int N_COMMAND = ( 1024 );
	static final public int N_COMMAND_TRACK			= ( N_COMMAND + 1 );
	static final public int N_COMMAND_DISMISS		= ( N_COMMAND + 2 );
	static final public int N_COMMAND_QUIT			= ( N_COMMAND + 2 );

	static final public int N_COMMAND_USER 			= ( N_COMMAND + 256 );

	static final public int N_NOTIFY				= ( N_COMMAND + 1024 );

	static final public int N_NOTIFY_USER			= (N_NOTIFY + 256 );

	static final public int N_RC_ERROR				= -1;
	static final public int N_RC_OK					= 0;
	static final public int N_RC_WARNING			= 1;
	static final public int N_RC_WARNING_NOTFOUND	= 2;
	static final public int N_RC_USER_CANCELLED		= 3;

	static private enum StateCode
	{
		IDLE,
		REPLY_PENDING,
		REPLY_READY
	}

	static public enum ReplyCode
	{
		CRITICAL_ERROR,
		SUCCESS_REQUEST,
		WARNING_MESSAGE,
		WARNING_NOTFOUND,
		USER_CANCELLED
	}

	public interface HookInterface
	{
		public void cleanupRequestHook();

		public void commandHook(int nCommand, Bundle args);

		public void tokenHook(String stToken, Bundle args);

		public void fatalRequestErrorHook(String stFatalError);

		public void invokeRequestHook(JSONObject jsonRequest, final JSONObject jsonReply);

		MsgTarget messageTarget();

		public void notificationHook(int nNotify, Bundle args);

		public boolean otherMessageHook(Message msg);

	};

	private StateCode RequestState = StateCode.IDLE;

	private Object mRequestMutex = new Object();

	private JSONObject mPendingReply = null;

	protected HookInterface mHookInterface = null;

	public void sendReply(ReplyCode replyCode, String stMessage)
	{
		if(mPendingReply == null)
			return;

		if(RequestState != StateCode.REPLY_PENDING)
			return;

		try
		{
			synchronized(mPendingReply)
			{
				switch(replyCode)
				{
					case CRITICAL_ERROR:
					{
						mPendingReply.put("$rc",N_RC_ERROR);

						if(stMessage == null)
							stMessage = "Operation caused a general error!";

						if(!mPendingReply.has("$error"))
							mPendingReply.put("$error",stMessage);
					}
					break;

					case SUCCESS_REQUEST:
					{
						mPendingReply.put("$rc",N_RC_OK);

						if(stMessage != null && !mPendingReply.has("$success"))
							mPendingReply.put("$success",stMessage);
					}
					break;

					case WARNING_MESSAGE:
					{
						mPendingReply.put("$rc",N_RC_WARNING);

						if(stMessage != null  && !mPendingReply.has("$warning"))
							mPendingReply.put("$warning",stMessage);
					}
					break;

					case WARNING_NOTFOUND:
					{
						mPendingReply.put("$rc", N_RC_WARNING_NOTFOUND);

						if(stMessage == null)
							stMessage = "Operation caused a NOTFOUND warning!";

						if(!mPendingReply.has("$warning"))
							mPendingReply.put("$warning",stMessage);
					}
					break;

					case USER_CANCELLED:
					{
						mPendingReply.put("$rc", N_RC_USER_CANCELLED);
					}
					break;

				}

				RequestState = StateCode.REPLY_READY;
				mPendingReply.notifyAll();
				mPendingReply = null;
				mHookInterface.cleanupRequestHook();
			};
		}
		catch(JSONException e)
		{
			mHookInterface.fatalRequestErrorHook(String.format("JSON exception: %s",e.getMessage()));
		}
	}

	@Override
	public void handleMessage(Message msg)
	{
		switch(msg.what)
		{
			case N_MSG_COMMAND:
			{
				if(mHookInterface != null)
					mHookInterface.commandHook(msg.arg1,msg.getData());
			}
			break;

			case N_MSG_REQUEST:
			{
				try
				{
					mPendingReply = (JSONObject)msg.obj;

					synchronized(mPendingReply)
					{
						RequestState = StateCode.REPLY_PENDING;

						// Don't monkey with this! As the request wait state can be set elsewhere!
						// while the handler is in progress!
						//
						if(mHookInterface == null)
						{
							mPendingReply.put("$rc",N_RC_ERROR);
							mPendingReply.put("$error","No handler was found for the operation!");

							RequestState = StateCode.REPLY_READY;
							mPendingReply.notifyAll();
							mPendingReply = null;
						}
						else
						{
							JSONObject jsonRequest = new JSONObject(msg.getData().getString("$request"));

							mHookInterface.invokeRequestHook(jsonRequest, mPendingReply);

							if(RequestState == StateCode.REPLY_PENDING)
								Log.d("MsgTarget", "Reply is still pending after start hook...");
						}
					}
				}
				catch(JSONException e)
				{
					mHookInterface.fatalRequestErrorHook(String.format("JSON Exception: %s",e.getMessage()));
					return;
				}
			}
			break;

			case N_MSG_NOTIFY:
			{
				if(mHookInterface != null)
					mHookInterface.notificationHook(msg.arg1,msg.getData());
			}
			break;

			case N_MSG_TOKEN:
			{
				if(mHookInterface != null)
				{
					String stToken = msg.getData().getString("_msg_token");

					if(stToken != null)
						mHookInterface.tokenHook(stToken,msg.getData());
				}
			}
			break;

			default:
			{
				if(!mHookInterface.otherMessageHook(msg))
					super.handleMessage(msg);
			}
			break;

		}
	}

	public void sendCommand(int nCommand,Bundle args)
	{
		Message msg = obtainMessage();

		msg.what = N_MSG_COMMAND;
		msg.arg1 = nCommand;

		msg.setData(args);

		sendMessage(msg);
	}

	public void sendNotify(int nNotify,Bundle args)
	{
		Message msg = obtainMessage();

		msg.what = N_MSG_NOTIFY;
		msg.arg1 = nNotify;

		msg.setData(args);

		sendMessage(msg);
	}

	public JSONObject sendRequest(JSONObject jsonRequest)
	{
		return sendRequest(jsonRequest.toString());
	}

	public JSONObject sendRequest(String stJSON)
	{
		Message msg = obtainMessage();

		JSONObject jsReply = new JSONObject();

		msg.what = N_MSG_REQUEST;
		msg.setData(new Bundle());
		msg.getData().putString("$request",stJSON);
		msg.obj = jsReply;

		if(this.getLooper().getThread().getId() == Thread.currentThread().getId())
			mHookInterface.fatalRequestErrorHook("MsgTarget: Requesting thread id matches the handler looper id!");

		synchronized(mRequestMutex)
		{
			RequestState = StateCode.REPLY_PENDING;

			synchronized(jsReply)
			{
				sendMessage(msg);

				Log.d("MsgTarget: " + Thread.currentThread().getId(), "Request posted, starting wait....");

				while(RequestState == StateCode.REPLY_PENDING)
				{
					try
					{
						jsReply.wait();

						Log.d("MsgTarget: "+Thread.currentThread().getId(),"Request wait completed....");
					}
					catch(InterruptedException e)
					{
						Log.d("MsgTarget: "+Thread.currentThread().getId(),"Request wait, interrupted....");
					}
				}
			}

			RequestState = StateCode.IDLE;
		}
		return jsReply;
	}

	public String jsJsonRequest(String stJSON)
	{
		String stReply = null;

		Log.d("MsgTarget", "jsJsonRequest: Sending: " + stJSON);

		JSONObject jsReply = sendRequest(stJSON);

		if(jsReply != null)
		{
			stReply = jsReply.toString();

			Log.d("MsgTarget", "jsJsonRequest: Request complete, reply: "+stReply);
		}
		else
		{
			Log.d("MsgTarget", "jsJsonRequest: Request cancelled!");
		}
		return stReply;
	}

	public void sendToken(String stToken,Bundle args)
	{
		Message msg = obtainMessage();

		msg.what = N_MSG_TOKEN;
		msg.arg1 = 0;
		
		if(args == null)
			args = new Bundle();

		args.putString("_msg_token",stToken);

		msg.setData(args);

		sendMessage(msg);
	}

	public void setTargetInterface(HookInterface hookInterface)
	{
		mHookInterface = hookInterface;
	}

	public MsgTarget()
	{
	}

	public MsgTarget(Looper looper)
	{
		super(looper);
	}

	public MsgTarget(HookInterface hookInterface)
	{
		mHookInterface = hookInterface;
	}
}
