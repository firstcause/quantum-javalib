package com.denizensoft.appcompatlib;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Patterns;
import com.denizensoft.dbclient.DbClient;
import com.denizensoft.droidlib.MsgTarget;
import com.denizensoft.droidlib.Requester;
import com.denizensoft.droidlib.UpdateNotifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by sjm on 1/29/2015.
 */
abstract public class AppActivity extends AppCompatActivity implements AppInterface
{
	protected String stDeviceId;

	private MsgTarget mHandler = null;

	protected UpdateNotifier mNotifier = null;

	protected Requester mRequester = new Requester();

	protected AlertDialog.Builder mMainAlertDialogBuilder = null;

	protected ArrayList<ResultListener> mResultListeners = null;

	public interface ResultListener
	{
		public boolean onActivityResultHook(int requestCode, int resultCode, Intent data);
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Android overrides
	//
	@Override
	protected void onActivityResult(int requestCode, int resultCode,Intent data)
	{
		if(mResultListeners != null)
		{
			for(ResultListener resultListener : mResultListeners)
			{
				if(resultListener.onActivityResultHook(requestCode,resultCode,data))
					return;
			}
		}

		// Nothing in this activtiy handles it...pass it on up...
		//
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		stDeviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

		mNotifier = new UpdateNotifier(mHandler);

		mMainAlertDialogBuilder = new AlertDialog.Builder(this);

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

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// new AppActivity methods
	//
	protected void doAppInvokeBusyHandler()
	{
		setProgressBarIndeterminateVisibility(true);
	}

	public Requester requester()
	{
		return mRequester;
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
}
