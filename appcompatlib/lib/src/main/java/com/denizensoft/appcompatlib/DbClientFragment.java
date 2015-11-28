package com.denizensoft.appcompatlib;

import android.os.Bundle;
import android.view.View;
import com.denizensoft.dbclient.DbClient;
import com.denizensoft.droidlib.Requester;
import com.denizensoft.droidlib.RequestNode;
import org.json.JSONException;
import org.json.JSONObject;

abstract public class DbClientFragment extends AppFragment
{
	protected DbClient mDbClient = null;

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// new DbClientFragment methods
	//
	protected DbClient dbc()
	{
		return mDbClient;
	}

	public void dbc(DbClient dbClient)
	{
		mDbClient = dbClient;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);

		mAppInterface.requester().addRequestNode(new RequestNode(this,"dbclient"){
			@Override
			public void invokeMethod(String stMethod, JSONObject jsRequest, JSONObject jsReply) throws JSONException
			{
				Requester.ReplyCode replyCode = Requester.ReplyCode.SUCCESS_REQUEST;

				if(mDbClient != null)
				{
					if(stMethod.equals("dropbyid"))
					{
						mDbClient.jsonDropById(jsRequest, jsReply);
					}
					else if(stMethod.equals("insertrows"))
					{
						replyCode = mDbClient.jsonInsertRows(jsRequest, jsReply);
					}
					else if(stMethod.equals("querybycolumn"))
					{
						replyCode = mDbClient.jsonQueryByColumn(jsRequest, jsReply);
					}
					else if(stMethod.equals("querybycolumn"))
					{
						replyCode = mDbClient.jsonQueryByColumn(jsRequest, jsReply);
					}
					else if(stMethod.equals("querybyid"))
					{
						replyCode = mDbClient.jsonQueryById(jsRequest, jsReply);
					}
					else if(stMethod.equals("querybyselect"))
					{
						replyCode = mDbClient.jsonQuerySelect(jsRequest, jsReply);
					}
					else if(stMethod.equals("querybysql"))
					{
						replyCode = mDbClient.jsonQuerySQL(jsRequest, jsReply);
					}
					else if(stMethod.equals("updatebyid"))
					{
						replyCode = mDbClient.jsonUpdateByRowId(jsRequest, jsReply);
					}
					else if(stMethod.equals("refresh-all-maps"))
					{
						mDbClient.refreshAllQueryMaps();
					}
					else if(stMethod.equals("stash-state-token"))
					{
						String stToken = jsRequest.getString("$token"), stValue = jsRequest.getString("$value");

						mDbClient.stashStateTokenString(stToken, stValue);
					}

					requester().commitReply(replyCode, null);
				}
			}
		});
	}

	public DbClientFragment()
	{
	}
}
