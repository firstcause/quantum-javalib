package com.denizensoft.dbclient;

/**
 * Created by sjm on 11/12/2014.
 */

import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

public final class StateToken
{
	public static final String TABLE_NAME = "StateToken";

	public static final String CREATE_TABLE =
		"CREATE TABLE " + TABLE_NAME + " (" +
			Row._ID 					+ " INTEGER PRIMARY KEY," +
			Row.FIELD_UPDATESTAMP		+ " TEXT DEFAULT CURRENT_TIMESTAMP,"+
			Row.FIELD_KEY				+ " TEXT NOT NULL UNIQUE,"+
			Row.FIELD_VALUE				+ " TEXT," +
			Row.FIELD_CATEGORY			+ " TEXT NOT NULL DEFAULT 'Token'" +
		" )";



	public static final String DELETE_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;

	public static abstract class Row implements BaseColumns
	{
		public static final String FIELD_UPDATESTAMP	= "UpdateStamp";

		public static final String FIELD_KEY 			= "TokenKey";

		public static final String FIELD_VALUE			= "TokenValue";

		public static final String FIELD_CATEGORY		= "Category";

	}

	public static void onCreate(SQLiteDatabase db)
	{
		db.execSQL(CREATE_TABLE);
		db.execSQL("CREATE INDEX StateTokenKey on StateToken(TokenKey)");
	}

	public static void onUpgrade(SQLiteDatabase db, int nOldVersion, int nNewVersion )
	{
	}
}
