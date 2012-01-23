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

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.spec.RSAPublicKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;

import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Intents;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.Toast;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter.ViewBinder;

/**
 * @author 
 *
 */
public class ShareKeyActivity extends FragmentActivity 
{

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
        		
	      // Create the list fragment and add it as our sole content.
        if (getSupportFragmentManager().findFragmentById(android.R.id.content) == null) {
        	CursorLoaderListFragment listFragment = new CursorLoaderListFragment();
            getSupportFragmentManager().beginTransaction().add(android.R.id.content, listFragment).commit();
        }
		
	}
		
	public static class viewBinder implements ViewBinder {

		private Context context;
		private Tools tools;
		
		public viewBinder(Context context) {
			this.context = context;
			tools = new Tools(context);
		}
			
		@Override
		public boolean setViewValue(View v, Cursor cursor, int columnIndex) {
			if(cursor.getColumnIndex(OpenHelper.ID) == columnIndex) {
				
				ImageView iv = (ImageView)v.findViewById(R.id.photo);
				TextView tv = (TextView)v.findViewById(R.id.line1);

				String phoneNumber = cursor.getString(cursor.getColumnIndex(OpenHelper.PHONE));
				
				try {

					String lookupKey = tools.getLookupFromPhoneNumber(phoneNumber);
					
					Uri contactUri = Contacts.lookupContact(context.getContentResolver(),  
							Contacts.getLookupUri(-1, lookupKey));

					Cursor cname = context.getContentResolver().query(contactUri, 
							new String[] { Contacts.DISPLAY_NAME }, null, null, null);
					
					StringBuffer displayName = new StringBuffer();
					
					if(cname.moveToFirst()) {
						displayName.append(cname.getString(0));
					}
					else {
						displayName.append(context.getString(R.string.unknow));
					}
					
					cname.close();
					
					displayName.append("\n");
					displayName.append(phoneNumber);
					
					//tv.setTextColor(Color.GREEN);
					tv.setText(displayName);
					
					java.io.InputStream input = Contacts.openContactPhotoInputStream(
							context.getContentResolver(),
							contactUri);
					
					if(input != null) {
						iv.setImageBitmap(BitmapFactory.decodeStream(input));
						try {
							input.close();
						} 
						catch (IOException e) {
							e.printStackTrace();
						}
					}
					else {
						iv.setImageResource(R.drawable.ic_launcher);
					}

					return true;
				}
				catch(Exception e) {
					e.printStackTrace();
					iv.setImageResource(android.R.drawable.ic_secure);
					//tv.setTextColor(Color.RED);
					tv.setText("erreur" + "\n" + phoneNumber);
					return true;
				}
			}
			
			return false;
		}
		
	}

	public static class CursorLoaderListFragment extends ListFragment 
		implements LoaderManager.LoaderCallbacks<Cursor> 
	{
		
		private static final String TAG = "CHARABIA";
		
		// Loader
		private static final int CONTACTS_LOADER = 1;

		// Intent result
		private static final int ADD_CONTACT = 1;
		
		private SimpleCursorAdapter mAdapter = null;
		
		private Tools tools;

		private String phoneNumber = "";
		private byte[] aesKey = null;
		private byte[] pubKey = null;
		private byte[] sendData = null;
		private long id = -1;
		
		private static final String SMS_SENT = "com.charabia.ShareKeyActivity.SMS_SENT";
		
		private ProgressDialog dialog = null;
		
		@Override
		public void onSaveInstanceState(Bundle outState) {
			super.onSaveInstanceState(outState);
		}
		
		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            
            tools = new Tools(getActivity());
            
            setEmptyText(getActivity().getString(R.string.no_contact));
            
            mAdapter = new SimpleCursorAdapter(getActivity(), 
            		R.layout.list_item, null, 
            		new String[] { 
            			OpenHelper.ID,
            		}, 
            		new int[] { R.id.item },
            		0);
            
            mAdapter.setViewBinder(new viewBinder(getActivity()));
            
            setListAdapter(mAdapter);
            
            setListShown(false);
            
            getLoaderManager().initLoader(CONTACTS_LOADER, null, this);
            
            if(savedInstanceState != null) {
            }
            
            getActivity().registerReceiver(sendReceiver, new IntentFilter(SMS_SENT));
		}
		
		private void deleteId() {
			ContentResolver cr = getActivity().getContentResolver();
			
			cr.delete(ContentUris.withAppendedId(DataProvider.PUBKEYS_CONTENT_URI, id), null, null);
			
			getLoaderManager().restartLoader(CONTACTS_LOADER, null, this);
		}
		
		private void send() {
			
			dialog = new ProgressDialog(getActivity(), 
					ProgressDialog.STYLE_SPINNER);
			dialog.setCancelable(false);
			dialog.setTitle(getString(R.string.send_message));
			dialog.setMessage(phoneNumber);
			dialog.show();
			
			Intent iSend = new Intent(SMS_SENT);
			PendingIntent piSend = PendingIntent.getBroadcast(getActivity(), 0, iSend, 0);
			SmsManager.getDefault().sendDataMessage(phoneNumber, null, CharabiaActivity.sms_port, sendData, piSend, null);
			
		}
		
		@Override
		public void onListItemClick(ListView lv, View v, int position, long id) {
			
			this.id = id;
			
			Cursor cursor = mAdapter.getCursor();
	
			
			if(cursor.moveToPosition(position)) {
				phoneNumber = cursor.getString(cursor.getColumnIndex(OpenHelper.PHONE));
				pubKey = cursor.getBlob(cursor.getColumnIndex(OpenHelper.KEY));
			}
			
	
			Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(R.string.app_name);
			builder.setMessage(R.string.query_chare_key);
			builder.setNegativeButton(R.string.no,  
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						deleteId();
					}
				}
			);
			builder.setPositiveButton(R.string.yes, 
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						aesKey = tools.generateKeyAES().getEncoded();
						
						try {
							Cipher rsaCipher;

							rsaCipher = Cipher.getInstance(Tools.RSA_CIPHER_ALGO);

							KeyFactory keyFact = KeyFactory.getInstance("RSA");
							
							PublicKey rsaPubkey = keyFact.generatePublic(
									new RSAPublicKeySpec(new BigInteger(pubKey), RSAKeyGenParameterSpec.F4));

							rsaCipher.init(Cipher.ENCRYPT_MODE, rsaPubkey);
							
							sendData = new byte[5 + rsaCipher.getOutputSize(aesKey.length)];
							
							sendData[0] = Tools.MAGIC[0];
							sendData[1] = Tools.MAGIC[1];
							sendData[2] = Tools.CRYPTED_KEY_TYPE;
							rsaCipher.doFinal(aesKey, 0, aesKey.length, sendData, 5);

							tools.updateOrCreateContactKey(phoneNumber, aesKey);
	
							send();
							
							return;
							
						} 
						catch (NoSuchAlgorithmException e) {
							e.printStackTrace();
						} 
						catch (NoSuchPaddingException e) {
							e.printStackTrace();
						} 
						catch (InvalidKeySpecException e) {
							e.printStackTrace();
						} 
						catch (InvalidKeyException e) {
							e.printStackTrace();
						} 
						catch (ShortBufferException e) {
							e.printStackTrace();
						} 
						catch (IllegalBlockSizeException e) {
							e.printStackTrace();
						} 
						catch (BadPaddingException e) {
							e.printStackTrace();
						} 
						catch (NoContactException e) {
							e.printStackTrace();

							// No contact with this phone so ask for create it
							Intent newIntent = new Intent(Intents.SHOW_OR_CREATE_CONTACT);
							newIntent.setData(Uri.fromParts("tel", phoneNumber, null));
							startActivityForResult(newIntent, ADD_CONTACT);
							return;
						}
						
						Toast.makeText(getActivity(), R.string.unexpected_error, Toast.LENGTH_LONG).show();
					}	
             	});
			builder.create().show();
		}
	
		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {

	        return new CursorLoader(getActivity(), 
	        		DataProvider.PUBKEYS_CONTENT_URI,
	        		new String[] { 
	        			OpenHelper.ID, 
	        			OpenHelper.PHONE, 
	        			OpenHelper.KEY
	        		}, 
	        		null, 
	        		null,
	        		null);
		}

		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
			mAdapter.swapCursor(data);

			// The list should now be shown.
            if (isResumed()) {
                setListShown(true);
            } 
            else {
                setListShownNoAnimation(true);
            }			
		}

		@Override
		public void onLoaderReset(Loader<Cursor> loader) {
			mAdapter.swapCursor(null);
		}

		@Override
		public void onActivityResult(int reqCode, int resultCode, Intent data) {
			super.onActivityResult(reqCode, resultCode, data);

			switch (reqCode) {
				case ADD_CONTACT:
					try {
						tools.updateOrCreateContactKey(phoneNumber, aesKey);
						send();
					} catch (NoContactException e) {
						e.printStackTrace();
					}
					break;
			}
		}

		private BroadcastReceiver sendReceiver = new BroadcastReceiver()
		{
			@Override
			public void onReceive(Context context, Intent intent)
			{
				String info = "Send information: ";

				int result = R.string.fail_send;
				
				switch(getResultCode())
				{
					case Activity.RESULT_OK: 
						info += "send ok"; 
						result = R.string.send_success; 
						deleteId();
						return;
					case SmsManager.RESULT_ERROR_GENERIC_FAILURE: info += "send failed, generic failure"; break;
					case SmsManager.RESULT_ERROR_NO_SERVICE: info += "send failed, no service"; break;
					case SmsManager.RESULT_ERROR_NULL_PDU: info += "send failed, null pdu"; break;
					case SmsManager.RESULT_ERROR_RADIO_OFF: info += "send failed, radio is off"; break;
				}

				Log.v("CHARABIA", info);
				
				dialog.dismiss();

				Toast.makeText(getActivity(), result, Toast.LENGTH_LONG).show();
			}
		};
	
		@Override
		public void onDestroy()
	    {
	    	getActivity().unregisterReceiver(sendReceiver);
	        super.onDestroy();
	    }

	}
}
