package com.denizensoft.applib;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.denizensoft.jlib.Tempus;
import com.denizensoft.oshelper.JsAlertDlg;
import com.denizensoft.oshelper.JsApiInterface;
import com.denizensoft.oshelper.JsConfirmDlg;
import com.denizensoft.oshelper.JsPromptDlg;
import com.denizensoft.oshelper.MsgTarget;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.TimeZone;

public class WebAppFragment extends DbClientFragment implements JsApiInterface
{
	protected class JsPopupWebViewChrome extends WebChromeClient
	{
		@Override
		public boolean onJsAlert(WebView view, String url, String message, JsResult result)
		{
			Dialog dialog = new JsAlertDlg((AppActivity)getActivity(),view,url,message,result);

			dialog.show();

			// Indicate that we're handling this manually
			return true;
		}

		@Override
		public boolean onJsConfirm(WebView view, String url, String message, JsResult result)
		{
			Dialog dialog = new JsConfirmDlg((AppActivity)getActivity(),view,url,message,result);

			dialog.show();

			// Indicate that we're handling this manually
			return true;
		}

		@Override
		public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result)
		{
			Dialog dialog = new JsPromptDlg((AppActivity)getActivity(),0,view,url,message,defaultValue,result);

			dialog.show();

			return true;
		}
	}

	protected WebView mWebView = null;

	protected String stHtmlFolder = null;

	///////////////////////////////////////////////////////////////////////////////
	// Android overrides
	//
	@Override
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);

		if(BuildConfig.DEBUG)
		{
			// stHtmlFolder = "file:///android_asset/html";
			stHtmlFolder = "http://huxley.quarknet/superhero";
		}
		else
		{
			stHtmlFolder = String.format("file://%s/html",activity.getFilesDir().getPath());
		}
	}

	///////////////////////////////////////////////////////////////////////////////
	// javascript api section start
	//
	@JavascriptInterface
	public String jsCalcAddStampMinutes(String stTimeStamp, String stMinutes)
	{
		return Tempus.calcAddStampMinutes(stTimeStamp, stMinutes);
	}

	@JavascriptInterface
	public String jsCalcElapsedHours(String stStampAfter, String stStampBefore)
	{
		return Tempus.calcElapsedHours(stStampAfter, stStampBefore);
	}

	@JavascriptInterface
	public String jsGetValue(String stTag)
	{
		return mAppInterface.getTagValue(stTag);
	}

	@JavascriptInterface
	public String jsFormatStamp(String stUtcStamp, String stFormat, String stTimeZone)
	{
		if(stUtcStamp == null)
			stUtcStamp = Tempus.utcStamp(null);

		if(stTimeZone == null)
			stTimeZone = TimeZone.getDefault().getID();

		if(stFormat == null)
			stFormat = Tempus.TIMEAMPM;

		return Tempus.formatUtcStamp(stUtcStamp, stFormat, TimeZone.getTimeZone(stTimeZone));
	}

	@JavascriptInterface
	public String jsJsonRequest(String stJSON)
	{
		String stReply = null;

		Log.d("jsJsonRequest", "Sending request, with JSON: " + stJSON);

		JSONObject jsReply = messageTarget().sendRequest(stJSON);

		if(jsReply != null)
		{
			stReply = jsReply.toString();

			Log.d("jsJsonRequest", "Request complete, reply: "+stReply);
		}
		else
		{
			Log.d("jsJsonRequest", "Request cancelled!");
		}
		return stReply;
	}

	@JavascriptInterface
	public void jsPutValue(String stTag, String stValue)
	{
		mAppInterface.setTagValue(stTag,stValue);
	}

	//
	// JsAppFragment section end
	///////////////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// AppFragment overrides
	//
	@Override
	public Populator allocatePopulator(String stParameter)
	{
		return new Populator(stParameter)
		{
			@Override
			public void run()
			{
				String stUrlSpec = String.format("%s/%s",stHtmlFolder,parameterString());

				webView().loadUrl(stUrlSpec);
			}

		};
	}

	@Override
	public boolean doOnBackPressed()
	{
		if(mWebView.canGoBack())
		{
			mWebView.goBack();
			return true;
		}
		return false;
	}

	@Override
	protected int getFragmentLayout()
	{
		Bundle bundle = getArguments();

		if(bundle != null && bundle.containsKey("fragment_layout"))
			return getArguments().getInt("fragment_layout");

		return R.layout.fragment_webapp;
	}

	@Override
	protected void initializeFragmentView(View view)
	{
		super.initializeFragmentView(view);

		mWebView = (WebView)view.findViewById(R.id.webView);

		mWebView.clearCache(true);
		mWebView.setWebViewClient(new WebViewClient());
		mWebView.setWebChromeClient(new JsPopupWebViewChrome());

		WebSettings webSettings = mWebView.getSettings();

		webSettings.setJavaScriptEnabled(true);
		webSettings.setLoadWithOverviewMode(true);
		webSettings.setUseWideViewPort(true);
		webSettings.setBuiltInZoomControls(false);
		webSettings.setSupportZoom(true);
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// MsgTarget.HookInterface overrides
	//
	@Override
	public void invokeRequestHook(JSONObject jsonRequest, final JSONObject jsonReply)
	{
		String stAction = null;

		try
		{
			stAction = jsonRequest.getString("$action");

			if(stAction.equals("load-page"))
			{
				String stPageSpec = String.format("%s/%s",stHtmlFolder,jsonRequest.getString("$pagespec"));

				webView().loadUrl(stPageSpec);
			}
			else
			{
				// not ours, try the super...
				//
				super.invokeRequestHook(jsonRequest, jsonReply);
			}
		}
		catch(JSONException e)
		{
			mAppInterface.appFatalErrorHook("WebApp",
					String.format("JSON Exception: Action: %s Message: %s", stAction, e.getMessage()));
		}

		messageTarget().sendReply(MsgTarget.ReplyCode.SUCCESS_REQUEST, null);
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// new WebAppFragment methods
	//
	public WebView webView()
	{
		return mWebView;
	}

	public WebAppFragment()
	{
	}
}
