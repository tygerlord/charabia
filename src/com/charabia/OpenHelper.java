/*
 * Copyright (C) 2011 Charabia authors
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

public class OpenHelper extends SQLiteOpenHelper 
{

	public static final String DATABASE_NAME = "CHARABIA_BDD";

	public static final String ID = "_id";
	public static final String KEY = "KEY";
	public static final String PHONE = "PHONE";
	public static final String LOOKUP = "LOOKUP";
	public static final String CONTACT_ID = "CONTACT_ID";
	
	public static final String TABLE_KEYS = "TABLE_KEYS";
	
	private static final int DATABASE_VERSION = 1;

	private static final String TABLE_KEYS_CREATE =
				"CREATE TABLE " + TABLE_KEYS + " (" +
				ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
				LOOKUP + " TEXT UNIQUE," +
				CONTACT_ID + " INTEGER," +
				PHONE + " TEXT," +
				KEY + " BLOB);";

	public OpenHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(TABLE_KEYS_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Example
		//db.execSQL("DROP TABLE IF EXISTS " + TABLE_KEYS);
		//onCreate(db);
		
	}

}
