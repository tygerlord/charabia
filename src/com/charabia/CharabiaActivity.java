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

import android.app.Activity;
import android.app.Dialog;
import android.app.AlertDialog;

import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;

import android.content.DialogInterface;

import android.content.Intent;
import android.content.Context;
import android.content.ContentResolver;
import android.content.SharedPreferences;

import android.util.Base64;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import android.widget.Toast;
import android.widget.TextView;
import android.widget.EditText;

import android.database.sqlite.SQLiteDatabase;

import android.telephony.SmsManager;
import android.telephony.TelephonyManager;

import com.google.zxing.integration.android.IntentIntegrator;

public class CharabiaActivity extends Activity
{
	// Menus
	private static final int SETTINGS_ID = 1;
	private static final int DELETE_ID = SETTINGS_ID+1;
	private static final int ADD_ID = DELETE_ID + 1;
	private static final int HELP_ID = ADD_ID + 1;
	private static final int ABOUT_ID = HELP_ID + 1;
	private static final int QUIT_ID = ABOUT_ID + 1;

	// Dialogs
	private static final int MODE_DIALOG = 0;

	// List of intent 
	private static final int PICK_CONTACT = 0;
	
	private TextView to = null;
	private EditText message = null;
	
	// Keys share mode
	private static final int MODE_MAITRE = 0;
	private static final int MODE_ESCLAVE = 1;

	private int mode = MODE_MAITRE;
	
	private byte[] key = null;
	
	private String phonenumber = null;
	
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
			//intent.setData(Uri.parse("file:///android_asset/html/help/enter_phone_number.html"));
			intent.setData(Uri.parse(WebViewActivity.getBaseUrl(this, "/help", "enter_phone_number.html")));
			startActivity(intent);
		}


		Intent intent = getIntent();
		String action = intent == null ? null : intent.getAction();

		if (intent != null && action != null) 
		{
			if(action.equals(Intent.ACTION_MAIN)) {
				String str = intent.getStringExtra("TO");
				if(str != null) {
					to.setText(str);
				}
			}
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		
		Tools.showNotification(getApplicationContext());
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, SETTINGS_ID, 0, R.string.options)
			.setIcon(android.R.drawable.ic_menu_preferences);
		menu.add(0, DELETE_ID, 0, R.string.delete)
			.setIcon(android.R.drawable.ic_menu_delete);
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
			case DELETE_ID:
				return true;
			case ADD_ID:
				showDialog(MODE_DIALOG);
				return true;
			case HELP_ID:
				intent = new Intent(Intent.ACTION_VIEW);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
				intent.setClassName(this, WebViewActivity.class.getName());
				//intent.setData(Uri.parse("file:///android_asset/html/help/index.html"));
				intent.setData(Uri.parse(WebViewActivity.getBaseUrl(this, "/help", "index.html")));
				startActivity(intent);
				return true;
			case ABOUT_ID:
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(getString(R.string.app_name));
				//builder.setMessage(Tools.getDisplayName(this,s));
				//builder.setMessage(getString(R.string.apropos) + "\n\n" + getString(R.string.urlweb));
				//builder.setIcon(R.drawable.charabia_icon);
				//builder.setPositiveButton(R.string.ouvrir_navigateur, aboutListener);
				//builder.setNegativeButton(R.string.abandon, null);
				builder.show();
				return true;
			case QUIT_ID:
				finish();
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
				TelephonyManager tm = (TelephonyManager)CharabiaActivity.this.getSystemService(Context.TELEPHONY_SERVICE); 
				mode = i;
				int size = key.length/2;
				switch(mode) {
					case MODE_ESCLAVE:
						//Slave 
						IntentIntegrator.shareText(CharabiaActivity.this, 
								tm.getVoiceMailNumber() + "\n" +
								Base64.encodeToString(key,size,size,Base64.DEFAULT));
						IntentIntegrator.initiateScan(CharabiaActivity.this);							
						break;
					case MODE_MAITRE:
					default:
						//Master
						//flagReadRightKeyPart = true;
						IntentIntegrator.initiateScan(CharabiaActivity.this);							
						IntentIntegrator.shareText(CharabiaActivity.this, 
								tm.getVoiceMailNumber() + "\n" +
								Base64.encodeToString(key,0,size,Base64.DEFAULT));
				}
		}
	};

	public void add_to(View view) {
		Intent intent = new Intent(PickContactActivity.class.getName());//Uri.parse("content://contacts/people"));
		startActivityForResult(intent, PICK_CONTACT);
	}
	
	@Override
	public void onActivityResult(int reqCode, int resultCode, Intent data) {
		super.onActivityResult(reqCode, resultCode, data);

		switch (reqCode) {
        	case (PICK_CONTACT):
        		if (resultCode == RESULT_OK) {
        			int id = data.getIntExtra("ID", -1);
        			String phoneNumber = data.getStringExtra("PHONE");
                    Toast.makeText(this,  "Contact ID=" + id, Toast.LENGTH_LONG).show();
        			to.setText(to.getText() + "\n" + phoneNumber);
        		}
	          break;
        	case(IntentIntegrator.REQUEST_CODE):
	            if (resultCode == RESULT_OK) {
	                OpenHelper oh = new OpenHelper(this);
	                SQLiteDatabase db = oh.getWritableDatabase();
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
		                
		                oh.insert(db, infos[0], key);
	            	}
	            	catch(Exception e) {
	            		Toast.makeText(this, R.string.error_create_key, Toast.LENGTH_LONG).show();
	            	}
	                
	                db.close();
	            }
        		break;
	         	
		}

  }

 	public void quit(View view) {
		finish();
	}
 	
 	public void clear(View view) {
 		to.setText("");
 	}
 	
	public void send(View view) {
		
		String phoneNumber = to.getText().toString();
		String texte = message.getText().toString();
		
		if(phoneNumber.equals("")) {
			Toast.makeText(this, "no phone number", Toast.LENGTH_LONG).show();
			return;
		}
		
		if(texte.equals("")) {
			Toast.makeText(this, "message empty", Toast.LENGTH_LONG).show();
			return;		
		}
		
		//TODO: preference
		ContentResolver contentResolver = getApplicationContext().getContentResolver();
		Tools.putSmsToDatabase(contentResolver, phoneNumber, 
			System.currentTimeMillis(), Tools.MESSAGE_TYPE_SENT,
			0, texte);

		SmsCipher cipher = new SmsCipher(this);
		String cryptedTexte = cipher.encrypt(SmsCipher.demo_key, phoneNumber, texte);
		if(cryptedTexte != null) {
			SmsManager.getDefault().sendTextMessage(phoneNumber, null, cryptedTexte, null, null);
		
			to.setText("");
			message.setText("");
		}
		else {
			Toast.makeText(getApplicationContext(), getString(R.string.fail_send), Toast.LENGTH_LONG).show();
		}
	}

}
