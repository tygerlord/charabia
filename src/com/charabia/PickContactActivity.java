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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Intents;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.database.Cursor;
import android.database.SQLException;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ContentUris;
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
public class PickContactActivity extends FragmentActivity 
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

					long contactId = cursor.getLong(cursor.getColumnIndex(OpenHelper.CONTACT_ID));
					String lookupKey = cursor.getString(cursor.getColumnIndex(OpenHelper.LOOKUP));
					
					if(!lookupKey.equals(tools.getLookupFromPhoneNumber(phoneNumber))) {
						String TAG = "CHARABIA";
						Log.v(TAG, "lookupKey = " + lookupKey);
						Log.v(TAG, "phoneNumber = " + phoneNumber);
						Log.v(TAG, "getLookup = " + tools.getLookupFromPhoneNumber(phoneNumber));
						throw new Exception("Bad lookup key");
					}
					
					Uri contactUri = Contacts.lookupContact(context.getContentResolver(),  
							Contacts.getLookupUri(contactId, lookupKey));

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
					
					tv.setTextColor(Color.GREEN);
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
					tv.setTextColor(Color.RED);
					tv.setText("erreur" + "\n" + phoneNumber);
					return true;
				}
			}
			
			return false;
		}
		
	}

	public static class CursorLoaderListFragment extends ListFragment 
		implements OnItemLongClickListener, LoaderManager.LoaderCallbacks<Cursor> 
	{
		
		private static final String TAG = "CHARABIA";
		
		// Loader
		private static final int CONTACTS_LOADER = 1;

		// Options for long click dialog, element 0 is for delete
		private String[] phoneList = null;
		
		// Intent result
		private static final int ADD_CONTACT = 1;
		
		private SimpleCursorAdapter mAdapter = null;
		
		private Tools tools;
		
		private boolean mode_save = false;
		
		private EditText editTextPassword;
		/*
		 * Use this to store clicked id on edit listener
		 */
		private long index;
		
		private void setIndex(long id) {
			index = id;
		}
		
		private long getIndex() {
			return index;
		}
		
		@Override
		public void onSaveInstanceState(Bundle outState) {
			super.onSaveInstanceState(outState);
			outState.putStringArray("phoneList", phoneList);
			outState.putLong("index", index);
			outState.putBoolean("mode_save", mode_save);
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
            
            getListView().setOnItemLongClickListener(this);
            
            if(savedInstanceState != null) {
            	phoneList = savedInstanceState.getStringArray("phoneList");
            	index = savedInstanceState.getLong("index");
            	mode_save = savedInstanceState.getBoolean("mode_save");
            }
            
            setHasOptionsMenu(true);
		}
		
		@Override
		public void onListItemClick(ListView lv, View v, int position, long id) {
			
			Cursor cursor = mAdapter.getCursor();
	
			String phoneNumber = "";
			String lookup = null;
			
			if(cursor.moveToPosition(position)) {
				phoneNumber = cursor.getString(cursor.getColumnIndex(OpenHelper.PHONE));
				lookup = cursor.getString(cursor.getColumnIndex(OpenHelper.LOOKUP));
			}
			
			try {
				lookup = checkLookupKey(id, lookup, phoneNumber);
			} 
			catch (NoContactException e) {
				e.printStackTrace();
				// No contact with this phone so ask for create it
				Intent newIntent = new Intent(Intents.SHOW_OR_CREATE_CONTACT);
				newIntent.setData(Uri.fromParts("tel", phoneNumber, null));
				startActivityForResult(newIntent, ADD_CONTACT);
				return;
			}

			Intent intent = getActivity().getIntent();
			if(intent != null) {
				intent.setData(Uri.parse("smsto:"+phoneNumber));
				getActivity().setResult(Activity.RESULT_OK, intent);
	        }
			getActivity().finish();		
		}
	
		private final DialogInterface.OnClickListener editListener =
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialogInterface, int i) {
						ContentResolver cr = getActivity().getContentResolver();
						Uri uri = ContentUris.withAppendedId(DataProvider.CONTENT_URI, getIndex());
						switch(i) {
							case 0: //delete
								cr.delete(uri, null, null);
								break;
							default:
								ContentValues values = new ContentValues();
								values.put(OpenHelper.PHONE, phoneList[i]);
								cr.update(uri, values, null, null);
						}
						getLoaderManager().restartLoader(CONTACTS_LOADER, null, CursorLoaderListFragment.this);
				}
			};

		
		private String checkLookupKey(long id, String lookup, String phoneNumber) throws NoContactException {
			String newLookup = null;
			
			newLookup = tools.getLookupFromPhoneNumber(phoneNumber);
			
			if(!lookup.equals(newLookup)) {
				/*
				 * Current lookup don't have this phone number 
				 * but another lookup have this phone so suppose that this
				 * lookup is no associate with this phone and change lookup key
				 * in table. 
				 */
				ContentResolver cr = getActivity().getContentResolver();
				Uri uri = ContentUris.withAppendedId(DataProvider.CONTENT_URI, id);
				ContentValues values = new ContentValues();
				values.put(OpenHelper.LOOKUP, newLookup);
				// TODO: test change ok
				cr.update(uri,  values, null, null);
				return newLookup;
			}
			
			return lookup;
		}
		
		
		/*
		 * Dialog to delete contact or change default phone to use with this contact
		 * @see android.widget.AdapterView.OnItemLongClickListener#onItemLongClick(android.widget.AdapterView, android.view.View, int, long)
		 */
		@Override
		public boolean onItemLongClick(AdapterView<?> av, View v, int position, long id) {
			
			setIndex(id);
			
			Cursor cursor = mAdapter.getCursor();

			String lookup = null;
			String phoneNumber = "";
			if(cursor.moveToPosition(position)) {
				lookup = cursor.getString(cursor.getColumnIndex(OpenHelper.LOOKUP));
				phoneNumber = cursor.getString(cursor.getColumnIndex(OpenHelper.PHONE));				
			}

			try {
				lookup = checkLookupKey(id, lookup, phoneNumber);
			} catch (NoContactException e) {
				e.printStackTrace();
			}
		
			ArrayList<String> options = new ArrayList<String>();
			
			options.add(getString(R.string.delete));
			
			Log.v("CHARABIA", "lookup="+lookup);
			
			if(lookup != null) {
				ContentResolver cr = getActivity().getContentResolver();
				cursor = cr.query(Data.CONTENT_URI, 
						new String[] { Phone.NUMBER },
						Data.MIMETYPE + "=? AND " + Data.LOOKUP_KEY + "=?",
						new String[] { Phone.CONTENT_ITEM_TYPE, lookup },
						null);
				Log.v("CHARABIA", "cursor count = "+cursor.getCount());
				while(cursor.moveToNext()) {
					options.add(cursor.getString(0));
				}
				cursor.close();
			}
	
			phoneList = new String[options.size()];
			
			Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(getString(R.string.del_or_change_number));
			builder.setItems(options.toArray(phoneList), editListener);
			builder.create().show();
			return true;
		}
	

		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {

	        return new CursorLoader(getActivity(), 
	        		DataProvider.CONTENT_URI,
	        		new String[] { 
	        			OpenHelper.ID, 
	        			OpenHelper.LOOKUP, 
	        			OpenHelper.CONTACT_ID,
	        			OpenHelper.PHONE
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
		public void onCreateOptionsMenu (Menu menu, MenuInflater inflater) {
			 inflater.inflate(R.menu.pick_contact, menu);
		}
	
		private Cipher getCipher(String password, byte[] iv) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
			MessageDigest digester = MessageDigest.getInstance("SHA256");
			
			SecretKey key = new SecretKeySpec(digester.digest(password.getBytes()), "AES");
			
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			
			if(iv == null) {
				cipher.init(Cipher.ENCRYPT_MODE, key);
			}
			else {
				cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
			}
			
			return cipher;
		}
		
		
		/** 
		 * Handles item selections 
		 */
		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
			switch (item.getItemId()) {
				case R.id.contact_menu_save:
					mode_save = true;
					break;
				case R.id.contact_menu_restore:
					mode_save = false;
					break;
				default:
					return super.onOptionsItemSelected(item);
			}
			
			// Start dialog to get file password
	        LayoutInflater factory = LayoutInflater.from(getActivity());
	        View dialogView = factory.inflate(R.layout.dialog_password_entry, null);
			
	        editTextPassword = (EditText) dialogView.findViewById(R.id.edittext_password);
	        
			Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(getString(R.string.app_name));
			builder.setView(dialogView);
			builder.setPositiveButton(getString(R.string.ok), 
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String password = editTextPassword.getText().toString();
						if(mode_save) {
							save(password);
						}
						else {
							restore(password);
						}
					}	
             	});
			builder.create().show();
			return true;
		}

		@Override
		public void onActivityResult(int reqCode, int resultCode, Intent data) {
			super.onActivityResult(reqCode, resultCode, data);

			switch (reqCode) {
				case ADD_CONTACT:
					getLoaderManager().restartLoader(CONTACTS_LOADER, null, this);
					break;
			}
		}
		
		public boolean checkMediaState(boolean writeableWanted) {
			String state = Environment.getExternalStorageState();
			Log.v("CHARABIA", "media state is " + state);
			if (Environment.MEDIA_MOUNTED.equals(state)) {
			    return true;
			} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
				if(!writeableWanted) {
					return true;
				}
			}
			return false;
		}

		public void popup(String message, String title) {
			Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(title);
			builder.setMessage(message);
			builder.setNeutralButton("OK", null);
			builder.create().show();
		}
		
		public void popup(String message) {
			popup(message, getActivity().getString(R.string.app_name));
		}
		
		public void popup(int resid, int titleid) {
			popup(getActivity().getString(resid), getActivity().getString(titleid));
		}
		
		void popup(int resid) {
			popup(getActivity().getString(resid));
		}
		
		/*
		 * Use this separator between elements
		 */
		private static final String SEPARATOR = ";";
		private static final String FILENAME = "keys.dat";

		/**
		 * Save keys
		 */
		protected void save(String password) {
			int message = R.string.save_failure;
			
			Cipher cipher = null;
			try {
				cipher = getCipher(password, null);
			} catch (InvalidKeyException e1) {
				e1.printStackTrace();
				popup(R.string.unexpected_error);
				return;
			} catch (NoSuchAlgorithmException e1) {
				e1.printStackTrace();
				popup(R.string.unexpected_error);
				return;
			} catch (NoSuchPaddingException e1) {
				e1.printStackTrace();
				popup(R.string.unexpected_error);
				return;
			} catch (InvalidAlgorithmParameterException e1) {
				e1.printStackTrace();
				popup(R.string.unexpected_error);
				return;
			}
			
			if(checkMediaState(true)) {
				ProgressDialog dialog = new ProgressDialog(getActivity());
				
				dialog.show();
				
				Cursor cursor = mAdapter.getCursor();
				FileWriter fw = null;
				try {
					File file = new File(Environment.getExternalStorageDirectory(), 
							getString(R.string.app_name));
					
					file.mkdir();
					
					File filename = new File(file, FILENAME);
					
					filename.createNewFile();
					
					fw = new FileWriter(filename);
					
					String phoneNumber;
					if(cursor.moveToFirst()) {
						fw.write(Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP) + "\n");
						do {
							StringBuffer buf = new StringBuffer();
							phoneNumber = cursor.getString(cursor.getColumnIndex(OpenHelper.PHONE));
							buf.append(cursor.getString(cursor.getColumnIndex(OpenHelper.LOOKUP)));
							buf.append(SEPARATOR);
							buf.append(cursor.getString(cursor.getColumnIndex(OpenHelper.CONTACT_ID)));
							buf.append(SEPARATOR);
							buf.append(phoneNumber);
							buf.append(SEPARATOR);
							try {
								buf.append(Base64.encodeToString(tools.getKey(phoneNumber), Base64.NO_WRAP));
								fw.write(Base64.encodeToString(cipher.doFinal(buf.toString().getBytes()), 
										Base64.NO_WRAP) + "\n");
							} catch (NoContactException e) {
								e.printStackTrace();
							} catch (NoCharabiaKeyException e) {
								e.printStackTrace();
							} 
						}while(cursor.moveToNext());
					}					
					message = R.string.save_success;
				}
				catch(IOException e) {
					e.printStackTrace();
				}
				catch (IllegalBlockSizeException e) {
					e.printStackTrace();
					message = R.string.unexpected_error;
				} 
				catch (BadPaddingException e) {
					e.printStackTrace();
					message = R.string.unexpected_error;
				}

				if(fw != null) {
					try {
						fw.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				
				dialog.dismiss();
			}
			
			popup(message);
		}
		
		/**
		 * Restore the keys
		 */
		protected void restore(String password) {
			ContentResolver cr = getActivity().getContentResolver();
			
			int message = R.string.restore_failure;
			
			if(checkMediaState(false)) {
				ProgressDialog dialog = new ProgressDialog(getActivity());
				
				dialog.show();
				
				LineNumberReader lnr = null;
				try {
					File file = new File(Environment.getExternalStorageDirectory(), 
							getString(R.string.app_name));
					File filename = new File(file, FILENAME);
					
					Log.v("CHARABIA", "filename = " + filename);
					
					lnr = new LineNumberReader(new FileReader(filename));

					ContentValues values = new ContentValues();

					String line, clearLine;
					String[] infos;
					Cipher cipher = null;
					while((line = lnr.readLine()) != null) {
						
						Log.v("CHARABIA", "line=" + line);
						
						if(cipher == null) {
							try {
								cipher = getCipher(password, Base64.decode(line, Base64.NO_WRAP));
								continue;
							} catch (InvalidKeyException e1) {
								e1.printStackTrace();
								popup(R.string.bad_password);
								return;
							} catch (NoSuchAlgorithmException e1) {
								e1.printStackTrace();
								popup(R.string.unexpected_error);
								return;
							} catch (NoSuchPaddingException e1) {
								e1.printStackTrace();
								popup(R.string.bad_password);
								return;
							} catch (InvalidAlgorithmParameterException e1) {
								e1.printStackTrace();
								popup(R.string.unexpected_error);
								return;
							}
							
						}
						
						clearLine = new String(cipher.doFinal(Base64.decode(line, Base64.NO_WRAP)));
						infos = clearLine.split(SEPARATOR);
						Log.v("CHARABIA", "infos = " + infos[0] + "," + infos[1] + "," + infos[2] + "," + infos[3]);
						if(infos.length == 4) {
							values.clear();
							values.put(OpenHelper.LOOKUP, infos[0]);
							values.put(OpenHelper.ID, Long.parseLong(infos[1]));
							values.put(OpenHelper.PHONE, infos[2]);
							values.put(OpenHelper.KEY, Base64.decode(infos[3], Base64.NO_WRAP));
							try {
								cr.insert(DataProvider.CONTENT_URI, values);
							}
							catch(SQLException e) {
								Log.v(TAG, "line " + lnr.getLineNumber() + " not inserted, already present?" );
							}
						}
						else {
							Log.v(TAG, "Bad file format line " + lnr.getLineNumber());
						}
					}
					
					message = R.string.restore_success;
				}
				catch(IOException e) {
					e.printStackTrace();
					message = R.string.error_read_file;
				} 
				catch (IllegalBlockSizeException e) {
					e.printStackTrace();
					message = R.string.error_decrypt_file;
				} 
				catch (BadPaddingException e) {
					e.printStackTrace();
					message = R.string.error_decrypt_file;
				}

				if(lnr != null) {
					try {
						lnr.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				
				dialog.dismiss();
			}
			
			getLoaderManager().restartLoader(CONTACTS_LOADER, null, this);
			
			popup(message);
		}
	}
}
