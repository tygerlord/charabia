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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.util.Log;

public class SendResultReceiver extends BroadcastReceiver 
{
	public static final String ACTION_RESULT_SMS  = "com.charabia.intent.SMS_SENT";

	@Override
	public void onReceive(Context context, Intent intent)
	{
		if (intent.getAction().equals(ACTION_RESULT_SMS)) {
            String info = "Send information: ";
            
            switch(getResultCode())
            {
                    case Activity.RESULT_OK: 
/*
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
*/
                    	return;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE: info += "send failed, generic failure"; break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE: info += "send failed, no service"; break;
                    case SmsManager.RESULT_ERROR_NULL_PDU: info += "send failed, null pdu"; break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF: info += "send failed, radio is off"; break;
            }
            
            Log.v("CHARABIA", info);
            
		}
	} 
}
