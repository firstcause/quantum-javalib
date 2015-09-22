package com.denizensoft.oshelper;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

/**
 * Created by sjm on 2/20/2015.
 */
public class MessageDlg extends Dialog implements View.OnClickListener
{
	private int nLayout = R.layout.dlg_confirm;

	private String stTitle, stMessage;

	protected TextView mMessageText;

	protected ImageButton mConfirmBtn, mCancelBtn;

	protected void confirmActionHook()
	{
		// default, do nothing...
		//
	}

	protected void cancelActionHook()
	{
		// default, do nothing...
		//
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(nLayout);						// defaults to confirm behavior

		setCanceledOnTouchOutside(false);

		mMessageText = (TextView)findViewById(R.id.messageText);
		mConfirmBtn = (ImageButton)findViewById(R.id.confirmButton);
		mCancelBtn = (ImageButton)findViewById(R.id.cancelButton);

		setTitle(stTitle);

		mMessageText.setText(stMessage);

		if(mConfirmBtn != null)
			mConfirmBtn.setOnClickListener(this);

		if(mCancelBtn != null)
			mCancelBtn.setOnClickListener(this);
	}

	@Override
	public void onClick(View view)
	{
		int i = view.getId();

		if (i == R.id.confirmButton)
		{
			confirmActionHook();

		}
		else if (i == R.id.cancelButton)
		{
			cancelActionHook();
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

	public MessageDlg(Activity activity, String stTitle, String stMessage, int nLayout)
	{
		super(activity);

		if(nLayout != 0)
			this.nLayout = nLayout;

		this.stTitle = stTitle;
		this.stMessage = stMessage;
	}
}
