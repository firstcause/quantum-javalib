package com.denizensoft.droidlib;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import com.denizensoft.jlib.FatalException;
import com.denizensoft.jlib.NotFoundException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Requester extends Handler
{
	static final public int N_APP_MESSAGE			= 0xDEADBEEF;
	static final public int N_MSG_REQUEST 			= ( N_APP_MESSAGE + 1 );
	static final public int N_MSG_TOKEN 			= ( N_APP_MESSAGE + 2 );

	private final Object mOwner;

	static public enum StateCode
	{
		REQUESTER_IDLE,
		REQUEST_PENDING,
		REPLY_PENDING,
		REPLY_READY
	}

	private Stack<ApiNode> mApiNodeStack = new Stack<>();

	private ApiNode mApiNode = null;

	private HashMap<String,ApiNode> mApiMap = new HashMap<String,ApiNode>();

	private ArrayList<TokenNode> mTokenNodeList = new ArrayList<>();

	protected Pattern mMatchNodeSpec = Pattern.compile("([\\w\\-\\_]+)\\.{1}([\\w\\-\\_]+)");

	public void addApiNode(ApiNode apiNode)
	{
		mApiMap.put(apiNode.nodeTag(), apiNode);
		apiNode.attachTo(this);
	}

	public void addTokenNode(TokenNode tokenNode)
	{
		mTokenNodeList.add(tokenNode);
	}

	public void dropApi(String stClass)
	{
		if(mApiMap.containsKey(stClass))
			mApiMap.remove(stClass);
	}

	public void dropOwnedNodes(Object owner)
	{
		Iterator<TokenNode> i1 = mTokenNodeList.iterator();

		while(i1.hasNext())
		{
			if(i1.next().nodeOwner().equals(owner))
				i1.remove();
		}

		Iterator<Map.Entry<String,ApiNode>> i2 = mApiMap.entrySet().iterator();

		while(i2.hasNext())
		{
			if(i2.next().getValue().nodeOwner().equals(owner))
				i2.remove();
		}
	}

	public boolean hasApi(String stClass)
	{
		return mApiMap.containsKey(stClass);
	}

	public Context getContext()
	{
		return null;
	}

	public ApiNode getApiRef(String stClass) throws NotFoundException
	{
		if(mApiMap.containsKey(stClass))
			return mApiMap.get(stClass);

		throw new NotFoundException(String.format("Requester: api not found: %s",stClass));
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

	public void loadApiClass(String stClassSpec)
	{
		try
		{
			Log.d("Requester", String.format("Mutiny Class Requested: %s", stClassSpec ));

			Class mutinyClass = Class.forName(stClassSpec);

			Constructor constructor = mutinyClass.getConstructor(Requester.class);

			ApiInvoker apiInvoker = (ApiInvoker) constructor.newInstance(this);

			post(apiInvoker);
		}
		catch(InstantiationException e)
		{
			throw new FatalException(String.format("Requester: Couldn't instantiate class: %s",stClassSpec),e);
		}
		catch(InvocationTargetException e)
		{
			throw new FatalException(String.format("Requester: Couldn't invoke constructor: %s",stClassSpec),e);
		}
		catch(NoSuchMethodException e)
		{
			throw new FatalException(String.format("Requester: Constructor not found? : %s",stClassSpec),e);
		}
		catch(IllegalAccessException e)
		{
			throw new FatalException(String.format("Requester: Constructor not public? : %s",stClassSpec),e);
		}
		catch(ClassNotFoundException e)
		{
			throw new FatalException(String.format("Requester: Class not found? : %s",stClassSpec),e);
		}
	}

	public Pattern matchNodeSpec()
	{
		return mMatchNodeSpec;
	}

	public Object owner()
	{
		return mOwner;
	}

	public JSONObject nodeMethodRequest(String stNodeSpec, String[] args)
	{
		JSONObject jsRequest = new JSONObject();

		try
		{
			jsRequest.put("$nodespec",stNodeSpec);

			if(args != null)
			{
				JSONArray jsArgs = new JSONArray();

				for(String s1 : args)
					jsArgs.put(s1);

				jsRequest.put("$args",jsArgs);
			}
		}
		catch(JSONException e)
		{
			throw new FatalException("JSON exception during node method request...");
		}
		return sendRequest(jsRequest.toString());
	}

	public ApiNode apiNode()
	{
		return mApiNodeStack.peek();
	}

	public JSONObject sendRequest(JSONObject jsRequest)
	{
		return sendRequest(jsRequest.toString());
	}

	public JSONObject sendRequest(String stJSON)
	{
		JSONObject jsReply = new JSONObject();

		String stTag = String.format("%08X: Requester",Thread.currentThread().getId());

		if(this.getLooper().getThread().getId() == Thread.currentThread().getId())
		{
			try
			{
				JSONObject jsRequest = new JSONObject(stJSON);

				String
						stNodeSpec = jsRequest.getString("$nodespec"),
						stNodeTag, stMethod = null;

				Matcher matcher = mMatchNodeSpec.matcher(stNodeSpec);

				if(!matcher.matches())
				{
					throw new HandlerException(String.format("%s: Malformed node spec not matched: %s",
							stTag, stNodeSpec));
				}

				stNodeTag = matcher.group(1);
				stMethod = matcher.group(2);

				if(!mApiMap.containsKey(stNodeTag))
					throw new HandlerException(String.format("%s Undefined action token: %s",stTag,stNodeTag));

				mApiNode = mApiMap.get(stNodeTag);

				synchronized(mApiNode)
				{
					mApiNode.startRequest(stMethod,jsRequest,jsReply);
				}
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

		synchronized(msg)
		{
			boolean bDequeued = false, bReplyReady = false;

			sendMessage(msg);

			Log.d("Requester: " + Thread.currentThread().getId(), "Request queued, starting wait....");

			while(!bDequeued)
			{
				try
				{
					msg.wait();

					bDequeued = true;

					synchronized(jsReply)
					{
						Log.d("Requester: " + Thread.currentThread().getId(), "Reply pending, starting wait....");

						while(!bReplyReady)
						{
							try
							{
								jsReply.wait();

								bReplyReady = true;

								Log.d("Requester: "+Thread.currentThread().getId(),"Reply wait completed....");
							}
							catch(InterruptedException e)
							{
								Log.d("Requester: "+Thread.currentThread().getId(),"Reply wait interrupted, restarting....");
							}
						}
					}
				}
				catch(InterruptedException e)
				{
					Log.d("Requester: "+Thread.currentThread().getId(),"Message wait interrupted, restarting....");
				}
			}
		}
		return jsReply;
	}

	public void sendToken(String stToken,Bundle args)
	{
		Message msg = obtainMessage();

		msg.what = N_MSG_TOKEN;
		msg.arg1 = 0;

		if(args == null)
			args = new Bundle();

		args.putString("$token",stToken);

		msg.setData(args);

		sendMessage(msg);
	}

	public void setMatchNodeSpec(Pattern pattern)
	{
		mMatchNodeSpec = pattern;
	}

	///////////////////////////////////////////////////////////////////////////////////////////////
	//
	//
	@Override
	public void handleMessage(Message msg)
	{
		switch(msg.what)
		{
			case N_MSG_REQUEST:
			{
				// Remember, there is only one looper thread, it handles the request invocations,
				// it is not possible for two threads to be here...
				//
				Log.d("Requester", String.format("%08X: Hanling a request...",Thread.currentThread().getId()));

				synchronized(msg)
				{
					msg.notify();

					try
					{
						JSONObject jsRequest = new JSONObject(msg.getData().getString("$request"));

						String
								stNodeSpec = jsRequest.getString("$nodespec"),
								stNodeTag, stMethod = null;

						Matcher matcher = mMatchNodeSpec.matcher(stNodeSpec);

						if(!matcher.matches())
							throw new HandlerException(String.format("Requester: Malformed node spec not matched: %s",stNodeSpec));

						stNodeTag = matcher.group(1);
						stMethod = matcher.group(2);

						if(!mApiMap.containsKey(stNodeTag))
						{
							throw new HandlerException(String.format("Undefined action token: %s", stNodeTag));
						}

						mApiNode = mApiMap.get(stNodeTag);

						mApiNode.startRequest(stMethod,jsRequest,(JSONObject) msg.obj);
					}
					catch(JSONException e)
					{
						throw new HandlerException(String.format("JSON exception: %s",e.getMessage()));
					}
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

	public Requester(Object owner)
	{
		mOwner = owner;
	}

	public Requester(Looper looper, Object mOwner)
	{
		super(looper);
		this.mOwner = mOwner;
	}
}
