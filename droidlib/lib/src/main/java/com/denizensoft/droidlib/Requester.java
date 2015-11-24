package com.denizensoft.droidlib;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

	static public enum StateCode
	{
		REQUESTER_IDLE,
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

	private StateCode mRequestState = StateCode.REQUESTER_IDLE;

	private TargetNode mCurrentNode = null;

	private HashMap<String,TargetNode> mTargetMap = new HashMap<String,TargetNode>();

	private ArrayList<TokenNode> mTokenNodeList = new ArrayList<>();

	private final Object mRequestMutex = new Object();

	private JSONObject mPendingRequest = null, mPendingReply = null;

	private Pattern stNodePattern = Pattern.compile("([\\w\\-\\_]+)\\.?([\\w\\-\\_]?)");

	public void addTargetNode(TargetNode targetNode)
	{
		mTargetMap.put(targetNode.nodeName(),targetNode);
		targetNode.attachTo(this);
	}

	public void addTokenNode(TokenNode tokenNode)
	{
		mTokenNodeList.add(tokenNode);
	}

	public JSONObject pendingRequest()
	{
		return mPendingRequest;
	}

	public JSONObject pendingReply()
	{
		return mPendingReply;
	}

	public StateCode updateState(StateCode stateCode)
	{
		return mRequestState = stateCode;
	}

	///////////////////////////////////////////////////////////////////////////////////////////////
	//
	//
	public boolean replyPending()
	{
		return( mRequestState == StateCode.REPLY_PENDING );
	}

	public void commitReply(ReplyCode replyCode, String stMessage)
	{
		if(mRequestState != StateCode.REPLY_PENDING)
			throw new HandlerException("Invalid state, cannot send reply!");

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

				mRequestState = StateCode.REPLY_READY;
				mPendingReply.notifyAll();
				mPendingReply = null;
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
			case N_MSG_REQUEST:
			{
				// Remember, there is only one looper thread, it handles the request invocations,
				// so there is no point trying to invoke multiple requests simultaneously, as that could
				// cause a deadlock on the request mutex, and even if the mutex were moved to the TargetNode.
				// Yet, it is still possible thatany number of client threads may queue requests, but only
				// ONE TargetNode can be active at any given time anyways...
				//
				// Sorry...the facts of life...so suck it up!
				//
				try
				{
					JSONObject jsRequest = new JSONObject(msg.getData().getString("$request"));

					String
							stNodeSpec = jsRequest.getString("$nodespec"),
							stNodeName, stAction = null;

					Matcher matcher = stNodePattern.matcher(stNodeSpec);

					if(matcher.matches())
					{
						stNodeName = matcher.group(1);
						stAction = matcher.group(2);
					}
					else
					{
						stNodeName = stNodeSpec;
					}

					if(!mTargetMap.containsKey(stNodeName))
						throw new HandlerException(String.format("Undefined action token: %s",stNodeName));

					mCurrentNode = mTargetMap.get(stNodeName);

					mPendingReply = (JSONObject)msg.obj;

					// This here is how we release the current waiting thread
					//
					synchronized(mPendingReply)
					{
						// Don't monkey with this! As the request wait state can be set elsewhere!
						// while the handler is in progress!
						//
						mCurrentNode.startPendingRequest(stAction);

						if(mRequestState == StateCode.REPLY_PENDING)
							Log.d("Requester", "Reply is still pending after invoke...");
					}
				}
				catch(JSONException e)
				{
					throw new HandlerException(String.format("JSON exception: %s",e.getMessage()));
				}
			}
			break;

			case N_MSG_TOKEN:
			{
				String stToken = msg.getData().getString("$token");

				if(stToken != null)
				{
					for(TokenNode node: mTokenNodeList)
					{
						Matcher matcher = node.tokenSpecification().matcher(stToken);

						if(matcher.matches())
							node.tokenHandler(matcher.group(1),msg.getData());
					}
				}
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
			if(mRequestState == StateCode.REPLY_PENDING)
				throw new HandlerException("Invalid requester state! A reply is pending!");

			// Another thread may also want to send a request...
			// so taking turns is Strictly Enforced!!
			//
			synchronized(mRequestMutex)
			{
				try
				{
					JSONObject jsRequest = new JSONObject(stJSON);

					String
							stNodeSpec = jsRequest.getString("$nodespec"),
							stNodeName, stAction = null;

					Matcher matcher = stNodePattern.matcher(stNodeSpec);

					if(matcher.matches())
					{
						stNodeName = matcher.group(1);
						stAction = matcher.group(2).substring(1);
					}
					else
					{
						stNodeName = stNodeSpec;
					}

					if(!mTargetMap.containsKey(stNodeName))
						throw new HandlerException(String.format("Undefined action token: %s",stNodeName));

					mCurrentNode = mTargetMap.get(stNodeName);

					mPendingRequest = jsRequest;

					mPendingReply = jsReply;

					mCurrentNode.startPendingRequest(stAction);

					if(mRequestState == StateCode.REPLY_PENDING)
						Log.d("Requester", "Warning: Reply pending for current thread...");
				}
				catch(JSONException e)
				{
					throw new HandlerException(String.format("JSON exception: %s",e.getMessage()));
				}
			}
			return jsReply;
		}

		Message msg = obtainMessage();

		msg.what = N_MSG_REQUEST;
		msg.setData(new Bundle());
		msg.getData().putString("$request",stJSON);
		msg.obj = jsReply;

		// Only one requesting thread can lock this at a given time...
		//
		synchronized(mRequestMutex)
		{
			if(mRequestState == StateCode.REPLY_PENDING)
				throw new HandlerException("Invalid request state! A reply is pending?");

			mRequestState = StateCode.REPLY_PENDING;

			synchronized(jsReply)
			{
				sendMessage(msg);

				Log.d("Requester: " + Thread.currentThread().getId(), "Request posted, starting wait....");

				while(mRequestState == StateCode.REPLY_PENDING)
				{
					try
					{
						jsReply.wait();

						Log.d("Requester: "+Thread.currentThread().getId(),"Reply wait completed....");
					}
					catch(InterruptedException e)
					{
						Log.d("Requester: "+Thread.currentThread().getId(),"Reply wait interrupted, restarting....");
					}
				}
			}

			mRequestState = StateCode.REQUESTER_IDLE;
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
