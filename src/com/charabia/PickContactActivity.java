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
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
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
			try {
				
				if(cursor.getColumnIndex(OpenHelper.ID) == columnIndex) {
					Bitmap image;
					
					ImageView iv = (ImageView)v.findViewById(R.id.photo);
					TextView tv = (TextView)v.findViewById(R.id.line1);

					long contactId = cursor.getLong(cursor.getColumnIndex(OpenHelper.CONTACT_ID));
					String lookupKey = cursor.getString(cursor.getColumnIndex(OpenHelper.LOOKUP));
					String phoneNumber = cursor.getString(cursor.getColumnIndex(OpenHelper.PHONE));
					
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
						input.close();
					}
					else {
						iv.setImageResource(R.drawable.ic_launcher);
					}

					return true;
				}
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			
			return false;
		}
		
	}

	public static class CursorLoaderListFragment extends ListFragment 
		implements OnItemLongClickListener, LoaderManager.LoaderCallbacks<Cursor> 
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
            
		}
		
		@Override
		public void onListItemClick(ListView lv, View v, int position, long id) {
			
			Intent intent = getActivity().getIntent();
			if(intent != null) {
				intent.setData(ContentUris.withAppendedId(DataProvider.CONTENT_URI, id));
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

	        return new CursorLoader(getActivity(), 
	        		DataProvider.CONTENT_URI,
	        		new String[] { 
	        			OpenHelper.ID, 
	        			OpenHelper.LOOKUP, 
	        			OpenHelper.CONTACT_ID
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

	}
	
	
}