package com.denizensoft.dbclient;

import com.denizensoft.droidlib.*;
import com.denizensoft.jlib.FatalException;
import com.denizensoft.jlib.LibException;
import org.json.JSONException;

/**
 * Created by sjm on 12/27/15.
 */
public class DbClientApi extends ApiInvoker
{
	protected DbClient mDBC = null;

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// new DbClientApi methods
	//
	protected DbClient dbc()
	{
		return mDBC;
	}

	public void dbc(DbClient dbc)
	{
		mDBC = dbc;
	}

	@Override
	protected void taskFunc() throws JSONException
	{
		requester().attachApiNode("dbClient",new ApiNode(this)
				.attachApiMethod("dropById", new ApiMethod()
				{
					@Override
					public void func(ApiContext ac) throws JSONException, LibException
					{
						mDBC.jsonDropById(ac.request(), ac.reply());
						ac.replySuccessComplete(null);
					}
				})
				.attachApiMethod("insertRows", new ApiMethod()
				{
					@Override
					public void func(ApiContext ac) throws JSONException, LibException
					{
						ApiContext.ReplyCode rc = mDBC.jsonInsertRows(ac.request(), ac.reply());
						ac.replyCommit(rc,null);
					}
				})
				.attachApiMethod("queryByColumn", new ApiMethod()
				{
					@Override
					public void func(ApiContext ac) throws JSONException, LibException
					{
						ApiContext.ReplyCode rc = mDBC.jsonQueryByColumn(ac.request(), ac.reply());
						ac.replyCommit(rc,null);
					}
				})
				.attachApiMethod("queryById", new ApiMethod()
				{
					@Override
					public void func(ApiContext ac) throws JSONException, LibException
					{
						ApiContext.ReplyCode rc = mDBC.jsonQueryById(ac.request(), ac.reply());
						ac.replyCommit(rc,null);
					}
				})
				.attachApiMethod("queryBySelect", new ApiMethod()
				{
					@Override
					public void func(ApiContext ac) throws JSONException, LibException
					{
						ApiContext.ReplyCode rc = mDBC.jsonQuerySelect(ac.request(), ac.reply());
						ac.replyCommit(rc,null);
					}
				})
				.attachApiMethod("querySQL", new ApiMethod()
				{
					@Override
					public void func(ApiContext ac) throws JSONException, LibException
					{
						ApiContext.ReplyCode rc = mDBC.jsonQuerySQL(ac.request(), ac.reply());
						ac.replyCommit(rc,null);
					}
				})
				.attachApiMethod("updateById", new ApiMethod()
				{
					@Override
					public void func(ApiContext ac) throws JSONException, LibException
					{
						ApiContext.ReplyCode rc = mDBC.jsonUpdateByRowId(ac.request(), ac.reply());
						ac.replyCommit(rc,null);
					}
				})
				.attachApiMethod("refreshAllMaps", new ApiMethod()
				{
					@Override
					public void func(ApiContext ac) throws JSONException, LibException
					{
						mDBC.refreshAllQueryMaps();
						ac.replyCommit(ApiContext.ReplyCode.SUCCESS_REQUEST,null);
					}
				})
				.attachApiMethod("stashStateToken", new ApiMethod()
				{
					@Override
					public void func(ApiContext ac) throws JSONException, LibException
					{
						try
						{
							String
									stToken = ac.request().getString("$token"),
									stValue = ac.request().getString("$value");

							mDBC.stashStateTokenString(stToken, stValue);
						}
						catch(JSONException e)
						{
							throw new FatalException("JSON exception in DbClientFragemnt");
						}
					}
				})
		);
	}

	public DbClientApi(Requester requester)
	{
		super(requester);
	}
}
