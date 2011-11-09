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

import javax.crypto.Cipher;

import android.app.Activity;
import android.app.Dialog;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;

import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.content.DialogInterface;

import android.content.BroadcastReceiver;
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
// TODO gesture mode choice on preference gesture only, gesture + buttons
// TODO more help messages
// TODO more error logs and trace
// TODO option preference to increase key size for key exchange
// TODO menu to test key and re-exchange key (perhaps by sms/mail too)
// TODO menu to add aggregation of contact if necessary

public class CharabiaActivity extends Activity implements OnGesturePerformedListener
{
	
	// Tag used to log messages
	private static final String TAG = "CHARABIA";

	//port where data sms are send
	private static final short sms_port = 1981;
	
	// Menus
	private static final int SETTINGS_ID = 1;
	private static final int EDIT_ID = SETTINGS_ID+1;
	private static final int ADD_ID = EDIT_ID + 1;
	private static final int HELP_ID = ADD_ID + 1;
	private static final int ABOUT_ID = HELP_ID + 1;
	private static final int QUIT_ID = ABOUT_ID + 1;

	// Dialogs
	private static final int MODE_DIALOG = 0;
	private static final int SEND_PROGRESS_DIALOG = MODE_DIALOG + 1;
	private static final int SEND_ERROR_DIALOG = SEND_PROGRESS_DIALOG + 1;
	private static final int EDIT_TO_DIALOG = SEND_ERROR_DIALOG + 1;
	
	// List of intent 
	private static final int PICK_CONTACT = 0;
	
	// widgets
	private TextView title_to = null;
	private TextView to = null;
	private EditText message = null;
	
	// Keys share mode
	private static final int MODE_MAITRE = 0;
	private static final int MODE_ESCLAVE = 1;

	// store the mode of key exchange
	private int mode = MODE_MAITRE;

	// RSA keypair use to process exchange of key 
	private KeyPair keypair = null;
	
	private String prefPhoneNumber = null;
	
	// Vector of uris of contact to send message
	private Vector<Uri> toList = new Vector<Uri>();
	
	private GestureLibrary mLibrary = null;
	
	private static final String SMS_SENT = "com.charabia.SMS_SENT";
	private static final String SMS_DELIVERED = "com.charabia.SMS_DELIVERED";
	
	// Utilities class instance
	private Tools tools = new Tools(this);
	
	private ProgressDialog sendProgressDialog;
	
	private void addToList(Uri uri) {
		if(uri != null) {
			toList.add(uri);
			CharSequence temp = to.getText();
			to.setText(tools.getDisplayNameAndPhoneNumber(uri)+"\n"+temp);
		}
	}

	/*
	 * return true if toList is empty
	 */
	private boolean removeFromToList(int index) {
		toList.remove(index);
		StringBuffer strBuf = new StringBuffer();
		for(int i = 0; i < toList.size(); i++) {
			if(i>0) strBuf.append("\n");
			strBuf.append(tools.getDisplayNameAndPhoneNumber(toList.get(i)));
		}
		to.setText(strBuf.toString());
		return toList.isEmpty();
	}
	
	/** Called when the activity is first created. */
	@SuppressWarnings("unused")
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	
        registerReceiver(sendreceiver, new IntentFilter(SMS_SENT));
        registerReceiver(deliveredreceiver, new IntentFilter(SMS_DELIVERED));

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if(prefs.getBoolean(PreferencesActivity.GESTURES_MODE, true)) {
			setContentView(R.layout.main_with_gestures);

			mLibrary = GestureLibraries.fromRawResource(this, R.raw.gestures);
			if (!mLibrary.load()) {
			    finish();
			}
			
			GestureOverlayView gestures = (GestureOverlayView) findViewById(R.id.gestures);
			gestures.addOnGesturePerformedListener(this);
		}
		else {
			setContentView(R.layout.main);
		}

		title_to = (TextView) findViewById(R.id.title_to);
		to = (TextView) findViewById(R.id.to);
		message = (EditText) findViewById(R.id.message);	
		
		to.addTextChangedListener(new TextWatcher() {

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
					Log.v("CHARABIA", "toList.size = " + toList.size());
					
					title_to.setText(getResources().getQuantityString(R.plurals.to, toList.size(), toList.size()));
				}
			}
		);
		
		to.setText("");
		
		to.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View arg0) {
				// be sure to rebuild completly the dialog 
				removeDialog(EDIT_TO_DIALOG);
				showDialog(EDIT_TO_DIALOG);
				return false;
			}
			
		});
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
		}


		Intent intent = getIntent();
		String action = intent == null ? null : intent.getAction();

		if(intent != null && action != null) 
		{
			if(action.equals(Intent.ACTION_VIEW)) {
				// call by http uri
				//message.setText(intent.getData().toString());
				Uri uri = intent.getData();
				if(uri != null) {
					String phoneNumber = uri.getLastPathSegment();
					try {
						addToList(tools.getUriFromPhoneNumber(phoneNumber));
					} 
					catch (NoLookupKeyException e) {
						e.printStackTrace();
						Toast.makeText(this, R.string.unexpected_error, Toast.LENGTH_LONG).show();
						finish();
					} 
					catch (NoCharabiaKeyException e) {
						e.printStackTrace();
						Toast.makeText(this, R.string.no_key_for_user, Toast.LENGTH_LONG).show();
						finish();
					}
				}
			}
			else if(action.equals(Intent.ACTION_SENDTO)) {
				// uri = sms:(...)
				Uri uri = intent.getData();
				try {
					Log.v(TAG, "send to uri = " + uri + ", "  + uri.getSchemeSpecificPart());
					addToList(tools.getUriFromPhoneNumber(uri.getSchemeSpecificPart()));
				} 
				catch (NoLookupKeyException e) {
					e.printStackTrace();
					Toast.makeText(this, R.string.unexpected_error, Toast.LENGTH_LONG).show();
					finish();
				} 
				catch (NoCharabiaKeyException e) {
					e.printStackTrace();
					Toast.makeText(this, R.string.no_key_for_user, Toast.LENGTH_LONG).show();
					finish();
				}
				catch (Exception e) {
					e.printStackTrace();
					Toast.makeText(this, R.string.unexpected_error, Toast.LENGTH_LONG).show();
					finish();
				}
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, SETTINGS_ID, 0, R.string.options)
			.setIcon(android.R.drawable.ic_menu_preferences);
		menu.add(0, EDIT_ID, 0, R.string.edit)
			.setIcon(android.R.drawable.ic_menu_edit);
		menu.add(0, ADD_ID, 0, R.string.keys)
			.setIcon(android.R.drawable.ic_menu_add);
		menu.add(0, HELP_ID, 0, R.string.help)
			.setIcon(android.R.drawable.ic_menu_help);
		menu.add(0, ABOUT_ID, 0, R.string.about)
			.setIcon(android.R.drawable.ic_menu_info_details);
		menu.add(0, QUIT_ID, 0, R.string.quit)
			.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
		return true;
	}

	/* Handles item selections */
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;
		switch (item.getItemId()) {
			case SETTINGS_ID:
				intent = new Intent(Intent.ACTION_VIEW);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
				intent.setClassName(this, PreferencesActivity.class.getName());
				startActivity(intent);
				return true;
			case EDIT_ID:
				intent = new Intent(Intent.ACTION_VIEW);
				intent.setClassName(this, PickContactActivity.class.getName());
				startActivity(intent);
				return true;
			case ADD_ID:
				showDialog(MODE_DIALOG);
				return true;
			case HELP_ID:
				intent = new Intent(Intent.ACTION_VIEW);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
				intent.setClassName(this, WebViewActivity.class.getName());
				intent.setData(Uri.parse(WebViewActivity.getBaseUrl(this, "/help", "index.html")));
				startActivity(intent);
				return true;
			case ABOUT_ID:
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
			case QUIT_ID:
				finish();
				return true;
			default:
				return false;
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
				dialog = sendProgressDialog = new ProgressDialog(this, 
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
				onPrepareDialog(SEND_ERROR_DIALOG, dialog);
				break;
			case EDIT_TO_DIALOG:
				builder = new AlertDialog.Builder(this);
				builder.setTitle(getString(R.string.remove_to));
				builder.setNegativeButton(getString(R.string.cancel), editToDialogListener);
				builder.setPositiveButton(getString(R.string.clear_all), editToDialogListener);
				String texte = to.getText().toString();
				if(!texte.equals("")) {
					builder.setSingleChoiceItems(to.getText().toString().split("\n"), -1, editToDialogListener);
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
				((ProgressDialog) dialog).setMessage(tools.getDisplayNameAndPhoneNumber(toList.get(0)));
				break;
			case SEND_ERROR_DIALOG:
				((AlertDialog) dialog).setMessage(getString(R.string.error_sending_message_to,  
						tools.getDisplayNameAndPhoneNumber(toList.get(0))));
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

	private final DialogInterface.OnClickListener sendErrorDialogListener =
		new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialogInterface, int i) {
				
				switch(i) {
					case AlertDialog.BUTTON_NEGATIVE:
						//pass this message and continue to send
						removeFromToList(0);
						break;
					case AlertDialog.BUTTON_POSITIVE:
						break;
					default:
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
							toList.clear();
							to.setText("");
							break;
						default:
							//Toast.makeText(getApplicationContext(), "clicked="+i, Toast.LENGTH_LONG).show();
							removeFromToList(i);
							removeDialog(EDIT_TO_DIALOG);
							break;
					}
					
				}
			};

	public void add_to(View view) {
		Intent intent = new Intent(Intent.ACTION_PICK);
		intent.setType(Tools.CONTENT_ITEM_TYPE);
		startActivityForResult(intent, PICK_CONTACT);
	}

	@Override
	public void onActivityResult(int reqCode, int resultCode, Intent data) {
		super.onActivityResult(reqCode, resultCode, data);

		switch (reqCode) {
        	case (PICK_CONTACT):
        		if (resultCode == RESULT_OK) {
        			addToList(data.getData());
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
		                
						byte[] key = null;
						
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
		                
						// store the key
						// TODO dialog to confirm add contact in mode SLAVE
		                new Tools(this).updateOrCreateContactKey(infos[0], key);
		                		                
		               	Toast.makeText(this, getString(R.string.contact_added) + "\n" + infos[0], Toast.LENGTH_LONG).show();
		                
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
 		toList.clear();
 		to.setText("");
 	}
 	
 	/*
 	 * Use to send a message from list
 	 */
 	private void sendMessage() throws Throwable {
 		
 		Uri uri = toList.get(0);
 		
		String phoneNumber = tools.getPhoneNumber(uri);

		sendProgressDialog.setMessage(phoneNumber);
		
		String texte = message.getText().toString();

		SmsCipher cipher = new SmsCipher(this);
		byte[] data = cipher.encrypt(tools.getKey(uri), texte);

		Intent iSend = new Intent(SMS_SENT);
		Intent iDelivered = new Intent(SMS_DELIVERED);
		PendingIntent piSend = PendingIntent.getBroadcast(this, 0, iSend, 0);
		PendingIntent piDelivered = PendingIntent.getBroadcast(this, 0, iDelivered, 0);
		//TODO piDelivered?
		SmsManager.getDefault().sendDataMessage(phoneNumber, null, sms_port, data, piSend, null);
 	}
 
	public void send(View view) {

		if(toList.isEmpty()) {
			Toast.makeText(this, R.string.no_to, Toast.LENGTH_LONG).show();
			return;
		}

		String texte;
		texte = message.getText().toString();
		if(texte.equals("")) {
			Toast.makeText(this, R.string.empty_message, Toast.LENGTH_LONG).show();
			return;		
		}
		
		showDialog(SEND_PROGRESS_DIALOG);

		try {
			sendMessage();
		} catch (Throwable e) {
			e.printStackTrace();
			dismissDialog(SEND_PROGRESS_DIALOG);
			showDialog(SEND_ERROR_DIALOG);
		}
		
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
	            send(null);
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
                        		tools.putSmsToDatabase(tools.getPhoneNumber(toList.get(0)), 
                        				System.currentTimeMillis(), 
                        				Tools.MESSAGE_TYPE_SENT,
                        				0, 
                        				message.getText().toString());
                        		
                            	if(removeFromToList(0)) {
                            		message.setText("");
                            		dismissDialog(SEND_PROGRESS_DIALOG);
                            	}
                            	else {
                            		try {
                            			sendMessage();
                            		} 
                            		catch (Throwable e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
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
