package com.denizensoft.appcompatlib;

import android.os.Bundle;
import android.view.View;
import com.denizensoft.dbclient.DbClient;
import com.denizensoft.droidlib.ApiContext;
import com.denizensoft.droidlib.ApiMethod;
import com.denizensoft.droidlib.ApiNode;
import com.denizensoft.jlib.FatalException;
import com.denizensoft.jlib.LibException;
import org.json.JSONException;

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

		mAppInterface.requester().attachApiNode("dbc",new ApiNode(this)
				.attachApiMethod("dropById", new ApiMethod()
					{
						@Override
						public void func(ApiContext ac) throws JSONException, LibException
						{
							mDbClient.jsonDropById(ac.request(), ac.reply());
							ac.replySuccessComplete(null);
						}
					})
				.attachApiMethod("insertRows", new ApiMethod()
					{
						@Override
						public void func(ApiContext ac) throws JSONException, LibException
						{
							ApiContext.ReplyCode rc = mDbClient.jsonInsertRows(ac.request(), ac.reply());
							ac.replyCommit(rc,null);
						}
					})
				.attachApiMethod("queryByColumn", new ApiMethod()
					{
						@Override
						public void func(ApiContext ac) throws JSONException, LibException
						{
							ApiContext.ReplyCode rc = mDbClient.jsonQueryByColumn(ac.request(), ac.reply());
							ac.replyCommit(rc,null);
						}
					})
				.attachApiMethod("queryById", new ApiMethod()
					{
						@Override
						public void func(ApiContext ac) throws JSONException, LibException
						{
							ApiContext.ReplyCode rc = mDbClient.jsonQueryById(ac.request(), ac.reply());
							ac.replyCommit(rc,null);
						}
					})
				.attachApiMethod("queryBySelect", new ApiMethod()
					{
						@Override
						public void func(ApiContext ac) throws JSONException, LibException
						{
							ApiContext.ReplyCode rc = mDbClient.jsonQuerySelect(ac.request(), ac.reply());
							ac.replyCommit(rc,null);
						}
					})
				.attachApiMethod("querySQL", new ApiMethod()
					{
						@Override
						public void func(ApiContext ac) throws JSONException, LibException
						{
							ApiContext.ReplyCode rc = mDbClient.jsonQuerySQL(ac.request(), ac.reply());
							ac.replyCommit(rc,null);
						}
					})
				.attachApiMethod("updateById", new ApiMethod()
					{
						@Override
						public void func(ApiContext ac) throws JSONException, LibException
						{
							ApiContext.ReplyCode rc = mDbClient.jsonUpdateByRowId(ac.request(), ac.reply());
							ac.replyCommit(rc,null);
						}
					})
				.attachApiMethod("refreshAllMaps", new ApiMethod()
					{
						@Override
						public void func(ApiContext ac) throws JSONException, LibException
						{
							mDbClient.refreshAllQueryMaps();
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

								mDbClient.stashStateTokenString(stToken, stValue);
							}
							catch(JSONException e)
							{
								throw new FatalException("JSON exception in DbClientFragemnt");
							}
						}
					})
		);
	}

	public DbClientFragment()
	{
	}
}
