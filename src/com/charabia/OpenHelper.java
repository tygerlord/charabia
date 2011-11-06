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

import android.util.Base64;

import android.content.Context;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class OpenHelper extends SQLiteOpenHelper 
{

	public static final String DATABASE_NAME = "CHARABIA_BDD";

	public static final String ID = "_id";
	public static final String SMS_PDU = "SMS_PDU";
	public static final String SMS_LOOKUP_KEY = "SMS_LOOKUP_KEY";
	public static final String SMS_CONTACT_ID = "SMS_CONTACT_ID";
	public static final String SMS_TABLE = "SMS_TABLE";
	
	private static final int DATABASE_VERSION = 2;

	private static final String SMS_TABLE_CREATE =
				"CREATE TABLE " + SMS_TABLE + " (" +
				ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
				SMS_PDU + " BLOB," +
				SMS_LOOKUP_KEY + " TEXT," +
				SMS_CONTACT_ID + " LONG" +
				SMS_PDU + " BLOB);";

	public OpenHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(SMS_TABLE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		//TODO: keep sms
		//db.execSQL("DROP TABLE IF EXISTS " + SMS_TABLE);
		//onCreate(db);
		if(oldVersion == 1 && newVersion == 2) {
			db.execSQL("DROP TABLE IF EXISTS " + SMS_TABLE);
			onCreate(db);
		}
	}

	
	@Deprecated
	public void deletePdu(SQLiteDatabase db, int id) {
		db.execSQL("DELETE FROM " + SMS_TABLE + " WHERE " + ID + "=" + id);
	}

	@Deprecated
	public void change(SQLiteDatabase db, int id, String name, byte[] key) {
	}

	@Deprecated
	public void insertPdu(SQLiteDatabase db, byte[] pdu) {
		db.execSQL("INSERT INTO " + SMS_TABLE + 
				"(" + SMS_PDU + ")" +
				" VALUES ('" + Base64.encodeToString(pdu, Base64.DEFAULT) + "')");
	}

	@Deprecated
	private byte[] readPdu(SQLiteDatabase db, String desc) {
		byte[] ret = null;
		Cursor cursor = db.rawQuery("SELECT * FROM " + SMS_TABLE + " ORDER BY " + ID + desc + " LIMIT 1", null);
		if(cursor.moveToFirst()) {
			ret = Base64.decode(cursor.getString(cursor.getColumnIndex(SMS_PDU)), Base64.DEFAULT);
		}
		cursor.close();
		return ret;
	}

	@Deprecated
	public byte[] readPdu(SQLiteDatabase db) {
		return readPdu(db, "");
	}

	@Deprecated
	public byte[] readLastPdu(SQLiteDatabase db) {
		return readPdu(db, " DESC");
	}
	
	@Deprecated
	public void deletePdu(SQLiteDatabase db) {
		Cursor cursor = db.rawQuery("SELECT " + ID + " FROM " + SMS_TABLE + " ORDER BY " + ID  + " LIMIT 1", null);
		if(cursor.moveToFirst()) {
			deletePdu(db, cursor.getInt(cursor.getColumnIndex(ID)));
		}
		cursor.close();
	}

}
