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

import android.net.Uri;

import android.util.Base64;

import android.content.Intent;
import android.content.Context;
import android.content.ContentValues;
import android.content.ContentResolver;

import android.telephony.SmsMessage;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import android.app.PendingIntent;
import android.app.Notification;
import android.app.NotificationManager;

import android.widget.Toast;

import android.provider.ContactsContract.PhoneLookup;

public class Tools
{
	//private OpenHelper openHelper = new OpenHelper(this);

	public static int id = 0;

	private static final String TAG = "CHARABIA_TOOLS";
	
	private static final byte[] demo_key = new byte[] { 
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 ,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 
		};

	
	public static final String SMS_EXTRA_NAME = "pdus";
	public static final String SMS_URI = "content://sms";
	
	public static final String ADDRESS = "address";
	public static final String PERSON = "person";
	public static final String DATE = "date";
	public static final String READ = "read";
	public static final String STATUS = "status";
	public static final String TYPE = "type";
	public static final String BODY = "body";
	public static final String SEEN = "seen";

	public static final int MESSAGE_TYPE_INBOX = 1;
	public static final int MESSAGE_TYPE_SENT = 2;

	public static final int MESSAGE_IS_NOT_READ = 0;
	public static final int MESSAGE_IS_READ = 1;

	public static final int MESSAGE_IS_NOT_SEEN = 0;
	public static final int MESSAGE_IS_SEEN = 1;


	public static void putSmsToDatabase( ContentResolver contentResolver, String originatingAddress, long timeStampMillis, int type, int status, String body)
	{
		// Create SMS row
		ContentValues values = new ContentValues();
		values.put( ADDRESS, originatingAddress );
		values.put( DATE, timeStampMillis );
		values.put( READ, MESSAGE_IS_READ );
		values.put( STATUS, status );
		values.put( TYPE, type );
		values.put( SEEN, MESSAGE_IS_SEEN );
		values.put( BODY, body );
		// Push row into the SMS table
		contentResolver.insert( Uri.parse( SMS_URI ), values );
	}

	public static void putSmsToDatabase( ContentResolver contentResolver, SmsMessage sms )
	{
		putSmsToDatabase(contentResolver, sms.getDisplayOriginatingAddress(), 
			System.currentTimeMillis(), MESSAGE_TYPE_INBOX, sms.getStatus(), sms.getMessageBody());
	}

	static public void showNotification(Context context, int nbMessages, SmsMessage message) {
			
			String messageBody = message.getMessageBody();
			String phoneNumber = message.getDisplayOriginatingAddress();

			// look up the notification manager service
			NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

			Intent intent = new Intent(SmsViewActivity.class.getName());
			intent.putExtra("DATA", message.getPdu());
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

			// The PendingIntent to launch our activity if the user selects this notification
			PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, 0);

			// The ticker text, this uses a formatted string so our message could be localized
			String tickerText = context.getString(R.string.imcoming_message_ticker_text, nbMessages);

			// construct the Notification object.
			Notification notif = new Notification(android.R.drawable.stat_notify_chat, tickerText,
				System.currentTimeMillis());

			// Set the info for the views that show in the notification panel.
			notif.setLatestEventInfo(context, phoneNumber, messageBody, contentIntent);

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
			nm.notify(R.string.imcoming_message_ticker_text, notif);
	}

	static public void showNotification(Context context) {
	
		MySmsManager msm = new MySmsManager(context);
		
		// look up the notification manager service
		NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

		int nbMessages = msm.getNbMessages();

		if(nbMessages <= 0) {
			nm.cancel(R.string.imcoming_message_ticker_text);
		}
		else {
			SmsMessage message = msm.getLastMessage();
			showNotification(context, nbMessages, message);
		}
    }

	public static String getDisplayName(Context context, String phoneNumber) {
		Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
		ContentResolver contentResolver = context.getContentResolver();
		Cursor cursor = contentResolver.query(uri, new String[]{PhoneLookup.DISPLAY_NAME}, null, null, null);
		
		String result = null;
		
		if(cursor.getCount() > 0) {
			 cursor.moveToFirst();
			 result = cursor.getString(cursor.getColumnIndex(PhoneLookup.DISPLAY_NAME));
		}
		else {	
			result = context.getString(R.string.unknow);
		}
		cursor.close();
		return result;
	}
	
//	public static String getPhone(Context context) {
//		Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
//		ContentResolver contentResolver = context.getContentResolver();
//		Cursor cursor = contentResolver.query(uri, new String[]{PhoneLookup.DISPLAY_NAME}, null, null, null);
//		
//		String result = null;
//		
//		if(cursor.getCount() > 0) {
//			 cursor.moveToFirst();
//			 result = cursor.getString(cursor.getColumnIndex(PhoneLookup.DISPLAY_NAME));
//		}
//		else {	
//			result = context.getString(R.string.unknow);
//		}
//		cursor.close();
//		return result;
//	}

}
