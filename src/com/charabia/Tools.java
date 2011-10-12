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

import java.util.ArrayList;

import android.net.Uri;
import android.os.RemoteException;

import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.Intent;
import android.content.Context;
import android.content.ContentValues;
import android.content.ContentResolver;
import android.content.OperationApplicationException;

import android.telephony.SmsMessage;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import android.database.Cursor;
import android.app.PendingIntent;
import android.app.Notification;
import android.app.NotificationManager;

import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.ContactsContract.RawContacts;

public class Tools {

	private static final String TAG = "Charabia_tools";
	
	private Context context;
	
	public static final String accountType = null;//"com.charabia";
	public static final String accountName = null;//"keys";
	
	public static final String PHONE = "data1";
	public static final String KEY = "data2";
	
	public static int id = 0;

	public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/com.charabia.key";
	
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

	public Tools(Context context) {
		this.context = context;
	}
	
	public static void putSmsToDatabase(ContentResolver contentResolver, String originatingAddress, long timeStampMillis, int type, int status, String body)
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

	public static void putSmsToDatabase(ContentResolver contentResolver, SmsMessage sms)
	{
		putSmsToDatabase(contentResolver, sms.getDisplayOriginatingAddress(), 
			sms.getTimestampMillis(), MESSAGE_TYPE_INBOX, sms.getStatus(), sms.getMessageBody());
	}

	/*
	 * @brief put sms on databases (charabia and sms base)
	 */
	public void putSmsToDatabases(SmsMessage sms) {
		try {
			ContentResolver cr = context.getContentResolver();
			// TODO preference option
			putSmsToDatabase(cr, sms);
			
			ContentValues values = new ContentValues();
			values.put(OpenHelper.SMS_PDU, sms.getPdu());
			cr.insert(DataProvider.CONTENT_URI_PDUS, values);
		}
		catch(Exception e) {
			Log.e(TAG, e.toString());
		}
	}
	
	public void showNotification(int nbMessages, SmsMessage message) {
		
		// TODO change in date time 
		String messageBody = "";//message.getMessageBody();
		String phoneNumber = message.getDisplayOriginatingAddress();

		// look up the notification manager service
		NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

		Intent intent = new Intent(SmsListe.class.getName());
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		// The PendingIntent to launch our activity if the user selects this notification
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, 0);

		// The ticker text, this uses a formatted string so our message could be localized
		String tickerText = context.getString(R.string.imcoming_message_ticker_text, nbMessages);

		// construct the Notification object.
		Notification notif = new Notification(R.drawable.ic_launcher, tickerText,
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
	
	public void showNotification() {
		
		// look up the notification manager service
		NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

		int nbMessages = getNbMessages();

		if(nbMessages <= 0) {
			nm.cancel(R.string.imcoming_message_ticker_text);
		}
		else {
			ContentResolver contentResolver = context.getContentResolver();
			Cursor cursor = contentResolver.query(DataProvider.CONTENT_URI_PDUS,
					new String[]{OpenHelper.SMS_PDU}, null, null, null);
			if(cursor.moveToFirst()) {
				byte[] pdu = cursor.getBlob(cursor.getColumnIndex(OpenHelper.SMS_PDU)); 
				showNotification(nbMessages, SmsMessage.createFromPdu(pdu));
			}
			else {
				Log.e(TAG, "Erreur retreive last message");
			}
		}
    }

	public boolean hasKey(Uri dataUri) {		
		ContentResolver cr = context.getContentResolver();

		Cursor cursor = cr.query(dataUri, new String[] { KEY }, 
				Data.MIMETYPE + "=?", new String[] { CONTENT_ITEM_TYPE }, null);
		
		boolean result = false;
		
		if(cursor.getCount() >= 1) {
			result = true;
		}
		
		return result;
	}
	
	public byte[] getKey(String phoneNumber) throws Exception {
        ContentResolver cr = context.getContentResolver();

        Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, 
        		Uri.encode(phoneNumber));
      
        Cursor cursor = cr.query(uri, new String[]{Contacts.LOOKUP_KEY}, 
        		null, null, null);
        
        String lookupKey = null;
        
        if(cursor.moveToFirst()) {
        	lookupKey = cursor.getString(0);
        }
        
        cursor.close();
        
        if(lookupKey == null) {
        	throw new Exception("No lookup key for " + phoneNumber);
        }
        
        cursor = cr.query(Data.CONTENT_URI, 
        		new String[] { KEY }, 
        		Data.LOOKUP_KEY + "=? AND " + Data.MIMETYPE + "=?", 
        		new String[] { lookupKey, CONTENT_ITEM_TYPE }, 
        		null);
        
        byte[] result = null;
        
        if(cursor.moveToFirst()) {
        	result = Base64.decode(cursor.getString(0), Base64.DEFAULT);
        }
        
        cursor.close();
        return result;
	}
	
	public void updateOrCreateContactKey(String phoneNumber, byte[] key) throws RemoteException, OperationApplicationException {
		 
		ContentResolver cr = context.getContentResolver();
		
		ArrayList<ContentProviderOperation> ops =
					new ArrayList<ContentProviderOperation>();

		ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
				.withValue(KEY, Base64.encodeToString(key, Base64.DEFAULT))
				.withSelection(PHONE + "=? AND " + Data.MIMETYPE + "=?", 
						new String[] { phoneNumber, CONTENT_ITEM_TYPE })
				.withExpectedCount(1).build());
		
		try {
 			cr.applyBatch(ContactsContract.AUTHORITY, ops);
		}
		catch(OperationApplicationException oae) {
			ops.clear();
 			int rawContactInsertIndex = ops.size();
			 
			 
 			ops.add(ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
					 .withValue(RawContacts.ACCOUNT_TYPE, accountType)
					 .withValue(RawContacts.ACCOUNT_NAME, accountName)
					 .build());
	
 			ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				 .withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
				 .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
				 .withValue(Phone.NUMBER, phoneNumber)
				 .withValue(Phone.TYPE, Phone.TYPE_MOBILE)
				 .build());
	
 			ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
					 .withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
					 .withValue(Data.MIMETYPE, CONTENT_ITEM_TYPE)
					 .withValue(PHONE, phoneNumber)
					 .withValue(KEY, Base64.encodeToString(key, Base64.DEFAULT))
					 .build());
	
 			cr.applyBatch(ContactsContract.AUTHORITY, ops);
 		}
	}

	public byte[] getKey(Uri dataUri) throws Exception {

		ContentResolver cr = context.getContentResolver();
	
		Cursor cursor = cr.query(dataUri, new String[] { KEY }, 
			null, null, null);
		
		if(cursor.moveToFirst()) {
			byte[] key;
			key = Base64.decode(cursor.getString(0), Base64.DEFAULT);
			cursor.close();
			return key;
		}
			
		throw new Exception("key not found for " + dataUri);
	}

	public String getPhoneNumber(Uri uri) {
		ContentResolver cr = context.getContentResolver();
		
		Cursor cursor = cr.query(uri, new String[] { PHONE }, 
				null, null, null);
		
		String result = null;
		
		if(cursor.moveToFirst()) {
			result = cursor.getString(0);
		}
		else {	
			result = context.getString(R.string.unknow);
		}
		
		cursor.close();
		return result;
	}

	public String getDisplayNameAndPhoneNumber(Uri uri) {
		ContentResolver cr = context.getContentResolver();
		
		Cursor cursor = cr.query(uri, new String[] { Contacts.DISPLAY_NAME, PHONE }, 
				null, null, null);
		
		String result = null;
		
		if(cursor.moveToFirst()) {
			result = cursor.getString(0) + ", " + cursor.getString(1);
		}
		else {	
			result = context.getString(R.string.unknow);
		}
		
		cursor.close();
		return result;
	}

	public int getNbMessages() {
		int count = -1;
		ContentResolver cr = context.getContentResolver();
		Cursor cursor = cr.query(DataProvider.CONTENT_URI_PDUS,
				new String[] { "COUNT(*)" }, null, null, null); 
		if(cursor.moveToFirst()) {		
			count = cursor.getInt(0);
		}
		else {
			Toast.makeText(context, "moveToNext failed", Toast.LENGTH_LONG).show();
		}
		cursor.close();
		return count;
	}

	public String getDisplayName(String phoneNumber) {
        Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(uri, new String[]{PhoneLookup.DISPLAY_NAME}, null, null, null);
        
        String result = null;
        
        if(cursor.moveToFirst()) {
        	result = cursor.getString(0);
        }
        else {  
            result = context.getString(R.string.unknow);
        }
        cursor.close();
        return result;
		
	}
	
	public Uri getUriFromPhoneNumber(String phoneNumber) throws Exception {
        ContentResolver cr = context.getContentResolver();

        Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, 
        		Uri.encode(phoneNumber));
      
        Cursor cursor = cr.query(uri, new String[]{Contacts.LOOKUP_KEY}, 
        		null, null, null);
        
        String lookupKey = null;
        
        if(cursor.moveToFirst()) {
        	lookupKey = cursor.getString(0);
        }
        
        cursor.close();
        
        if(lookupKey == null) {
        	throw new Exception("No lookup key for " + phoneNumber);
        }
        
        cursor = cr.query(Data.CONTENT_URI, 
        		new String[] { Data._ID }, 
        		Data.LOOKUP_KEY + "=? AND " + Data.MIMETYPE + "=?", 
        		new String[] { lookupKey, CONTENT_ITEM_TYPE }, 
        		null);
        
        Uri result = null;
        
        if(cursor.moveToFirst()) {
        	result = ContentUris.withAppendedId(Data.CONTENT_URI, cursor.getLong(0));
        }
        
        cursor.close();
        return result;
	}
}
