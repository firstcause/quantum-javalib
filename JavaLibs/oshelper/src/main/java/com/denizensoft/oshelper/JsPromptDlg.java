package com.denizensoft.oshelper;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JsPromptResult;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

/**
 * Created by sjm on 2/20/2015.
 */
public class JsPromptDlg extends Dialog implements View.OnClickListener
{
	private int nLayout;

	protected TextView promptText;

	protected EditText editText;

	protected ImageButton confirmButton, cancelButton;

	private WebView mWebView;

	private String stURL, stPromptText, stDefaultValue;

	private final JsPromptResult mJsPromptResult;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		if(nLayout != 0)
			setContentView(nLayout);
		else
			setContentView(R.layout.dlg_jsprompt);

		promptText =		(TextView)findViewById(R.id.promptText);
		editText =			(EditText)findViewById(R.id.editText);

		confirmButton = 	(ImageButton)findViewById(R.id.confirmButton);
		cancelButton =		(ImageButton)findViewById(R.id.cancelButton);

		setTitle(mWebView.getTitle());
		promptText.setText(stPromptText);
		editText.setText(stDefaultValue);

		if(confirmButton != null)
			confirmButton.setOnClickListener(this);

		if(cancelButton != null)
			cancelButton.setOnClickListener(this);
	}

	@Override
	public void onClick(View view)
	{
		if(view.getId() == R.id.confirmButton)
		{
			mJsPromptResult.confirm();
		}
		else if(view.getId() == R.id.cancelButton)
		{
			mJsPromptResult.cancel();
		}

		dismiss();
	}

	@Override
	public void show()
	{
		WindowManager.LayoutParams lp = new WindowManager.LayoutParams();

		lp.copyFrom(getWindow().getAttributes());
		lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
		lp.height = WindowManager.LayoutParams.WRAP_CONTENT;

		super.show();

		getWindow().setAttributes(lp);
	}

	public JsPromptDlg(Activity activity, int nLayout, WebView webView, String stURL,
					   String stPrompt, String stDefaultValue, final JsPromptResult jPromptResult)
	{
		super(activity);
		this.nLayout = nLayout;
		this.mWebView = webView;
		this.stURL = stURL;
		this.stPromptText = stPrompt;
		this.stDefaultValue = stDefaultValue;
		this.mJsPromptResult = jPromptResult;
	}
}
