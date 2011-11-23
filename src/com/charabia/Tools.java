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

import java.io.InputStream;
import android.net.Uri;
import android.content.Intent;
import android.content.Context;
import android.content.ContentValues;
import android.content.ContentResolver;
import android.text.format.DateFormat;
import android.util.Log;
import android.database.Cursor;
import android.database.SQLException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.app.PendingIntent;
import android.app.Notification;
import android.app.NotificationManager;

import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.PhoneLookup;

class NoLookupKeyException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public NoLookupKeyException(String message) {
		super(message);
	}


}

class NoCharabiaKeyException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public NoCharabiaKeyException(String message) {
		super(message);
	}


}

class NoContactException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public NoContactException(String message) {
		super(message);
	}	
}

public class Tools {

	private static final String TAG = "Charabia_tools";
	
	private Context context;
	
	public static final String accountType = "com.charabia";
	public static final String accountName = "keys";
	
	public static final String PHONE = "data1";
	public static final String KEY = "data2";
	
	public static int id = 0;

	public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/com.charabia.key";
	
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
	
	public Uri putSmsToDatabase(String originatingAddress, long timeStampMillis, int type, int status, String body)
	{
		ContentResolver cr = context.getContentResolver();
		ContentValues values = new ContentValues();
		values.put( ADDRESS, originatingAddress );
		values.put( DATE, timeStampMillis );
		//values.put( READ, MESSAGE_IS_READ );
		values.put( STATUS, status );
		values.put( TYPE, type );
		//values.put( SEEN, MESSAGE_IS_SEEN );
		values.put( BODY, body );
		// Push row into the SMS table
		return cr.insert( Uri.parse( SMS_URI ), values );
	}
	
	public void showNotification(String originatingAddress, long timeStamp) {
		
		CharSequence line1 = context.getString(R.string.from) + " " + getDisplayName(originatingAddress);
		CharSequence line2 = DateFormat.format(context.getString(R.string.dateformat), timeStamp);

		// look up the notification manager service
		NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		intent.setClassName("com.android.mms", "com.android.mms.ui.ConversationList");
		

		// The PendingIntent to launch our activity if the user selects this notification
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, 0);

		// The ticker text, this uses a formatted string so our message could be localized
		String tickerText = context.getResources().getString(R.string.incoming_message_ticker_text);

		// construct the Notification object.
		Notification notif = new Notification(R.drawable.ic_launcher, tickerText,
			System.currentTimeMillis());

		// Set the info for the views that show in the notification panel.
		notif.setLatestEventInfo(context, line1, line2, contentIntent);

		notif.flags |= Notification.FLAG_AUTO_CANCEL;

		notif.defaults = Notification.DEFAULT_SOUND;
		
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
		nm.notify(R.string.incoming_message_ticker_text, notif);
	}
	
	@Deprecated
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
	
	/*
	 * get lookup key of contact from the phone number
	 */
	public String getLookupFromPhoneNumber(String phoneNumber) throws NoContactException {
        String lookupKey = null;
       
		try {
			ContentResolver cr = context.getContentResolver();
	       
			Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, 
	        		Uri.encode(phoneNumber));
	      
	        Cursor cursor = cr.query(uri, new String[]{Contacts.LOOKUP_KEY}, 
	        		null, null, null);
	        
	        
	        if(cursor.moveToFirst()) {
	        	lookupKey = cursor.getString(0);
	        }
	        
	        cursor.close();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
        if(lookupKey == null) {
        	throw new NoContactException("No contact with this phone number " + phoneNumber);
        }
        
        return lookupKey;
	}

	/*
	 * return the lookup key from table
	 */
	@Deprecated
	public String _getLookupKey(Uri uri) throws NoLookupKeyException {
	      ContentResolver cr = context.getContentResolver();
	       
	        Cursor cursor = cr.query(uri, new String[]{ OpenHelper.LOOKUP }, 
	        		null, null, null);
	        
	        String lookupKey = null;
	        
	        if(cursor.moveToFirst()) {
	        	lookupKey = cursor.getString(0);
	        }
	        
	        cursor.close();
	        
	        if(lookupKey == null) {
	        	throw new NoLookupKeyException("No lookup key for " + uri);
	        }
	        
	        return lookupKey;
	}
	
	/*
	 * Get key from uri
	 */
	@Deprecated
	public byte[] _getKey(Uri dataUri) throws NoCharabiaKeyException {

		ContentResolver cr = context.getContentResolver();
	
		Cursor cursor = cr.query(dataUri, new String[] { KEY }, 
			null, null, null);
		
		byte[] key = null;
		
		if(cursor.moveToFirst()) {
			key = cursor.getBlob(0);
		}

		cursor.close();
		
		if(key == null) {
			throw new NoCharabiaKeyException("key not found for " + dataUri);
		}
		
		return key;
	}


	/*
	 * Get key from phoneNumber
	 */
	public byte[] getKey(String phoneNumber) throws NoContactException, NoCharabiaKeyException  {
        ContentResolver cr = context.getContentResolver();

        String lookupKey = getLookupFromPhoneNumber(phoneNumber);
      
        Cursor cursor = cr.query(DataProvider.CONTENT_URI, 
        		new String[]{OpenHelper.KEY}, 
        		OpenHelper.LOOKUP + "=?", 
        		new String[] { lookupKey }, null);
        
        byte[] key = null;
        
        if(cursor.moveToFirst()) {
        	key = cursor.getBlob(0);
        }
        
        cursor.close();
        
        if(key == null) {
        	throw new NoCharabiaKeyException("No Charabia key for " + phoneNumber);
        }
        
        return key;
	}
	
	/*
	 * Insert a key associate with a phone number and a contact 
	 * The contact must exist in contact table to be associated with
	 * 
	 * @param overwrite true if contact key must be erased with new key
	 */
	public Uri updateOrCreateContactKey(String phoneNumber, byte[] key, boolean overwrite) throws NoContactException  {
		 
		
		String lookupKey = getLookupFromPhoneNumber(phoneNumber);
		
		ContentResolver cr = context.getContentResolver();

		ContentValues values = new ContentValues();
		
		values.put(OpenHelper.KEY, key);
		values.put(OpenHelper.LOOKUP, lookupKey);
		values.put(OpenHelper.PHONE, phoneNumber);
		values.put(OpenHelper.CONTACT_ID, 0);
		
		Uri uri = null;
		
		try {
			uri = cr.insert(DataProvider.CONTENT_URI, values);
		}
		catch(SQLException e) {
			Log.v(TAG, phoneNumber + " not inserted, already present?" );
		}
		
		if(overwrite) {
			int count = cr.update(DataProvider.CONTENT_URI, 
					values, 
					OpenHelper.LOOKUP + "=?", 
					new String[] { lookupKey } );
	
			Log.v("CHARABIA", "update count " + count);
		}
		
		return uri;
	}

	public Uri updateOrCreateContactKey(String phoneNumber, byte[] key) throws NoContactException  {
		return updateOrCreateContactKey(phoneNumber, key, true);
	}

	@Deprecated
	public Uri _getContactUri(Uri dataUri) throws NoContactException {
		ContentResolver cr = context.getContentResolver();

		long contactId = 0;
		String lookupKey = null;
		
		Cursor cursor = cr.query(DataProvider.CONTENT_URI, 
				new String[] { OpenHelper.LOOKUP, OpenHelper.CONTACT_ID },
				null, null, null);
				
		if(cursor.moveToFirst()) {
			contactId = cursor.getLong(cursor.getColumnIndex(OpenHelper.CONTACT_ID));
			lookupKey = cursor.getString(cursor.getColumnIndex(OpenHelper.LOOKUP));
		}	
		
		Uri uri = Contacts.lookupContact(cr, Contacts.getLookupUri(contactId, lookupKey));
		
		if(uri == null) {
			throw new NoContactException("No contact for uri " + dataUri);
		}
		
		return uri;
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
	
	@Deprecated
	public Bitmap getBitmapPhotoFromPhoneNumber(String phoneNumber) throws Exception {
        ContentResolver cr = context.getContentResolver();

        
        Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, 
        		Uri.encode(phoneNumber));
      
        Cursor cursor = cr.query(uri, new String[]{ PhoneLookup.LOOKUP_KEY }, 
        		null, null, null);
        
        String lookupKey = null;
        
        if(cursor.moveToFirst()) {
        	lookupKey = cursor.getString(0);
        }
        
        cursor.close();
        
        Bitmap result = null;
        
        if(lookupKey != null) {
        	uri = Contacts.lookupContact(cr, Contacts.getLookupUri(0, lookupKey));
        	
        	try {
        		InputStream input = Contacts.openContactPhotoInputStream(cr, uri);
        	
        		if(input != null) {
        			result = BitmapFactory.decodeStream(input);
        		}
        	}
        	catch(Exception e) {
        		e.printStackTrace();
        	}
        }
		
        	
        if(result == null) {
        	result = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher);
        }

        return result; 
	}
	
}
