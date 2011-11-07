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

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import android.telephony.SmsMessage;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;

public class SMSReceiver extends BroadcastReceiver 
{
	private static final String ACTION_RECEIVE_SMS  = "android.intent.action.DATA_SMS_RECEIVED";

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
					byte[] messageBody = messages[0].getUserData();

					Log.v("CHARABIA SMSRECVEIVER","data length = " + messageBody.length);
					
					if(messageBody != null && 
							messageBody[0] == SmsCipher.MAGIC[0] && 
									messageBody[1] == SmsCipher.MAGIC[1] && 
											messageBody[2] == SmsCipher.MAGIC[2] && 
													messageBody[3] == SmsCipher.MAGIC[3]) {

						
							Tools tools = new Tools(context);

							String originatingAddress = messages[0].getOriginatingAddress();
							
							byte[] key = null;
							try {
								key = tools.getKey(originatingAddress);
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							
							SmsCipher cipher = new SmsCipher(context);
							
							String message = "";
							try {
								message = cipher.decrypt(key, messageBody);
							} catch (InvalidKeyException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (NoSuchAlgorithmException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (NoSuchPaddingException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (InvalidAlgorithmParameterException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (IllegalBlockSizeException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (BadPaddingException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							
							//String escapedOriginatingAddress = TextUtils.htmlEncode(originatingAddress);
							//String texte = String.format(context.getString(R.string.answer_link), escapedOriginatingAddress);
							
							long timeStamp = messages[0].getTimestampMillis(); 
							Uri uri = tools.putSmsToDatabase(originatingAddress, 
									timeStamp, 
									Tools.MESSAGE_TYPE_INBOX, 
									messages[0].getStatus(), 
									message + "\n" +
									context.getString(R.string.answer_link, originatingAddress));
									//Html.fromHtml(texte));

							tools.showNotification(uri, originatingAddress, timeStamp);

							abortBroadcast();
						 
						
					}
				}
			}
		}
	} 
}
