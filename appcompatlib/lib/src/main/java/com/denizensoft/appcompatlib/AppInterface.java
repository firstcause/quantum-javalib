package com.denizensoft.appcompatlib;

import android.content.Context;
import com.denizensoft.dbclient.DbClient;
import com.denizensoft.droidlib.Requester;
import com.denizensoft.droidlib.UpdateNotifier;

import java.util.Date;

public interface AppInterface
{
	public void appAddResultListener(AppActivity.ResultListener listener);

	public void appAlertDialog(String stTitle, String stMessage);

	public Context appContext();

	public void appDropResultListener(AppActivity.ResultListener listener);

	public void appFatalErrorHook(String stTitle, String stMessage);

	public String appGetGoogleAccountString();

	public String appGetLicenseKey();

	public boolean appIsDebugMode();

	public DbClient dbClient();
	
	public int getResourceId(String stType, String stTag);
	
	public String getStringResourceByTag(String stTag);

	public String getTagValue(String stTag);

	public Requester requester();

	public Date packageBuildTimestamp();

	public void setTagValue(String stTag, String stValue);

	public void restartActivity();

	public UpdateNotifier updateNotifier();

}
