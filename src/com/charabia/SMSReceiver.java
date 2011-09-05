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
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.widget.Toast;
 
import android.net.Uri;
 
import android.app.PendingIntent;
import android.app.Notification;
import android.app.NotificationManager;

import android.util.Log;

public class SMSReceiver extends BroadcastReceiver 
{
	public static final String NOTIF_TAG = "CHARABIA";
	
	public static int id = 0;
	
	private final String ACTION_RECEIVE_SMS  = "android.provider.Telephony.SMS_RECEIVED";

    protected void showNotification(Context context, CharSequence from, CharSequence message) {
		id += 1;

		// look up the notification manager service
		NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

		// The details of our fake message
		//CharSequence from = "Joe";
		//CharSequence message = "message id " + id;

		//Intent intent = new Intent(Intent.ACTION_VIEW);
		//intent.setClassName(context, SmsViewActivity.class.getName());
		//intent.putExtra("ID", (short)id);
		Intent intent = new Intent("com.confidentialsms.SmsViewActivity.VIEW");
		
        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent
                /*new Intent(Intent.ACTION_VIEW, ConfidentialSmsActivity.class.getName())*/, 0);

        // The ticker text, this uses a formatted string so our message could be localized
        String tickerText = "hello";//getString(R.string.imcoming_message_ticker_text, message);

        // construct the Notification object.
        Notification notif = new Notification(android.R.drawable.ic_menu_help /*R.drawable.stat_sample*/, tickerText,
                System.currentTimeMillis());

        // Set the info for the views that show in the notification panel.
        notif.setLatestEventInfo(context, from, message, contentIntent);

		notif.flags |= Notification.FLAG_AUTO_CANCEL;

        /*
        // On tablets, the ticker shows the sender, the first line of the message,
        // the photo of the person and the app icon.  For our sample, we just show
        // the same icon twice.  If there is no sender, just pass an array of 1 Bitmap.
        notif.tickerTitle = from;
        notif.tickerSubtitle = message;
        notif.tickerIcons = new Bitmap[2];
        notif.tickerIcons[0] = getIconBitmap();;
        notif.tickerIcons[1] = getIconBitmap();;
        */

        // after a 0ms delay, vibrate for 250ms, pause for 100 ms and
        // then vibrate for 500ms.
        //notif.vibrate = new long[] { 0, 250, 100, 500};

        // Note that we use R.layout.incoming_message_panel as the ID for
        // the notification.  It could be any integer you want, but we use
        // the convention of using a resource id for a string related to
        // the notification.  It will always be a unique number within your
        // application.
        nm.notify(id /*R.string.imcoming_message_ticker_text*/, notif);
    }

    protected void showNotification(Context context, SmsMessage message) {
		id += 1;

		String messageBody = message.getMessageBody();
		String phoneNumber = message.getDisplayOriginatingAddress();

		// look up the notification manager service
		NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

		// The details of our fake message
		//CharSequence from = "Joe";
		//CharSequence message = "message id " + id;

		//Intent intent = new Intent(Intent.ACTION_VIEW);
		//intent.setClassName(context, SmsViewActivity.class.getName());
		//intent.putExtra("ID", (short)id);
		Intent intent = new Intent(SmsViewActivity.class.getName());//"com.charabia.SmsViewActivity.VIEW");
		intent.putExtra("DATA", message.getPdu());
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		//intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		//intent.addFlag(Intent.FLAG_ACTIVITY_NO_HISTORY);
		//intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		
        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, 0);

        // The ticker text, this uses a formatted string so our message could be localized
        String tickerText = "hello";//getString(R.string.imcoming_message_ticker_text, message);

        // construct the Notification object.
        Notification notif = new Notification(android.R.drawable.stat_notify_chat /*R.drawable.stat_sample*/, tickerText,
                System.currentTimeMillis());

        // Set the info for the views that show in the notification panel.
        notif.setLatestEventInfo(context, phoneNumber, messageBody, contentIntent);

		notif.flags |= Notification.FLAG_AUTO_CANCEL;
		notif.flags |= Notification.FLAG_NO_CLEAR;
		
        /*
        // On tablets, the ticker shows the sender, the first line of the message,
        // the photo of the person and the app icon.  For our sample, we just show
        // the same icon twice.  If there is no sender, just pass an array of 1 Bitmap.
        notif.tickerTitle = from;
        notif.tickerSubtitle = message;
        notif.tickerIcons = new Bitmap[2];
        notif.tickerIcons[0] = getIconBitmap();;
        notif.tickerIcons[1] = getIconBitmap();;
        */

        // after a 0ms delay, vibrate for 250ms, pause for 100 ms and
        // then vibrate for 500ms.
        //notif.vibrate = new long[] { 0, 250, 100, 500};

        // Note that we use R.layout.incoming_message_panel as the ID for
        // the notification.  It could be any integer you want, but we use
        // the convention of using a resource id for a string related to
        // the notification.  It will always be a unique number within your
        // application.
        nm.notify(id /*R.string.imcoming_message_ticker_text*/, notif);
    }

	/*
    private Bitmap getIconBitmap() {
        BitmapFactory f = new BitmapFactory();
        return f.decodeResource(getResources(), R.drawable.app_sample_code);
    }
	*/
	
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

						SmsViewActivity.smsListe.add(messages[0]);
						
						Tools.showNotification(context);
						
						abortBroadcast();
					}
				}
			}
		}
	}
 
}
