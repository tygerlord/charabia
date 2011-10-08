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

import android.net.Uri;
import android.os.Bundle;

import android.app.Activity;

import android.util.Log;
import android.view.View;

import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import android.content.ContentResolver;
import android.database.Cursor;

import android.telephony.PhoneNumberUtils;
import android.telephony.SmsMessage;


public class SmsViewActivity extends Activity
{

	private static final String TAG = "Charabia.SmsViewActivity";
	
	private TextView from = null;
	private TextView message = null;
	
	private String phoneNumber = null;
	
	private Uri uri = null;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.viewsms);
		
		from = (TextView) findViewById(R.id.from);
		message = (TextView) findViewById(R.id.message);
 	}
	
	@Override 
	public void onResume() {
		super.onResume();
	
		uri = null;
		
		Intent intent = getIntent();
		
		if(intent != null) {
			uri = intent.getData();
		}
		
		if(uri == null) {
			Toast.makeText(this, R.string.unexpected_error, Toast.LENGTH_LONG).show();
			return;
		}
		
		Tools tools = new Tools(this);
		
		try {
			ContentResolver cr = getContentResolver();
			
			Cursor cursor = cr.query(uri, new String[] { OpenHelper.SMS_PDU }, null, null, null);
			if(cursor.moveToFirst()) {
				SmsMessage sms = SmsMessage.createFromPdu(
						cursor.getBlob(cursor.getColumnIndex(OpenHelper.SMS_PDU)));
				phoneNumber = PhoneNumberUtils.formatNumber(
						sms.getDisplayOriginatingAddress());
				from.setText(tools.getDisplayName(phoneNumber) + "," + phoneNumber);
				
				SmsCipher cipher = new SmsCipher(this);
				String result = cipher.decrypt(tools.getKey(phoneNumber), sms.getMessageBody());
				
				//TODO: preference
				ContentResolver contentResolver = getContentResolver();
				Tools.putSmsToDatabase(contentResolver, phoneNumber, 
					sms.getTimestampMillis()+1, Tools.MESSAGE_TYPE_INBOX,
					sms.getStatus(), result);
		
				message.setText(result);
			}
		}
		catch(Exception e) {
			//TODO details more exception
			Log.e(TAG, e.toString());
			Toast.makeText(this, R.string.unexpected_error, Toast.LENGTH_LONG).show();
		}		
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		new Tools(this).showNotification();
		
	}

	public void answer(View view) {	
		try {
			Intent intent = new Intent(Intent.ACTION_MAIN);
			intent.setClassName(this, CharabiaActivity.class.getName());
			intent.setData(new Tools(this).getUriFromPhoneNumber(phoneNumber));
			startActivity(intent);
		}
		catch(Exception e) {
			e.printStackTrace();
			Toast.makeText(this, R.string.unexpected_error, Toast.LENGTH_LONG).show();
		}
	}

	public void quit(View view) {
		
		//delete current view sms before quit
		ContentResolver cr = getContentResolver();
		cr.delete(uri, null, null);
		
		finish();
	}

}
