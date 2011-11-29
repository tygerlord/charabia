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

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.Vector;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import android.app.Activity;
import android.app.Dialog;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;

import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Intents;
import android.content.DialogInterface;

import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.GestureOverlayView.OnGesturePerformedListener;
import android.gesture.Prediction;
import android.util.Base64;
import android.util.Log;
import android.view.MenuInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View.OnLongClickListener;

import android.widget.Toast;
import android.widget.TextView;
import android.widget.EditText;

import android.telephony.SmsManager;
import android.text.Editable;
import android.text.TextWatcher;

import com.google.zxing.integration.android.IntentIntegrator;

// TODO option preference to store or not message
// TODO more help messages
// TODO more error logs and trace
// TODO option preference to increase key size for key exchange
// TODO menu to test key and re-exchange key (perhaps by sms/mail too)

public class CharabiaActivity extends Activity implements OnGesturePerformedListener
{
	
	// Tag used to log messages
	private static final String TAG = "CHARABIA";

	//port where data sms are send
	private static final short sms_port = 1981;
	
	// Extra text data share by sms
	public static final String SMS_BODY = "sms_body";
	
	// Dialogs
	private static final int MODE_DIALOG = 0;
	private static final int SEND_PROGRESS_DIALOG = MODE_DIALOG + 1;
	private static final int SEND_ERROR_DIALOG = SEND_PROGRESS_DIALOG + 1;
	private static final int EDIT_TO_DIALOG = SEND_ERROR_DIALOG + 1;
	
	// List of intent 
	private static final int PICK_CONTACT = 1;
	private static final int ADD_CONTACT = PICK_CONTACT + 1;
	
	// widgets
	private TextView titleRecipientView = null;
	private TextView recipientsView = null;
	private TextView titleMessageView = null;
	private EditText messageView = null;
	
	// Keys share mode
	private static final int MODE_MAITRE = 0;
	private static final int MODE_ESCLAVE = 1;

	// store the mode of key exchange
	private int mode = MODE_MAITRE;

	// RSA keypair use to process exchange of key 
	private KeyPair keypair = null;
	
	private String prefPhoneNumber = null;
	
	// Vector of lookup key of contact to send message
	private Vector<String> recipientsList = new Vector<String>();
	
	private GestureLibrary mLibrary = null;
	
	private static final String SMS_SENT = "com.charabia.SMS_SENT";
	private static final String SMS_DELIVERED = "com.charabia.SMS_DELIVERED";
	
	// Utilities class instance
	private Tools tools = new Tools(this);
	
	// Max length of data that can be send by sms
	private static final int BLOCK_SIZE = 112;
	
	// Message part currently sending block are 112 byte length max
	private int mFragment;
	
	private byte[] key = null;
	private String phoneNumber = null;
	

	// Manage state changes
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("mode", mode);
		outState.putString("prefPhoneNumber", prefPhoneNumber);
		outState.putSerializable("keypair", keypair);
		outState.putSerializable("recipientsList", recipientsList);
		outState.putInt("mFragment", mFragment);
		outState.putBoolean("dismissAction", dismissAction);
		outState.putByteArray("key", key);
		outState.putString("phoneNumber", phoneNumber);
	}
	
	@SuppressWarnings("unchecked")
	@Override 
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		mode = savedInstanceState.getInt("mode");
		prefPhoneNumber = savedInstanceState.getString("prefPhoneNumber");
		keypair = (KeyPair) savedInstanceState.getSerializable("keypair");
		recipientsList = (Vector<String>) savedInstanceState.getSerializable("recipientsList");
		mFragment = savedInstanceState.getInt("mFragment");
		dismissAction = savedInstanceState.getBoolean("dismissAction");
		key = savedInstanceState.getByteArray("key");
		phoneNumber = savedInstanceState.getString("phoneNumber");
	}
	
	/*
	 * use addToRecipientsList(null) to refresh
	 */
	private synchronized void addToRecipientsList(String phoneNumber) {
		if(phoneNumber != null) {
			recipientsList.add(phoneNumber);
		}
		// convenient way to refresh list view
		removeFromRecipientsList(-1);
	}

	/*
	 * return true if recipientsList is empty
	 */
	private synchronized boolean removeFromRecipientsList(int index) {
		if(index>=0 && index < recipientsList.size()) {
			recipientsList.remove(index);
		}
		StringBuffer strBuf = new StringBuffer();
		String phoneNumber = null;
		for(int i = 0; i < recipientsList.size(); i++) {
			if(i>0) strBuf.append("\n");
			phoneNumber = recipientsList.get(i);
			strBuf.append(tools.getDisplayName(phoneNumber) + ", " + phoneNumber);
		}
		recipientsView.setText(strBuf.toString());
		return recipientsList.isEmpty();
	}
	
	/** Called when the activity is first created. */
	@SuppressWarnings("unused")
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	
        registerReceiver(sendreceiver, new IntentFilter(SMS_SENT));
        registerReceiver(deliveredreceiver, new IntentFilter(SMS_DELIVERED));

        setContentView(R.layout.main);
        
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if(prefs.getBoolean(PreferencesActivity.GESTURES_MODE, true)) {
			mLibrary = GestureLibraries.fromRawResource(this, R.raw.gestures);
			if (!mLibrary.load()) {
			    finish();
			}
			
			GestureOverlayView gestures = (GestureOverlayView) findViewById(R.id.gestures);
			gestures.addOnGesturePerformedListener(this);
		}

		titleRecipientView = (TextView) findViewById(R.id.title_recipients);
		recipientsView = (TextView) findViewById(R.id.recipients);
		titleMessageView = (TextView) findViewById(R.id.title_message);
		messageView = (EditText) findViewById(R.id.message);	
		
		recipientsView.addTextChangedListener(new TextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
				}
	
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count,
						int after) {
				}
	
				@Override
				public void onTextChanged(CharSequence s, int start, int before,
						int count) {
					Log.v("CHARABIA", "toList.size = " + recipientsList.size());
					
					titleRecipientView.setText(getResources().getQuantityString(R.plurals.to, 
							recipientsList.size(), recipientsList.size()));
				}
			}
		);
		
		recipientsView.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View arg0) {
				// be sure to rebuild all the dialog 
				removeDialog(EDIT_TO_DIALOG);
				showDialog(EDIT_TO_DIALOG);
				return false;
			}
			
		});
		
		messageView.addTextChangedListener(new TextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				int lg = messageView.length();
				titleMessageView.setText(getResources().getString(R.string.message,  
						lg, (lg/BLOCK_SIZE)+1));
			}
		});
		
		titleRecipientView.setText(getResources().getQuantityString(R.plurals.to, 
				recipientsList.size(), recipientsList.size()));

		int lg = messageView.length();
		titleMessageView.setText(getResources().getString(R.string.message,  
				lg, (lg/BLOCK_SIZE)+1));
	}
	
	@Override
	protected void onResume() {
		super.onResume();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefPhoneNumber = prefs.getString(PreferencesActivity.PHONE_NUMBER, null);
		if(prefPhoneNumber == null || prefPhoneNumber.length() <= 0) {
			
			Intent intent;
	
			intent = new Intent(Intent.ACTION_VIEW);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			intent.setClassName(this, PreferencesActivity.class.getName());
			startActivity(intent);
	
			intent = new Intent(Intent.ACTION_VIEW);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			intent.setClassName(this, WebViewActivity.class.getName());
			intent.setData(Uri.parse(WebViewActivity.getBaseUrl(this, "/help", "enter_phone_number.html")));
			startActivity(intent);
			
			
			//Attempt to retrieve old keys
			
			android.content.ContentResolver cr = getContentResolver();
			android.database.Cursor cursor = cr.query(Data.CONTENT_URI, 
					new String[] { Data._ID, Tools.PHONE, Tools.KEY },
					Data.MIMETYPE + "=?",
					new String[] { Tools.CONTENT_ITEM_TYPE },
					null);
			while(cursor.moveToNext()) {
				try {
					tools.updateOrCreateContactKey(
						cursor.getString(cursor.getColumnIndex(Tools.PHONE)), 
						Base64.decode(cursor.getString(cursor.getColumnIndex(Tools.KEY)),
								Base64.DEFAULT),
								false);
					
					cr.delete(ContentUris.withAppendedId(Data.CONTENT_URI, 
							cursor.getLong(cursor.getColumnIndex(Data._ID))), 
							null, null);
				} catch (NoContactException e) {
					e.printStackTrace();
					Toast.makeText(this, "No contact for " + cursor.getColumnIndex(Tools.PHONE), 
							Toast.LENGTH_SHORT).show();
				}
				
			}
		}


		Intent intent = getIntent();
		String action = intent == null ? null : intent.getAction();

		if(intent != null && action != null) 
		{
			if(action.equals(Intent.ACTION_SENDTO)) {
				// uri = smsto:(...)
				Uri uri = intent.getData();
				
				String texte = null;
				
				if(uri != null) {
					texte = uri.getSchemeSpecificPart();
				}
				
				Log.v(TAG, "send to uri = " + uri + ", "  + texte);
				
				if(texte != null && texte.length()>0) {
					try {
						tools.getKey(texte);
					} 
					catch (NoContactException e) {
						e.printStackTrace();
						Toast.makeText(this, R.string.no_contact, Toast.LENGTH_SHORT).show();
						finish();
					} 
					catch (NoCharabiaKeyException e) {
						e.printStackTrace();
						Toast.makeText(this, R.string.no_key_for_user, Toast.LENGTH_SHORT).show();
						finish();
					}
	
					addToRecipientsList(texte);
				}
				
				if(intent.hasExtra(Intent.EXTRA_TEXT)) {
					messageView.setText(intent.getCharSequenceExtra(Intent.EXTRA_TEXT));
				}
				else if(intent.hasExtra(SMS_BODY)) {
					messageView.setText(intent.getCharSequenceExtra(SMS_BODY));
				}
				
				StringBuffer s = new StringBuffer();
				Bundle extras = intent.getExtras();
				
				java.util.Set<String> set = extras.keySet();
				
				String[] result =new String[set.size()]; 
				set.toArray(result);

				StringBuffer buf = new StringBuffer();
				for(int i = 0; i < result.length; i++) {
					if(i>0) buf.append("\n");
					buf.append(result[i]);
				}
				Log.v(TAG, buf.toString());
			}
		}
		
		// Convenient way to refresh list
		removeFromRecipientsList(-1);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	/* Handles item selections */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;
		switch (item.getItemId()) {
			case R.id.main_menu_options: 
				intent = new Intent(Intent.ACTION_VIEW);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
				intent.setClassName(this, PreferencesActivity.class.getName());
				startActivity(intent);
				return true;
			case R.id.main_menu_edit: 
				intent = new Intent(Intent.ACTION_VIEW);
				intent.setClassName(this, PickContactActivity.class.getName());
				startActivity(intent);
				return true;
			case R.id.main_menu_keys:
				showDialog(MODE_DIALOG);
				return true;
			case R.id.main_menu_help:
				intent = new Intent(Intent.ACTION_VIEW);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
				intent.setClassName(this, WebViewActivity.class.getName());
				intent.setData(Uri.parse(WebViewActivity.getBaseUrl(this, "/help", "index.html")));
				startActivity(intent);
				return true;
			case R.id.main_menu_about:
				try {
					PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
					AlertDialog.Builder builder = new AlertDialog.Builder(this);
					builder.setTitle(getString(R.string.app_name));
					builder.setMessage(getString(R.string.info, pi.versionName));
					builder.setIcon(R.drawable.ic_launcher);
					builder.setPositiveButton(R.string.quit, null);
					builder.show();
					return true;
				} catch (NameNotFoundException e) {
					e.printStackTrace();
				}
				return false;
			case R.id.main_menu_quit:
				finish();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}


	/* Handles dialogs */
	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		AlertDialog.Builder builder;
		switch(id) {
			case MODE_DIALOG:
				builder = new AlertDialog.Builder(this);
				builder.setTitle(getString(R.string.app_name));
				builder.setItems(new String[] { 
					getString(R.string.master), 
					getString(R.string.slave) }, modeListener);
				dialog = builder.create();
				break;
			case SEND_PROGRESS_DIALOG:
				dialog = new ProgressDialog(this, 
						ProgressDialog.STYLE_SPINNER);
				dialog.setCancelable(false);
				dialog.setTitle(getString(R.string.send_message));
				onPrepareDialog(SEND_PROGRESS_DIALOG, dialog);
				break;
			case SEND_ERROR_DIALOG:
				builder = new AlertDialog.Builder(this);
				builder.setTitle(getString(R.string.app_name));
				builder.setMessage(getString(R.string.error_sending_message));
				builder.setNegativeButton(getString(R.string.cancel), sendErrorDialogListener);	
				builder.setPositiveButton(getString(R.string.try_again), sendErrorDialogListener);
				dialog = builder.create();
				dialog.setOnDismissListener(sendErrorDismissListener);
				onPrepareDialog(SEND_ERROR_DIALOG, dialog);
				break;
			case EDIT_TO_DIALOG:
				builder = new AlertDialog.Builder(this);
				builder.setTitle(getString(R.string.remove_to));
				builder.setNegativeButton(getString(R.string.cancel), editToDialogListener);
				builder.setPositiveButton(getString(R.string.clear_all), editToDialogListener);
				String texte = recipientsView.getText().toString();
				if(!texte.equals("")) {
					builder.setSingleChoiceItems(recipientsView.getText().toString().split("\n"), 
							-1, editToDialogListener);
				}
				dialog = builder.create();
				break;
			default:
				dialog = null;
		}
		return dialog;
	}
	
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch(id) {
			case SEND_PROGRESS_DIALOG:
				/*
				 * It seems that when state is restored dialog creation is restarted
				 * with a call to onPrepareDialog (even if onCreateDialog don't call 
				 * onPrepareDialog) so we must check if list is not empty before get 
				 * element 0. This happen only if dialog was previously created.
				 * 
				 *    This can be called when recipientsList is empty after
				 *    onSavedInstanceState, onRestoreInstanceState sequence
				 *    if instance of this dialog exist when onSavedInstanceState
				 *    is called.
				 */
				if(!recipientsList.isEmpty()) {
					String phoneNumber = recipientsList.get(0);
					((ProgressDialog) dialog).setMessage(tools.getDisplayName(phoneNumber) + "\n" + phoneNumber);
				}
				break;
			case SEND_ERROR_DIALOG:
				/*
				 * see SEND_PROGRESS_DIALOG comment. 
				 */
				dismissAction = false;
				if(!recipientsList.isEmpty()) {
					String phoneNumber = recipientsList.get(0);
					((AlertDialog) dialog).setMessage(getString(R.string.error_sending_message_to,  
							tools.getDisplayName(phoneNumber) + "\n" + phoneNumber));
				}
				break;
		}
	}
			
	private final DialogInterface.OnClickListener modeListener =
		new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialogInterface, int i) {
				mode = i;
				switch(mode) {
					case MODE_ESCLAVE:
						IntentIntegrator.initiateScan(CharabiaActivity.this);							
						break;
					case MODE_MAITRE:
					default:
						//Master
						KeyPairGenerator gen;
						try {
							gen = KeyPairGenerator.getInstance("RSA");
						} catch (NoSuchAlgorithmException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							Toast.makeText(getApplicationContext(), R.string.unexpected_error, Toast.LENGTH_LONG).show();
							return;
						}
						//TODO preference to increase key size and so increase security
						// but this increase amount of data to show in QRcode and can
						// be more difficult to read
						gen.initialize(256);
						keypair = gen.generateKeyPair();
						RSAPublicKey pubKey = (RSAPublicKey) keypair.getPublic();
						
						IntentIntegrator.initiateScan(CharabiaActivity.this);							
						IntentIntegrator.shareText(CharabiaActivity.this, 
								prefPhoneNumber + "\n" +
								pubKey.getModulus() + "\n" + 
								pubKey.getPublicExponent());
			}

		}
	};

	private final DialogInterface.OnDismissListener sendErrorDismissListener =
		new DialogInterface.OnDismissListener() {
			public void onDismiss(DialogInterface dialogInterface) {
				if(dismissAction) {
					sendMessage();
				}
			}
		};

	/*
	 * dismissAction set to True for continue sending
	 */
	protected boolean dismissAction;
	
	private final DialogInterface.OnClickListener sendErrorDialogListener =
		new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialogInterface, int i) {
				
				switch(i) {
					case AlertDialog.BUTTON_NEGATIVE:
						// Pass this message and continue to send
						removeFromRecipientsList(0);
						if(!recipientsList.isEmpty()) {
							dismissAction = true;
						}
						break;
					case AlertDialog.BUTTON_POSITIVE:
						// Try again
						dismissAction = true;
						break;
					default:
						dismissAction = false;
						break;
				}
				
			}
		};

	private final DialogInterface.OnClickListener editToDialogListener =
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialogInterface, int i) {
					
					switch(i) {
						case AlertDialog.BUTTON_NEGATIVE:
							break;
						case AlertDialog.BUTTON_POSITIVE:
							recipientsList.clear();
							recipientsView.setText("");
							break;
						default:
							//Toast.makeText(getApplicationContext(), "clicked="+i, Toast.LENGTH_LONG).show();
							removeFromRecipientsList(i);
							removeDialog(EDIT_TO_DIALOG);
							break;
					}
					
				}
			};

	public synchronized void add_to(View view) {
		Intent intent = new Intent(Intent.ACTION_PICK);
		intent.setClassName(this, PickContactActivity.class.getName());
		startActivityForResult(intent, PICK_CONTACT);
	}

	@Override
	public void onActivityResult(int reqCode, int resultCode, Intent data) {
		super.onActivityResult(reqCode, resultCode, data);

		switch (reqCode) {
        	case (PICK_CONTACT):
        		if (resultCode == RESULT_OK) {
        			addToRecipientsList(data.getData().getSchemeSpecificPart());
        		}
	           break;
        	case (ADD_CONTACT):
                try {
  					new Tools(this).updateOrCreateContactKey(phoneNumber, key);
  		           	Toast.makeText(this, getString(R.string.contact_added) + "\n" + phoneNumber, Toast.LENGTH_LONG).show();
  		    	} 
  		        catch (NoContactException e) {
  					e.printStackTrace();
  	        		Toast.makeText(this, R.string.error_create_key, Toast.LENGTH_LONG).show();
  		        }
  	    		break;
        	case(IntentIntegrator.REQUEST_CODE):
	            if (resultCode == RESULT_OK) {
	                try {
		            	String contents = data.getStringExtra("SCAN_RESULT");
		                @SuppressWarnings("unused")
						String format = data.getStringExtra("SCAN_RESULT_FORMAT");
		                // Handle successful scan
		                
		        		// TODO: add more tests control
		                
		                String[] infos = contents.split("\n");
		                
						Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
						
						if(mode == MODE_ESCLAVE) {
							// Save key and show crypted key on QRCode
							SmsCipher cipher = new SmsCipher(CharabiaActivity.this);
							key = cipher.generateKeyAES().getEncoded();
							
							KeyFactory keyFact = KeyFactory.getInstance("RSA");
							
							PublicKey pubkey = keyFact.generatePublic(
									new RSAPublicKeySpec(new BigInteger(infos[1]), 
											new BigInteger(infos[2])));
							
							rsaCipher.init(Cipher.ENCRYPT_MODE, pubkey);
							
							int blockSize = rsaCipher.getBlockSize();
							
							int nbBlock = key.length/blockSize;
							int reste = key.length%blockSize;
							
							byte[] cryptedKey = new byte[(nbBlock+1)*rsaCipher.getOutputSize(blockSize)];
							
							int offset = 0;
							
							for(int i = 0; i < nbBlock; i++){
								offset += rsaCipher.doFinal(key, i*blockSize, blockSize, cryptedKey, offset);
							}
							
							rsaCipher.doFinal(key, nbBlock*blockSize, reste, cryptedKey, offset);
							
							IntentIntegrator.shareText(CharabiaActivity.this, 
									prefPhoneNumber + "\n" +
									Base64.encodeToString(cryptedKey,Base64.NO_WRAP));
							
						}
						else {
							
							// We have read crypted key, so decode it
							rsaCipher.init(Cipher.DECRYPT_MODE, keypair.getPrivate());
							
							byte[] cryptedData = Base64.decode(infos[1], Base64.NO_WRAP);

							int blockSize = rsaCipher.getBlockSize();
							int nbBlock = cryptedData.length/blockSize;
							
							int offset = 0;
												
							byte[] tempKey = new byte[(nbBlock+1)*blockSize];
							
							for(int i = 0; i < nbBlock; i++) {
								offset += rsaCipher.doFinal(cryptedData, i*blockSize, blockSize, tempKey, offset);
							}
							
							key = new byte[offset];
							System.arraycopy(tempKey, 0, key, 0, offset);
						}
		                
						phoneNumber = infos[0];
						
						// store the key
						// TODO dialog to confirm add contact in mode SLAVE
		                try {
							new Tools(this).updateOrCreateContactKey(phoneNumber, key);
						} 
		                catch (NoContactException e) {
							e.printStackTrace();
							// propose to add contact
							Intent newIntent = new Intent(Intents.SHOW_OR_CREATE_CONTACT);
							newIntent.setData(Uri.fromParts("tel", phoneNumber, null));
							startActivityForResult(newIntent, ADD_CONTACT);
							return;
		                }
		                		                
		               	Toast.makeText(this, getString(R.string.contact_added) + "\n" + phoneNumber, Toast.LENGTH_LONG).show();
		                
	            	}
	            	catch(Exception e) {
	            		e.printStackTrace();
	            		Toast.makeText(this, R.string.error_create_key, Toast.LENGTH_LONG).show();
	            	}
	                
	            }
	            else {
	            	// TODO: string
	            	Toast.makeText(this, R.string.fail_reading_tag, Toast.LENGTH_LONG).show();
	            }
        		break;
	         	
		}

  }

 	public void quit(View view) {
		finish();
	}
 	
 	public void clear(View view) {
 		recipientsList.clear();
 		recipientsView.setText("");
 	}
 	
 	/*
 	 * Use to send a message from list
 	 */
 	private synchronized void sendFragment() throws Exception, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, NoLookupKeyException, NoCharabiaKeyException  {
 		
  		String phoneNumber = recipientsList.get(0);
 		
		String texte = messageView.getText().toString();

		SmsCipher cipher = new SmsCipher(this);
		int start = mFragment*BLOCK_SIZE;
		int end = (start+BLOCK_SIZE)<texte.length()?(start+BLOCK_SIZE):texte.length();
		byte[] data = cipher.encrypt(tools.getKey(phoneNumber), 
				texte.substring(start, end));

		Intent iSend = new Intent(SMS_SENT);
		Intent iDelivered = new Intent(SMS_DELIVERED);
		PendingIntent piSend = PendingIntent.getBroadcast(this, 0, iSend, 0);
		//PendingIntent piDelivered = PendingIntent.getBroadcast(this, 0, iDelivered, 0);
		//TODO piDelivered?
		SmsManager.getDefault().sendDataMessage(phoneNumber, null, sms_port, data, piSend, null);
 	}
 
	public synchronized void sendMessage() {

		showDialog(SEND_PROGRESS_DIALOG);

		try {
			sendFragment();
			return;
		} catch (Throwable e) {
			e.printStackTrace();
		}

		dismissDialog(SEND_PROGRESS_DIALOG);
		showDialog(SEND_ERROR_DIALOG);
		
	}

	/*
	 * Called by button send
	 */
	public void buttonSend(View view) {
		if(recipientsList.isEmpty()) {
			Toast.makeText(this, R.string.no_recipient, Toast.LENGTH_LONG).show();
			return;
		}

		if(messageView.length()<=0) {
			Toast.makeText(this, R.string.empty_message, Toast.LENGTH_LONG).show();
			return;		
		}
		
		mFragment = 0;
		sendMessage();
	}
	
	@Override
	public void onGesturePerformed(GestureOverlayView overlay, Gesture gesture) {
	    ArrayList<Prediction> predictions = mLibrary.recognize(gesture);
	    if (predictions.size() > 0 && predictions.get(0).score > 1.0) {
	        String action = predictions.get(0).name;
	        if ("HELP".equals(action) || "HELP2".equals(action)) {
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
				intent.setClassName(this, WebViewActivity.class.getName());
				intent.setData(Uri.parse(WebViewActivity.getBaseUrl(this, "/help", "CharabiaActivity.html")));
				startActivity(intent);
	        } else if ("ADD".equals(action)) {
	            add_to(null);
	        } else if ("SEND".equals(action)) {
	            buttonSend(null);
	        } else if ("OUT".equals(action) || "QUIT".equals(action)) {
	            quit(null);
	        } else if ("CLEAR".equals(action)) {
	            clear(null);
	        } else if ("MODE".equals(action)) {
	        	showDialog(MODE_DIALOG);
	        }
	    }	
	}
	
	private BroadcastReceiver deliveredreceiver = new BroadcastReceiver()
    {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                    String info = "Delivery information: ";
                    
                    switch(getResultCode())
                    {
                            case Activity.RESULT_OK: info += "delivered"; break;
                            case Activity.RESULT_CANCELED: info += "not delivered"; break;
                    }
                    
                    Toast.makeText(getBaseContext(), info, Toast.LENGTH_SHORT).show();
            }
    };
    
    private BroadcastReceiver sendreceiver = new BroadcastReceiver()
    {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                    String info = "Send information: ";
                    
                    switch(getResultCode())
                    {
                            case Activity.RESULT_OK: 

                            	//TODO: preference
                        		tools.putSmsToDatabase(recipientsList.get(0), 
                        				System.currentTimeMillis(), 
                        				Tools.MESSAGE_TYPE_SENT,
                        				0, 
                        				messageView.getText().toString());
                        		
                        		mFragment+=1;
                        		if(mFragment*BLOCK_SIZE < messageView.length()) {
                        			sendMessage();
                        		}
                        		else if(removeFromRecipientsList(0)) {
                            		clear(null);
                            		dismissDialog(SEND_PROGRESS_DIALOG);
                            		messageView.setText("");
                            	}
                            	else {
                            		mFragment = 0;
                            		sendMessage();
                            	}
                            	Toast.makeText(getBaseContext(), R.string.send_success, Toast.LENGTH_SHORT).show();
                            	return;
                            case SmsManager.RESULT_ERROR_GENERIC_FAILURE: info += "send failed, generic failure"; break;
                            case SmsManager.RESULT_ERROR_NO_SERVICE: info += "send failed, no service"; break;
                            case SmsManager.RESULT_ERROR_NULL_PDU: info += "send failed, null pdu"; break;
                            case SmsManager.RESULT_ERROR_RADIO_OFF: info += "send failed, radio is off"; break;
                    }
                    
                    //Toast.makeText(getBaseContext(), info, Toast.LENGTH_SHORT).show();
                    Log.v("CHARABIA", info);
                    
                    dismissDialog(SEND_PROGRESS_DIALOG);
                    showDialog(SEND_ERROR_DIALOG);
                    
            }
    };


    @Override
    protected void onDestroy()
    {
    	unregisterReceiver(sendreceiver);
    	unregisterReceiver(deliveredreceiver);
    	
        super.onDestroy();
    }

}
