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

import android.app.Activity;
import android.app.Dialog;
import android.app.AlertDialog;

import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.content.DialogInterface;

import android.content.Intent;
import android.content.ContentResolver;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Base64;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import android.widget.Toast;
import android.widget.TextView;
import android.widget.EditText;

import android.telephony.SmsManager;
import com.google.zxing.integration.android.IntentIntegrator;

public class CharabiaActivity extends Activity
{
	// Menus
	private static final int SETTINGS_ID = 1;
	private static final int EDIT_ID = SETTINGS_ID+1;
	private static final int ADD_ID = EDIT_ID + 1;
	private static final int HELP_ID = ADD_ID + 1;
	private static final int ABOUT_ID = HELP_ID + 1;
	private static final int QUIT_ID = ABOUT_ID + 1;

	// Dialogs
	private static final int MODE_DIALOG = 0;

	// List of intent 
	private static final int PICK_CONTACT = 0;
	private static final int PICK_CONTACT_ADD_KEY = PICK_CONTACT + 1;
	
	private TextView to = null;
	private EditText message = null;
	
	// Keys share mode
	private static final int MODE_MAITRE = 0;
	private static final int MODE_ESCLAVE = 1;

	private int mode = MODE_MAITRE;
	
	private byte[] key = null;
	
	private String phonenumber = null;
	
	private ArrayList<Uri> toList = new ArrayList<Uri>();
	
	private void addToList(Uri uri) {
		if(uri != null) {
			Tools tools = new Tools(this);
			toList.add(uri);
			String phoneNumber = tools.getPhoneNumber(uri);
		    CharSequence temp = to.getText();
			to.setText(tools.getDisplayName(phoneNumber)+","+phoneNumber+"\n"+temp);
		}
	}
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		to = (TextView) findViewById(R.id.to);
		message = (EditText) findViewById(R.id.message);	
		
	}
	
	@Override
	protected void onResume() {
		super.onResume();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		phonenumber = prefs.getString(PreferencesActivity.PHONE_NUMBER, null);
		if(phonenumber == null || phonenumber.length() <= 0) {
			
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

		if (intent != null && action != null) 
		{
			if(action.equals(Intent.ACTION_MAIN)) {
				addToList(intent.getData());
			}
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		new Tools(this).showNotification();
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
					builder.setMessage(getString(R.string.info, pi.versionName, pi.versionCode));
					builder.setIcon(R.drawable.ic_launcher);
					builder.setPositiveButton(R.string.quit, null);
					builder.show();
					return true;
				} catch (NameNotFoundException e) {
					e.printStackTrace();
				}
				return false;
			case QUIT_ID:
				//finish();

				
			try {
				new Tools(this).__updateOrCreateContactKey("5556", SmsCipher.demo_key);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (OperationApplicationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
				return true;
			default:
				return false;
		}
	}


	/* Handles dialogs */
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
			default:
				dialog = null;
		}
		return dialog;
	}
		
	private final DialogInterface.OnClickListener modeListener =
		new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialogInterface, int i) {
				SmsCipher cipher = new SmsCipher(CharabiaActivity.this);
				key = cipher.generateKeyAES().getEncoded();
				mode = i;
				int size = key.length/2;
				switch(mode) {
					case MODE_ESCLAVE:
						//Slave 
						IntentIntegrator.shareText(CharabiaActivity.this, 
								phonenumber + "\n" +
								Base64.encodeToString(key,size,size,Base64.DEFAULT));
						IntentIntegrator.initiateScan(CharabiaActivity.this);							
						break;
					case MODE_MAITRE:
					default:
						//Master
						IntentIntegrator.initiateScan(CharabiaActivity.this);							
						IntentIntegrator.shareText(CharabiaActivity.this, 
								phonenumber + "\n" +
								Base64.encodeToString(key,0,size,Base64.DEFAULT));
				}
		}
	};

	public void add_to(View view) {
		Intent intent = new Intent(Intent.ACTION_PICK);
		intent.setType(Tools.CONTENT_ITEM_TYPE);
		startActivityForResult(intent, PICK_CONTACT);
	}
	
	public void pickContact() {
		Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
		startActivityForResult(intent, PICK_CONTACT_ADD_KEY);
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
        	case (PICK_CONTACT_ADD_KEY):
        		// contact to add key
           		if (resultCode == RESULT_OK) {
        			Uri contactUri = data.getData();
        			new Tools(this).updateOrCreateContactKey(contactUri, key);
        		}
        		break;
        	case(IntentIntegrator.REQUEST_CODE):
	            if (resultCode == RESULT_OK) {
	                try {
		            	String contents = data.getStringExtra("SCAN_RESULT");
		                String format = data.getStringExtra("SCAN_RESULT_FORMAT");
		                // Handle successful scan
		                
		                // TODO: add more tests control
		                
		                String[] infos = contents.split("\n");
		                
		                byte[] key_part = Base64.decode(infos[1], Base64.DEFAULT);
		                int size = key.length/2;
		                
		                if(mode == MODE_ESCLAVE) {
		                	System.arraycopy(key_part, 0, key, 0, size);
		                }
		                else {
		                	System.arraycopy(key_part, 0, key, size, size);		                	
		                }
		                
		                new Tools(this).updateOrCreateContactKey(infos[0], key);
		                		                
		               	Toast.makeText(this, R.string.contact_added + "\n" + infos[0], Toast.LENGTH_LONG).show();
		                
	            	}
	            	catch(Exception e) {
	            		Toast.makeText(this, R.string.error_create_key, Toast.LENGTH_LONG).show();
	            	}
	                
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
		
		String phoneNumber;
		Uri uri;
		
		Tools tools = new Tools(this);
		
		for(int i = 0; i < toList.size(); i++)
		{
			try {
				uri = toList.get(i);
				phoneNumber = tools.getPhoneNumber(uri);
		
				//TODO: preference
				ContentResolver contentResolver = getContentResolver();
				Tools.putSmsToDatabase(contentResolver, phoneNumber, 
					System.currentTimeMillis(), Tools.MESSAGE_TYPE_SENT,
					0, texte);
				
				SmsCipher cipher = new SmsCipher(this);
				String cryptedTexte = cipher.encrypt(tools.getKey(uri), texte);
				if(cryptedTexte != null) {
					SmsManager.getDefault().sendTextMessage(phoneNumber, null, cryptedTexte, null, null);
					Toast.makeText(this, getString(R.string.message_send_to, phoneNumber), Toast.LENGTH_SHORT).show();
				}
				else {
					Toast.makeText(this, R.string.fail_send, Toast.LENGTH_LONG).show();
				}
			}
			catch(Exception e) {
				Toast.makeText(this, R.string.unexpected_error, Toast.LENGTH_LONG).show();				
			}
		}
		toList.clear();
		to.setText("");
		message.setText("");
	}
}
