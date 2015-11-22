package com.denizensoft.droidlib;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class Requester extends Handler
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
		START_PENDING,
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

	private StateCode RequestState = StateCode.IDLE;

	abstract public class TargetNode
	{
		Requester mRequester = null;

		void attachTo(Requester requester)
		{
		}

		public TargetNode()
		{
		}
	}

	private TargetNode mCurrentNode = null;

	private HashMap<String,TargetNode> mNodeMap = new HashMap<String,TargetNode>();

	private Object mRequestMutex = new Object();

	private JSONObject mPendingRequest = null, mPendingReply = null;

	public void addTargetNode(String stTag, TargetNode targetNode)
	{
		mNodeMap.put(stTag,targetNode);
	}

	///////////////////////////////////////////////////////////////////////////////////////////////
	//
	//
	//

	public void cleanupRequestHook()
	{
	}

	public void commandHook(int nCommand, Bundle args)
	{
	}

	public void tokenHook(String stToken, Bundle args)
	{
	}

	public void jsonRequestHook(JSONObject jsRequest, final JSONObject jsReply)
	{
	}

	public void notificationHook(int nNotify, Bundle args)
	{
	}

	public boolean otherMessageHook(Message msg)
	{
		return false;
	}

	//
	//
	//
	///////////////////////////////////////////////////////////////////////////////////////////////

	public boolean requestStartPending()
	{
		return( RequestState == StateCode.START_PENDING );
	}

	public boolean replyPending()
	{
		return( RequestState == StateCode.REPLY_PENDING );
	}

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
				cleanupRequestHook();
			};
		}
		catch(JSONException e)
		{
			throw new HandlerException(String.format("JSON exception: %s",e.getMessage()));
		}
	}

	@Override
	public void handleMessage(Message msg)
	{
		switch(msg.what)
		{
			case N_MSG_COMMAND:
			{
				commandHook(msg.arg1,msg.getData());
			}
			break;

			case N_MSG_REQUEST:
			{
				try
				{
					JSONObject jsRequest = new JSONObject(msg.getData().getString("$request"));

					String stAction = jsRequest.getString("$action");

					if(!mNodeMap.containsKey(stAction))
						throw new HandlerException(String.format("Undefined action token: %s",stAction));

					TargetNode targetNode = mNodeMap.get(stAction);

					mPendingReply = (JSONObject)msg.obj;

					synchronized(mPendingReply)
					{
						RequestState = StateCode.START_PENDING;

						// Don't monkey with this! As the request wait state can be set elsewhere!
						// while the handler is in progress!
						//
						jsonRequestHook(jsRequest, mPendingReply);

						if(RequestState == StateCode.REPLY_PENDING)
							Log.d("Requester", "Reply is still pending after start hook...");
					}
				}
				catch(JSONException e)
				{
					throw new HandlerException(String.format("JSON exception: %s",e.getMessage()));
				}
			}
			break;

			case N_MSG_NOTIFY:
			{
				notificationHook(msg.arg1,msg.getData());
			}
			break;

			case N_MSG_TOKEN:
			{
				String stToken = msg.getData().getString("_msg_token");

				if(stToken != null)
					tokenHook(stToken,msg.getData());
			}
			break;

			default:
			{
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

	public JSONObject sendRequest(JSONObject jsRequest)
	{
		return sendRequest(jsRequest.toString());
	}

	public JSONObject sendRequest(String stJSON)
	{
		JSONObject jsReply = new JSONObject();

		if(this.getLooper().getThread().getId() == Thread.currentThread().getId())
		{
			try
			{
				JSONObject jsRequest = new JSONObject(stJSON);

				jsonRequestHook(jsRequest, jsReply);
			}
			catch(JSONException e)
			{
				throw new HandlerException(String.format("JSON exception: %s",e.getMessage()));
			}
			return jsReply;
		}

		Message msg = obtainMessage();

		msg.what = N_MSG_REQUEST;
		msg.setData(new Bundle());
		msg.getData().putString("$request",stJSON);
		msg.obj = jsReply;

		if(this.getLooper().getThread().getId() == Thread.currentThread().getId())
			throw new RuntimeException("Requester: Requesting thread id matches the handler looper id!");

		synchronized(mRequestMutex)
		{
			RequestState = StateCode.REPLY_PENDING;

			synchronized(jsReply)
			{
				sendMessage(msg);

				Log.d("Requester: " + Thread.currentThread().getId(), "Request posted, starting wait....");

				while(RequestState == StateCode.REPLY_PENDING)
				{
					try
					{
						jsReply.wait();

						Log.d("Requester: "+Thread.currentThread().getId(),"Request wait completed....");
					}
					catch(InterruptedException e)
					{
						Log.d("Requester: "+Thread.currentThread().getId(),"Request wait, interrupted....");
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

		Log.d("Requester", "jsJsonRequest: Sending: " + stJSON);

		JSONObject jsReply = sendRequest(stJSON);

		if(jsReply != null)
		{
			stReply = jsReply.toString();

			Log.d("Requester", "jsJsonRequest: Request complete, reply: "+stReply);
		}
		else
		{
			Log.d("Requester", "jsJsonRequest: Request cancelled!");
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

	public Requester()
	{
	}

	public Requester(Looper looper)
	{
		super(looper);
	}
}
