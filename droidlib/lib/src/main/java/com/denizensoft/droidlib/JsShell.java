package com.denizensoft.droidlib;

import android.util.Log;

import java.io.InputStream;

/**
 * Created by sjm on 1/16/16.
 */
public class JsShell
{
	public interface OsApi
	{
		public String appName();

		public InputStream openInputStream(String stFileSpec);

		public void closeInputStream(InputStream inputStream);

		public Requester hostTarget();
	}

	final private OsApi mOsApi;

	public Requester hostTarget()
	{
		return mOsApi.hostTarget();
	}

	public int loadXML(String stFileSpec)
	{
		XmlLoader loader = new XmlLoader();

		loader.loadDocument(mOsApi.openInputStream(stFileSpec), mOsApi.hostTarget());

		return 0;
	}

	public OsApi osApi()
	{
		return mOsApi;
	}

	public void mssleep(int nIntervalMillis)
	{
		try
		{
			logInfo(String.format("Sleeping for: %s milliseconds...", nIntervalMillis));
			Thread.sleep(nIntervalMillis, 0);
		}
		catch(InterruptedException e)
		{
			logWarning("Thread sleep was interrupted!");
		}
	}

	public void logDebug(String stLogEntry)
	{
		Log.d(mOsApi.appName(), stLogEntry);
	}

	public void logError(String stLogEntry)
	{
		Log.e(mOsApi.appName(), stLogEntry);
	}

	public void logWarning(String stLogEntry)
	{
		Log.w(mOsApi.appName(), stLogEntry);
	}

	public void logInfo(String stLogEntry)
	{
		Log.i(mOsApi.appName(), stLogEntry);
	}

	public void postNotification(String stNotifySpec)
	{
		mOsApi.hostTarget().sendToken(stNotifySpec, null);
	}

	public String jsJsonRequest(String stJsRequest)
	{
		return this.mOsApi.hostTarget().jsJsonRequest(stJsRequest);
	}

	public JsShell(OsApi osApi)
	{
		this.mOsApi = osApi;
	}
}
