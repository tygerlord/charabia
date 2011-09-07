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

import android.os.Bundle;

import android.content.DialogInterface;

import android.net.Uri;

import android.content.Intent;
import android.content.Context;
import android.content.ContentResolver;

import android.text.format.Time;

import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import android.provider.Contacts;  
import android.provider.Contacts.People;

import android.widget.Toast;
import android.widget.EditText;

import android.database.Cursor;

import android.telephony.SmsManager;

public class CharabiaActivity extends Activity
{
	private OpenHelper openHelper = new OpenHelper(this);

	// Menus
	private static final int SETTINGS_ID = 1;
	private static final int ADD_ID = SETTINGS_ID + 1;
	private static final int HELP_ID = ADD_ID + 1;
	private static final int ABOUT_ID = HELP_ID + 1;
	private static final int QUIT_ID = ABOUT_ID + 1;

	// Dialogs
	private static final int MODE_DIALOG = 0;

	private static final int MODE_MAITRE = 0;
	private static final int MODE_ESCLAVE = 1;

	private static final int PICK_CONTACT = 0;
	
	private EditText to = null;
	private EditText message = null;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		to = (EditText) findViewById(R.id.to);
		message = (EditText) findViewById(R.id.message);
		
		message.setText(
			android.util.Base64.encodeToString(Tools.generateKeyAES(getApplicationContext()).getEncoded(), 
			android.util.Base64.DEFAULT));
		
	//	Intent intent = new Intent(Intent.ACTION_SENDTO);
	//	intent.setData(Uri.parse("sms:"));
	//	startActivity(intent);
		
	//	ContentResolver contentResolver = getContentResolver();
	//	Time t = new Time(); t.setToNow();
	//	putSmsToDatabase(contentResolver, "0102030405", t.toMillis(false), SmsManager.STATUS_ON_ICC_UNREAD, "Hello World!!!");
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		Intent intent = getIntent();
		String action = intent == null ? null : intent.getAction();
		//String dataString = intent == null ? null : intent.getDataString();
		//int id = intent == null ? -2 : intent.getShortExtra("ID", (short)-1);

		if (intent != null && action != null) 
		{
			if(action.equals(Intent.ACTION_VIEW)) {
				//String url = "sms:12345678?body=hello%20there";
				//intent = new Intent(Intent.ACTION_SENDTO);
				//intent.setData(Uri.parse(url));
				//intent.setData("toto");
				//startActivity(intent);
				intent = new Intent(Intent.ACTION_PICK, android.provider.Contacts.People.CONTENT_URI); 
				startActivity(intent);
			}
			
			if(action.equals(Intent.ACTION_MAIN)) {
				String str = intent.getStringExtra("TO");
				if(str != null) {
					to.setText(str);
				}
			}


		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, SETTINGS_ID, 0, R.string.options)
			.setIcon(android.R.drawable.ic_menu_preferences);
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
				//intent.setClassName(this, PreferencesActivity.class.getName());
				startActivity(intent);
				return true;
			case ADD_ID:
				showDialog(MODE_DIALOG);
				return true;
			case HELP_ID:
				intent = new Intent(Intent.ACTION_VIEW);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
				//intent.setClassName(this, WebViewActivity.class.getName());
				intent.setData(Uri.parse("file:///android_asset/html/help/index.html"));
				startActivity(intent);
				return true;
			case ABOUT_ID:
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(getString(R.string.app_name));
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
						/*
                        if(itemsAdd[i].equals(getString(R.string.generate_key)))
                        {
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                                intent.setClassName(getApplicationContext(), GenerateKeyActivity.class.getName());
                                startActivity(intent);
                        }
						*/
			}
		};

	public void hello(View view) {
		Toast.makeText(getApplicationContext(), "ok", Toast.LENGTH_LONG).show();
	}
	
	public void add_to(View view) {
		Intent intent = new Intent(Intent.ACTION_PICK, People.CONTENT_URI);//Uri.parse("content://contacts/people"));
		startActivityForResult(intent, PICK_CONTACT);
	}
	
	public void onActivityResult(int reqCode, int resultCode, Intent data) {
      super.onActivityResult(reqCode, resultCode, data);

      switch (reqCode) {
        case (PICK_CONTACT) :
          if (resultCode == Activity.RESULT_OK) {
              Uri contactData = data.getData();
                Cursor c =  managedQuery(contactData, null, null, null, null);
                startManagingCursor(c);
                if (c.moveToFirst()) {
                  String name = c.getString(c.getColumnIndexOrThrow(People.NAME));  
                  String number = c.getString(c.getColumnIndexOrThrow(People.NUMBER));
                  //perrsonname.setText(name);
                  Toast.makeText(this,  name + " has number " + number, Toast.LENGTH_LONG).show();
                 }
           }
         break;
      }

  }

 	public void quit(View view) {
		finish();
	}

	public void send(View view) {
		
		String phoneNumber = to.getText().toString();
		String texte = message.getText().toString();
		
		//TODO: preference
		ContentResolver contentResolver = getApplicationContext().getContentResolver();
		Tools.putSmsToDatabase(contentResolver, phoneNumber, 
			System.currentTimeMillis(), Tools.MESSAGE_TYPE_SENT,
			0, texte);

		String cryptedTexte = Tools.encrypt(getApplicationContext(), phoneNumber, texte);
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