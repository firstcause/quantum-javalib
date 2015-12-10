package com.denizensoft.appcompatlib;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import com.denizensoft.dbclient.DbException;
import com.denizensoft.droidlib.ApiNode;
import com.denizensoft.droidlib.WorkItem;
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

					if(testReplySuccessful(mAppInterface.requester().sendRequest(jsRequest,null)))
					{
						Log.d("AsyncRequester", "Request completed, exit retry loop!");

						return true;
					}

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

					JSONObject jsReply = mAppInterface.requester().sendRequest(jsAction,null);

					if(jsReply == null)
					{
						s1 = String.format("Got null reply during: %s",jsAction.getString("$action"));

						Log.e("AsyncRequester",s1);

						throw new LibException(s1);
					}
					else if(jsReply.getInt("$rc") != 0)
					{
						s1 = String.format(Locale.US,"Got error reply: %d, during: %s",jsReply.getInt("$rc"),
								jsAction.getString("$action"));

						Log.e("AsyncRequester",s1);

						throw new LibException(s1);
					}
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

			mAppInterface.requester().addApiNode(new ApiNode(this,"iab"){

				@Override
				public void builtins(String stMethod, JSONObject jsRequest, JSONObject jsReply) throws JSONException, LibException
				{
					String s1, s2;

					if(stMethod.equals("invoke-async-requester"))
					{
						try
						{
							WorkItem asyncRequester = new AsyncRequester(jsRequest);

							asyncRequester.execute();

							replyCommit(ApiNode.ReplyCode.SUCCESS_REQUEST,null);
						}
						catch(LibException e)
						{
							s1 = String.format(Locale.US,"AsyncRequester exception: %s", e.getMessage());

							Log.e("IAB",s1);

							mAppInterface.appFatalErrorHook("IAB Request",s1);
						}
					}
					else if(stMethod.equals("iab-query-inventory-async"))
					{
						if(mInventory == null ||
								jsRequest.has("$param") && jsRequest.getString("$param").equals("force-requery"))
						{
							mIabHelper.queryInventoryAsync((IABFragment)nodeOwner());
						}
						else
						{
							Log.d("IAB Inventory", "com.denizensoft.iablib.Inventory already present, refresh not requested...");

							reply().put("$iabrc", 0);

							replyCommit(ApiNode.ReplyCode.SUCCESS_REQUEST, null);
						}
					}
					else if(stMethod.equals("iab-inventory-status"))
					{
						jsReply.put("$status", ( mInventory != null ? "ready" : "notready"));
						jsReply.put("$timestamp", ( mInventory != null ? stInventoryStamp : "(none)"));

						replyCommit(ApiNode.ReplyCode.SUCCESS_REQUEST, null);
					}
					else if(stMethod.equals("iab-consume-async"))
					{
						s1 = jsRequest.getString("$sku");

						if(mInventory == null)
						{
							Log.e("IAB","Can't do consume, the inventory is empty!");

							replyCommit(ApiNode.ReplyCode.CRITICAL_ERROR,"Can't do consume, the inventory is empty!");
						}
						else
						{
							if(!mInventory.hasPurchase(s1))
							{
								replyCommit(ApiNode.ReplyCode.WARNING_NOTFOUND,
										String.format(Locale.US,"Item not in inventory: %s",s1));
							}
							else
							{
								Log.d("IAB", String.format(Locale.US,"Starting consume, item: %s", s1));

								mIabHelper.consumeAsync(mInventory.getPurchase(s1), (IABFragment)nodeOwner());
							}
						}

					}
					else if(stMethod.equals("iab-update-inventory-cache"))
					{
						if(mInventory == null)
						{
							replyCommit(ApiNode.ReplyCode.WARNING_NOTFOUND,"No inventory to cache yet!");
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

							replyCommit(ApiNode.ReplyCode.SUCCESS_REQUEST,null);
						}
					}
					else if(stMethod.equals("iab-purchase-subscription"))
					{
						s1 = jsRequest.getString("$sku");
						s2 = jsRequest.getString("$tag-string");

						if(mInventory != null && mInventory.hasPurchase(s1))
						{
							replyCommit(ApiNode.ReplyCode.SUCCESS_REQUEST, null);
						}
						else
						{
							mIabHelper.launchSubscriptionPurchaseFlow(mAppActivity, s1, 16661, (IABFragment)nodeOwner(), s2);
						}
					}
					else if(stMethod.equals("iab-purchase-managed"))
					{
						s1 = jsRequest.getString("$sku");
						s2 = jsRequest.getString("$tag-string");

						if(mInventory != null && mInventory.hasPurchase(s1))
						{
							replyCommit(ApiNode.ReplyCode.SUCCESS_REQUEST, null);
						}
						else
						{
							mIabHelper.launchPurchaseFlow(mAppActivity, s1, 16661, (IABFragment)nodeOwner(), s2);
						}
					}
					else if(stMethod.equals("iab-has-purchase"))
					{
						s1 = jsRequest.getString("$sku");

						if(mInventory == null)
						{
							replyCommit(ApiNode.ReplyCode.WARNING_MESSAGE,
									"Can't check for purchase, inventory not ready!");
						}
						else
						{
							if(mInventory.hasPurchase(s1))
							{
								replyCommit(ApiNode.ReplyCode.SUCCESS_REQUEST, null);
							}
							else
							{
								replyCommit(ApiNode.ReplyCode.WARNING_NOTFOUND, null);
							}
						}
					}
				}
			});
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
		if(mAppInterface.requester().apiNode().reply() != null)
		{
			try
			{
				Log.d("IAB Request", "JSON request present, sending reply...");
				mAppInterface.requester().apiNode().reply().put("$iabrc", result.getResponse());
				mAppInterface.requester().apiNode().replyCommit(ApiNode.ReplyCode.SUCCESS_REQUEST,null);
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
