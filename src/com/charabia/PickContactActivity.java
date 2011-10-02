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

import com.charabia.SmsListe.viewBinder;

import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;

import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.util.Log;
import android.view.View;

import android.content.ContentResolver;
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
import android.telephony.SmsMessage;

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
		private Uri baseUri;
		private Uri uri;
		private ContentResolver cr;
		private Cursor cursor;
		private int count;
		
		public viewBinder(Context context) {
			this.context = context;
			tools = new Tools(context);
			cr = context.getContentResolver();
		}
		
		@Override
		public boolean setViewValue(View v, Cursor cursor, int columnIndex) {
			try {
				TextView tv = (TextView)v;
				
				baseUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, 
						cursor.getLong(cursor.getColumnIndex(ContactsContract.Contacts._ID)));
				
				uri = Uri.withAppendedPath(baseUri, ContactsContract.Contacts.Data.CONTENT_DIRECTORY);
				
				this.cursor = cr.query(uri, new String[] { ContactsContract.Contacts._ID }, 
						ContactsContract.Contacts.Data.MIMETYPE + "=?",
						new String[] { Tools.CONTENT_ITEM_TYPE }, null);						
				
				Log.v("CHARABIA", "uri=" + uri);
				
				if(this.cursor.getCount() <= 0) {
					tv.setClickable(false);
					//tv.setEnabled(false);
					tv.setTextColor(Color.RED);
					tv.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
				}
				
				tv.setText(cursor.getString(columnIndex));
				
				return true;
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			
			return false;
		}
		
	}

	public static class CursorLoaderListFragment extends ListFragment implements OnItemLongClickListener, LoaderManager.LoaderCallbacks<Cursor> 
	{
		// Loader
		private static final int CONTACTS_LOADER = 1;

		private long id = -1;
		
		private SimpleCursorAdapter mAdapter = null;
		
		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            
            setEmptyText(getActivity().getString(R.string.no_contact));
            
            mAdapter = new SimpleCursorAdapter(getActivity(), 
            		android.R.layout.simple_list_item_1, null, 
            		new String[] { 
            			ContactsContract.Contacts.DISPLAY_NAME,
            		}, 
            		new int[] { android.R.id.text1 },
            		0);
            
            mAdapter.setViewBinder(new viewBinder(getActivity()));
            
            setListAdapter(mAdapter);
            
            setListShown(false);
            
            getListView().setOnItemLongClickListener(this);
            
            getLoaderManager().initLoader(CONTACTS_LOADER, null, this);
		}
		
		@Override
		public void onListItemClick(ListView lv, View v, int position, long id) {
			
			Intent intent = getActivity().getIntent();
			if(intent != null) {
				intent.setData(ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id));
				getActivity().setResult(Activity.RESULT_OK, intent);
	        }
			getActivity().finish();		
		}
	
		private final DialogInterface.OnClickListener editListener =
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialogInterface, int i) {
						switch(i) {
							case 0: //delete
								ContentResolver cr = getActivity().getContentResolver();
								
								Uri uri = ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, id);
								cr.delete(uri, null, null);
								break;
							default:
						}
				}
			};

		@Override
		public boolean onItemLongClick(AdapterView<?> av, View v, int position, long id) {
			this.id = id;
			
			Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(getString(R.string.app_name));
			builder.setItems(new String[] { 
				getString(R.string.delete), 
				 }, editListener);
			builder.create().show();
			return true;
		}
	

		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			Uri baseUri = ContactsContract.Contacts.CONTENT_URI;
			//Uri uri = Uri.withAppendedPath(baseUri, ContactsContract.Contacts.Data.CONTENT_DIRECTORY);
					
	        return new CursorLoader(getActivity(), baseUri,
	        		new String[] { 
	        			ContactsContract.Contacts._ID, //ContactsContract.Data._ID,
	        			ContactsContract.Contacts.DISPLAY_NAME //CommonDataKinds.Phone.DISPLAY_NAME,
	        			//ContactsContract.CommonDataKinds.Phone.NUMBER,
	        		}, 
	        		null, //ContactsContract.Data.MIMETYPE + "=?", 
	        		null, //new String[] { Tools.CONTENT_ITEM_TYPE },
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

	}
	
	
}