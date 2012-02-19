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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.spec.RSAKeyGenParameterSpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import android.net.Uri;
import android.content.ContentUris;
import android.content.Intent;
import android.content.Context;
import android.content.ContentValues;
import android.content.ContentResolver;
import android.telephony.SmsManager;
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

class OverflowCharabiaCounterException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public OverflowCharabiaCounterException(String message) {
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
	
	public static final byte[] MAGIC = { (byte)0x19, (byte)0x81 };
	
	public static final String CIPHER_ALGO = "AES/CTR/PKCS5Padding";
	public static final String RSA_CIPHER_ALGO = "RSA/ECB/PKCS1Padding";
	
	public static final byte MESSAGE_TYPE = (byte)0x00; // we receive a message
	public static final byte PUBLIC_KEY_TYPE = (byte)0x80; // we receive a public key
	public static final byte CRYPTED_KEY_TYPE = (byte)0xC0; // we receive aes key crypted by public key

	public static final int MESSAGE_ERROR = 1;
	public static final int MESSAGE = MESSAGE_ERROR + 1;
	public static final int MESSAGE_SEND = MESSAGE + 1;
	public static final int MESSAGE_DELIVERED = MESSAGE_SEND + 1;
	public static final int MESSAGE_RECEIVED = MESSAGE_DELIVERED + 1;
	public static final int INVITATION = MESSAGE_RECEIVED + 1;
	public static final int INVITATION_SEND = INVITATION + 1;
	public static final int INVITATION_DELIVERED = INVITATION_SEND + 1;
	public static final int INVITATION_RECEIVED = INVITATION_DELIVERED + 1;
	public static final int INVITATION_ANSWER = INVITATION_RECEIVED + 1;
	public static final int INVITATION_ANSWER_SEND = INVITATION_ANSWER + 1;
	public static final int INVITATION_ANSWER_DELIVERED = INVITATION_ANSWER_SEND + 1;
	
	//port where data sms are send
	public static final short sms_port = 1981;
	

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

	public byte[] decrypt(String originatingAddress, byte[] data) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, NoContactException, NoCharabiaKeyException {
		
		byte[] result = context.getString(R.string.unexpected_error).getBytes();
		
		byte[] key_data = getKey(originatingAddress);
				
		Cipher c = Cipher.getInstance(CIPHER_ALGO);
	
		SecretKey key = new SecretKeySpec(key_data, "AES");
	
		byte[] IV = new byte[16];
		
		IV[1] = data[2];
		IV[2] = data[3];
		IV[3] = data[4];
		
		c.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(IV));

		result =c.doFinal(data, 5, data.length-5);
		
		return result;
	}

	public byte[] encrypt(String destinationAddress, byte[] clearData) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, NoCharabiaKeyException, NoContactException, OverflowCharabiaCounterException, ShortBufferException {
		ContentResolver cr = context.getContentResolver();

        String lookupKey = getLookupFromPhoneNumber(destinationAddress);
      
        Cursor cursor = cr.query(DataProvider.CONTENT_URI, 
        		new String[]{OpenHelper.ID, OpenHelper.KEY, OpenHelper.COUNTER}, 
        		OpenHelper.LOOKUP + "=?", 
        		new String[] { lookupKey }, null);
        
        byte[] key = null;
        int counter = -1;
        long id = -1;
        
        if(cursor.moveToFirst()) {
        	key = cursor.getBlob(cursor.getColumnIndex(OpenHelper.KEY));
        	counter = cursor.getInt(cursor.getColumnIndex(OpenHelper.COUNTER));
        	id = cursor.getLong(cursor.getColumnIndex(OpenHelper.ID));
        }
        
        cursor.close();
        
        if(key == null || counter < 0) {
        	throw new NoCharabiaKeyException("No Charabia key for " + destinationAddress);
        }
	    
        if(counter>=(0x3fffff)) {
        	throw new OverflowCharabiaCounterException("key exeed use");
        }
        counter += 1;
      
        // save new counter value
        ContentValues values = new ContentValues();
        values.put(OpenHelper.COUNTER, counter);
        cr.update(ContentUris.withAppendedId(DataProvider.CONTENT_URI, id), values, null, null);
        
        
		Cipher c = Cipher.getInstance(CIPHER_ALGO);

		SecretKey keySpec = new SecretKeySpec(key, "AES");

		byte[] bIV = new byte[16];
		ByteBuffer bbuf = ByteBuffer.wrap(bIV, 0, 4);
		bbuf.putInt(counter);
		
		c.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(bIV));
		
		//byte[] cryptedTexte = c.doFinal(texte.getBytes());
		//byte[] data = new byte[MAGIC.length+1+cryptedTexte.length];

		byte[] data = new byte[2+3+c.getOutputSize(clearData.length)];
				
		data[0] = MAGIC[0];
		data[1] = MAGIC[1];
		data[2] = (byte)(bIV[1] & MESSAGE_TYPE);
		data[3] = bIV[2];
		data[4] = bIV[3];
		
		c.doFinal(clearData, 0, clearData.length, data, 5);
		
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

	public void manageMsg(String originatingAddress, byte[] messageBody, long timeStamp) {
		
		ContentResolver cr = context.getContentResolver();
		
		ContentValues values = new ContentValues();
		
		Log.v(TAG, "from " + originatingAddress + ", " + bytesToHex(messageBody));
		
		try {
			String lookupKey = getLookupFromPhoneNumber(originatingAddress);
			Uri contactUri = Contacts.lookupContact(context.getContentResolver(),  
					Contacts.getLookupUri(0, lookupKey));
			values.put(OpenHelper.CONTACT_URI, contactUri.toString());
		} catch (NoContactException e1) {
			e1.printStackTrace();
		}
		
		byte msgType = (byte)(messageBody[2] & 0xC0);
		
		values.put(OpenHelper.PHONE, originatingAddress);
		values.put(OpenHelper.MSG_DATA, messageBody);
		values.put(OpenHelper.MSG_DATE, timeStamp);
		
		if(msgType == MESSAGE_TYPE) {
			String message = "";
			
			try {
				message = new String(decrypt(originatingAddress, messageBody));
				
				message += "\n" + 
						context.getString(R.string.secured_by_appname, 
								context.getString(R.string.app_name));
				
			} catch (InvalidKeyException e) {
				e.printStackTrace();
				message = context.getString(R.string.unexpected_error);
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
				message = context.getString(R.string.unexpected_error);
			} catch (NoSuchPaddingException e) {
				e.printStackTrace();
				message = context.getString(R.string.unexpected_error);
			} catch (InvalidAlgorithmParameterException e) {
				e.printStackTrace();
				message = context.getString(R.string.unexpected_error);
			} catch (IllegalBlockSizeException e) {
				e.printStackTrace();
				message = context.getString(R.string.blocksize_error);
			} catch (BadPaddingException e) {
				e.printStackTrace();
				message = context.getString(R.string.padding_error);
			} catch (NoContactException e) {
				e.printStackTrace();
				message = context.getString(R.string.unknown_user);
			} catch (NoCharabiaKeyException e) {
				e.printStackTrace();
				message = context.getString(R.string.no_key_for_user);
			}
			
			values.put(OpenHelper.MSG_TYPE, MESSAGE_RECEIVED);
			values.put(OpenHelper.MSG_TEXT, message);
			
		}
		else if(msgType == PUBLIC_KEY_TYPE) {
			values.put(OpenHelper.MSG_TYPE, INVITATION_RECEIVED);
			values.put(OpenHelper.MSG_TEXT, context.getString(R.string.invitation_received));
		}
		else if(msgType == CRYPTED_KEY_TYPE) {
			values.put(OpenHelper.MSG_TYPE, INVITATION_ANSWER);
			values.put(OpenHelper.MSG_TEXT, context.getString(R.string.invitation_answer));
			
		}
		else {
			values.put(OpenHelper.MSG_TYPE, MESSAGE_ERROR);
			values.put(OpenHelper.MSG_TEXT, context.getString(R.string.unexpected_error));
		}
		
		cr.insert(DataProvider.MSG_CONTENT_URI, values);
		
		showNotification(originatingAddress, timeStamp);
	}
	
	public synchronized void sendData(String phoneNumber, int type,  String text, byte[] data) {
 		ContentResolver cr = context.getContentResolver();
 		
		Log.v(TAG, "send block data size = " + data.length);
		Log.v(TAG, "to " + phoneNumber + ", " + bytesToHex(data));
		
		ContentValues values = new ContentValues();
		
		try {
			String lookupKey = getLookupFromPhoneNumber(phoneNumber);
			Uri contactUri = Contacts.lookupContact(context.getContentResolver(),  
					Contacts.getLookupUri(0, lookupKey));
			values.put(OpenHelper.CONTACT_URI, contactUri.toString());
		} catch (NoContactException e1) {
			e1.printStackTrace();
		}

		values.put(OpenHelper.PHONE, phoneNumber);
		values.put(OpenHelper.MSG_TYPE, type);
		values.put(OpenHelper.MSG_TEXT, text);
		values.put(OpenHelper.MSG_DATA, data);
		values.put(OpenHelper.MSG_PORT, sms_port);
		values.put(OpenHelper.MSG_STATUS, 0);
		
		Uri uri = cr.insert(DataProvider.MSG_CONTENT_URI, values);
	
		
		Intent iSend = new Intent(SendResultReceiver.ACTION_RESULT_SMS, 
				Uri.parse("message:"+uri.getPathSegments().get(1)));
		PendingIntent piSend = PendingIntent.getBroadcast(context, 0, iSend, 0);
		Log.v(TAG, "sendDataMessage " + phoneNumber + " port " + sms_port);
		SmsManager.getDefault().sendDataMessage(phoneNumber, null, sms_port, 
						data, piSend, null);
 		
 	}

	public synchronized void sendData(Uri msgUri) {
		
		Log.v(TAG, "sendData:" + msgUri);
		
        Uri uri = ContentUris.withAppendedId(DataProvider.MSG_CONTENT_URI, 
        		Long.parseLong(msgUri.getSchemeSpecificPart()));

		ContentResolver cr = context.getContentResolver();
        
        Cursor cursor = cr.query(uri, 
        		new String[] { OpenHelper.PHONE, OpenHelper.MSG_DATA, OpenHelper.MSG_PORT, OpenHelper.COUNTER },
        		null, null, null);
        
        String phoneNumber = null;
        short sms_port = 0;
        byte[] data = null;
        int count = 0;
        
        if(cursor.moveToFirst()) {
            phoneNumber = cursor.getString(cursor.getColumnIndex(OpenHelper.PHONE));
            sms_port = cursor.getShort(cursor.getColumnIndex(OpenHelper.MSG_PORT));
            data = cursor.getBlob(cursor.getColumnIndex(OpenHelper.MSG_DATA));
            count = cursor.getInt(cursor.getColumnIndex(OpenHelper.COUNTER));
        }
        
        cursor.close();
        
        if(phoneNumber != null && sms_port != 0 && data != null) {
        	ContentValues values = new ContentValues();
        	values.put(OpenHelper.COUNTER, count+1);
        	cr.update(uri, values, null, null);
     		Intent iSend = new Intent(SendResultReceiver.ACTION_RESULT_SMS, msgUri);
     		PendingIntent piSend = PendingIntent.getBroadcast(context, 0, iSend, 0);
     		Log.v("CHARABIA", "sendDataMessage again " + phoneNumber + " port " + sms_port);
     		SmsManager.getDefault().sendDataMessage(phoneNumber, null, sms_port, 
     						data, piSend, null);
        }
	}
}
