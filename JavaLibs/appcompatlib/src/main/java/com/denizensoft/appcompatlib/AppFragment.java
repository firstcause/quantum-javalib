package com.denizensoft.appcompatlib;

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.denizensoft.oshelper.MsgTarget;
import com.denizensoft.oshelper.UpdateNotifier;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Observable;
import java.util.Observer;

public class AppFragment extends Fragment implements
		Observer,
		MsgTarget.HookInterface,
		AppActivity.ResultListener
{
	protected MsgTarget mMsgTarget = null;

	protected AppActivity mAppActivity = null;

	protected AppInterface mAppInterface = null;

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Android overrides
	//
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		int nLayout = getFragmentLayout();

		if(nLayout == 0)
			throw new RuntimeException("AppFragment: No fragment_layout defined!");

		return inflater.inflate(getFragmentLayout(), container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState)
	{
		mAppActivity = (AppActivity)view.getContext();

		mAppInterface = (AppActivity)view.getContext();
	}
	
	@Override
	public void onPause()
	{
		super.onPause();

		mAppInterface.updateNotifier().deleteObserver(this);
	}

	@Override
	public void onResume()
	{
		super.onResume();

		mAppInterface.updateNotifier().addObserver(this);
	}

	@Override
	public void setUserVisibleHint(boolean isVisibleToUser)
	{
		super.setUserVisibleHint(isVisibleToUser);

		View view = getView();

		if(view != null)
		{
			if(isVisibleToUser)
				updateFragmentUI();
		}

	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Observer overrides
	//
	@Override
	public void update(Observable observable, Object o)
	{
		if(observable instanceof UpdateNotifier)
		{
			notifyContentUpdated();
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// MsgTarget.HookInterface overrides
	//
	@Override
	public void commandHook(int nCommand,Bundle args)
	{
		// default, do nothing!
	}

	public void fatalRequestErrorHook(String stFatalError)
	{
		mAppInterface.appFatalErrorHook("Request Error",stFatalError);
	}

	@Override
	public void cleanupRequestHook()
	{
	}

	@Override
	public MsgTarget messageTarget()
	{
		return mMsgTarget;
	}

	@Override
	public void notificationHook(int nNotify,Bundle args)
	{
		// default, do nothing!
	}

	@Override
	public void tokenHook(String stToken,Bundle args)
	{
		// default, do nothing!
	}

	@Override
	public boolean otherMessageHook(Message msg)
	{
		return false;
	}

	@Override
	public void invokeRequestHook(JSONObject jsonRequest, final JSONObject jsonReply)
	{
		String stAction = null;

		try
		{
			stAction = jsonRequest.getString("$action");

			if(stAction.equals("show-toast"))
			{
				String stMessage = jsonRequest.getString("$message");

				Toast.makeText(mAppInterface.appContext(), stMessage, Toast.LENGTH_LONG).show();
			}
			else if(stAction.equals("fatal-error"))
			{
				mAppInterface.appFatalErrorHook("Fatal Termination Request",jsonRequest.getString("$error"));
			}
			else if(stAction.equals("restart-activity"))
			{
				mAppInterface.restartActivity();
			}
			else
			{
				messageTarget().sendReply(MsgTarget.ReplyCode.CRITICAL_ERROR,"No handler for this action: "+stAction);
			}
		}
		catch(JSONException e)
		{
			mAppInterface.appFatalErrorHook("AppFragment",
					String.format("Action: %s Message: %s", stAction, e.getMessage()));
		}

		messageTarget().sendReply(MsgTarget.ReplyCode.SUCCESS_REQUEST,null);
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// AppActivity ResultListener overrides
	//
	@Override
	public boolean onActivityResultHook(int requestCode, int resultCode, Intent data)
	{
		return false;
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// new AppFragment methods
	//
	public Populator allocatePopulator(String stParameter)
	{
		return null;
	}

	public boolean doOnBackPressed()
	{
		// default don't use...
		//
		return false;
	}

	protected int getFragmentLayout() throws RuntimeException
	{
		Bundle args = getArguments();

		if(args != null && args.containsKey("fragment_layout"))
		{
			int nLayout = args.getInt("fragment_layout");
			
			return nLayout;
		}
		return 0;
	}

	public void notifyContentUpdated()
	{
		// Default, do nothing...
	}

	protected void updateFragmentUI()
	{
		// Default, do nothing...
	}

	public AppFragment()
	{
		mMsgTarget = new MsgTarget(this);
	}

}
