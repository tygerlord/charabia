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

import android.widget.SimpleCursorAdapter;

public class PickContactActivity extends ListActivity
{
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.contactlist);

		OpenHelper oh = new OpenHelper(this);
		
		SQLiteDatabase db = oh.getReadableDatabase();
		
		Cursor cursor = db.rawQuery("SELECT ?,? IN ?", new String[] { OpenHelper.NAME, OpenHelper.PHONE} );
		
		startManagingCursor(cursor);

		String[] columns = new String[] { OpenHelper.NAME, OpenHelper.PHONE };
		int[] to = new int[] { R.id.name, R.id.number };

		SimpleCursorAdapter mAdapter = new SimpleCursorAdapter(this, R.layout.contactlist_entry, cursor, columns, to);

		this.setListAdapter(mAdapter);
	}

}