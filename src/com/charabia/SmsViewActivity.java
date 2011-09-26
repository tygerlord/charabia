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

import android.view.View;

import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import android.content.Intent;
import android.content.ContentResolver;
import android.database.Cursor;

import android.telephony.SmsMessage;


public class SmsViewActivity extends Activity
{

	private TextView from = null;
	private TextView message = null;
	//private Button next = null;

	//private MySmsManager msm = null;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.viewsms);
		
		from = (TextView) findViewById(R.id.from);
		message = (TextView) findViewById(R.id.message);
		//next = (Button) findViewById(R.id.next);
		
		//msm = new MySmsManager(getApplicationContext());
		
		//doNext(null);
 	}
	
	@Override 
	public void onResume() {
		super.onResume();
	
		Intent intent = getIntent();
		
		if(intent == null) {
			Toast.makeText(this, text, duration)
			return;
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		new Tools(this).showNotification();
		
	}

	public void answer(View view) {	
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.setClassName(this, CharabiaActivity.class.getName());
		intent.putExtra("TO", from.getText());
		startActivity(intent);
	}

	public void quit(View view) {
		finish();
	}

	/*
	 * @brief Action called to view next message
	 */
//	public void doNext(View view) {
//	
//		quit(view);
//		
//		int nbMessages = msm.getNbMessages();
//		
//		if(nbMessages <= 0) {
//			finish();
//			return;
//		}
//		else if(nbMessages == 1) {
//			next.setText(getString(R.string.quit));
//		}
//		else {
//			next.setText(getString(R.string.next));
//		}
//		
//		SmsMessage sms = msm.readSMS();
//		if(sms == null) {
//			message.setText(getString(R.string.unexpected_error));
//			return;
//		}
//		
//		String phoneNumber = sms.getDisplayOriginatingAddress() ;
//		from.setText(phoneNumber);
//		
//		SmsCipher cipher = new SmsCipher(this);
//		String result = cipher.decrypt(SmsCipher.demo_key, phoneNumber, sms.getMessageBody());
//		
//		//TODO: preference
//		ContentResolver contentResolver = getApplicationContext().getContentResolver();
//		Tools.putSmsToDatabase(contentResolver, phoneNumber, 
//			System.currentTimeMillis(), Tools.MESSAGE_TYPE_INBOX,
//			sms.getStatus(), result);
//
//		message.setText(result);
//		
//		msm.removeSMS();
//	}

}
