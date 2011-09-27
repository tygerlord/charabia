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

import android.database.Cursor;

import android.widget.ListView;
import android.widget.TextView;
import android.view.View;

import android.content.Context;
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
public class SmsListe extends FragmentActivity 
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

		Context context;
		
		public viewBinder(Context context) {
			this.context = context;
		}
		
		@Override
		public boolean setViewValue(View v, Cursor cursor, int columnIndex) {
			Tools tools = new Tools(context);
			try {
				TextView tv = (TextView)v;
				
				if(cursor.getColumnIndex(OpenHelper.SMS_PDU) == columnIndex) {
					byte[] pdu = cursor.getBlob(columnIndex);
					SmsMessage sms = SmsMessage.createFromPdu(pdu);
					
					String phoneNumber = sms.getOriginatingAddress();
					String texte = tools.getDisplayName(phoneNumber) + "(" + phoneNumber + ")";
					
					tv.setText(context.getString(R.string.from) + " " +  texte + "\n" + sms.getDisplayMessageBody());
					
					return true;
				}
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			
			return false;
		}
		
	}
	
	public static class CursorLoaderListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> 
	{
		// Loader
		private static final int SMS_LOADER = 1;

		private SimpleCursorAdapter mAdapter = null;
		
		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            
            setEmptyText(getActivity().getString(R.string.no_message));
            
            mAdapter = new SimpleCursorAdapter(getActivity(), 
            		 android.R.layout.simple_list_item_1, null, 
            		 new String[] { OpenHelper.SMS_PDU }, 
            		 new int[] { android.R.id.text1 },
            		 0);
            
            mAdapter.setViewBinder(new viewBinder(getActivity()));
            
            setListAdapter(mAdapter);
            
            setListShown(false);
            
            getLoaderManager().initLoader(SMS_LOADER, null, this);
		}
		
		@Override
		public void onListItemClick(ListView lv, View v, int position, long id) {
			
			Cursor cursor = mAdapter.getCursor();
			
			Intent intent = new Intent();
			intent.setClassName(getActivity(), SmsViewActivity.class.getName());
			intent.setData(ContentUris.withAppendedId(DataProvider.CONTENT_URI_PDUS, id));
			startActivity(intent);
		}
	
		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			Uri baseUri = DataProvider.CONTENT_URI_PDUS;
			
	        return new CursorLoader(getActivity(), baseUri,
	        		new String[] { OpenHelper.ID, OpenHelper.SMS_PDU}, 
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