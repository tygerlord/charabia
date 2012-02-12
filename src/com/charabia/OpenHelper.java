/*
 * Copyright (C) 2011,2012 Charabia authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.charabia;

import android.content.Context;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class OpenHelper extends SQLiteOpenHelper 
{

	public static final String DATABASE_NAME = "CHARABIA_BDD";

	private static final int DATABASE_VERSION = 3;

	public static final String ID = "_id";
	public static final String KEY = "KEY";
	public static final String PHONE = "PHONE";
	public static final String LOOKUP = "LOOKUP";
	public static final String CONTACT_ID = "CONTACT_ID";
	public static final String DATE_KEY = "DATE_KEY";
	public static final String COUNTER = "COUNTER";
	public static final String MSG_TYPE = "MSG_TYPE";
	public static final String MSG_DATE = "MSG_DATE";
	public static final String MSG_DATA = "MSG_DATA";
	public static final String MSG_TEXT = "MSG_TEXT";
	public static final String MSG_ERROR = "MSG_ERROR";
	public static final String MSG_STATUS = "MSG_STATUS";
	public static final String CONTACT_URI = "CONTACT_URI";
	public static final String MSG_PORT = "MSG_PORT";
	
	public static final String TABLE_KEYS = "TABLE_KEYS";

	//public static final String PUBLIC_KEYS = "PUBLIC_KEYS";

	public static final String MSG_TABLE = "MSG_TABLE";
	
	private static final String TABLE_KEYS_CREATE =
		"CREATE TABLE " + TABLE_KEYS + " (" +
		ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
		LOOKUP + " TEXT UNIQUE," +
		CONTACT_ID + " INTEGER," +
		PHONE + " TEXT," +
		KEY + " BLOB);";

	/*
	private static final String PUBLIC_KEYS_CREATE =
			"CREATE TABLE " + PUBLIC_KEYS + " (" +
			ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
			PHONE + " TEXT NOT NULL," +		
			DATE_KEY + " TEXT DEFAULT CURRENT_TIMESTAMP, " +
			KEY + " BLOB);";
	*/
	
	private static final String ADD_DATE_TO_TABLE_KEYS = 
		"ALTER TABLE " + TABLE_KEYS + " ADD " + DATE_KEY + " TEXT";

	private static final String ADD_COUNTER_TO_TABLE_KEYS = 
			"ALTER TABLE " + TABLE_KEYS + " ADD " + COUNTER + " INTEGER DEFAULT 0";
	
	private static final String ADD_TRIGGER_DATE =
		"CREATE TRIGGER table_keys_date AFTER UPDATE OF " + KEY + " ON " + TABLE_KEYS + "\n" + 
		"BEGIN\n" +
		"	UPDATE " + TABLE_KEYS + " SET " + DATE_KEY + " = CURRENT_TIMESTAMP WHERE " +  ID + " = OLD." + ID + ";\n" +    
		"END";
			
	private static final String MSG_TABLE_CREATE =
			"CREATE TABLE " + MSG_TABLE + " (" +
			ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
			PHONE + " TEXT," +
			CONTACT_URI + " TEXT DEFAULT NULL," + 
			COUNTER + " INTEGER DEFAULT 0," +
			MSG_DATE + " INTEGER DEFAULT 0," + 
			MSG_TYPE + " INTEGER DEFAULT 0," +
			MSG_STATUS + " INTEGER DEFAULT 0," +
			MSG_ERROR + " TEXT," +
			MSG_DATA + " BLOB," +
			MSG_PORT + " INTEGER DEFAULT 1981," +
			MSG_TEXT + " TEXT);";
 
	public OpenHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		onUpgrade(db, 1, DATABASE_VERSION);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		
		Log.v("CHARABIA", "database update from " + oldVersion + " to " + newVersion);
		
		db.beginTransaction();
		try {
			if(oldVersion <= 1) {
				db.execSQL(TABLE_KEYS_CREATE);
			}
			if(oldVersion <= 2) {
				db.execSQL(ADD_DATE_TO_TABLE_KEYS);
				db.execSQL(ADD_COUNTER_TO_TABLE_KEYS);
				db.execSQL(ADD_TRIGGER_DATE);
				db.execSQL(MSG_TABLE_CREATE);
			}
			db.setTransactionSuccessful();
		}
		finally {
			db.endTransaction();
		}
		
	}

}
