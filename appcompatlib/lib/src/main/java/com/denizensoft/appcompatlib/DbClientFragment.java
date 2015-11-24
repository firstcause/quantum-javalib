package com.denizensoft.appcompatlib;

import com.denizensoft.dbclient.DbClient;
import com.denizensoft.droidlib.Requester;
import com.denizensoft.droidlib.TargetNode;
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

	public DbClientFragment()
	{
		requester().addTargetNode(new TargetNode("dbclient"){
			@Override
			public void invokeRequest(String stAction, JSONObject jsRequest, JSONObject jsReply) throws JSONException
			{
				Requester.ReplyCode replyCode = Requester.ReplyCode.SUCCESS_REQUEST;

				if(mDbClient != null)
				{
					if(stAction.equals("dropbyid"))
					{
						mDbClient.jsonDropById(jsRequest, jsReply);
					}
					else if(stAction.equals("insertrows"))
					{
						replyCode = mDbClient.jsonInsertRows(jsRequest, jsReply);
					}
					else if(stAction.equals("querybycolumn"))
					{
						replyCode = mDbClient.jsonQueryByColumn(jsRequest, jsReply);
					}
					else if(stAction.equals("querybycolumn"))
					{
						replyCode = mDbClient.jsonQueryByColumn(jsRequest, jsReply);
					}
					else if(stAction.equals("querybyid"))
					{
						replyCode = mDbClient.jsonQueryById(jsRequest, jsReply);
					}
					else if(stAction.equals("querybyselect"))
					{
						replyCode = mDbClient.jsonQuerySelect(jsRequest, jsReply);
					}
					else if(stAction.equals("querybysql"))
					{
						replyCode = mDbClient.jsonQuerySQL(jsRequest, jsReply);
					}
					else if(stAction.equals("updatebyid"))
					{
						replyCode = mDbClient.jsonUpdateByRowId(jsRequest, jsReply);
					}
					else if(stAction.equals("refresh-all-maps"))
					{
						mDbClient.refreshAllQueryMaps();
					}
					else if(stAction.equals("stash-state-token"))
					{
						String stToken = jsRequest.getString("$token"), stValue = jsRequest.getString("$value");

						mDbClient.stashStateTokenString(stToken, stValue);
					}

					requester().commitReply(replyCode, null);
				}
			}
		});
	}
}
