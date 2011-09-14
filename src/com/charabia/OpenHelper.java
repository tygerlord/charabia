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

import android.telephony.SmsMessage;
import android.util.Base64;

import android.content.Context;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.sql.Blob;

public class OpenHelper extends SQLiteOpenHelper 
{

	public static final String DATABASE_NAME = "CHARABIA_BDD";

	public static final String ID = "_id";
	public static final String PHONE = "PHONE";
	public static final String KEY = "KEY";
	public static final String SMS_PDU = "SMS_PDU";
	public static final String KEYS_TABLE = "KEYS_TABLE";
	public static final String SMS_TABLE = "SMS_TABLE";

	private static final int DATABASE_VERSION = 1;

	private static final String KEYS_TABLE_CREATE =
								"CREATE TABLE " + KEYS_TABLE + " (" +
								ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
								PHONE + " TEXT, " +
								KEY + " TEXT);";

	private static final String SMS_TABLE_CREATE =
				"CREATE TABLE " + SMS_TABLE + " (" +
				ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
				SMS_PDU + " TEXT);";

	public OpenHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(KEYS_TABLE_CREATE);
		db.execSQL(SMS_TABLE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		//TODO: change to keep KEYS on recreate
		db.execSQL("DROP TABLE IF EXISTS " + KEYS_TABLE);
		db.execSQL("DROP TABLE IF EXISTS " + SMS_TABLE);
		onCreate(db);
	}

	public void insert(SQLiteDatabase db, String phone, byte[] key) {
		db.execSQL("INSERT INTO " + KEYS_TABLE + " " +
				"(" + PHONE + "," + KEY + ") " + 
				"VALUES ('" + phone + "','" + Base64.encodeToString(key, Base64.DEFAULT) + "')");
	}

	public void delete(SQLiteDatabase db, int id) {
		db.execSQL("DELETE FROM " + KEYS_TABLE + " WHERE " + ID + "=" + id);
	}

	public void deleteSMS(SQLiteDatabase db, int id) {
		db.execSQL("DELETE FROM " + SMS_TABLE + " WHERE " + ID + "=" + id);
	}

	public void change(SQLiteDatabase db, int id, String name, byte[] key) {
	}

	public void insert(SQLiteDatabase db, SmsMessage sms) {
		db.execSQL("INSERT INTO " + SMS_TABLE + " " +
				"(" + SMS_PDU + ") " + 
				"VALUES ('" + Base64.encodeToString(sms.getPdu(), Base64.DEFAULT) + "')");
	}

	public byte[] get(SQLiteDatabase db, int id) {
		Cursor cursor = db.rawQuery("SELECT " + KEY + " FROM " + KEYS_TABLE + " WHERE " + ID + "=?" , new String[] { Integer.toString(id) } );
		if(cursor.moveToFirst()) {
			return Base64.decode(cursor.getString(cursor.getColumnIndex(KEY)), Base64.DEFAULT);
		}
		return null;
	}

	public SmsMessage getSMS(SQLiteDatabase db, int id) {
		Cursor cursor = db.rawQuery("SELECT " + SMS_PDU + " FROM " + SMS_TABLE + " WHERE " + ID + "=?" , new String[] { Integer.toString(id) } );
		if(cursor.moveToFirst()) {
			return SmsMessage.createFromPdu(Base64.decode(cursor.getString(cursor.getColumnIndex(SMS_PDU)), Base64.DEFAULT));
		}
		return null;
	}

}
