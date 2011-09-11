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


import android.util.Log;

import android.telephony.SmsMessage;

import android.content.Context;

import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class MySmsManager
{
	private Context context = null;
	
	private static final String TAG = "CHARABIA_MY_SMS_MANAGER";
	
	public static final String sms_dirname = "messages";
	
	public MySmsManager(Context context) {
		this.context = context;
	}
	
	public void writeSMS(SmsMessage message) {
		try {
			String filename = System.currentTimeMillis() + ".sms"; 
			Log.v(TAG, "writeSMS " + filename); 
			File dir = context.getDir(sms_dirname, Context.MODE_PRIVATE);
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File(dir,filename)));
			oos.writeObject(message.getPdu());
			oos.close();
		}
		catch(Exception e) {
			Log.v(TAG, "error saving sms" + e.toString());
		}
	}

	public SmsMessage readSMS(String filename) {
		Log.v(TAG, "readSMS " + filename); 
		try {
			File dir = context.getDir(sms_dirname, Context.MODE_PRIVATE);
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(dir,filename)));
			byte[] pdu = (byte[]) ois.readObject();
			SmsMessage message = SmsMessage.createFromPdu(pdu);
			ois.close();
			return message;
		}
		catch(Exception e) {
			Log.v(TAG, "error reading sms" + e.toString());
			// Error reading, perhaps file corrupted, try to remove to access next
			removeSMS(filename);
		}
		return null;
	}

	public SmsMessage getLastMessage() {
		File dir = context.getDir(sms_dirname, Context.MODE_PRIVATE);
		String[] filelist = dir.list();
		if(filelist.length>0) {
			return readSMS(filelist[filelist.length-1]);
		}
		return null;
	}

	public SmsMessage getFirstMessage() {
		File dir = context.getDir(sms_dirname, Context.MODE_PRIVATE);
		String[] filelist = dir.list();
		if(filelist.length>0) {
			return readSMS(filelist[0]);
		}
		return null;
	}

	public void removeSMS(String filename) {
		File dir = context.getDir(sms_dirname, Context.MODE_PRIVATE);
		File f = new File(dir, filename);
		
		Log.v(TAG, "remove message " +filename);
		f.delete();
		
	}
	
	public void removeSMS() {
		File dir = context.getDir(sms_dirname, Context.MODE_PRIVATE);
		String[] filelist = dir.list();
		if(filelist.length>0) {
			File f = new File(dir, filelist[0]);
			Log.v(TAG, "remove message " + filelist[0]);
			f.delete();
		}
	}
	
	public int getNbMessages() {
		return context.getDir(sms_dirname, Context.MODE_PRIVATE).list().length;
	}

}
