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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import android.telephony.SmsMessage;
import android.util.Log;

public class SMSReceiver extends BroadcastReceiver 
{
	private final String ACTION_RECEIVE_SMS  = "android.intent.action.DATA_SMS_RECEIVED";

	@Override
	public void onReceive(Context context, Intent intent)
	{
		if (intent.getAction().equals(ACTION_RECEIVE_SMS)) {
			Bundle bundle = intent.getExtras();
			if (bundle != null) {
				Object[] pdus = (Object[]) bundle.get("pdus");
 
				SmsMessage[] messages = new SmsMessage[pdus.length];
				for (int i = 0; i < pdus.length; i++) {
					messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);  
				}
				
				if (messages.length > -1) {
					byte[] data = messages[0].getUserData();

					Log.e("SMSRECVEIVER","data length = " + data.length);
					
					if(data != null && 
							data[0] == SmsCipher.MAGIC[0] && 
							data[1] == SmsCipher.MAGIC[1] && 
							data[2] == SmsCipher.MAGIC[2] && 
							data[3] == SmsCipher.MAGIC[3]) {

						Tools tools = new Tools(context);

						tools.putSmsToDatabases(messages[0]);

						tools.showNotification(tools.getNbMessages(), messages[0]);

						abortBroadcast();
					}
				}
			}
		}
	}
 
}
