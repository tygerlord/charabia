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
	
	private OpenHelper oh = null;
	
	private SQLiteDatabase db = null;
	
	private Cursor cursor = null;
	
	private static final String TAG = "CHARABIA_MY_SMS_MANAGER";
	
	public static final String sms_dirname = "messages";
	
	public MySmsManager(Context context) {
		this.context = context;
		oh = new OpenHelper(this.context);
		db = oh.getWritableDatabase();
		cursor = db.rawQuery("SELECT * FROM "+ OpenHelper.SMS_TABLE, null);
	}
	
	public void closeAll() {
		if(cursor != null) {
			cursor.close();
		}
		if(db != null) {
			db.close();
		}
	}
	
	public void writeSMS(SmsMessage message) {
		oh.insert(db, message);
	}

	public SmsMessage readSMS() {
		Log.v(TAG, "readSMS");
		
		return SmsMessage.createFromPdu(
				Base64.decode(cursor.getString(cursor.getColumnIndex(OpenHelper.SMS_PDU)), Base64.DEFAULT));
	}

	public SmsMessage getNextMessage() {
		Log.v(TAG, "getNextMessage");
		if(cursor.moveToNext()) {
			return readSMS();
		}
		return null;
	}

	public SmsMessage getLastMessage() {
		Log.v(TAG, "getLastMessage");
		if(cursor.moveToLast()) {
			return readSMS();
		}
		return null;
	}

	public SmsMessage getFirstMessage() {
		Log.v(TAG, "getFirstMessage");
		if(cursor.moveToFirst()) {
			return readSMS();
		}
		return null;
	}

	public void removeSMS() {
		oh.deleteSMS(db, cursor.getInt(cursor.getColumnIndex(OpenHelper.ID)));
	}
	
	public int getNbMessages() {
		//cursor = db.rawQuery("SELECT COUNT(*) FROM "+ OpenHelper.SMS_TABLE, null);
		
		return cursor.getCount();
	}
	
	public boolean moveToNext() {
		return cursor.moveToNext();
	}

	public boolean moveToFirst() {
		return cursor.moveToNext();
	}

	
}
