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

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.ArrayList;
import java.util.Vector;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import android.app.Activity;
import android.app.Dialog;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;

import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.content.DialogInterface;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.GestureOverlayView.OnGesturePerformedListener;
import android.gesture.Prediction;
import android.util.Log;
import android.view.MenuInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View.OnLongClickListener;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.ViewSwitcher;

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

	// Extra text data share by sms
	public static final String SMS_BODY = "sms_body";
	
	// Dialogs
	private static final int SEND_PROGRESS_DIALOG = 1;
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
	
	private String prefPhoneNumber = null;
	
	// Vector of lookup key of contact to send message
	private Vector<String> recipientsList = new Vector<String>();
	
	private GestureLibrary mLibrary = null;
	
	// Utilities class instance
	private Tools tools = new Tools(this);
	
	// Max length of data that can be send by sms
	private static final int BLOCK_SIZE = 127;
	
	// Message part currently sending block are BLOCK_SIZE bytes length max
	private int mFragment;
	
	private byte[] key = null;
	private String phoneNumber = null;
	
	// Manage state changes
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString("prefPhoneNumber", prefPhoneNumber);
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
		prefPhoneNumber = savedInstanceState.getString("prefPhoneNumber");
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
				titleMessageView.setText(getResources().getString(R.string.title_message,  
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
			}
		}
		
		// Convenient way to refresh list
		removeFromRecipientsList(-1);
	}

	public void buttonOptions(View view) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		intent.setClassName(this, PreferencesActivity.class.getName());
		startActivity(intent);
	}

	public void buttonHelp(View view) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		intent.setClassName(this, WebViewActivity.class.getName());
		intent.setData(Uri.parse(WebViewActivity.getBaseUrl(this, "/help", "index.html")));
		startActivity(intent);
	}
	
	public void buttonAbout(View view) {
		try {
			PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(getString(R.string.app_name));
			builder.setMessage(getString(R.string.info, pi.versionName));
			builder.setIcon(R.drawable.ic_launcher);
			builder.setPositiveButton(R.string.quit, null);
			builder.show();
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public void buttonDirectory(View view) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setClassName(this, PickContactActivity.class.getName());
		startActivity(intent);
	}

	public void buttonInvite(View view) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setClassName(this, SmsViewActivity.class.getName());
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		startActivity(intent);
	}

	/* Handles item selections */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.main_menu_options:
				buttonOptions(null);
				return true;
			case R.id.main_menu_edit: 
				buttonDirectory(null);
				return true;
			case R.id.main_menu_help:
				buttonHelp(null);
				return true;
			case R.id.main_menu_about:
				buttonAbout(null);
				return true;
			case R.id.main_menu_quit:
				buttonQuit(null);
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

	public synchronized void addRecipient(View view) {
		Intent intent = new Intent(Intent.ACTION_PICK);
		intent.setClassName(this, PickContactActivity.class.getName());
		startActivityForResult(intent, PICK_CONTACT);
	}

	@Override
	public void onActivityResult(int reqCode, int resultCode, Intent data) {
		super.onActivityResult(reqCode, resultCode, data);

		switch (reqCode) {
        	case PICK_CONTACT:
        		if (resultCode == RESULT_OK) {
        			addToRecipientsList(data.getData().getSchemeSpecificPart());
        		}
	            break;
        	case ADD_CONTACT:
                try {
  					new Tools(this).updateOrCreateContactKey(phoneNumber, key);
  		           	Toast.makeText(this, getString(R.string.contact_added) + "\n" + phoneNumber, Toast.LENGTH_LONG).show();
  		    	} 
  		        catch (NoContactException e) {
  					e.printStackTrace();
  	        		Toast.makeText(this, R.string.error_create_key, Toast.LENGTH_LONG).show();
  		        }
  	    		break;
 		}

  }

 	public void buttonQuit(View view) {
		finish();
	}
 	
 	public void clear(View view) {
 		recipientsList.clear();
 		recipientsView.setText("");
 	}
 	
  	private synchronized void sendFragment(ContentResolver cr) throws Exception, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, NoLookupKeyException, NoCharabiaKeyException  {
 		
  		String phoneNumber = recipientsList.get(0);
 		
		String texte = messageView.getText().toString();

		int start = mFragment*BLOCK_SIZE;
		int end = (start+BLOCK_SIZE)<texte.length()?(start+BLOCK_SIZE):texte.length();
		String subText = texte.substring(start, end);
		byte[] data = tools.encrypt(phoneNumber, subText.getBytes());
		
		tools.sendData(phoneNumber, Tools.MESSAGE, subText, data);
 	}
 
	public synchronized void sendMessage() {
		ContentResolver cr = getContentResolver();
		
		try {
			do {
	      		mFragment=0;
	    		while(mFragment*BLOCK_SIZE < messageView.length()) {
	    			sendFragment(cr);
	    			mFragment++;
	    		}
			}while(removeFromRecipientsList(0)==false);
			
			messageView.setText("");
			finish();
		}
		catch(Exception e) {
			e.printStackTrace();
			Toast.makeText(this, R.string.unexpected_error, Toast.LENGTH_LONG).show();
		}
		
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
	            addRecipient(null);
	        } else if ("SEND".equals(action)) {
	            buttonSend(null);
	        } else if ("OUT".equals(action) || "QUIT".equals(action)) {
	            buttonQuit(null);
	        } else if ("CLEAR".equals(action)) {
	            clear(null);
	        } 
	    }	
	}
	
}
