package com.denizensoft.droidlib;

import android.app.Dialog;

/**
 * Created by sjm on 3/21/2015.
 */
public interface ApiDialogListener
{
	void onConfirm(Dialog dlg, ApiContext apiContext);

	void onCancel(Dialog dlg, ApiContext apiContext);
}
