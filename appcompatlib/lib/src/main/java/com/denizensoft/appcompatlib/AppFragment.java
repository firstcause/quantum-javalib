package com.denizensoft.appcompatlib;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.denizensoft.droidlib.*;
import com.denizensoft.jlib.LibException;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Observable;
import java.util.Observer;

public class AppFragment extends Fragment implements Observer,ResultListener
{
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

		mAppInterface.requester().attachApiNode("AppFragment",new ApiNode(this){

			@Override
			public void builtins(String stMethod, JSONObject jsRequest, JSONObject jsReply) throws JSONException, LibException
			{
				switch(stMethod.substring(1))
				{
					case "show-toast" :
					{
						String stMessage = null;
						try
						{
							stMessage = jsRequest.getString("$message");

							Toast.makeText(mAppInterface.appContext(), stMessage, Toast.LENGTH_LONG).show();

							replySuccessComplete(null);
						}
						catch(JSONException e)
						{
							throw new HandlerException(String.format("JSON exception during: %s",e.getMessage()));
						}

					}
					break;

					case "restart-activity" :
					{
						mAppInterface.restartActivity();

						replySuccessComplete(null);
					}
					break;

				}
			}
		});
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
	}
}
