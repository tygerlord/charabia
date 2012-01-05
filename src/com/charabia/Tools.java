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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.RSAKeyGenParameterSpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import android.net.Uri;
import android.content.Intent;
import android.content.Context;
import android.content.ContentValues;
import android.content.ContentResolver;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Toast;
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

	private static final String KEYPAIR_FILENAME= "keypair.data";
	
	public static final byte[] MAGIC = { (byte)0x84, (byte)0x15, (byte)0x61, (byte)0xB7 };
	
	public static final String CIPHER_ALGO = "AES/CBC/PKCS5Padding";
	public static final String RSA_CIPHER_ALGO = "RSA/ECB/PKCS1Padding";
	
	public static final byte MESSAGE_TYPE = 0x01; // we receive a message
	public static final byte KEY_TYPE = 0x02; // we receive a public key
	public static final byte CRYPTED_KEY_TYPE = 0x03; // we receive aes key crypted by public key

	public static final byte[] demo_key = new byte[] { 
		0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
		0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 ,
		0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
		0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 
	};

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
	
	/*
	 * load or create RSA keypair for sms key exchange process 
	 */
	public KeyPair loadKeyPair() {
	
		KeyPair keyPair = null;
		
		try {
			FileInputStream fis = context.openFileInput(KEYPAIR_FILENAME);
			ObjectInputStream ois = new ObjectInputStream(fis);
			keyPair = (KeyPair) ois.readObject();
			fis.close();
		} 
		catch (StreamCorruptedException e1) {
			e1.printStackTrace();
		} 
		catch (IOException e1) {
			e1.printStackTrace();
			
			if(e1 instanceof FileNotFoundException) {
				try {
					KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
					gen.initialize(new RSAKeyGenParameterSpec(1024, RSAKeyGenParameterSpec.F4));
					keyPair = gen.generateKeyPair();

					FileOutputStream fos = context.openFileOutput(KEYPAIR_FILENAME, Context.MODE_PRIVATE);
					ObjectOutputStream oos = new ObjectOutputStream(fos);
					oos.writeObject(keyPair);
					oos.close();
				} 
				catch (NoSuchAlgorithmException e) {
					e.printStackTrace();
				} 
				catch (IOException e) {
					e.printStackTrace();
				} 
				catch (InvalidAlgorithmParameterException e) {
					e.printStackTrace();
				} 
				
			}
		} 
		catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		return keyPair;
	}
	
    public static String bytesToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();

        for (int i = 0; i < data.length; i++) {
            buf.append(byteToHex(data[i]).toUpperCase());
        }
        return (buf.toString());
    }

    private static final String tab = "0123456789ABCDEF";
    
    public static String byteToHex(byte data) {

    	
        StringBuffer buf = new StringBuffer();

        buf.append(tab.charAt((data >>> 4) & 0x0F));

        buf.append(tab.charAt(data & 0x0F));

        return buf.toString();

    }

	public String decrypt(String originatingAddress, byte[] data) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, NoContactException, NoCharabiaKeyException {
		
		String result = context.getString(R.string.unexpected_error);
		
		if(data[4] == KEY_TYPE) {
			// receive public key 
			ContentResolver cr = context.getContentResolver();
			ContentValues values = new ContentValues();
			
			byte[] key = new byte[data.length-5];
			System.arraycopy(data, 6, key, 0, key.length);
			
			values.put(OpenHelper.KEY, key);
			values.put(OpenHelper.PHONE, originatingAddress);
			
			cr.insert(DataProvider.PUBKEYS_CONTENT_URI, values);
			
			result = "Ce contact demande à partager une clé charabia avec vous, cliquez sur ce lien pour ";
		}
		else if(data[4] == CRYPTED_KEY_TYPE) {
			// receive crypted aes key
		}
		else {
			byte[] key_data = getKey(originatingAddress);
					
			Cipher c = Cipher.getInstance(CIPHER_ALGO);
		
			SecretKey key = new SecretKeySpec(key_data, "AES");
		
			byte[] IV = new byte[16];
			int pos = MAGIC.length+1;
			System.arraycopy(data, pos, IV, 0, 7); pos += 7;
			
			c.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(IV));
	
			result = new String(c.doFinal(data, pos, data.length-pos));
		}
		
		return result;
	}

	public byte[] encrypt(byte[] key_data, String texte) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
		Cipher c = Cipher.getInstance(CIPHER_ALGO);

		SecretKey key = new SecretKeySpec(key_data, "AES");

		SecureRandom sr = new SecureRandom();
		
		// generate salt but keep only first 7 bytes, set other to 0
		// this for keep message size at 140 bytes total size sms data allowed
		byte[] bIV = sr.generateSeed(16);
		for(int i = 7; i < 16; i++) bIV[i] = (byte)0x00;
		
		c.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(bIV));

		byte[] cryptedTexte = c.doFinal(texte.getBytes());
		byte[] data = new byte[MAGIC.length+1+7+cryptedTexte.length];

		int pos = 0;
		System.arraycopy(MAGIC, 0, data, pos, MAGIC.length); pos += MAGIC.length;
		data[pos] = MESSAGE_TYPE; pos += 1;
		System.arraycopy(bIV, 0, data, pos, 7); pos += 7;
		System.arraycopy(cryptedTexte, 0, data, pos, cryptedTexte.length);

		return data;
	}
 
	public SecretKey generateKeyAES(int size) {
		try {
			KeyGenerator keyGen = KeyGenerator.getInstance("AES");
			keyGen.init(size);
			SecretKey key = keyGen.generateKey();
			
			// key.getEncoded to have byte[]
			return key;
		}
		catch(Exception e) {
			Toast.makeText(context, context.getString(R.string.unexpected_error) + "\n" + e.toString(), Toast.LENGTH_LONG).show();
		}

		return null;
	}

	public SecretKey generateKeyAES() {
		return generateKeyAES(256);
	}

}
