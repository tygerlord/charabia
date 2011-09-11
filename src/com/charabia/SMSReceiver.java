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
import android.content.ContentResolver;

import android.os.Bundle;

import android.telephony.SmsMessage;

public class SMSReceiver extends BroadcastReceiver 
{
	private final String ACTION_RECEIVE_SMS  = "android.provider.Telephony.SMS_RECEIVED";

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
					String messageBody = messages[0].getMessageBody();
					//String phoneNumber = messages[0].getDisplayOriginatingAddress();
 
					//Toast.makeText(context, "Expediteur : " + phoneNumber, Toast.LENGTH_LONG).show();
					//Toast.makeText(context, "Message : " + messageBody, Toast.LENGTH_LONG).show();
				
					if(messageBody.startsWith(Tools.KEYWORD)) {
						//showNotification(context, phoneNumber, messageBody);
						//Tools.showNotification(context, messages[0]);

						// store crypted message 
						// TODO: preference option
						ContentResolver contentResolver = context.getContentResolver();
						Tools.putSmsToDatabase(contentResolver, messages[0]);

						MySmsManager msm = new MySmsManager(context);
						msm.writeSMS(messages[0]);

						Tools.showNotification(context, msm.getNbMessages(), messages[0]);

						abortBroadcast();
					}
				}
			}
		}
	}
 
}
