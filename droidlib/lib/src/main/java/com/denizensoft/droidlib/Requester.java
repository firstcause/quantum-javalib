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

	static final public int N_RC_ERROR				= -1;
	static final public int N_RC_OK					= 0;
	static final public int N_RC_WARNING			= 1;
	static final public int N_RC_WARNING_NOTFOUND	= 2;
	static final public int N_RC_USER_CANCELLED		= 3;

	private final Object mOwner;

	private long mRequestSequence = 0;

	public class ApiContext
	{
		public final long mSequence;

		public String mState = "new";

		public ApiNode mApiNode = null;

		public String mMethod = null;

		public JSONObject mJsRequest = null, mJsReply = null;

		public ApiResultHandler mResultHandler = null;

		public ApiContext(long nSequence,ApiNode apiNode, String stMethod, JSONObject jsRequest,JSONObject jsReply,
						  ApiResultHandler resultHandler)
		{
			mSequence = nSequence;
			mApiNode = apiNode;
			mMethod = stMethod;
			mJsRequest = jsRequest;
			mJsReply = jsReply;
			mResultHandler = resultHandler;
		}
	};

	private Stack<ApiContext> mContextStack = new Stack<>();

	private HashMap<String,ApiNode> mApiMap = new HashMap<String,ApiNode>();

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
				// Remember, there is only one looper thread, it handles the request invocations,
				// it is not possible for two threads to be here...
				//
				ApiContext apiContext = (ApiContext) msg.obj;

				String stLogTag = String.format("Requester:%08X:%05X",Thread.currentThread().getId(),apiContext.mSequence);

				Log.d(stLogTag, "invoke start");

				apiContext.mState = "invoked";

				mContextStack.push(apiContext);

				apiContext.mApiNode.invokeMethod(apiContext.mMethod,apiContext.mJsRequest,apiContext.mJsReply);

				Log.d(stLogTag, "invoke complete");
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
	public ApiNode addApiNode(ApiNode apiNode)
	{
		mApiMap.put(apiNode.nodeTag(), apiNode);
		apiNode.attachTo(this);
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

		JSONObject jsReply = sendRequest(stJSON,null);

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

	public ApiInvoker loadApiInvoker(String stClassSpec) throws LibException
	{
		try
		{
			Log.d("Requester", String.format("Loading class: %s", stClassSpec ));

			Class mutinyClass = Class.forName(stClassSpec);

			Constructor constructor = mutinyClass.getConstructor(Requester.class);

			ApiInvoker apiInvoker = (ApiInvoker) constructor.newInstance(this);

			return apiInvoker;
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
		return mMatchApiSpec;
	}

	public Object owner()
	{
		return mOwner;
	}

	public JSONObject apiMethodCall(String stNodeSpec, String[] args, ApiResultHandler resultHandler)
	{
		JSONObject jsRequest = new JSONObject();

		try
		{
			jsRequest.put("$apispec",stNodeSpec);

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
		return sendRequest(jsRequest.toString(),resultHandler);
	}

	public ApiContext apiContext()
	{
		return mContextStack.peek();
	}

	public ApiNode apiNode()
	{
		return mContextStack.peek().mApiNode;
	}

	public void replyCommit(ApiNode.ReplyCode replyCode, String stReply) throws HandlerException
	{
		try
		{
			ApiContext ac = apiContext();

			String stLogTag = String.format("Requester:%08X:%05X",Thread.currentThread().getId(),ac.mSequence);

			synchronized(ac)
			{
				switch(replyCode)
				{
					case CRITICAL_ERROR:
					{
						Log.d(stLogTag,"Committing reply with ERROR");

						ac.mJsReply.put("$rc",N_RC_ERROR);

						if(stReply == null)
							stReply = "Unknown error!";
					}
					break;

					case SUCCESS_REQUEST:
					{
						Log.d(stLogTag,"Committing reply with SUCCESS");

						ac.mJsReply.put("$rc",N_RC_OK);

						if(stReply == null)
							stReply = "Unspecified SUCCESS!";
					}
					break;

					case WARNING_MESSAGE:
					{
						Log.d(stLogTag,"Committing reply with WARNING");

						ac.mJsReply.put("$rc",N_RC_WARNING);

						if(stReply == null)
							stReply = "Unspecified WARNING!";
					}
					break;

					case WARNING_NOTFOUND:
					{
						Log.d(stLogTag,"Committing reply with NOTFOUND");

						ac.mJsReply.put("$rc", N_RC_WARNING_NOTFOUND);

						if(stReply == null)
							stReply = "Unknown NOTFOUND warning!";
					}
					break;

					case USER_CANCELLED:
					{
						Log.d(stLogTag,"Committing reply with USERCANCEL");

						ac.mJsReply.put("$rc", N_RC_USER_CANCELLED);

						if(stReply == null)
							stReply = "Unknown,was cancelled by user!";
					}
					break;

				}

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

	public JSONObject sendRequest(JSONObject jsRequest, ApiResultHandler resultHandler)
	{
		return sendRequest(jsRequest.toString(),resultHandler);
	}

	public JSONObject sendRequest(String stJSON, ApiResultHandler resultHandler)
	{
		JSONObject jsReply = new JSONObject();

		try
		{
			long nSequence = mRequestSequence++;

			JSONObject jsRequest = new JSONObject(stJSON);

			String
					stLogTag = String.format("Requester:%08X:%05X:%08X",getLooper().getThread().getId(),
						nSequence,Thread.currentThread().getId()),

					stApiSpec = jsRequest.getString("$apispec"),

					stClassTag, stMethod = null;

			Matcher matcher = mMatchApiSpec.matcher(stApiSpec);

			if(!matcher.matches())
				throw new HandlerException(String.format("Requester: Invalid node specification: %s",stApiSpec));

			stClassTag = matcher.group(1);
			stMethod = matcher.group(2);

			if(!mApiMap.containsKey(stClassTag))
			{
				throw new HandlerException(String.format("Undefined API class: %s", stClassTag));
			}

			ApiNode apiNode = mApiMap.get(stClassTag);

			ApiContext apiContext = new ApiContext(nSequence,apiNode,stMethod,jsRequest,jsReply,resultHandler);

			Message msg = obtainMessage();

			msg.what = N_MSG_REQUEST;
			msg.obj = apiContext;

			if(this.getLooper().getThread().getId() != Thread.currentThread().getId())
			{
				synchronized(apiContext)
				{
					Log.d(stLogTag, "sendRequest: sending message....");

					apiContext.mState = "queued";

					if(!sendMessage(msg))
						throw new HandlerException("Send failed for request message?!");

					Log.d(stLogTag, "Request queued, starting wait....");

					boolean bReplyReady = false;

					while(!bReplyReady)
					{
						try
						{
							apiContext.wait();

							bReplyReady = true;

							Log.d(stLogTag, "sendRequest: reply wait completed....");

							if(apiContext.mResultHandler != null)
								jsReply = apiContext.mResultHandler.fnCallback(jsReply.getString("$reply"),jsReply);
						}
						catch(InterruptedException e)
						{
							Log.d(stLogTag, "sendRequest: reply wait interrupted, restarting....");
						}
					}
				}
			}
			else
			{
				Log.d(stLogTag, "sendRequest: handling synchronous message....");

				handleMessage(msg);

				if(!apiContext.mState.equals("done"))
				{
					Log.d(stLogTag, "sendRequest: context state not done...");
				}
			}

			mContextStack.pop();

			Log.d(stLogTag, "sendRequest: leaving....");
		}
		catch(JSONException e)
		{
			throw new HandlerException("Requester: JSON error during request",e);
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
		mMatchApiSpec = pattern;
	}

	public Requester(Object owner)
	{
		mOwner = owner;
		init();
	}

	private void init()
	{
		addApiNode(new ApiNode(this,"Requester"){
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

					case "loadAPI" :
					{
						if(!jsRequest.has("$args"))
							throw new HandlerException("Requester: request has no $args!");

						String stApiSpec = jsRequest.getJSONArray("$args").getString(0);

						Log.d("Requester", String.format("Load Mutiny Class: %s", stApiSpec ));

						ApiInvoker apiInvoker = loadApiInvoker(stApiSpec);

						String stApiTag = apiInvoker.initAPI();

						reply().put("$apiTag",stApiTag);

						replySuccessComplete(null);
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

	public Requester(Looper looper, Object mOwner)
	{
		super(looper);
		this.mOwner = mOwner;
		init();
	}
}
