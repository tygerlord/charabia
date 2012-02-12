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

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.telephony.SmsManager;
import android.util.Log;

public class SendResultReceiver extends BroadcastReceiver 
{
	public static final String ACTION_RESULT_SMS  = "com.charabia.intent.SMS_SENT";

	@Override
	public void onReceive(Context context, Intent intent)
	{
		if (intent.getAction().equals(ACTION_RESULT_SMS)) {
			
            String info = "Send information: ";
            
            int resultCode = getResultCode();
            
            Uri uri = intent.getParcelableExtra("MSG_URI");
            Log.v("CHARABIA", "SendResultReceiver + " + uri);
            
            ContentResolver cr = context.getContentResolver();
            
            ContentValues values = new ContentValues();
            
            values.put(OpenHelper.MSG_DATE, System.currentTimeMillis());
            
            switch(resultCode)
            {
                    case Activity.RESULT_OK: 
                    	info += "send successful";
                    	values.put(OpenHelper.MSG_STATUS, Tools.MESSAGE_SEND);
                    	cr.update(uri, values, null, null);
                    	return;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE: info += "send failed, generic failure"; break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE: info += "send failed, no service"; break;
                    case SmsManager.RESULT_ERROR_NULL_PDU: info += "send failed, null pdu"; break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF: info += "send failed, radio is off"; break;
            }
            
            Log.v("CHARABIA", info);

            values.put(OpenHelper.MSG_ERROR, info);
            values.put(OpenHelper.MSG_STATUS, resultCode);
            
            cr.update(uri, values, null, null);
            
            Cursor cursor = cr.query(uri, 
            		new String[] { OpenHelper.PHONE, OpenHelper.MSG_DATA, OpenHelper.MSG_PORT },
            		null, null, null);
            
            String phoneNumber = null;
            short sms_port = 0;
            byte[] data = null;
            
            if(cursor.moveToFirst()) {
                phoneNumber = cursor.getString(cursor.getColumnIndex(OpenHelper.PHONE));
                sms_port = cursor.getShort(cursor.getColumnIndex(OpenHelper.MSG_PORT));
                data = cursor.getBlob(cursor.getColumnIndex(OpenHelper.MSG_DATA));
            }
            
            cursor.close();
            
            if(phoneNumber != null && sms_port != 0 && data != null) {
	     		Intent iSend = new Intent(SendResultReceiver.ACTION_RESULT_SMS);
	     		iSend.putExtra("MSG_URI", uri);
	     		PendingIntent piSend = PendingIntent.getBroadcast(context, 0, iSend, PendingIntent.FLAG_ONE_SHOT);
	     		Log.v("CHARABIA", "sendDataMessage again " + phoneNumber + " port " + sms_port);
	     		SmsManager.getDefault().sendDataMessage(phoneNumber, null, sms_port, 
	     						data, piSend, null);
            }
		}
	} 
}
