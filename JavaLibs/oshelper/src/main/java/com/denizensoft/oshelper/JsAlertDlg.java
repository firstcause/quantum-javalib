package com.denizensoft.oshelper;

import android.app.Activity;
import android.webkit.JsResult;
import android.webkit.WebView;

/**
 * Created by sjm on 9/1/15.
 */
public class JsAlertDlg extends JsMessageDlg
{
	public JsAlertDlg(Activity activity, WebView webView, String stURL, String stMessage, JsResult jResult)
	{
		super(activity, R.layout.dlg_alert, webView, stURL, stMessage, jResult);
	}
}
