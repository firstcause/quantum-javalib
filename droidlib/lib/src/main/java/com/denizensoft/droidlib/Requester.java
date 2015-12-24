package com.denizensoft.droidlib;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import com.denizensoft.jlib.CriticalException;
import com.denizensoft.jlib.FatalException;
import com.denizensoft.jlib.LibException;
import com.denizensoft.jlib.NotFoundException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Requester extends Handler
{
	private static final int N_APP_MESSAGE			= 0xDEADBEEF;
	private static final int N_MSG_REQUEST 			= ( N_APP_MESSAGE + 1 );
	private static final int N_MSG_ASYNC_REQUEST	= ( N_APP_MESSAGE + 2 );
	private static final int N_MSG_TOKEN 			= ( N_APP_MESSAGE + 3 );

	static final public int N_RC_ERROR				= -1;
	static final public int N_RC_OK					= 0;
	static final public int N_RC_WARNING			= 1;
	static final public int N_RC_WARNING_NOTFOUND	= 2;
	static final public int N_RC_USER_CANCELLED		= 3;

	private final Object mOwner;

	public static long nRequestSequence = 0;

	private static ExecutorService mExecutor = null;

	public class ApiContext
	{
		public final long mSequenceNo;

		public int mRC = N_RC_OK, nRetries = 0;

		public String mState = "new";

		public ApiNode mApiNode = null;

		public String mMethod = null;

		public JSONObject mJsRequest = null, mJsReply = null;

		public Handler mReplyTo = null;

		public ApiResultHandler mResultHandler = null;

		public ApiContext(Handler replyTo, ApiNode apiNode, String stMethod, JSONObject jsRequest,JSONObject jsReply,
						  ApiResultHandler resultHandler)
		{
			mSequenceNo = ++Requester.nRequestSequence;
			mReplyTo = replyTo;
			mApiNode = apiNode;
			mMethod = stMethod;
			mJsRequest = jsRequest;
			mJsReply = ( jsReply != null ? jsReply : new JSONObject());
			mResultHandler = resultHandler;
		}

	};

	private Stack<ApiContext> mContextStack = new Stack<>();

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
				ApiContext apiContext = (ApiContext) msg.obj;

				Log.d("Requester", "invoke start");

				apiContext.mState = "invoked";

				mContextStack.push(apiContext);

				apiContext.mApiNode.invokeMethod(apiContext.mMethod,apiContext.mJsRequest,apiContext.mJsReply);

				Log.d("Requester", "invoke complete");
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

	public ApiNode attachApiMethod(String stApiTag, ApiMethod apiMethod)
	{
		ApiNode apiNode = mApiMap.get(stApiTag);

		apiNode.attachApiMethod(apiMethod);

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
		JSONObject jsReply = new JSONObject();

		Log.d("Requester", "jsJsonRequest: Sending: " + stJSON);

		sendRequest(stJSON, jsReply, new ApiResultHandler()
		{
			@Override
			public void fnCallback(int nRC, String stReply, JSONObject jsReply) throws JSONException
			{
				Log.d("Requester", "jsJsonRequest: callback reached...");
			}
		});

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

	public JSONObject callAPI(String stNodeSpec, String[] args)
	{
		try
		{
			JSONObject jsRequest = new JSONObject(), jsReply = new JSONObject();

			jsRequest.put("$apispec",stNodeSpec);

			if(args != null)
			{
				JSONArray jsArgs = new JSONArray();

				for(String s1 : args)
					jsArgs.put(s1);

				jsRequest.put("$args",jsArgs);
			}

			sendRequest(jsRequest.toString(),jsReply,null);

			return jsReply;
		}
		catch(JSONException e)
		{
			throw new FatalException("JSON exception during node method request...");
		}
	}

	public void callAPI(String stNodeSpec, String[] args, Handler replyTo, ApiResultHandler resultHandler)
	{
		try
		{
			JSONObject jsRequest = new JSONObject(), jsReply = new JSONObject();

			jsRequest.put("$apispec",stNodeSpec);

			if(args != null)
			{
				JSONArray jsArgs = new JSONArray();

				for(String s1 : args)
					jsArgs.put(s1);

				jsRequest.put("$args",jsArgs);
			}

			postRequest(replyTo, jsRequest.toString(),resultHandler);
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

	private void execApiContext(final ApiContext apiContext)
	{
		Message msg = obtainMessage();

		msg.what = N_MSG_REQUEST;
		msg.obj = apiContext;

		synchronized(apiContext)
		{
			Log.d("Requester", "sending message....");

			apiContext.mState = "queued";

			if(!sendMessage(msg))
				throw new HandlerException("execApiContext: send failed?!");

			Log.d("Requester", "context queued, starting wait....");

			boolean bExitWait = false;

			while(!bExitWait)
			{
				try
				{
					apiContext.wait();

					bExitWait = true;

					Log.d("Requester", "reply wait completed....");

					if(apiContext.mResultHandler != null)
					{
						Log.d("Requester", "posting the callback....");

						apiContext.mReplyTo.post(new ParamHelper<ApiContext>(apiContext)
							{
								@Override
								public void run()
								{
									// Whilst this bit is executed in the looper thread context
									//
									try
									{
										// So, now in here we are execute the callback in the looper context...
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

		// to get here the incoming apiContext must have been pushed onto the stack...
		// ...thus, this thread must pop it off.
		//
		mContextStack.pop();

		Log.d("Requester", "leaving....");
	}

	private ApiContext prepareContext(Handler replyTo, String stJSON, JSONObject jsReply, ApiResultHandler resultHandler)
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

	public void replyCommit(ApiNode.ReplyCode replyCode, String stReply) throws HandlerException
	{
		try
		{
			ApiContext ac = apiContext();

			String stMsg = "";

			synchronized(ac)
			{
				switch(replyCode)
				{
					case CRITICAL_ERROR:
					{
						Log.d("Requester","Committing reply with ERROR");

						ac.mRC = N_RC_ERROR;

						stMsg = "Unknown error!";
					}
					break;

					case SUCCESS_REQUEST:
					{
						Log.d("Requester","Committing reply with SUCCESS");

						ac.mRC = N_RC_OK;

						stMsg = "Unspecified SUCCESS!";
					}
					break;

					case WARNING_MESSAGE:
					{
						Log.d("Requester","Committing reply with WARNING");

						ac.mRC = N_RC_WARNING;

						stMsg = "Unspecified WARNING!";
					}
					break;

					case WARNING_NOTFOUND:
					{
						Log.d("Requester","Committing reply with NOTFOUND");

						ac.mRC = N_RC_WARNING_NOTFOUND;

						stMsg = "Unknown NOTFOUND warning!";
					}
					break;

					case USER_CANCELLED:
					{
						Log.d("Requester","Committing reply with USERCANCEL");

						ac.mRC = N_RC_USER_CANCELLED;

						stMsg = "Unknown,was cancelled by user!";
					}
					break;

				}

				ac.mJsReply.put("$rc",ac.mRC);

				if(stReply == null)
					stReply = stMsg;

				if(!ac.mJsReply.has("$reply"))
					ac.mJsReply.put("$reply",stReply);

				ac.mState = "done";

				ac.notify();
			};
		}
		catch(JSONException e)
		{
			throw new HandlerException(String.format("JSON exception: %s",e.getMessage()));
		}
	}

	public void sendRequest(JSONObject jsRequest, JSONObject jsReply, ApiResultHandler resultHandler)
	{
		sendRequest(jsRequest.toString(),jsReply, resultHandler);
	}

	public void sendRequest(String stJSON, JSONObject jsReply, ApiResultHandler resultHandler)
	{
		ApiContext apiContext = prepareContext(this,stJSON,jsReply,resultHandler);

		if(Thread.currentThread().getId() == getLooper().getThread().getId())
			throw new HandlerException("Requester: dead-lock condition, current thread cannot send on this requester!");

		Log.d("Requester", "Sending synchronous request...");

		// This way, the calling thread can go into execApiContext and wait
		//
		execApiContext(apiContext);
	}

	public void postRequest(Handler replyTo, String stJSON, ApiResultHandler resultHandler)
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

	protected ApiTask loadApiTask(String stTaskSpec)
	{
		Log.d("Requester", String.format("Looking for TASK: %s", stTaskSpec));

		try
		{
			Class specClass = Class.forName(stTaskSpec);

			Constructor constructor = specClass.getConstructor(Requester.class);

			Object obj = constructor.newInstance(this);

			Class apiTaskClass = Class.forName("ApiTask");

			if(!apiTaskClass.isInstance(obj))
			{
				throw new CriticalException(String.format("Class: %s, is not an extension of API task...",stTaskSpec));
			}
			return(ApiTask)obj;
		}
		catch(ClassNotFoundException e)
		{
			throw new CriticalException(String.format("Class not found: %s",e.getMessage()));
		}
		catch(NoSuchMethodException e)
		{
			throw new CriticalException(String.format("Constructor not found: %s",e.getMessage()));
		}
		catch(IllegalAccessException e)
		{
			throw new CriticalException(String.format("Constructor not public? : %s",e.getMessage()));
		}
		catch(InstantiationException e)
		{
			throw new CriticalException(String.format("Constructor/Class not available? : %s",e.getMessage()));
		}
		catch(InvocationTargetException e)
		{
			throw new CriticalException(String.format("Invocation target error? : %s",e.getMessage()));
		}
	}

	private void init()
	{
		attachApiNode("Requester", new ApiNode(this){
			@Override
			public void builtins(String stMethod, JSONObject jsRequest, JSONObject jsReply) throws JSONException, LibException
			{
				switch(stMethod)
				{
					case "hasAPI" :
					{
						if(!jsRequest.has("$args"))
							throw new HandlerException("Requester: request has no $args!");

						String stApiSpec = jsRequest.getJSONArray("$args").getString(0);

						Log.d("Requester", String.format("Looking for API: %s", stApiSpec ));

						if(mApiMap.containsKey(stApiSpec))
							replySuccessComplete("found");
						else
							replySuccessComplete("notfound");
					}
					break;

					case "invokeTASK" :
					{
						boolean bAsync = true;

						if(!jsRequest.has("$args"))
							throw new HandlerException("Requester: request has no $args!");

						String stTaskSpec = jsRequest.getJSONArray("$args").getString(0);

						if(jsRequest.has("$bAsynchronous"))
							bAsync = jsRequest.getBoolean("$bAsynchronous");

						ApiTask apiTask = requester().loadApiTask(stTaskSpec);

						if(bAsync)
						{
							// Post on a new thread...
							//
							executor().execute(apiTask);
						}
						else
						{
							post(apiTask);
						}
						replySuccessComplete(null);
					}
					break;

					case "loadAPI" :
					{
						boolean bAsync = true;

						if(!jsRequest.has("$args"))
							throw new HandlerException("Requester: request has no $args!");

						String stApiSpec = jsRequest.getJSONArray("$args").getString(0);

						if(jsRequest.has("$bAsynchronous"))
							bAsync = jsRequest.getBoolean("$bAsynchronous");

						ApiTask apiTask = requester().loadApiTask(stApiSpec);

						if(bAsync)
						{
							// Post on a new thread...
							//
							executor().execute(apiTask);
						}
						else
						{
							post(apiTask);
						}
					}
					break;

					case "dropAPI" :
					{
						if(!jsRequest.has("$args"))
							throw new HandlerException("Requester: request has no $args!");

						String stApiTag = jsRequest.getJSONArray("$args").getString(0);

						Log.d("Requester", String.format("Drop Mutiny API: %s", stApiTag ));

						dropApi(stApiTag);

						replySuccessComplete(null);
					}
					break;

					default:
						throw new HandlerException("unknown method");

				}
			}
		});
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
