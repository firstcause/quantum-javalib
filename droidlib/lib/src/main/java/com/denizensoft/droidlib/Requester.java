package com.denizensoft.droidlib;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import com.denizensoft.jlib.FatalException;
import com.denizensoft.jlib.LibException;
import com.denizensoft.jlib.NotFoundException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Requester extends Handler
{
	private static final int N_APP_MESSAGE			= 0xDEADBEEF;
	private static final int N_MSG_REQUEST 			= ( N_APP_MESSAGE + 1 );
	private static final int N_MSG_TOKEN 			= ( N_APP_MESSAGE + 2 );

	private final Object mOwner;

	public static long nRequestSequence = 0;

	private static ExecutorService mExecutor = null;

	private final Stack<ApiContext> mContextStack = new Stack<>();

	private final HashMap<String,ApiNode> mApiMap = new HashMap<String,ApiNode>();

	private ArrayList<TokenNode> mTokenNodeList = new ArrayList<>();

	protected Pattern mMatchApiSpec = Pattern.compile("([^\\.]+)\\.{1}([\\d\\w\\-\\_]+)");

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
				// Remember, there is only one looper thread, as far as this handler is concerned, 
				// it handles the request invocations, it is not possible for two threads to be here...
				//
				ApiContext ac = (ApiContext) msg.obj;

				Log.d("Requester", "invoke start");

				ac.mState = "invoked";

				synchronized(mContextStack)
				{
					mContextStack.push(ac);
				}

				ac.invokeNodeMethod();
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

	///////////////////////////////////////////////////////////////////////////////////////////////
	//
	//
	public ApiNode attachApiNode(String stApiTag, ApiNode apiNode)
	{
		synchronized(mApiMap)
		{
			apiNode.attachedTo(this,stApiTag);
			mApiMap.put(apiNode.nodeTag(), apiNode);
		}
		return apiNode;
	}

	public void addTokenNode(TokenNode tokenNode)
	{
		mTokenNodeList.add(tokenNode);
	}

	public void dropApi(String stApiTag)
	{
		synchronized(mApiMap){
			if(mApiMap.containsKey(stApiTag))
				mApiMap.remove(stApiTag);
		}
	}

	public void dropApiNode(ApiNode apiNode)
	{
		dropApi(apiNode.nodeTag());
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

	protected Executor executor()
	{
		if(mExecutor == null)
		{
			Log.d("Requester", "allocating executor service....");

			mExecutor = Executors.newCachedThreadPool();
		}
		return mExecutor;
	}

	public Context getContext()
	{
		Class context = Context.class;

		if(context.isInstance(mOwner))
		{
			return (Context)mOwner;
		}
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
		Log.d("Requester", "jsJsonRequest: Sending: " + stJSON);

		JSONObject jsReply = sendRequest(stJSON);

		String stReply = jsReply.toString();

		Log.d("Requester", "jsJsonRequest: reply: "+stReply);

		return stReply;
	}

	public Pattern matchNodeSpec()
	{
		return mMatchApiSpec;
	}

	public Object owner()
	{
		return mOwner;
	}

	public void popApiContext()
	{
		synchronized(mContextStack){
			mContextStack.pop();
		}
	}

	public JSONObject callAPI(String stNodeSpec, String[] args)
	{
		try
		{
			JSONObject jsRequest = new JSONObject();

			jsRequest.put("$apispec",stNodeSpec);

			if(args != null)
			{
				JSONArray jsArgs = new JSONArray();

				for(String s1 : args)
					jsArgs.put(s1);

				jsRequest.put("$args",jsArgs);
			}

			JSONObject jsReply = sendRequest(jsRequest.toString());

			return jsReply;
		}
		catch(JSONException e)
		{
			throw new FatalException("JSON exception during node method request...");
		}
	}

	public void callAPI(String stNodeSpec, String[] args, Handler replyTo, ApiCallback resultHandler)
	{
		try
		{
			JSONObject jsRequest = new JSONObject();

			jsRequest.put("$apispec",stNodeSpec);

			if(args != null)
			{
				JSONArray jsArgs = new JSONArray();

				for(String s1 : args)
					jsArgs.put(s1);

				jsRequest.put("$args",jsArgs);
			}

			postRequest(replyTo == null ? this : replyTo, jsRequest.toString(),resultHandler);
		}
		catch(JSONException e)
		{
			throw new FatalException("JSON exception during node method request...");
		}
	}

	public ApiContext apiContext()
	{
		return mContextStack.peek();
	}

	public ApiNode apiNode()
	{
		return mContextStack.peek().mApiNode;
	}

	private void execApiContext(final ApiContext ac)
	{
		Message msg = obtainMessage();

		msg.what = N_MSG_REQUEST;
		msg.obj = ac;

		synchronized(ac)
		{
			Log.d("Requester", "sending message....");

			ac.mState = "queued";

			if(!sendMessage(msg))
				throw new HandlerException("execApiContext: send failed?!");

			Log.d("Requester", "context queued, starting wait....");

			boolean bExitWait = false;

			while(!bExitWait)
			{
				try
				{
					ac.wait();

					bExitWait = true;

					Log.d("Requester", "reply wait completed....");

					if(ac.mResultHandler != null)
					{
						Log.d("Requester", "posting the func....");

						ac.mReplyTo.post(new ParamHelper<ApiContext>(ac)
							{
								@Override
								public void run()
								{
									// Whilst this bit is executed in the looper thread context
									//
									try
									{
										// So, now in here we are execute the func in the looper context...
										// (i.e. a "synchronous" asynchronous request...
										//
										if(param().mJsReply.has("$reply"))
										{
											param().mResultHandler.fnCallback(param().mRC,param().mJsReply.getString("$reply"),
													param().mJsReply);
										}
										else
										{
											param().mResultHandler.fnCallback(param().mRC, null, param().mJsReply);
										}
									}
									catch(JSONException e)
									{
										throw new HandlerException("JSON exception!",e);
									}
								}
							});
					}
				}
				catch(InterruptedException e)
				{
					Log.d("Requester", "reply wait interrupted, restarting....");
				}
			}
		}

		Log.d("Requester", "leaving....");
	}

	private ApiContext prepareContext(Handler replyTo, String stJSON, JSONObject jsReply, ApiCallback resultHandler)
	{
		try
		{
			JSONObject jsRequest = new JSONObject(stJSON);

			String
					stApiSpec = jsRequest.getString("$apispec"),
					stClassTag, stMethod = null;

			Matcher matcher = mMatchApiSpec.matcher(stApiSpec);

			if(!matcher.matches())
				throw new HandlerException(String.format("Requester: Invalid node specification: %s",stApiSpec));

			stClassTag = matcher.group(1);
			stMethod = matcher.group(2);

			if(!mApiMap.containsKey(stClassTag))
			{
				throw new HandlerException(String.format("Unknown API class: %s", stClassTag));
			}

			ApiNode apiNode = mApiMap.get(stClassTag);

			ApiContext apiContext = new ApiContext(replyTo,apiNode,stMethod,jsRequest,jsReply,resultHandler);

			return apiContext;
		}
		catch(JSONException e)
		{
			throw new HandlerException("Requester: JSON error during request",e);
		}
	}

	public void sendRequest(JSONObject jsRequest)
	{
		sendRequest(jsRequest.toString());
	}

	public JSONObject sendRequest(String stJSON)
	{
		if(Thread.currentThread().getId() == getLooper().getThread().getId())
			throw new HandlerException("Requester: dead-lock condition, current thread cannot send on this requester!");

		JSONObject jsReply = new JSONObject();

		ApiContext apiContext = prepareContext(null,stJSON,jsReply,null);

		Log.d("Requester", "Sending synchronous request...");

		// This way, the calling thread can go into execApiContext and wait
		//
		execApiContext(apiContext);

		return jsReply;
	}

	public void postRequest(Handler replyTo, String stJSON, ApiCallback resultHandler)
	{
		ApiContext apiContext = prepareContext(replyTo,stJSON,null,resultHandler);

		Log.d("Requester", "posting asynchronous request...");

		executor().execute(new ParamHelper<ApiContext>(apiContext)
		{
			@Override
			public void run()
			{
				param().mApiNode.requester().execApiContext(param());
			}
		});
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
		mMatchApiSpec = pattern;
	}

	private void init()
	{
		attachApiNode("Requester", new ApiNode(this)
				.attachApiMethod("hasAPI", new ApiMethod(){
					@Override
					public void func(ApiContext ac) throws JSONException, LibException
					{
						if(!ac.mJsRequest.has("$args"))
							throw new HandlerException("Requester: request has no $args!");

						String stApiSpec = ac.request().getJSONArray("$args").getString(0);

						Log.d("Requester", String.format("Looking for API: %s", stApiSpec ));

						if(mApiMap.containsKey(stApiSpec))
							ac.replySuccessComplete("found");
						else
							ac.replySuccessComplete("notfound");
					}
				})
				.attachApiMethod("invokeTASK", new ApiMethod(){
					@Override
					public void func(ApiContext ac) throws JSONException, LibException
					{
						boolean bAsync = true;

						if(!ac.request().has("$args"))
							throw new HandlerException("Requester: request has no $args!");

						String stTaskSpec = ac.request().getJSONArray("$args").getString(0);

						if(ac.request().has("$bAsynchronous"))
							bAsync = ac.request().getBoolean("$bAsynchronous");

						ApiTask apiTask = ac.loadApiTask(stTaskSpec);

						if(bAsync)
						{
							// Post on a new thread...
							//
							executor().execute(apiTask);
						}
						else
						{
							// Post it to run on the looper thread...
							//
							post(apiTask);
						}

						if(!ac.request().has("$bDontReplyOnInvoke"))
							ac.replySuccessComplete(null);
					}
				})
				.attachApiMethod("sleep", new ApiMethod(){
					@Override
					public void func(ApiContext ac) throws JSONException, LibException
					{
						if(!ac.request().has("$args"))
							throw new HandlerException("Requester: request has no $args!");

						ac.request().put("$interval",ac.request().getJSONArray("$args").getInt(0));

						ApiTask apiTask = ac.loadApiTask(getClass().getPackage().getName()+".ApiSleep");

						// Post on a new thread...
						//
						executor().execute(apiTask);
					}
				})
				.attachApiMethod("loadAPI", new ApiMethod()
				{
					@Override
					public void func(ApiContext ac) throws JSONException, LibException
					{
						if(!ac.request().has("$args"))
							throw new HandlerException("Requester: request has no $args!");

						String stApiSpec = ac.request().getJSONArray("$args").getString(0);

						ApiTask apiTask = ac.loadApiTask(stApiSpec);

						post(apiTask);
					}
				})
				.attachApiMethod("dropAPI", new ApiMethod()
				{
					@Override
					public void func(ApiContext ac) throws JSONException, LibException
					{
						if(!ac.request().has("$args"))
							throw new HandlerException("Requester: request has no $args!");

						String stApiTag = ac.request().getJSONArray("$args").getString(0);

						Log.d("Requester", String.format("Drop Mutiny API: %s", stApiTag ));

						dropApi(stApiTag);

						ac.replySuccessComplete(null);
					}
				})
		);
	}

	public Requester(Object owner)
	{
		mOwner = owner;
		init();
	}

	public Requester(Looper looper, Object owner)
	{
		super(looper);
		mOwner = owner;
		init();
	}
}
