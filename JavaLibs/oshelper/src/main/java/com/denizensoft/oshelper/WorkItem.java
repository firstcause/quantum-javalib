package com.denizensoft.oshelper;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * Created by sjm on 3/21/2015.
 */
abstract public class WorkItem implements Runnable
{
	private Looper mLooper = null;

	private Thread mThread = null;

	private Handler mHandler = null;

	private class WorkThread extends Thread
	{
		private WorkItem mWorkItem = null;

		@Override
		public void run()
		{
			super.run();

			Looper.prepare();

			mLooper = Looper.myLooper();

			mHandler = new Handler(mLooper);

			mHandler.postDelayed(mWorkItem, 3000);

			Log.d("WorkThread","Loop starts...");

			Looper.loop();

			Log.d("WorkThread","Loop exited...");

		}

		WorkThread(WorkItem workItem)
		{
			this.mWorkItem = workItem;
		}
	}

	abstract protected void doWork();

	@Override
	public void run()
	{
		doWork();

		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				mLooper.quit();

				mHandler = null;

				mLooper = null;
			}
		});
	}

	public void execute()
	{
		mThread = new WorkThread(this);

		mThread.start();
	}

	public WorkItem()
	{
	}
}
