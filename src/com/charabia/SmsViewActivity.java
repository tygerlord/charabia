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

import android.os.Bundle;

import android.app.Activity;

import android.util.Log;

import android.view.View;

import android.widget.TextView;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.widget.Button;

import android.content.Intent;
import android.content.ContentResolver;

import android.database.Cursor;

import android.net.Uri;

import android.telephony.SmsMessage;


import java.util.ArrayList;

public class SmsViewActivity extends Activity
{
	private static final String TAG = "SmsViewActivity";

	public static java.util.Vector<SmsMessage> smsListe = new java.util.Vector<SmsMessage>();

	private TextView from = null;
	private TextView message = null;
	private Button next = null;


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.viewsms);
		
		from = (TextView) findViewById(R.id.from);
		message = (TextView) findViewById(R.id.message);
		next = (Button) findViewById(R.id.next);
		
		doNext(null);
        //Cursor c = getContentResolver().query(Uri.parse("content://sms"), null, null /*"address=12345678"*/,
          //         null, null);
 
		//ArrayList<String> liste = new ArrayList<String>();
			
		// if(c != null) {
			// c.moveToFirst ();
		
			// do {
				// String str = c.getString(c.getColumnIndexOrThrow("address")) + "\n" +
					// c.getString(c.getColumnIndexOrThrow("person")) + "\n" +
					// c.getInt(c.getColumnIndexOrThrow("type")) + "\n" +
					// c.getInt(c.getColumnIndexOrThrow("read")) + "\n" +
					// c.getString(c.getColumnIndexOrThrow("date")) + "\n" +
					// c.getString(c.getColumnIndexOrThrow("body")) + "\n";
				// liste.add(str);
			// }while(c.moveToNext());
		// }
		
		//String [] str = liste.toArray(new String[liste.size()]);

		//String[] listeStrings = {"France","Allemagne","Russie"};
		//setListAdapter(new ArrayAdapter<String>(this, R.layout.list_item, str));//listeStrings));
	}
	
	@Override
	protected void onResume() {
		super.onResume();

		/*
		onNewIntent(getIntent());
		
		Intent intent = getIntent();
		String action = intent == null ? null : intent.getAction();
		byte[] data = intent == null ? null : intent.getByteArrayExtra("DATA");

		
        Log.v(TAG, "intent=" + intent + ","+ action + "," + data);
		
		
		if (intent != null && action != null) {
			if(action.equals(SmsViewActivity.class.getName())) {
				if(data != null) {
					SmsMessage sms = SmsMessage.createFromPdu(data);
					if(sms != null) {
						Log.v(TAG, "info:" + sms.getDisplayOriginatingAddress() + "," + sms.getMessageBody());
					
						from.setText(sms.getDisplayOriginatingAddress());
						message.setText(sms.getMessageBody());
					}
				}
			}
		}
		*/

	}

	@Override
	protected void onPause() {
		super.onPause();
		
		Tools.showNotification(getApplicationContext());
		
	}

	public void answer(View view) {
	
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.setClassName(this, CharabiaActivity.class.getName());
		intent.putExtra("TO", from.getText());
		startActivity(intent);
	}

	public void hello(View view) {
		Toast.makeText(getApplicationContext(), "send", Toast.LENGTH_LONG).show();
	}

	public void quit(View view) {
		finish();
	}

	public void doNext(View view) {
	
		Toast.makeText(getApplicationContext(), "doNext " + smsListe.size(), Toast.LENGTH_LONG).show();
	
		if(smsListe.isEmpty()) {
			finish();
		}
		else {
			SmsMessage sms = smsListe.get(0);
			smsListe.remove(0);
			
			String phoneNumber = sms.getDisplayOriginatingAddress() ;
			from.setText(phoneNumber);
			
			String result = Tools.decrypt(getApplicationContext(), phoneNumber, sms.getMessageBody());
			
			//TODO: preference
			ContentResolver contentResolver = getApplicationContext().getContentResolver();
			Tools.putSmsToDatabase(contentResolver, phoneNumber, 
				System.currentTimeMillis(), Tools.MESSAGE_TYPE_INBOX,
				sms.getStatus(), result);

			message.setText(result);
			
			if(smsListe.isEmpty()) {
				next.setText(getString(R.string.quit));
			}
			else {
				next.setText(getString(R.string.next));
			}
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {

		Toast.makeText(getApplicationContext(), "new intent event", Toast.LENGTH_LONG).show();

		// Fixe the original intent to this new intent so getIntent
		// will now return this intent. onResume called after onnewintent
		// use so this new intent.
		// setIntent(intent);

		String action = intent == null ? null : intent.getAction();
		byte[] data = intent == null ? null : intent.getByteArrayExtra("DATA");

		
        Log.v(TAG, "intent=" + intent + ","+ action + "," + data);
		

		if (intent != null && action != null) {
			if(action.equals(SmsViewActivity.class.getName())) {
				if(data != null) {
					SmsMessage sms = SmsMessage.createFromPdu(data);
					if(sms != null) {
						Log.v(TAG, "info:" + sms.getDisplayOriginatingAddress() + "," + sms.getMessageBody());
					
						from.setText(sms.getDisplayOriginatingAddress());
						message.setText(sms.getMessageBody());
					}
				}
			}
		}
		
	}
	
}
