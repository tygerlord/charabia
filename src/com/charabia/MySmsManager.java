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


import android.util.Log;

import android.telephony.SmsMessage;

import android.content.Context;

import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;

/**
 * @author Fleblanc
 *
 */
public class MySmsManager
{
	private Context context = null;
	
	private static final String TAG = "CHARABIA_MY_SMS_MANAGER";
	
	public MySmsManager(Context context) {
		this.context = context;
	}
	
	public void writeSMS(SmsMessage message) {
		Log.v(TAG, "writeSMS");
		OpenHelper oh = new OpenHelper(context);
		SQLiteDatabase db = oh.getWritableDatabase();
		oh.insertPdu(db, message.getPdu());
		db.close();
	}

	public SmsMessage readSMS() {
		Log.v(TAG, "readSMS");

		OpenHelper oh = new OpenHelper(context);
		SQLiteDatabase db = oh.getWritableDatabase();
		SmsMessage sms = SmsMessage.createFromPdu(oh.readPdu(db));
		db.close();
		return sms;
	}

	public SmsMessage getLastMessage() {
		Log.v(TAG, "getLastMessage");

		OpenHelper oh = new OpenHelper(context);
		SQLiteDatabase db = oh.getWritableDatabase();
		SmsMessage sms = SmsMessage.createFromPdu(oh.readLastPdu(db));
		db.close();
		return sms;
	}

	public void removeSMS() {
		OpenHelper oh = new OpenHelper(context);
		SQLiteDatabase db = oh.getWritableDatabase();

		oh.deletePdu(db);
		
		db.close();
	}
	
	public int getNbMessages() {
		OpenHelper oh = new OpenHelper(context);
		SQLiteDatabase db = oh.getWritableDatabase();

		int count = -1;
		Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM "+ OpenHelper.SMS_TABLE, null);
		if(cursor.moveToFirst()) {
			count = cursor.getInt(0);
		}
		
		cursor.close();
		db.close();
		
		return count;
	}
	
}
