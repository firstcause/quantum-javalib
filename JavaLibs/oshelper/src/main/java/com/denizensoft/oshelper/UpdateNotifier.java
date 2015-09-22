package com.denizensoft.oshelper;

import android.os.Handler;

import java.util.Observable;

/**
 * Created by sjm on 2/17/2015.
 */
public class UpdateNotifier extends Observable
{
	private Handler mHandler;

	public void triggerUpdate()
	{
		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				setChanged();
				notifyObservers();
			}
		});
	}

	public UpdateNotifier(Handler handler)
	{
		this.mHandler = handler;
	}
}
