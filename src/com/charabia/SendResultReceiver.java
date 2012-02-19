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
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
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
                
            Log.v("CHARABIA", "SendResultReceiver + " + intent.getData());

            Uri uri = ContentUris.withAppendedId(DataProvider.MSG_CONTENT_URI, 
            		Long.parseLong(intent.getData().getSchemeSpecificPart()));
            		
            Log.v("CHARABIA", "uri = " + uri);
            
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

            Intent iSend = new Intent(SmsSender.ACTION_SEND_SMS, intent.getData());
     		PendingIntent piAlarm = PendingIntent.getBroadcast(context, 0, iSend, 0);
          
     		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
     		
     		am.set(AlarmManager.RTC, System.currentTimeMillis()+(60*1000), piAlarm);
		}
	} 
}
