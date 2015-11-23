package com.denizensoft.appcompatlib;

import com.denizensoft.dbclient.DbClient;
import com.denizensoft.dbclient.DbException;
import com.denizensoft.droidlib.MsgTarget;
import org.json.JSONException;
import org.json.JSONObject;

abstract public class DbClientFragment extends AppFragment
{
	protected DbClient mDbClient = null;

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Android callbacks section
	//

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// MsgTarget.HookInterface section
	//
	@Override
	public void invokeRequestHook(JSONObject jsonRequest, final JSONObject jsonReply)
	{
		int nRC;

		String stAction = null;

		try
		{
			stAction = jsonRequest.getString("$action");

			MsgTarget.ReplyCode replyCode = MsgTarget.ReplyCode.SUCCESS_REQUEST;

			if(mDbClient != null)
			{
				if(stAction.equals("dropbyid"))
				{
					mDbClient.jsonDropById(jsonRequest, jsonReply);
				}
				else if(stAction.equals("insertrows"))
				{
					replyCode = mDbClient.jsonInsertRows(jsonRequest, jsonReply);
				}
				else if(stAction.equals("querybycolumn"))
				{
					replyCode = mDbClient.jsonQueryByColumn(jsonRequest, jsonReply);
				}
				else if(stAction.equals("querybycolumn"))
				{
					replyCode = mDbClient.jsonQueryByColumn(jsonRequest, jsonReply);
				}
				else if(stAction.equals("querybyid"))
				{
					replyCode = mDbClient.jsonQueryById(jsonRequest, jsonReply);
				}
				else if(stAction.equals("querybyselect"))
				{
					replyCode = mDbClient.jsonQuerySelect(jsonRequest, jsonReply);
				}
				else if(stAction.equals("querybysql"))
				{
					replyCode = mDbClient.jsonQuerySQL(jsonRequest, jsonReply);
				}
				else if(stAction.equals("updatebyid"))
				{
					replyCode = mDbClient.jsonUpdateByRowId(jsonRequest, jsonReply);
				}
				else if(stAction.equals("refresh-all-maps"))
				{
					mDbClient.refreshAllQueryMaps();
				}
				else if(stAction.equals("stash-state-token"))
				{
					String stToken = jsonRequest.getString("$token"), stValue = jsonRequest.getString("$value");

					mDbClient.stashStateTokenString(stToken, stValue);
				}
				else
				{
					// not ours, try the super...
					//
					super.invokeRequestHook(jsonRequest, jsonReply);
				}

				messageTarget().sendReply(replyCode,null);
			}
			else
			{
				// can't handle even if ours, so pass it to the super...
				//
				super.invokeRequestHook(jsonRequest, jsonReply);
			}
		}
		catch(DbException e)
		{
			mAppInterface.appFatalErrorHook("Db Exception",
					String.format("Action: %s Message: %s", stAction, e.getMessage()));
		}
		catch(JSONException e)
		{
			mAppInterface.appFatalErrorHook("JSON Exception",
					String.format("Action: %s Message: %s", stAction, e.getMessage()));
		}
	}

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
		requester().addTargetNode();
	}
}
