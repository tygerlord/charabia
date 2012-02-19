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

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.spec.RSAPublicKeySpec;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import javax.crypto.Cipher;

import com.google.zxing.integration.android.IntentIntegrator;

import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Intents;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.Toast;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
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
public class SmsViewActivity extends FragmentActivity 
{

	private static final int MODE_DIALOG = 0;
	
	// Keys share mode
	private static final int MODE_MAITRE = 0;
	private static final int MODE_ESCLAVE = MODE_MAITRE + 1;
	private static final int MODE_SMS = MODE_ESCLAVE + 1;

	// List of intent 
	private static final int SMS_KEY_CONTACT = 1;
	private static final int ADD_CONTACT = SMS_KEY_CONTACT + 1;

	// store the mode of key exchange
	private int mode = MODE_MAITRE;

	// RSA keypair use to process exchange of key 
	private KeyPair keypair = null;
	
	private String prefPhoneNumber = null;
	private byte[] key = null;
	private String phoneNumber = null;
	
	// Utilities class instance
	private Tools tools = new Tools(this);

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
        		
	      // Create the list fragment and add it as our sole content.
        if (getSupportFragmentManager().findFragmentById(android.R.id.content) == null) {
        	CursorLoaderListFragment listFragment = new CursorLoaderListFragment();
            getSupportFragmentManager().beginTransaction().add(android.R.id.content, listFragment).commit();
        }

		try {
			String texte = "bonjour";
			
			byte[] data = tools.encrypt("15555215556", texte.getBytes());
			
			Log.v("CHARABIA", "data="+Base64.encodeToString(data, Base64.NO_WRAP));
			Log.v("CHARABIA", "data="+Tools.bytesToHex(data));
			
			String result = new String(tools.decrypt("15555215556", data));
			
			Log.v("CHARABIA", "result="+result);
			
		}
		catch(Exception e) {
			e.printStackTrace();
		}

	}

	// Manage state changes
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("mode", mode);
		outState.putString("prefPhoneNumber", prefPhoneNumber);
		outState.putSerializable("keypair", keypair);
		outState.putByteArray("key", key);
		outState.putString("phoneNumber", phoneNumber);
	}
	
	@Override 
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		mode = savedInstanceState.getInt("mode");
		prefPhoneNumber = savedInstanceState.getString("prefPhoneNumber");
		keypair = (KeyPair) savedInstanceState.getSerializable("keypair");
		key = savedInstanceState.getByteArray("key");
		phoneNumber = savedInstanceState.getString("phoneNumber");
	}

	@Override 
	public void onResume() {
		super.onResume();
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefPhoneNumber = prefs.getString(PreferencesActivity.PHONE_NUMBER, null);
		if(prefPhoneNumber == null || prefPhoneNumber.length() <= 0) {
			
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
			
			
			//Attempt to retrieve old keys
			
			android.content.ContentResolver cr = getContentResolver();
			android.database.Cursor cursor = cr.query(Data.CONTENT_URI, 
					new String[] { Data._ID, Tools.PHONE, Tools.KEY },
					Data.MIMETYPE + "=?",
					new String[] { Tools.CONTENT_ITEM_TYPE },
					null);
			while(cursor.moveToNext()) {
				try {
					tools.updateOrCreateContactKey(
						cursor.getString(cursor.getColumnIndex(Tools.PHONE)), 
						Base64.decode(cursor.getString(cursor.getColumnIndex(Tools.KEY)),
								Base64.DEFAULT),
								false);
					
					cr.delete(ContentUris.withAppendedId(Data.CONTENT_URI, 
							cursor.getLong(cursor.getColumnIndex(Data._ID))), 
							null, null);
				} catch (NoContactException e) {
					e.printStackTrace();
					Toast.makeText(this, "No contact for " + cursor.getColumnIndex(Tools.PHONE), 
							Toast.LENGTH_SHORT).show();
				}
				
			}
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		//return super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}
	
	public void buttonShare(View v) {
		showDialog(MODE_DIALOG);
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

	public void buttonEdit(View view) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setClassName(this, CharabiaActivity.class.getName());
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		startActivity(intent);
	}

 	public void buttonQuit(View view) {
		finish();
	}

	/* Handles item selections */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.main_menu_options:
				buttonOptions(null);
				return true;
			case R.id.main_menu_edit: 
				buttonEdit(null);
				return true;
			case R.id.main_menu_share:
				buttonShare(null);
				return true;
			case R.id.main_menu_contacts: 
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
			case MODE_DIALOG:
				builder = new AlertDialog.Builder(this);
				builder.setTitle(getString(R.string.app_name));
				builder.setItems(new String[] { 
						getString(R.string.master), 
						getString(R.string.slave), 
						getString(R.string.by_sms) 
					}, modeListener);
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
					mode = i;
					switch(mode) {
						case MODE_SMS:
							//sms
							Intent intent = new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI);  
							startActivityForResult(intent, SMS_KEY_CONTACT);  						
							break;
						case MODE_ESCLAVE:
							IntentIntegrator.initiateScan(SmsViewActivity.this);							
							break;
						case MODE_MAITRE:
						default:
							//Master
							KeyPairGenerator gen;
							try {
								gen = KeyPairGenerator.getInstance("RSA");
								//TODO preference to increase key size and so increase security
								// but this increase amount of data to show in QRcode and can
								// be more difficult to read
								gen.initialize(new RSAKeyGenParameterSpec(256, RSAKeyGenParameterSpec.F4));

								keypair = gen.generateKeyPair();
								RSAPublicKey pubKey = (RSAPublicKey) keypair.getPublic();
								
								IntentIntegrator.initiateScan(SmsViewActivity.this);							
								IntentIntegrator.shareText(SmsViewActivity.this, 
										prefPhoneNumber + "\n" +
										pubKey.getModulus() + "\n" + 
										pubKey.getPublicExponent());
								
								return;
							} 
							catch (NoSuchAlgorithmException e) {
								e.printStackTrace();
							} 
							catch (InvalidAlgorithmParameterException e) {
								e.printStackTrace();
							}

							Toast.makeText(getApplicationContext(), R.string.unexpected_error, Toast.LENGTH_LONG).show();
				}

			}
		};

	@Override
	public void onActivityResult(int reqCode, int resultCode, Intent data) {
		super.onActivityResult(reqCode, resultCode, data);

			switch (reqCode) {
				case SMS_KEY_CONTACT:
					if (resultCode == RESULT_OK) {
						Uri uri = data.getData();
						
						ContentResolver cr = getContentResolver();
						
						Cursor cursor =  cr.query(uri, new String[] { Contacts.LOOKUP_KEY }, 
								null, null, null);
						
						String lookup = null;
						
						if(cursor.moveToFirst()) {
							lookup = cursor.getString(0);
						}
						
						cursor.close();
		
						if(lookup == null) {
							Toast.makeText(this, R.string.unexpected_error, Toast.LENGTH_LONG).show();
							return;
						}
						
						cursor = cr.query(Data.CONTENT_URI, 
								new String[] { Phone.NUMBER },
								Data.MIMETYPE + "=? AND " + Data.LOOKUP_KEY + "=?",
								new String[] { Phone.CONTENT_ITEM_TYPE, lookup },
								null);
		
						ArrayList<String> options = new ArrayList<String>();
						
						while(cursor.moveToNext()) {
							options.add(cursor.getString(0));
						}
							
						cursor.close();
						
						final String[] phoneList = options.toArray(new String[0]);
		
						Builder builder = new AlertDialog.Builder(this);
						builder.setTitle(R.string.send_invit_on_phone);
						builder.setItems(phoneList, 				
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialogInterface, int i) {
									
									keypair = tools.loadKeyPair();
									RSAPublicKey pubKey = (RSAPublicKey) keypair.getPublic();
									
									byte[] encoded = pubKey.getModulus().toByteArray();
											
									byte[] data = new byte[3 + encoded.length];
									
									data[0] = Tools.MAGIC[0];
									data[1] = Tools.MAGIC[1];
									data[2] = Tools.PUBLIC_KEY_TYPE;
		
									System.arraycopy(encoded, 0, data, 3, encoded.length);
									
									tools.sendData(phoneList[i], Tools.INVITATION, "", data);
									
							}
						});

						builder.create().show();
					}
					else {
						Toast.makeText(this, R.string.error_create_key, Toast.LENGTH_LONG).show();
					}
					break;
	        	case IntentIntegrator.REQUEST_CODE:
		            if (resultCode == RESULT_OK) {
		                try {
			            	String contents = data.getStringExtra("SCAN_RESULT");
			                @SuppressWarnings("unused")
							String format = data.getStringExtra("SCAN_RESULT_FORMAT");
			                // Handle successful scan
			                
			        		// TODO: add more tests control
			                
			                String[] infos = contents.split("\n");
			                
							Cipher rsaCipher = Cipher.getInstance(Tools.RSA_CIPHER_ALGO);
							
							if(mode == MODE_ESCLAVE) {
								// Save key and show crypted key on QRCode
								key = tools.generateKeyAES().getEncoded();
								
								KeyFactory keyFact = KeyFactory.getInstance("RSA");
								
								PublicKey pubkey = keyFact.generatePublic(
										new RSAPublicKeySpec(new BigInteger(infos[1]), 
												new BigInteger(infos[2])));
								
								rsaCipher.init(Cipher.ENCRYPT_MODE, pubkey);
								
								int blockSize = rsaCipher.getBlockSize();
								
								int nbBlock = key.length/blockSize;
								int reste = key.length%blockSize;
								
								byte[] cryptedKey = new byte[(nbBlock+1)*rsaCipher.getOutputSize(blockSize)];
								
								int offset = 0;
								
								for(int i = 0; i < nbBlock; i++){
									offset += rsaCipher.doFinal(key, i*blockSize, blockSize, cryptedKey, offset);
								}
								
								rsaCipher.doFinal(key, nbBlock*blockSize, reste, cryptedKey, offset);
								
								IntentIntegrator.shareText(SmsViewActivity.this, 
										prefPhoneNumber + "\n" +
										Base64.encodeToString(cryptedKey,Base64.NO_WRAP));
								
							}
							else {
								
								// We have read crypted key, so decode it
								rsaCipher.init(Cipher.DECRYPT_MODE, keypair.getPrivate());
								
								byte[] cryptedData = Base64.decode(infos[1], Base64.NO_WRAP);

								int blockSize = rsaCipher.getBlockSize();
								int nbBlock = cryptedData.length/blockSize;
								
								int offset = 0;
													
								byte[] tempKey = new byte[(nbBlock+1)*blockSize];
								
								for(int i = 0; i < nbBlock; i++) {
									offset += rsaCipher.doFinal(cryptedData, i*blockSize, blockSize, tempKey, offset);
								}
								
								key = new byte[offset];
								System.arraycopy(tempKey, 0, key, 0, offset);
							}
			                
							phoneNumber = infos[0];
							
							// store the key
							// TODO dialog to confirm add contact in mode SLAVE
			                try {
								new Tools(this).updateOrCreateContactKey(phoneNumber, key);
							} 
			                catch (NoContactException e) {
								e.printStackTrace();
								// propose to add contact
								Intent newIntent = new Intent(Intents.SHOW_OR_CREATE_CONTACT);
								newIntent.setData(Uri.fromParts("tel", phoneNumber, null));
								startActivityForResult(newIntent, ADD_CONTACT);
								return;
			                }
			                		                
			               	Toast.makeText(this, getString(R.string.contact_added) + "\n" + phoneNumber, Toast.LENGTH_LONG).show();
			                
		            	}
		            	catch(Exception e) {
		            		e.printStackTrace();
		            		Toast.makeText(this, R.string.error_create_key, Toast.LENGTH_LONG).show();
		            	}
		                
		            }
		            else {
		            	// TODO: string
		            	Toast.makeText(this, R.string.fail_reading_tag, Toast.LENGTH_LONG).show();
		            }
	        		break;       	
	           	case ADD_CONTACT:
	                try {
	  					tools.updateOrCreateContactKey(phoneNumber, key);
	  		           	Toast.makeText(this, getString(R.string.contact_added) + "\n" + phoneNumber, Toast.LENGTH_LONG).show();
	  		    	} 
	  		        catch (NoContactException e) {
	  					e.printStackTrace();
	  	        		Toast.makeText(this, R.string.error_create_key, Toast.LENGTH_LONG).show();
	  		        }
	  	    		break;
		}

	 }

	public static class viewBinder implements ViewBinder {

		private Context context;
		
		public viewBinder(Context context) {
			this.context = context;
		}
			
		@Override
		public boolean setViewValue(View v, Cursor cursor, int columnIndex) {
			if(cursor.getColumnIndex(OpenHelper.ID) == columnIndex) {
				
				ImageView iv = (ImageView)v.findViewById(R.id.photo);
				TextView tv = (TextView)v.findViewById(R.id.line1);

				String phoneNumber = cursor.getString(cursor.getColumnIndex(OpenHelper.PHONE));
				
				try {

					StringBuffer displayText = new StringBuffer();
					
					//String lookupKey = tools.getLookupFromPhoneNumber(phoneNumber);
					
					//Uri contactUri = Contacts.lookupContact(context.getContentResolver(),  
					//		Contacts.getLookupUri(0, lookupKey));

					Uri contactUri = Uri.parse(cursor.getString(
							cursor.getColumnIndex(OpenHelper.CONTACT_URI)));
							
					Cursor cname = context.getContentResolver().query(contactUri, 
							new String[] { Contacts.DISPLAY_NAME }, null, null, null);
					
					if(cname.moveToFirst()) {
						displayText.append(cname.getString(0));
					}
					else {
						//displayName.append(context.getString(R.string.unknow));
						displayText.append(phoneNumber);
					}
					
					cname.close();
					
					//displayName.append("\n");
					//displayName.append(phoneNumber);

					DateFormat df = DateFormat.getDateTimeInstance (DateFormat.MEDIUM, DateFormat.MEDIUM);
					long date = cursor.getLong(cursor.getColumnIndex(OpenHelper.MSG_DATE));
					String stringDateTime = df.format(new Date(date));

					int type = cursor.getInt(cursor.getColumnIndex(OpenHelper.MSG_TYPE));
					if(type == Tools.MESSAGE) {
						displayText.append("\n");
						displayText.append(context.getString(R.string.message));
						displayText.append("\n");
						displayText.append(context.getString(R.string.sending));
						tv.setTextColor(Color.WHITE);
					}
					else if(type == Tools.MESSAGE_SEND) {
						displayText.append("\n");
						displayText.append(context.getString(R.string.message));
						displayText.append("\n");
						displayText.append(stringDateTime);
						tv.setTextColor(Color.WHITE);
					}
					else if (type == Tools.MESSAGE_RECEIVED) {
						tv.setTextColor(Color.BLUE);
						displayText.append("\n");
						displayText.append(context.getString(R.string.message_received));
						displayText.append("\n");
						displayText.append(stringDateTime);
					}
					else if (type == Tools.INVITATION) {
						tv.setTextColor(Color.GREEN);
						displayText.append("\n");
						displayText.append(context.getString(R.string.invitation));
						displayText.append("\n");
						displayText.append(context.getString(R.string.sending));
					}
					else if (type == Tools.INVITATION_SEND) {
						tv.setTextColor(Color.GREEN);
						displayText.append("\n");
						displayText.append(context.getString(R.string.invitation));
						displayText.append("\n");
						displayText.append(stringDateTime);
					}
					else if (type == Tools.INVITATION_RECEIVED) {
						tv.setTextColor(Color.YELLOW);
						displayText.append("\n");
						displayText.append(context.getString(R.string.invitation_received));
						displayText.append("\n");
						displayText.append(stringDateTime);
				}
					else if (type == Tools.INVITATION_ANSWER) {
						tv.setTextColor(Color.MAGENTA);
						displayText.append("\n");
						displayText.append(context.getString(R.string.invitation_answer));
						displayText.append("\n");
						displayText.append(context.getString(R.string.sending));
				}
					else if (type == Tools.INVITATION_ANSWER_SEND) {
						tv.setTextColor(Color.MAGENTA);
						displayText.append("\n");
						displayText.append(context.getString(R.string.invitation_answer));
						displayText.append("\n");
						displayText.append(stringDateTime);
				}
					
					tv.setText(displayText);
					
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
					tv.setTextColor(Color.RED);
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

		private SimpleCursorAdapter mAdapter = null;
		
		private long id = -1;
		
		@Override
		public void onSaveInstanceState(Bundle outState) {
			super.onSaveInstanceState(outState);
		}
		
		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            
            setEmptyText(getActivity().getString(R.string.no_action));
            
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
            
            /*
            getListView().setOnItemLongClickListener(new OnItemLongClickListener() {
    			@Override
    			public boolean onItemLongClick(AdapterView<?> parent, View view,
    					int position, long id) {
    				Intent intent = new Intent(Intent.ACTION_VIEW);
    				intent.setClassName(getActivity(), CharabiaActivity.class.getName());
    				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
    				startActivity(intent);
    				return true;
    			}
    		});
			*/
		}
		
		private void deleteId(long id) {
			ContentResolver cr = getActivity().getContentResolver();
			cr.delete(ContentUris.withAppendedId(DataProvider.MSG_CONTENT_URI, id), null, null);
		}
		
		private void dialogMessageSend(final long id, String displayName, Drawable photo, String message) {


			Builder builder = new AlertDialog.Builder(getActivity());
			if(photo == null) {
				builder.setIcon(R.drawable.ic_launcher);
			}
			else {
				builder.setIcon(photo);
			}
			
			builder.setNegativeButton(R.string.clear,  
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							deleteId(id);
						}
					}
				);
			builder.setPositiveButton(R.string.ok, null);
		
			builder.setTitle(displayName);
			builder.setMessage(message);
			builder.create().show();	
		}
		
		private void dialogMessageReceived(final long id, String displayName, Drawable photo, String message)  {
			Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(displayName);
			if(photo != null) {
				builder.setIcon(photo);
			}
			else {
				builder.setIcon(R.drawable.ic_launcher);
			}
			builder.setMessage(message);
			builder.setNegativeButton(R.string.clear,  
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						deleteId(id);
					}
				}
			);
			builder.setPositiveButton(R.string.save, 
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
					}	
             	}
			);
			builder.create().show();

		}

		private void dialogInvitationReceived(final long id, String displayName, Drawable photo, String message) {
			Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(displayName);
			if(photo == null) {
				builder.setIcon(R.drawable.ic_launcher);
			}
			else {
				builder.setIcon(photo);
			}
			builder.setMessage(message);
			builder.setNegativeButton(R.string.refuse,  
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						
					}
				}
			);
			builder.setPositiveButton(R.string.accept, 
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						//TODO:
						
					}
             	});
			builder.create().show();

		}

		private void dialogInvitationAnswer(final long id, String displayName, Drawable photo, String message) {
			Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(displayName);
			if(photo == null) {
				builder.setIcon(R.drawable.ic_launcher);
			}
			else {
				builder.setIcon(photo);
			}
			builder.setMessage(message);
			builder.setNegativeButton(R.string.no,  
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						
					}
				}
			);
			builder.setPositiveButton(R.string.yes, 
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
					
					}
             	});
			builder.create().show();

		}
		
		@Override
		public void onListItemClick(ListView lv, View v, int position, long id) {
			
			this.id = id;
			
			try {
				Cursor cursor = mAdapter.getCursor();
		
				int type = 0;
				String message = null;
				Uri contactUri = null;
				String phoneNumber = null;
				
				if(cursor.moveToPosition(position)) {
					phoneNumber = cursor.getString(cursor.getColumnIndex(OpenHelper.PHONE));
					type = cursor.getInt(cursor.getColumnIndex(OpenHelper.MSG_TYPE));
					int count = cursor.getInt(cursor.getColumnIndex(OpenHelper.COUNTER));
					message = "attempt " + count + "," + cursor.getString(cursor.getColumnIndex(OpenHelper.MSG_ERROR));
					if(message == null || message.equals("")) {
						message = cursor.getString(cursor.getColumnIndex(OpenHelper.MSG_TEXT));
					}
					contactUri = Uri.parse(cursor.getString(
							cursor.getColumnIndex(OpenHelper.CONTACT_URI)));
				}
				
				ContentResolver cr = getActivity().getContentResolver();
				
				Cursor cname = cr.query(contactUri, 
						new String[] { Contacts.DISPLAY_NAME }, null, null, null);
	
				
				String displayName = null;
				
				if(cname.moveToFirst()) {
					displayName = cname.getString(0);
				}
				else {
					displayName = getActivity().getString(R.string.unknow);
				}
				
				cname.close();
	
				java.io.InputStream input = Contacts.openContactPhotoInputStream(
						cr,
						contactUri);
	
				Drawable photo = null;
				if(input != null) {
					photo = Drawable.createFromStream(input, null);
				}
				
				if(type == Tools.MESSAGE || type == Tools.MESSAGE_SEND) {
					dialogMessageSend(id, displayName, photo, message);
				}
				else if (type == Tools.MESSAGE_RECEIVED) {
					dialogMessageReceived(id, displayName, photo, message);
				}
				else if (type == Tools.INVITATION || type == Tools.INVITATION_SEND) {
					dialogMessageSend(id, displayName, photo, message);
				}
				else if (type == Tools.INVITATION_RECEIVED) {
					dialogInvitationReceived(id, displayName, photo, message);
				}
				else if (type == Tools.INVITATION_ANSWER || type == Tools.INVITATION_ANSWER_SEND) {
					dialogInvitationAnswer(id, displayName, photo, message);
				}
				else {
					throw new Exception("Unexpected error");
				}
			}
			catch(Exception e ){
				e.printStackTrace();
				
				final long _id = id;
				Builder builder = new AlertDialog.Builder(getActivity());
				builder.setIcon(R.drawable.ic_launcher);
				builder.setNegativeButton(R.string.yes,  
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								ContentResolver cr = getActivity().getContentResolver();
								cr.delete(ContentUris.withAppendedId(DataProvider.MSG_CONTENT_URI, _id), null, null);
							}
						}
					);
				builder.setPositiveButton(R.string.no, null);
			
				builder.setTitle(R.string.delete);
				builder.setMessage(R.string.unexpected_error);
				builder.create().show();	
			}
		}
	
		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {

	        return new CursorLoader(getActivity(), 
	        		DataProvider.MSG_CONTENT_URI,
	        		new String[] { 
	        			OpenHelper.ID, 
	        			OpenHelper.PHONE, 
	        			OpenHelper.CONTACT_URI,
	        			OpenHelper.MSG_TYPE,
	        			OpenHelper.MSG_DATE,
	        			OpenHelper.MSG_TEXT,
	        			OpenHelper.MSG_STATUS,
	        			OpenHelper.COUNTER,
	        			OpenHelper.MSG_ERROR
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

	}
}
