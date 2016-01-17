package com.denizensoft.appcompatlib;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Patterns;
import android.widget.Toast;
import com.denizensoft.dbclient.DbClient;
import com.denizensoft.droidlib.*;
import com.denizensoft.jlib.FatalException;
import com.denizensoft.jlib.LibException;
import org.json.JSONException;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by sjm on 1/29/2015.
 */
abstract public class AppActivity extends AppCompatActivity implements AppInterface, UncaughtExceptionHandler
{
	protected String stDeviceId;

	protected UpdateNotifier mNotifier = null;

	private Requester mRequester = new Requester(this){
			@Override
			public Context getContext()
			{
				return (AppActivity)owner();
			}
		};


	protected ArrayList<ResultListener> mResultListeners = null;

	protected AlertDialog.Builder mMainAlertDialogBuilder = null;

	@Override
	public void uncaughtException(Thread thread, Throwable ex)
	{
		Log.d("Runtime Exception",ex.getMessage());

		appAlertDialog("Runtime Exception",ex.getMessage());
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Android overrides
	//
	@Override
	public void onActivityResult(int requestCode, int resultCode,Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);

		if(mResultListeners != null)
		{
			for(ResultListener resultListener : mResultListeners)
			{
				if(resultListener.onActivityResultHook(requestCode,resultCode,data))
					return;
			}
		}

	}

	@Override
	protected void onPostCreate(@Nullable Bundle savedInstanceState)
	{
		super.onPostCreate(savedInstanceState);

		requester().attachApiNode("AppFragment",new ApiNode(this)
				.attachApiMethod("showToast", new ApiMethod()
				{
					@Override
					public void func(ApiContext ac) throws JSONException, LibException
					{
						String stMessage = null;
						try
						{
							stMessage = ac.request().getString("$message");

							Toast.makeText((Activity)ac.node().nodeOwner(), stMessage, Toast.LENGTH_LONG).show();

							ac.replySuccessComplete(null);
						}
						catch(JSONException e)
						{
							throw new HandlerException(String.format("JSON exception during: %s",e.getMessage()));
						}
					}
				})
				.attachApiMethod("restartActivity", new ApiMethod()
				{
					@Override
					public void func(ApiContext ac) throws JSONException, LibException
					{
						restartActivity();
						ac.replySuccessComplete(null);
					}
				}));

	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		stDeviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

		mNotifier = new UpdateNotifier(mRequester);

		mMainAlertDialogBuilder = new AlertDialog.Builder(this);

		Thread.currentThread().setUncaughtExceptionHandler(this);

		super.onCreate(savedInstanceState);
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// AppInterface overrides
	//
	@Override
	public void appAlertDialog(String stTitle, String stMessage)
	{
		mMainAlertDialogBuilder.setTitle(stTitle);
		mMainAlertDialogBuilder.setMessage(stMessage);
		mMainAlertDialogBuilder.setPositiveButton(getString(android.R.string.ok),
				new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialogInterface, int i)
					{
						dialogInterface.dismiss();
					}
				});

		mMainAlertDialogBuilder.show();
	}

	@Override
	abstract public String appGetLicenseKey();

	@Override
	public void appAddResultListener(ResultListener listener)
	{
		if(mResultListeners == null)
			mResultListeners = new ArrayList<ResultListener>();

		mResultListeners.add(listener);
	}

	@Override
	public void appAddApiClassResultListener(String stApiSpec)
	{
		ApiNode node = requester().getApiRef(stApiSpec);

		if(node != null)
		{
			appAddResultListener(node);
		}
		else
		{
			throw new FatalException(String.format("AppActivity: couldn't add result listener, class not found: %s",
					stApiSpec));
		}
	}

	@Override
	public void appDropResultListener(ResultListener listener)
	{
		if(mResultListeners != null)
			mResultListeners.remove(listener);
	}

	@Override
	public Context appContext()
	{
		return this;
	}

	@Override
	public void appFatalErrorHook(String stTitle, String stMessage)
	{
		Log.e("Fatal",stMessage);

		mMainAlertDialogBuilder.setTitle("Fatal Error:" + stTitle);
		mMainAlertDialogBuilder.setMessage(stMessage);
		mMainAlertDialogBuilder.setPositiveButton(getString(android.R.string.ok),
				new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialogInterface, int i)
					{
						finish();
					}
				});

		mMainAlertDialogBuilder.show();
	}

	@Override
	public String appGetGoogleAccountString()
	{
		Pattern emailPattern = Patterns.EMAIL_ADDRESS;

		Account[] accounts = AccountManager.get(this).getAccounts();

		for (int i=0; i < accounts.length; ++i)
		{
			Account a1 = accounts[i];

			if(emailPattern.matcher(a1.name).matches())
			{
				String
						possibleEmail = a1.name,
						stType = a1.type;

				Log.d("Superhero", String.format("Found possible account %d: %s type: %s", i, possibleEmail, stType));

				if(stType.equals("com.google"))
				{
					return possibleEmail;
				}
			}
		}
		return null;
	}

	@Override
	public DbClient dbClient()
	{
		return null;
	}

	@Override
	public int getResourceId(String stType, String stTag)
	{
		String packageName = getPackageName();

		return getResources().getIdentifier(stTag, stType, packageName);
	}

	@Override
	public String getStringResourceByTag(String stTag)
	{
		int nid = getResourceId("string",stTag);

		if(nid == 0)
			return stTag;

		return getString(nid);
	}

	@Override
	public String getTagValue(String stTag)
	{
		return null;
	}

	@Override
	public Requester requester()
	{
		return mRequester;
	}

	@Override
	public Date packageBuildTimestamp()
	{
		try
		{
			ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), 0);

			ZipFile zf = new ZipFile(ai.sourceDir);

			ZipEntry ze = zf.getEntry("classes.dex");

			return new Date(ze.getTime());
		}
		catch(PackageManager.NameNotFoundException e)
		{
			appFatalErrorHook("App",e.getMessage());
		}
		catch(IOException e)
		{
			appFatalErrorHook("App",e.getMessage());
		}
		return null;
	}

	@Override
	public void setTagValue(String stTag, String stValue)
	{

	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// new AppActivity methods
	//
	protected void doAppInvokeBusyHandler()
	{
		setProgressBarIndeterminateVisibility(true);
	}

	public void invokeFragment(int nFrameContainer, Fragment fragment)
	{
		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

		transaction.replace(nFrameContainer, fragment);

		transaction.commit();
	}

	public void restartActivity()
	{
		Intent i = getBaseContext().getPackageManager().getLaunchIntentForPackage(getPackageName());

		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

		startActivity(i);
	}

	public UpdateNotifier updateNotifier()
	{
		return mNotifier;
	}

	public AppActivity()
	{
		requester().attachApiNode("AppActivity",new ApiNode(this)
			.attachApiMethod("addApiResultListener", new ApiMethod()
			{
				@Override
				public void func(ApiContext ac) throws JSONException, LibException
				{
					String stApiSpec = ac.request().getJSONArray("$args").getString(0);

					Log.d("AppActivity", String.format("Adding API result listener: %s",stApiSpec));

					ApiNode node = requester().getApiRef(stApiSpec);

					appAddResultListener(node);

					ac.replySuccessComplete(null);
				}
			})
			.attachApiMethod("dropApiResultListener", new ApiMethod()
			{
				@Override
				public void func(ApiContext ac) throws JSONException, LibException
				{
					String stApiSpec = ac.request().getJSONArray("$args").getString(0);

					Log.d("AppActivity", String.format("Dropping result listener: %s",stApiSpec));

					ApiNode node = requester().getApiRef(stApiSpec);

					appDropResultListener(node);

					ac.replySuccessComplete(null);
				}
			})
		);
	}
}
