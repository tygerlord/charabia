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

import android.os.Bundle;

import android.app.ListActivity;

import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;

import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import android.view.View;

import android.content.Intent;

/**
 * @author 
 *
 */
public class PickContactActivity extends ListActivity implements OnItemLongClickListener
{
	private Cursor cursor = null;
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		//setContentView(R.layout.contactlist);

		OpenHelper oh = new OpenHelper(this);
		
		//Writeable in case of creation
		SQLiteDatabase db = oh.getWritableDatabase();
		
		cursor = db.rawQuery("SELECT " + OpenHelper.ID + "," + OpenHelper.PHONE + " FROM "+ OpenHelper.KEYS_TABLE, null );
		
		startManagingCursor(cursor);

		int n = cursor.getCount();
		String[] t = new String[n];
		String phoneNumber = "";
		
		for(int i = 0; i < n; i++) {
			if(cursor.moveToPosition(i)) {
				phoneNumber = cursor.getString(cursor.getColumnIndex(OpenHelper.PHONE));
				t[i] = Tools.getDisplayName(this, phoneNumber) + "\n" + phoneNumber;				
			}
		}
		
        db.close();
        
		ArrayAdapter<String> mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, t);
		
		this.setListAdapter(mAdapter);
		
        ListView lv = getListView();

        lv.setOnItemLongClickListener(this);
	}
	
	@Override
	protected void onListItemClick(ListView lv, View v, int position, long id) {
		cursor.moveToPosition(position);
		Intent intent = getIntent();
		if(intent != null) {
			intent.putExtra("ID", cursor.getInt(cursor.getColumnIndex(OpenHelper.ID)));
			intent.putExtra("PHONE", cursor.getString(cursor.getColumnIndex(OpenHelper.PHONE)));
			setResult(RESULT_OK, intent);
        }
		cursor.close();
		finish();		
	}
	
	@Override
	public boolean onItemLongClick(AdapterView<?> av, View v, int position, long id) {
		Toast.makeText(this, "long click", Toast.LENGTH_LONG).show();
		return true;
	}
}