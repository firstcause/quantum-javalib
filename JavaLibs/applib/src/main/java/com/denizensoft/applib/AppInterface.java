package com.denizensoft.applib;

import android.content.Context;

import com.denizensoft.dbclient.DbClient;
import com.denizensoft.oshelper.MsgTarget;
import com.denizensoft.oshelper.UpdateNotifier;

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

	public String getTagValue(String stTag);

	public Date packageBuildTimestamp();

	public void setTagValue(String stTag,String stValue);

	public MsgTarget messageTarget();

	public void restartActivity();

	public UpdateNotifier updateNotifier();

}
