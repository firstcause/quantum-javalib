package com.denizensoft.droidlib;

import android.app.Activity;
import android.webkit.JsResult;
import android.webkit.WebView;

/**
 * Created by sjm on 2/20/2015.
 */
public class JsMessageDlg extends MessageDlg
{
	private WebView mWebView;

	private String stURL;

	private final JsResult mJsResult;

	@Override
	protected void cancelActionHook()
	{
		mJsResult.cancel();
	}

	@Override
	protected void confirmActionHook()
	{
		mJsResult.confirm();
	}

	public JsMessageDlg(Activity activity, int nLayout, WebView webView, String stURL, String stMessage,
						final JsResult jResult)
	{
		super(activity,webView.getTitle(),stMessage,nLayout);

		this.mWebView = webView;
		this.stURL = stURL;
		this.mJsResult = jResult;
	}
}
