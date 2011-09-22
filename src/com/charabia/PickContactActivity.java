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
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;

import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;

import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.view.View;

import android.content.DialogInterface;
import android.content.Intent;

import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;

/**
 * @author 
 *
 */
public class PickContactActivity extends FragmentActivity 
{
	
	// Dialogs
	private static final int EDIT_DIALOG = 0;

	// Loader
	private static final int CONTACTS_LOADER = 1;
	
	private String[] contactsListe = null;

	private ArrayAdapter<String> mAdapter = null;

	private Cursor cursor = null;
	
	private ArrayListFragment list = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
        		
	      // Create the list fragment and add it as our sole content.
        if (getSupportFragmentManager().findFragmentById(android.R.id.content) == null) {
            list = new ArrayListFragment();
            getSupportFragmentManager().beginTransaction().add(android.R.id.content, list).commit();
        }
		
//		mAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, list);
//		
//		setListAdapter(mAdapter);
//
//		getLoaderManager().initLoader(CONTACTS_LOADER, null, this);
//		
//        getListView().setOnItemLongClickListener(this);
	}
	
	
	/* Handles dialogs */
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = null;
		AlertDialog.Builder builder;
		switch(id) {
			case EDIT_DIALOG:
				builder = new AlertDialog.Builder(this);
				builder.setTitle(getString(R.string.app_name));
				builder.setItems(new String[] { 
					getString(R.string.delete), 
					 }, editListener);
				dialog = builder.create();
			break;
			default:
				dialog = null;
		}
		return dialog;
	}
		
	private final DialogInterface.OnClickListener editListener =
		new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialogInterface, int i) {
				switch(i) {
					case 0: //delete
						//OpenHelper oh = new OpenHelper(getApplicationContext());
						//SQLiteDatabase db = oh.getWritableDatabase();
						
						//oh.delete(db, cursor.getInt(cursor.getColumnIndex(OpenHelper.ID)));
						
						//db.close();
						break;
					default:
				}
		}
	};

	public static class ArrayListFragment extends ListFragment implements OnItemLongClickListener, LoaderManager.LoaderCallbacks<Cursor> 
	{
		private Cursor cursor = null;
		private SimpleCursorAdapter mAdapter = null;
		
		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            
            mAdapter = new SimpleCursorAdapter(getActivity(), 
            		 android.R.layout.simple_list_item_1, null, 
            		 new String[] { OpenHelper.PHONE }, 
            		 new int[] { android.R.id.text1 },
            		 SimpleCursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
            
            setListAdapter(mAdapter);
            
            setListShown(false);
            
            getListView().setOnItemLongClickListener(this);
            
            getLoaderManager().initLoader(0, null, this);
		}
		
		@Override
		public void onListItemClick(ListView lv, View v, int position, long id) {
			if(cursor == null) {
				return; 
			}
			
			cursor.moveToPosition(position);
			Intent intent = getActivity().getIntent();
			if(intent != null) {
				intent.putExtra("ID", cursor.getInt(cursor.getColumnIndex(OpenHelper.ID)));
				intent.putExtra("PHONE", cursor.getString(cursor.getColumnIndex(OpenHelper.PHONE)));
				getActivity().setResult(Activity.RESULT_OK, intent);
	        }
			cursor.close();
			getActivity().finish();		
		}
	
		@Override
		public boolean onItemLongClick(AdapterView<?> av, View v, int position, long id) {
			getActivity().showDialog(EDIT_DIALOG);
			return true;
		}
	

		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			Uri baseUri = ContactProvider.CONTENT_URI;
			
	        return new CursorLoader(getActivity(), baseUri,
	        		new String[] { OpenHelper.ID, OpenHelper.PHONE}, 
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