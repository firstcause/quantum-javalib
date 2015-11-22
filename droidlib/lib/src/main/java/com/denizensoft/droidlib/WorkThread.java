package com.denizensoft.droidlib;

import android.os.Handler;
import android.os.Looper;

import java.util.Observable;

/**
 * Created by sjm on 3/21/2015.
 */
public class WorkThread extends Thread
{
	private Handler mHandler = null;

	public Handler handler()
	{
		return mHandler;
	}

	public Looper looper()
	{
		return Looper.myLooper();
	}

	public void quitLooper() throws InterruptedException
	{
		if(this.isAlive())
		{
			if(getId() != Thread.currentThread().getId())
			{
				mHandler.post(new Runnable()
				{
					@Override
					public void run()
					{
						mHandler.getLooper().quit();
					}
				});

				join();
			}
			else
			{
				mHandler.getLooper().quit();
			}
		}
	}

	public void run()
	{
		Looper.prepare();

		Looper.myLooper();

		mHandler = new Handler();

		Looper.loop();

		mHandler = null;
	}

	WorkThread()
	{
		super("WorkLooper");
	}
}
