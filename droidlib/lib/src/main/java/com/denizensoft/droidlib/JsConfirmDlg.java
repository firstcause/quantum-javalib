package com.denizensoft.droidlib;

import android.app.Activity;
import android.webkit.JsResult;
import android.webkit.WebView;

/**
 * Created by sjm on 9/1/15.
 */
public class JsConfirmDlg extends JsMessageDlg
{
	public JsConfirmDlg(Activity activity, WebView webView, String stURL, String stMessage, JsResult jResult)
	{
		super(activity, R.layout.dlg_confirm, webView, stURL, stMessage, jResult);
	}
}
