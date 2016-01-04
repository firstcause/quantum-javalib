package com.denizensoft.appcompatlib;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import com.denizensoft.dbclient.DbException;
import com.denizensoft.droidlib.*;
import com.denizensoft.iablib.IabHelper;
import com.denizensoft.iablib.IabResult;
import com.denizensoft.iablib.Inventory;
import com.denizensoft.iablib.Purchase;
import com.denizensoft.jlib.LibException;
import com.denizensoft.jlib.Tempus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by sjm on 4/15/2015.
 */
public class IABFragment extends WebAppFragment implements
		IabHelper.OnConsumeFinishedListener,
		IabHelper.OnIabSetupFinishedListener,
		IabHelper.QueryInventoryFinishedListener,
		IabHelper.OnIabPurchaseFinishedListener
{
	protected IabHelper mIabHelper = null;

	protected Inventory mInventory = null;

	protected String stInventoryStamp = null;

	protected class AsyncRequester extends WorkItem
	{
		private JSONObject mCapsule = null;

		boolean testReplySuccessful(JSONObject jsReply)
		{
			int nRC = 0;

			try
			{
				if(jsReply == null)
				{
					Log.d("AsyncRequester", "Request not successful, reply is null");

					return false;
				}

				nRC = jsReply.getInt("$rc");

				if(!jsReply.has("$iabrc"))
				{
					Log.d("AsyncRequester", String.format("Non-IAB request successful, return code: %d",nRC));

					return true;
				}

				nRC = jsReply.getInt("$iabrc");

				if(nRC == 0)
				{
					Log.d("AsyncRequester", String.format("Request successful, response code: %d",nRC));

					return true;
				}
			}
			catch(JSONException e)
			{
				e.printStackTrace();
			}

			Log.d("AsyncRequester", String.format("Request not successful, response code: %d",nRC));

			return false;
		}

		protected boolean invokeRequestCycle(JSONObject jsRequest) throws LibException, InterruptedException
		{
			String s1 = null;

			try
			{
				int nRetries = 0, nInterval = 60000;

				if(jsRequest.has("$retries"))
					nRetries = jsRequest.getInt("$retries");

				if(jsRequest.has("$interval"))
					nInterval = jsRequest.getInt("$interval");

				for(int i = 0; nRetries == 0 || i < nRetries; ++i)
				{
					Log.d("AsyncRequester",String.format("Starting retry: %d of %d", i+1, nRetries));

//					if(testReplySuccessful(mAppInterface.requester().sendRequest(jsRequest,null)))
//					{
//						Log.d("AsyncRequester", "Request completed, exit retry loop!");
//
//						return true;
//					}

					Thread.sleep(nInterval,0);
				}

				Log.d("AsyncRequester", "No more retries left!");
			}
			catch(JSONException e)
			{
				s1 = String.format("JSON: Request cycle: %s",e.getMessage());

				Log.e("AsyncRequester", s1);

				throw new LibException(s1);
			}
			return false;
		}

		protected void invokeOnSuccessCycle(JSONObject jsRequest) throws LibException
		{
			String s1 = null;

			try
			{
				Log.d("AsyncRequester", "Starting on success cycle...");

				if(!jsRequest.has("$onsuccess"))
				{
					Log.d("AsyncRequester", "Request has no success script...");

					return;
				}

				JSONArray jsScriptArray = jsRequest.getJSONArray("$onsuccess");

				for(int i = 0; i < jsScriptArray.length(); ++i)
				{
					JSONObject jsAction = jsScriptArray.getJSONObject(i);

					mAppInterface.requester().postRequest(mAppInterface.requester(), jsAction.toString(),
							new ApiCallback()
								{
									@Override
									public void fnCallback(int nRC, String stReply, JSONObject jsReply) throws JSONException
									{
										if(jsReply.getInt("$rc") != 0)
										{
											String s1= String.format(Locale.US,"Got error reply: %d",nRC);

											Log.e("AsyncRequester",s1);

											throw new LibException(s1);
										}
									}
								});

				}
			}
			catch(JSONException e)
			{
				s1 = String.format("JSON: Success cycle: %s",e.getMessage());

				Log.e("AsyncRequester",s1);

				throw new LibException(s1);
			}
		}

		@Override
		public void doWork()
		{
			try
			{
				JSONArray jsCapsule = mCapsule.getJSONArray("$capsule");

				for(int i=0; i < jsCapsule.length(); ++i)
				{
					JSONObject jsRequest = jsCapsule.getJSONObject(i);

					try
					{
						if(!invokeRequestCycle(jsRequest))
						{
							Log.e("AsyncRequester", "Request failed, cannot continue...");

							return;
						}
						else
						{
							Log.d("AsyncRequester", "Request succeeded...");

							invokeOnSuccessCycle(jsRequest);
						}
					}
					catch(InterruptedException e)
					{
						e.printStackTrace();
					}
					catch(LibException e)
					{
						mAppInterface.appFatalErrorHook("IAB error",e.getMessage());
					}
				}
			}
			catch(JSONException e)
			{
				mAppInterface.appFatalErrorHook("IAB error",
						String.format("A JSON exception has been invoked: %s",e.getMessage()));
			}
		}

		public AsyncRequester(JSONObject jsCapsule) throws LibException
		{
			super();

			mCapsule = jsCapsule;
		}
	}

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Android callbacks section start
	//
	@Override
	public void onDestroy()
	{
		super.onDestroy();

		if(mIabHelper != null)
			mIabHelper.dispose();

		mIabHelper = null;

		mAppInterface.appDropResultListener(this);
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// AppFragment overrides
	//
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState)
	{
		super.onViewCreated(view,savedInstanceState);

		if(savedInstanceState == null)
		{
			mIabHelper = new IabHelper(mAppActivity, mAppInterface.appGetLicenseKey());

			mAppInterface.appAddResultListener(this);

			mIabHelper.startSetup(this);

			mWebView.addJavascriptInterface(this, "APPAPI");

			mAppInterface.requester().attachApiNode("iab",new ApiNode(this)
				.attachApiMethod("queryInventoryAsync", new ApiMethod()
					{
						@Override
						public void func(ApiContext ac) throws JSONException, LibException
						{
							if(mInventory == null ||
									ac.request().has("$param") && ac.request().getString("$param").equals("force-requery"))
							{
								mIabHelper.queryInventoryAsync((IABFragment)ac.node().nodeOwner());
							}
							else
							{
								Log.d("IAB Inventory", "com.denizensoft.iablib.Inventory already present, refresh not requested...");

								ac.reply().put("$iabrc", 0);

								ac.replyCommit(ApiContext.ReplyCode.SUCCESS_REQUEST, null);
							}
						}
					})
				.attachApiMethod("inventoryStatus", new ApiMethod()
					{
						@Override
						public void func(ApiContext ac) throws JSONException, LibException
						{
							ac.reply().put("$status", ( mInventory != null ? "ready" : "notready"));
							ac.reply().put("$timestamp", ( mInventory != null ? stInventoryStamp : "(none)"));

							ac.replyCommit(ApiContext.ReplyCode.SUCCESS_REQUEST, null);
						}
					})
				.attachApiMethod("consumeAsync", new ApiMethod()
					{
						@Override
						public void func(ApiContext ac) throws JSONException, LibException
						{
							String sku = ac.request().getString("$sku");

							if(mInventory == null)
							{
								Log.e("IAB","Can't do consume, the inventory is empty!");

								ac.replyCommit(ApiContext.ReplyCode.CRITICAL_ERROR,"Can't do consume, the inventory is empty!");
							}
							else
							{
								if(!mInventory.hasPurchase(sku))
								{
									ac.replyCommit(ApiContext.ReplyCode.WARNING_NOTFOUND,
											String.format(Locale.US,"Item not in inventory: %s",sku));
								}
								else
								{
									Log.d("IAB", String.format(Locale.US,"Starting consume, item: %s", sku));

									mIabHelper.consumeAsync(mInventory.getPurchase(sku), (IABFragment)ac.node().nodeOwner());
								}
							}
						}
					})
				.attachApiMethod("updateInventoryCache", new ApiMethod()
					{
						@Override
						public void func(ApiContext ac) throws JSONException, LibException
						{
							if(mInventory == null)
							{
								ac.replyCommit(ApiContext.ReplyCode.WARNING_NOTFOUND,"No inventory to cache yet!");
							}
							else
							{
								List<Purchase> purchases = mInventory.getAllPurchases();

								for(Purchase purchase: purchases)
									updateIabCache(purchase);

								try
								{
									dbc().stashStateTokenString("IAB_INVENTORY_STATUS","valid");
								}
								catch(DbException e)
								{
									mAppInterface.appFatalErrorHook("IAB","Couldn't update the IAB_INVENTORY_STATUS!");
								}

								ac.replyCommit(ApiContext.ReplyCode.SUCCESS_REQUEST,null);
							}
						}
					})
				.attachApiMethod("purchaseSubscription", new ApiMethod()
					{
						@Override
						public void func(ApiContext ac) throws JSONException, LibException
						{
							String
									sku = ac.request().getString("$sku"),
									s2 = ac.request().getString("$tag-string");

							if(mInventory != null && mInventory.hasPurchase(sku))
							{
								ac.replyCommit(ApiContext.ReplyCode.SUCCESS_REQUEST, null);
							}
							else
							{
								mIabHelper.launchSubscriptionPurchaseFlow(mAppActivity, sku, 16661,
										(IABFragment)ac.node().nodeOwner(), s2);
							}
						}
					})
				.attachApiMethod("purchaseManagedItem", new ApiMethod()
					{
						@Override
						public void func(ApiContext ac) throws JSONException, LibException
						{
							String
									sku = ac.request().getString("$sku"),
									s2 = ac.request().getString("$tag-string");

							if(mInventory != null && mInventory.hasPurchase(sku))
							{
								ac.replyCommit(ApiContext.ReplyCode.SUCCESS_REQUEST, null);
							}
							else
							{
								mIabHelper.launchPurchaseFlow(mAppActivity, sku, 16661,
										(IABFragment)ac.node().nodeOwner(), s2);
							}
						}
					})
				.attachApiMethod("inventoryItemAvailable", new ApiMethod()
					{
						@Override
						public void func(ApiContext ac) throws JSONException, LibException
						{
							String sku = ac.request().getString("$sku");

							if(mInventory != null && mInventory.hasPurchase(sku))
							{
								ac.replyCommit(ApiContext.ReplyCode.SUCCESS_REQUEST, null);
							}
							else
							{
								ac.replyCommit(ApiContext.ReplyCode.WARNING_NOTFOUND, null);
							}
						}
					})
			);

//			{
//
//				@Override
//				public void builtins(String stMethod, JSONObject jsRequest, JSONObject jsReply) throws JSONException, LibException
//				{
//					String s1, s2;
//
//					if(stMethod.equals("invoke-async-requester"))
//					{
//						try
//						{
//							WorkItem asyncRequester = new AsyncRequester(jsRequest);
//
//							asyncRequester.execute();
//
//							replyCommit(ApiContext.ReplyCode.SUCCESS_REQUEST,null);
//						}
//						catch(LibException e)
//						{
//							s1 = String.format(Locale.US,"AsyncRequester exception: %s", e.getMessage());
//
//							Log.e("IAB",s1);
//
//							mAppInterface.appFatalErrorHook("IAB Request",s1);
//						}
//					}
//				}
//			});
		}
	}

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// IAB callbacks section
	//
	@Override
	public void onConsumeFinished(Purchase purchase, IabResult result)
	{
		Log.d("IAB Consume", String.format("Result: %d", result.getResponse()));

		if(result.isSuccess())
		{
			if(mInventory != null)
				mInventory.erasePurchase(purchase.getSku());

			if(dbc() != null)
			{
				try
				{
					dbc().dropStateToken(purchase.getSku());
				}
				catch(DbException e)
				{
					mAppInterface.appFatalErrorHook("IABCACHE",e.getMessage());
				}
			}
		}
		else
		{
			Log.d("IAB Consume", String.format("Message: %s", result.getMessage()));
		}

		updateJsonRequest(result);
	}

	@Override
	public void onIabPurchaseFinished(IabResult result, Purchase purchase)
	{
		Log.d("IAB Purchase", String.format(Locale.US,"Result: %d", result.getResponse()));

		if(result.isSuccess())
		{
			if(purchase.getPurchaseState() == 0)
			{
				if(mInventory != null)
				{
					if(mInventory.hasPurchase(purchase.getSku()))
						mInventory.erasePurchase(purchase.getSku());

					mInventory.addPurchase(purchase);
				}

				updateIabCache(purchase);
			}
		}
		else
		{
			Log.d("IAB .Purchase", String.format(Locale.US,"Message: %s", result.getMessage()));
		}

		updateJsonRequest(result);
	}

		@Override
	public void onIabSetupFinished(IabResult result)
	{
		Log.d("IAB Setup", String.format(Locale.US,"Result: %d", result.getResponse()));

		if(result.isFailure())
		{
			mAppInterface.appAlertDialog("In-App Billing Message",
					"Sorry, but the Google Play Store cannot be reached right now!\n\n" +
							"Please check your network connectivity as some features of this application " +
							"may temporarily be unavailable.");
		}
	}

	@Override
	public void onQueryInventoryFinished(IabResult result, Inventory inventory)
	{
		Log.d("IAB .Inventory", String.format(Locale.US,"Result: %d", result.getResponse()));

		if(result.isSuccess())
		{
			mInventory = inventory;

			stInventoryStamp = Tempus.formatDate(null, Tempus.FRIENDLYFULLAMPM, TimeZone.getDefault());
		}
		else
		{
			Log.d("IAB .Inventory",String.format(Locale.US,"Message: %s",result.getMessage()));
		}

		updateJsonRequest(result);
	}

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// AppActivity overrides
	//
	@Override
	public boolean onActivityResultHook(int requestCode, int resultCode, Intent data)
	{
		if(mIabHelper != null && mIabHelper.checkUsableStatus())
			return mIabHelper.handleActivityResult(requestCode,resultCode,data);

		return false;
	}

	protected void updateJsonRequest(IabResult result)
	{
		if(mAppInterface.requester().apiContext().reply() != null)
		{
			try
			{
				Log.d("IAB Request", "JSON request present, sending reply...");
				mAppInterface.requester().apiContext().reply().put("$iabrc", result.getResponse());
				mAppInterface.requester().apiContext().replyCommit(ApiContext.ReplyCode.SUCCESS_REQUEST,null);
			}
			catch(JSONException e)
			{
				throw new RuntimeException(String.format(Locale.US,"JSON Request: exception during reply: %s",e.getMessage()));
			}
		}
	}

	protected void updateIabCache(Purchase purchase)
	{
		if(dbc() != null)
		{
			try
			{
				dbc().stashCryptTokenString(purchase.getSku(),purchase.getOriginalJson(),"IABCACHE");
			}
			catch(DbException e)
			{
				mAppInterface.appFatalErrorHook("IABCACHE",e.getMessage());
			}
		}
	}

	public IABFragment()
	{
	}
}
