package com.denizensoft.dbclient;

import android.annotation.SuppressLint;
import android.content.ContentQueryMap;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.denizensoft.jlib.Tempus;
import com.denizensoft.droidlib.Crypter;
import com.denizensoft.droidlib.MsgTarget;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by sjm on 11/6/2014.
 */

@SuppressLint("UseSparseArrays")

public class DbClient
{
	@SuppressLint("DefaultLocale")

	public interface OnUpdateListener
	{
		public void onMapUpdate(String stMapTag);

		public void onMapDropped(String stMapTag);
	}

	private OnUpdateListener mUpdateListener = null;

	private HashMap<String,ContentQueryMap> mQueryMaps = new HashMap<String, ContentQueryMap>();

	private HashMap<Integer,TreeMap<String,ContentValues>> mTreeMaps = new HashMap<Integer,TreeMap<String, ContentValues>>();

	protected String stGoogleAccount = null;

	protected Crypter mCrypter = null;

	protected SQLiteOpenHelper mHelper = null;

	public void dropAllQueryMaps()
	{
		for(Map.Entry<String,ContentQueryMap> entry: mQueryMaps.entrySet())
			dropQueryMap(entry.getKey());
	}

	public void dropQueryMap(String stMapTag)
	{
		ContentQueryMap queryMap = mQueryMaps.get(stMapTag);

		if(queryMap != null)
		{
			if(mTreeMaps.containsKey(queryMap.hashCode()))
				mTreeMaps.remove(queryMap.hashCode());

			mQueryMaps.remove(stMapTag);

			if(mUpdateListener != null)
				mUpdateListener.onMapDropped(stMapTag);
		}
	}

	public void dropRecordAt(String stTableName,long nRowId) throws DbException
	{
		String[] stArgs = new String[]{ Long.toString(nRowId) };

		int rc = writableDB().delete(stTableName, "_id = ?", stArgs);

		if(rc == 0)
		{
			throw new DbNotFoundException(
					String.format("Record id: %d, in table: %s not deleted, or does not exist", nRowId, stTableName));
		}

		refreshQueryMap(stTableName);
	}

	public void dropStateToken(String stToken) throws DbException
	{
		String[] stArgs = new String[]{ stToken };

		int rc = writableDB().delete(StateToken.TABLE_NAME, "TokenKey = ?", stArgs);

		Log.i("StateToken",String.format("Got result: %d, while dropping: %s",rc,stToken));

		refreshQueryMap(StateToken.TABLE_NAME);
	}

	public void jsonDropById(JSONObject jsonRequest,final JSONObject jsonReply) throws DbException, JSONException
	{
		String
				stTableName = jsonRequest.getString("$tableName"),
				stRowId = jsonRequest.getString("$rowId");

		dropRecordAt(stTableName, Long.parseLong(stRowId));
	}

	public JSONArray jsonMakeRowsetArray(Cursor cursor, int nMaxRows) throws JSONException
	{
		int i = 0, nCount = cursor.getCount();

		String stColumn, stValue;

		JSONArray jsRowset = new JSONArray();

		if(nCount > nMaxRows)
			nCount = nMaxRows;

		for(i=0; i < nCount; ++i)
		{
			JSONObject jsonRow = new JSONObject();

			cursor.moveToPosition(i);

			for(int j = 0; j < cursor.getColumnCount(); ++j)
			{
				stColumn = cursor.getColumnName(j);
				stValue = cursor.getString(j);

				Log.d("DBC",String.format("Column: %s Value: %s",stColumn,stValue));

				if(stValue != null)
					jsonRow.put(stColumn,stValue);
				else
					jsonRow.put(stColumn,JSONObject.NULL);
			}

			jsRowset.put(jsonRow);
		}
		return jsRowset;
	}

	public JSONArray jsonMakeRowsetArray(Map<String,ContentValues> rowMap, int nMaxRows) throws JSONException
	{
		int nCount = 0;

		String stValue;

		JSONArray jsRowset = new JSONArray();

		for(Map.Entry<String,ContentValues> row : rowMap.entrySet())
		{
			JSONObject jsonRow = new JSONObject();

			jsonRow.put("$rowId",row.getKey());

			ContentValues values = row.getValue();

			for(String stKey : values.keySet())
			{
				stValue = values.getAsString(stKey);

				if(stValue != null)
					jsonRow.put(stKey,stValue);
				else
					jsonRow.put(stKey,JSONObject.NULL);
			}

			jsRowset.put(jsonRow);

			if(++nCount >= nMaxRows)
				break;
		}
		return jsRowset;
	}

	public MsgTarget.ReplyCode jsonInsertRows(JSONObject jsonRequest,final JSONObject jsonReply) throws DbException, JSONException
	{
		String stColumn, stValue, stTableName = jsonRequest.getString("$tableName");

		JSONArray jsRowset = jsonRequest.getJSONArray("$rowset");

		ContentValues values = new ContentValues();

		for(int i = 0; i < jsRowset.length(); ++i)
		{
			values.clear();

			JSONObject jsRow = jsRowset.getJSONObject(i);

			Iterator<String> iterator = jsRow.keys();

			while(iterator.hasNext())
			{
				stColumn = iterator.next();
				stValue = jsRow.getString(stColumn);

				if(stValue != null && stValue != JSONObject.NULL)
					values.put(stColumn, stValue);
			}

			long nRowId = writableDB().insert(stTableName,null,values);

			if(nRowId <= 0)
				throw new DbException("JSON: Couldn't insert row!");
		}

		refreshAllQueryMaps();

		return MsgTarget.ReplyCode.SUCCESS_REQUEST;
	}

	public MsgTarget.ReplyCode jsonQueryByColumn(JSONObject jsonRequest,final JSONObject jsonReply) throws DbException, JSONException
	{
		String
				stTableName = jsonRequest.getString("$tableName"),
				stColumn = jsonRequest.getString("$column"),
				stValue = jsonRequest.getString("$value");

		TreeMap<String,ContentValues> treeMap;

		try
		{
			treeMap = queryValuesByColumn(stTableName, stColumn, stValue);
		}
		catch(DbNotFoundException e)
		{
			return MsgTarget.ReplyCode.WARNING_NOTFOUND;
		}

		int nMaxRows = 20;

		if(jsonRequest.has("$maxrows"))
			nMaxRows = jsonRequest.getInt("$maxrows");

		JSONArray jsRowset = jsonMakeRowsetArray(treeMap, nMaxRows);

		jsonReply.put("$rowset", jsRowset);

		return MsgTarget.ReplyCode.SUCCESS_REQUEST;
	}

	public MsgTarget.ReplyCode jsonQueryById(JSONObject jsonRequest,final JSONObject jsonReply) throws DbException, JSONException
	{
		String
				stTableName = jsonRequest.getString("$tableName"),
				stRowId = jsonRequest.getString("$rowId"), stValue;

		TreeMap<String,ContentValues> treeMap = null;

		treeMap = getTreeMap(stTableName);

		ContentValues values = treeMap.get(stRowId);

		if(values == null)
		{
			jsonReply.put("$warning","QUERY: Row not found: "+stRowId);

			return MsgTarget.ReplyCode.WARNING_NOTFOUND;
		}

		jsonReply.put("$tableName", stTableName);
		jsonReply.put("$rowset",new JSONArray());

		jsonReply.getJSONArray("$rowset").put(new JSONObject());

		JSONObject jsonRow = jsonReply.getJSONArray("$rowset").getJSONObject(0);

		jsonRow.put("$rowId",stRowId);

		for(String stKey : values.keySet())
		{
			stValue = values.getAsString(stKey);

			if(stValue != null)
				jsonRow.put(stKey, stValue);
			else
				jsonRow.put(stKey, JSONObject.NULL);
		}
		return MsgTarget.ReplyCode.SUCCESS_REQUEST;
	}

	public MsgTarget.ReplyCode jsonQuerySelect(JSONObject jsonRequest,final JSONObject jsonReply) throws DbException, JSONException
	{
		String
				stTableName = jsonRequest.getString("$tableName"),

				stSelect = jsonRequest.getString("$select"),		// not optional!!
				stIndexKey = ( jsonRequest.has("$indexkey") ? jsonRequest.getString("$indexkey") : "_id" ),
				stGroupBy = ( jsonRequest.has("$groupby") ? jsonRequest.getString("$groupby") : null ),
				stOrderBy = ( jsonRequest.has("$orderby") ? jsonRequest.getString("$orderby") : null );

		String[] stArgs = null;

		if(jsonRequest.has("$args"))
		{
			Log.d("DBC", "Args found in request...");

			JSONArray jsonArgsArray = jsonRequest.getJSONArray("$args");

			if(jsonArgsArray != null)
			{
				stArgs = new String[jsonArgsArray.length()];

				for(int i = 0; i < jsonArgsArray.length(); ++i)
					stArgs[i] = jsonArgsArray.getString(i);
			}
		}

		Log.d("DBC", "Order by: "+stOrderBy);

		Cursor cursor = readableDB().query(stTableName, null, stSelect, stArgs, stGroupBy, null, stOrderBy);

		if(cursor.getCount() == 0)
			return MsgTarget.ReplyCode.WARNING_NOTFOUND;

		int nMaxRows = 20;

		if(jsonRequest.has("$maxrows"))
			nMaxRows = jsonRequest.getInt("$maxrows");

		JSONArray jsRowset = null;

		if(stGroupBy == null && stOrderBy == null)
		{
			ContentQueryMap queryMap = new ContentQueryMap(cursor,stIndexKey,false,null);

			jsRowset = jsonMakeRowsetArray(queryMap.getRows(), nMaxRows);

			queryMap.close();
			queryMap = null;
		}
		else
		{
			jsRowset = jsonMakeRowsetArray(cursor, nMaxRows);
		}

		jsonReply.put("$tableName",stTableName);
		jsonReply.put("$rowset", jsRowset);

		cursor.close();
		cursor = null;

		return MsgTarget.ReplyCode.SUCCESS_REQUEST;
	}

	public MsgTarget.ReplyCode jsonQuerySQL(JSONObject jsonRequest,final JSONObject jsonReply) throws DbException, JSONException
	{
		String stSQL = jsonRequest.getString("$sql");

		String[] stArgs = null;

		if(jsonRequest.has("$args"))
		{
			Log.d("DBC", "Args found in request...");

			JSONArray jsonArgsArray = jsonRequest.getJSONArray("$args");

			if(jsonArgsArray != null)
			{
				stArgs = new String[jsonArgsArray.length()];

				for(int i = 0; i < jsonArgsArray.length(); ++i)
					stArgs[i] = jsonArgsArray.getString(i);
			}
		}

		Log.d("DBC", "Raw SQL: " + stSQL);

		Cursor cursor = readableDB().rawQuery(stSQL, stArgs);

		if(cursor.getCount() == 0)
			return MsgTarget.ReplyCode.WARNING_NOTFOUND;

		cursor.moveToFirst();

		int nMaxRows = 20;

		if(jsonRequest.has("$maxrows"))
			nMaxRows = jsonRequest.getInt("$maxrows");

		JSONArray jsRowset = jsonMakeRowsetArray(cursor, nMaxRows);

		jsonReply.put("$tableName", "$sql");
		jsonReply.put("$rowset", jsRowset);

		cursor.close();

		return MsgTarget.ReplyCode.SUCCESS_REQUEST;
	}

	public MsgTarget.ReplyCode jsonUpdateByRowId(JSONObject jsonRequest, JSONObject jsonReply) throws DbException, JSONException
	{
		String stRowId = null, stColumn, stValue, stTableName = jsonRequest.getString("$tableName");

		JSONArray jsRowset = jsonRequest.getJSONArray("$rowset");

		ContentValues values = new ContentValues();

		for(int i = 0; i < jsRowset.length(); ++i)
		{
			values.clear();

			JSONObject jsRow = jsRowset.getJSONObject(i);

			Iterator<String> iterator = jsRow.keys();

			while(iterator.hasNext())
			{
				stColumn = iterator.next();
				stValue = jsRow.getString(stColumn);

				if(stColumn.equals("$rowId"))
				{
					stRowId = stValue;
				}
				else if(stValue != null && stValue != JSONObject.NULL)
				{
					values.put(stColumn, stValue);
				}
			}

			updateValuesAtRowId(stTableName, Long.parseLong(stRowId), values);
		}
		return MsgTarget.ReplyCode.SUCCESS_REQUEST;
	}

	public ContentQueryMap execQuerySpec(String stTableName,String stMapKey,JSONObject jsQueryArgs) throws DbException
	{
		String stQuerySpec = String.format("%s:%s:%s", stTableName, stMapKey, jsQueryArgs.toString());

		return getQueryMap(stQuerySpec);
	}

	public ContentQueryMap getQueryMap(String stMapTag) throws DbException
	{
		ContentQueryMap queryMap = mQueryMaps.get(stMapTag);

		if(queryMap == null)
		{
			queryMap = queryMapNotFoundHook(stMapTag);

			mQueryMaps.put(stMapTag, queryMap);
		}
		return queryMap;
	}

	public TreeMap<String,ContentValues> getTreeMap(String stMapTag) throws DbException
	{
		ContentQueryMap queryMap = getQueryMap(stMapTag);

		TreeMap<String,ContentValues> treeMap = mTreeMaps.get(queryMap.hashCode());

		if(treeMap == null)
		{
			treeMap = new TreeMap<String,ContentValues>(queryMap.getRows());

			mTreeMaps.put(queryMap.hashCode(),treeMap);
		}
		return treeMap;
	}

	public void refreshAllQueryMaps()
	{
		Log.d("DbClient","Refreshing all query maps...");

		for(Map.Entry<String,ContentQueryMap> entry: mQueryMaps.entrySet())
			refreshQueryMap(entry.getKey());
	}

	public void refreshQueryMap(String stMapTag)
	{
		Log.d("DbClient",String.format("Refreshing query map, tag: %s",stMapTag));

		ContentQueryMap queryMap = mQueryMaps.get(stMapTag);

		if(queryMap != null)
		{
			Log.d("DbClient","Removing existing tree map!");

			if(mTreeMaps.containsKey(queryMap.hashCode()))
				mTreeMaps.remove(queryMap.hashCode());

			Log.d("DbClient","Re-querying!");

			queryMap.requery();

			if(mUpdateListener !=null)
				mUpdateListener.onMapUpdate(stMapTag);
		}
	}

	protected ContentQueryMap queryMapNotFoundHook(String stMapTag) throws DbException
	{
		String[] stArgs = null;

		String stSQL, stWhere = null;

		ContentQueryMap queryMap = null;

		if(stMapTag == StateToken.TABLE_NAME)
		{
			queryMap = queryMapFromRawSQL(StateToken.Row.FIELD_KEY, "SELECT * FROM StateToken", null);
		}
		else
		{
			// Last ditch try the stMapTag as a table name and '_id' as the row key...
			//
			stSQL = String.format("select * from %s", stMapTag);

			queryMap = queryMapFromRawSQL("_id", stSQL, null);
		}
		return queryMap;
	}

	public String[] queryKeysByColumn(String stMapTag, String stColumn, String stValue)
			throws DbException
	{
		ArrayList<String> keys = null;

		for(Map.Entry<String, ContentValues> row : getQueryMap(stMapTag).getRows().entrySet())
		{
			if(row.getValue().getAsString(stColumn).equals(stValue))
			{
				if(keys == null)
					keys = new ArrayList<String>();

				keys.add(row.getKey());
			}
		}

		if(keys == null)
		{
			throw new DbNotFoundException(String.format("No keys found! Map: %s Column: %s Match: %s",
					stMapTag,stColumn,stValue));
		}
		return keys.toArray(new String[keys.size()]);
	}

	public TreeMap<String, List<ContentValues>> querySortOnColumn(String stMapTag, String stColumn)
			throws DbException
	{
		String stValue;

		TreeMap<String, List<ContentValues>> treeMap = null;

		for(Map.Entry<String, ContentValues> row : getQueryMap(stMapTag).getRows().entrySet())
		{
			stValue = row.getValue().getAsString(stColumn);

			if(!treeMap.containsKey(stValue))
				treeMap.put(stValue, new ArrayList<ContentValues>());

			treeMap.get(stValue).add(row.getValue());
		}
		return treeMap;
	}

	public TreeMap<String, ContentValues> queryValuesByColumn(String stMapTag, String stColumn, String stValue)
			throws DbException
	{
		return queryValuesByColumn(getQueryMap(stMapTag),stColumn,stValue);
	}

	public TreeMap<String, ContentValues> queryValuesByColumn(ContentQueryMap queryMap, String stColumn, String stValue)
			throws DbException
	{
		TreeMap<String, ContentValues> treeMap = null;

		Map<String,ContentValues> rowMap = queryMap.getRows();

		for(Map.Entry<String, ContentValues> row : rowMap.entrySet())
		{
			if(row.getValue().getAsString(stColumn).equals(stValue))
			{
				if(treeMap == null)
					treeMap = new TreeMap<String, ContentValues>();

				treeMap.put(row.getKey(), row.getValue());
			}
		}

		if(treeMap == null)
			throw new DbNotFoundException(String.format("No matches found, Column: %s Match: %s",stColumn,stValue));

		return treeMap;
	}

	public TreeMap<String, ContentValues> queryTreeByColumn(String stMapTag,String stColumn,String stValue)
			throws DbException
	{
		// Differs from queryValuesByColumn, keep for reference
		// here we use getTreeMap vs getQueryMap
		//
		TreeMap<String, ContentValues>
				treeMap1 = getTreeMap(stMapTag),
				treeMap2 = null;

		for(Map.Entry<String, ContentValues> row : treeMap1.entrySet())
		{
			if(row.getValue().getAsString(stColumn).equals(stValue))
			{
				if(treeMap2 == null)
					treeMap2 = new TreeMap<String, ContentValues>();

				treeMap2.put(row.getKey(), row.getValue());
			}
		}

		if(treeMap2 == null)
		{
			throw new DbNotFoundException(String.format("No records found! Tree: %s Column: %s Match: %s",
					stMapTag,stColumn,stValue));
		}
		return treeMap2;
	}

	protected ContentQueryMap queryMapFromRawSQL(String stRowKey, String stSQL, String[] stSelectArgs)
			throws DbException
	{
		ContentQueryMap queryMap = null;

		@SuppressLint("Recyle")
		Cursor cursor = readableDB().rawQuery(stSQL, stSelectArgs);

		if(cursor == null || cursor.getCount() <= 0)
			throw new DbNotFoundException(String.format("DbHandler: no records found: %s",stSQL));

		queryMap = new ContentQueryMap(cursor,stRowKey,true,null);

		// cursor.close();

/* Debugging the integer to string key sort order issue

		TreeMap<String,ContentValues> treeMap = new TreeMap<String, ContentValues>(queryMap.getRows());

		String stKey = treeMap.lastEntry().getKey();

		ContentValues values = treeMap.lastEntry().getValue();

		String s1 = values.getAsString("type");

		Log.d("RawMap", "Type: "+s1);
*/
		return queryMap;
	}

	public long queryStateTokenInteger(String stToken) throws DbException
	{
		String stValue = queryStateTokenString(stToken);

		return Long.parseLong(stValue);
	}

	public String queryCryptTokenString(String stToken) throws DbException
	{
		String stValue = queryStateTokenString(stToken);

		return mCrypter.decrypt(stGoogleAccount, stValue);
	}

	public String queryStateTokenString(String stToken) throws DbException
	{
		ContentQueryMap queryMap = getQueryMap(StateToken.TABLE_NAME);

		ContentValues row = queryMap.getValues(stToken);

		if(row == null)
			throw new DbNotFoundException(String.format("DbHandler: state token not found: %s",stToken));

		return row.getAsString("TokenValue");
	}

	public Cursor queryWithRawSQL(String stSQL, String[] stSelectArgs) throws DbException
	{
		Cursor cursor;

		cursor = readableDB().rawQuery(stSQL, stSelectArgs);

		if(cursor.getCount() == 0)
			throw new DbNotFoundException("No records found: " + stSQL + " : " + stSelectArgs.toString());

		cursor.moveToPosition(0);

		return cursor;
	}

	public SQLiteDatabase readableDB()
	{
		SQLiteDatabase db = mHelper.getReadableDatabase();

		return db;
	}

	public String toRowId(long nId)
	{
		return String.format(Locale.US,"%012d",nId);
	}

	protected TreeMap<String, ContentValues> sortEntriesByString(ContentQueryMap queryMap,
																 Comparator<String> comparator, String stColumn)
	{
		TreeMap<String, ContentValues> treeMap = new TreeMap<String, ContentValues>(comparator);

		for(Map.Entry<String, ContentValues> row : queryMap.getRows().entrySet())
		{
			String s1 = row.getValue().getAsString(stColumn);

			treeMap.put(s1, row.getValue());
		}
		return treeMap;
	}

	protected TreeMap<Long, ContentValues> sortEntriesByLong(ContentQueryMap queryMap,
															   Comparator<Long> comparator, String stColumn)
	{
		TreeMap<Long, ContentValues> treeMap = new TreeMap<Long, ContentValues>(comparator);

		for(Map.Entry<String, ContentValues> row : queryMap.getRows().entrySet())
		{
			Long n1 = row.getValue().getAsLong(stColumn);

			treeMap.put(n1, row.getValue());
		}
		return treeMap;
	}

	public void stashCryptTokenString(String stToken, String stValue) throws DbException
	{
		String stEncryptedToken = mCrypter.encrypt(stGoogleAccount, stValue);

		stashStateTokenString(stToken, stEncryptedToken,null);
	}

	public void stashCryptTokenString(String stToken, String stValue, String stCategory) throws DbException
	{
		String stEncryptedToken = mCrypter.encrypt(stGoogleAccount, stValue);

		stashStateTokenString(stToken, stEncryptedToken,stCategory);
	}

	public void stashStateTokenString(String stToken, String stValue) throws DbException
	{
		stashStateTokenString(stToken, stValue, null);
	}

	public void stashStateTokenString(String stToken, String stValue, String stCategory) throws DbException
	{
		int rc;

		ContentQueryMap queryMap = getQueryMap(StateToken.TABLE_NAME);

		ContentValues values = queryMap.getRows().get(stToken);

		if(values != null)
		{
			String[] stSelectArgs = { String.format("%s",stToken) };

			values.remove(StateToken.Row._ID);
			values.put(StateToken.Row.FIELD_UPDATESTAMP, Tempus.utcStamp(null));
			values.put(StateToken.Row.FIELD_VALUE,stValue);

			if(stCategory != null)
				values.put(StateToken.Row.FIELD_CATEGORY,stCategory);

			rc = writableDB().update(StateToken.TABLE_NAME,values,"TokenKey = ?",stSelectArgs);

			if(rc <= 0)
				throw new DbException("Couldn't update state token: "+stToken);
		}
		else
		{
			values = new ContentValues();

			Log.i("StashToken",String.format("Token: %s",stToken));
			Log.i("StashToken",String.format("Value: %s",stValue));

			values.put(StateToken.Row.FIELD_KEY, stToken);
			values.put(StateToken.Row.FIELD_VALUE, stValue);

			if(stCategory != null)
			{
				Log.i("StashToken",String.format("Category: %s",stCategory));
				values.put(StateToken.Row.FIELD_CATEGORY, stCategory);
			}
			else
			{
				Log.i("StashToken","Category is null, defaults to 'Token'");
			}

			long nRowId = writableDB().insert(StateToken.TABLE_NAME,null,values);

			if(nRowId <= 0)
				throw new DbException("Couldn't insert state token: "+stToken);
		}

		refreshQueryMap(StateToken.TABLE_NAME);
	}

	public void updateFieldAtRowId(String stTableName,long nRowId,String stField, String stValue) throws DbException
	{
		ContentValues values = new ContentValues();

		values.put(stField, stValue);

		updateValuesAtRowId(stTableName, nRowId, values);
	}

	public void updateValuesAtRowId(String stTableName,long nRowId,ContentValues values) throws DbException
	{
		int rc;

		String[] stSelectArgs = { Long.toString(nRowId) };

		rc = writableDB().update(stTableName,values,"_id = ?",stSelectArgs);

		if(rc <= 0)
			throw new DbException(String.format("Error during content update? Table: %s Row: %d",stTableName,nRowId));

		refreshAllQueryMaps();
	}

	public SQLiteDatabase writableDB()
	{
		return mHelper.getWritableDatabase();
	}

	public DbClient(SQLiteOpenHelper helper, String stGoogleAccount, Crypter crypter, OnUpdateListener updateListener)
	{
		this.mHelper = helper;
		this.mCrypter = crypter;
		this.stGoogleAccount = stGoogleAccount;
		this.mUpdateListener = updateListener;
	}
}
