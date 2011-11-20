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
import java.util.ArrayList;

import android.R.color;
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
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.Toast;
import android.util.Log;
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
		
		// Loader
		private static final int CONTACTS_LOADER = 1;

		// Id of selection for long click
		private long id = -1;
		
		// Options for long click dialog, element 0 is for delete
		private String[] phoneList = null;
		
		// Intent result
		private static final int ADD_CONTACT = 1;
		
		private SimpleCursorAdapter mAdapter = null;
		
		private Tools tools;
		
		@Override
		public void onSaveInstanceState(Bundle outState) {
			super.onSaveInstanceState(outState);
			outState.putLong("id", id);
			outState.putStringArray("phoneList", phoneList);
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
            	id = savedInstanceState.getInt("id");
            	phoneList = savedInstanceState.getStringArray("phoneList");
            }
            
            setHasOptionsMenu(true);
		}
		
		@Override
		public void onListItemClick(ListView lv, View v, int position, long id) {
			
			Uri uri = ContentUris.withAppendedId(DataProvider.CONTENT_URI, id);

			ContentResolver cr = getActivity().getContentResolver();
			
			Cursor cursor = cr.query(uri, 
					new String[] { OpenHelper.PHONE, OpenHelper.LOOKUP }, 
					null, null, null);
		
			String phoneNumber = "";
			String lookup = null;
			
			if(cursor.moveToFirst()) {
				phoneNumber = cursor.getString(cursor.getColumnIndex(OpenHelper.PHONE));
				lookup = cursor.getString(cursor.getColumnIndex(OpenHelper.LOOKUP));
			}
			
			cursor.close();
			
			try {
				String currentLookup = new Tools(getActivity()).getLookupFromPhoneNumber(phoneNumber);
				if(lookup != currentLookup) {
					ContentValues values = new ContentValues();
					values.put(OpenHelper.LOOKUP, currentLookup);
					cr.update(uri, values, null, null);
					getLoaderManager().restartLoader(CONTACTS_LOADER, null, CursorLoaderListFragment.this);
					return;
				}
			} catch (NoLookupKeyException e) {
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
						Uri uri = ContentUris.withAppendedId(DataProvider.CONTENT_URI, id);
						switch(i) {
							case 0: //delete
								cr.delete(uri, null, null);
								break;
							default:
								// Change phone number
								//try {
									//String newLookup = tools.getLookupFromPhoneNumber(phoneList[i]);
									ContentValues values = new ContentValues();
									//values.put(OpenHelper.LOOKUP, newLookup);
									values.put(OpenHelper.PHONE, phoneList[i]);
									cr.update(uri, values, null, null);
								//} 
								//catch (NoLookupKeyException e) {
								//	e.printStackTrace();
								//}
								
						}
						getLoaderManager().restartLoader(CONTACTS_LOADER, null, CursorLoaderListFragment.this);
				}
			};

		/*
		 * Dialog to delete contact or change default phone to use with this contact
		 * @see android.widget.AdapterView.OnItemLongClickListener#onItemLongClick(android.widget.AdapterView, android.view.View, int, long)
		 */
		@Override
		public boolean onItemLongClick(AdapterView<?> av, View v, int position, long id) {
			this.id = id;
			
			ContentResolver cr = getActivity().getContentResolver();
			
			Uri uri = ContentUris.withAppendedId(DataProvider.CONTENT_URI, id);
			
			Cursor cursor = cr.query(uri, new String[] { OpenHelper.LOOKUP }, 
					null, null, null);
			
			String lookup = null;
			if(cursor.moveToFirst()) {
				lookup = cursor.getString(0);
			}
			
			cursor.close();
			
			ArrayList<String> options = new ArrayList<String>();
			
			options.add(getString(R.string.delete));
			
			if(lookup != null) {
				cursor = cr.query(Data.CONTENT_URI, 
						new String[] { Phone.NUMBER },
						Data.MIMETYPE + "=? AND " + Data.LOOKUP_KEY + "=?",
						new String[] { Phone.CONTENT_ITEM_TYPE, lookup },
						null);
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
	            } else {
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
	
		/** 
		 * Handles item selections 
		 */
		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
			switch (item.getItemId()) {
				case R.id.contact_menu_save:
					save();
					return true;
				case R.id.contact_menu_revert:
					restore();
					return true;
				default:
					return super.onOptionsItemSelected(item);
			}
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
			if (Environment.MEDIA_MOUNTED.equals(state)) {
			    // We can read and write the media
			    return true;
			} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			    // We can only read the media
				if(!writeableWanted) {
					return true;
				}
			}
		    // Something else is wrong. It may be one of many other states, but all we need
		    //  to know is we can neither read nor write
			return false;
		}
		
		protected boolean checkData(long id, String lookup, String phoneNumber) {
			String current_lookup;
			
			try {
				current_lookup = new Tools(getActivity()).getLookupFromPhoneNumber(phoneNumber);
				if(current_lookup == lookup) {
					// Ok phone associate with this contact
					return true;
				}
				else {
					// Change the lookup key associate with this contact
				}
			}
			catch(NoLookupKeyException e) {
				// No contact with this phone number change the phone
				// number needed
			}
			return false;
		}
		
		/**
		 * Save keys
		 */
		protected void save() {
			ContentResolver cr = getActivity().getContentResolver();
			
			int message = R.string.unknow;
			
			if(checkMediaState(true)) {
				Cursor cursor = cr.query(DataProvider.CONTENT_URI, 
						new String[] { OpenHelper.KEY, OpenHelper.LOOKUP, OpenHelper.PHONE },
						null, null, null);
				
			}
			
			Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(getString(R.string.app_name));
			builder.setMessage(message);
			builder.setNeutralButton("OK", null);
			builder.create().show();

		}
		
		/**
		 * Restore the keys
		 */
		protected void restore() {
			
		}
	}
}
