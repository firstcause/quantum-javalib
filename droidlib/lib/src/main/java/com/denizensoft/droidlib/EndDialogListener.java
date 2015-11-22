package com.denizensoft.droidlib;

import android.app.Dialog;
import android.os.Bundle;

/**
 * Created by sjm on 3/21/2015.
 */
public interface EndDialogListener
{
	void onConfirm(Dialog dlg, Bundle args);

	void onCancel(Dialog dlg, Bundle args);
}
