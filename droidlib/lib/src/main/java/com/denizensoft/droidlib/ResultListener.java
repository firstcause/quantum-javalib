package com.denizensoft.droidlib;

import android.content.Intent;

public interface ResultListener
	{
		public boolean onActivityResultHook(int requestCode, int resultCode, Intent data);
	}

