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

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;


public class DataProvider extends ContentProvider 
{
	public static final String PROVIDER_NAME = 
		"com.charabia.provider.data";
	
	public static final Uri CONTENT_URI = 
		Uri.parse("content://"+ PROVIDER_NAME + "/contacts");

	public static final Uri CONTENT_URI_PDUS = 
			Uri.parse("content://"+ PROVIDER_NAME + "/pdus");

	private static final int CONTACTS = 1;
	private static final int CONTACT_ID = 2;   
	private static final int PDUS = 3;
	private static final int PDU_ID = 4;
	
	private static final UriMatcher uriMatcher;
	static{
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(PROVIDER_NAME, "contacts", CONTACTS);
		uriMatcher.addURI(PROVIDER_NAME, "contacts/#", CONTACT_ID);      
		uriMatcher.addURI(PROVIDER_NAME, "pdus", PDUS);
		uriMatcher.addURI(PROVIDER_NAME, "pdus/#", PDU_ID);
	}

	private SQLiteDatabase db = null;
      
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int count=0;
		String id = null;
		String table = null;
		switch (uriMatcher.match(uri)) {
			case CONTACTS:
				table = OpenHelper.KEYS_TABLE;
				break;
			case CONTACT_ID:
				id = uri.getPathSegments().get(1);
				table = OpenHelper.KEYS_TABLE;
				break;
			case PDUS:
				table = OpenHelper.SMS_TABLE;
				break;
			case PDU_ID:
				id = uri.getPathSegments().get(1);
				table = OpenHelper.SMS_TABLE;
				break;
			default: throw new IllegalArgumentException(
				"Unknown URI " + uri);    
		}    
		
		if(id == null) {
			count = db.delete(table, selection, selectionArgs);			
		}
		else {
			count = db.delete(
					table,
					OpenHelper.ID + " = " + id + 
					(!TextUtils.isEmpty(selection) ? " AND (" + 
					selection + ')' : ""), selectionArgs);		
		}
		
		getContext().getContentResolver().notifyChange(uri, null);
		return count;      
  	}
	
	@Override
	public String getType(Uri uri) {
		switch (uriMatcher.match(uri)) {
			case CONTACTS:
				return "vnd.android.cursor.dir/vnd.charabia.contacts";
			case CONTACT_ID:                
				return "vnd.android.cursor.item/vnd.charabia.contact";
			case PDUS:
				return "vnd.android.cursor.dir/vnd.charabia.pdus";
			case PDU_ID:                
				return "vnd.android.cursor.item/vnd.charabia.pdu";
			default:
				throw new IllegalArgumentException("Unsupported URI: " + uri);        
		}   
	}
	
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		String table = null;
		
		switch (uriMatcher.match(uri)) {
			case CONTACTS:
				table = OpenHelper.KEYS_TABLE;
				break;
			case PDUS:
				table = OpenHelper.SMS_TABLE;
				break;
			default: throw new IllegalArgumentException(
				"Unknown URI " + uri);    
		}    

		long rowID = db.insert(table, "", values);
	           
	      if (rowID>0)
	      {
	         Uri _uri = ContentUris.withAppendedId(uri, rowID);
	         getContext().getContentResolver().notifyChange(_uri, null);    
	         return _uri;                
	      }        
	      throw new SQLException("Failed to insert row into " + uri);
	}
	
	@Override
	public boolean onCreate() {
	    Context context = getContext();
	    OpenHelper oh = new OpenHelper(context);
	    db = oh.getWritableDatabase();
	    return (db == null)? false:true;
	}
	
	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder sqlBuilder = new SQLiteQueryBuilder();
		String id = null;
		String table = null;
		
		switch (uriMatcher.match(uri)) {
			case CONTACTS:
				table = OpenHelper.KEYS_TABLE;
				break;
			case CONTACT_ID:
				id = uri.getPathSegments().get(1);
				table = OpenHelper.KEYS_TABLE;
				break;
			case PDUS:
				table = OpenHelper.SMS_TABLE;
				break;
			case PDU_ID:
				id = uri.getPathSegments().get(1);
				table = OpenHelper.SMS_TABLE;
				break;
			default: throw new IllegalArgumentException(
				"Unknown URI " + uri);    
		}    
		
		sqlBuilder.setTables(table);
		       
		if (id != null) {
			sqlBuilder.appendWhere(
		        OpenHelper.ID + " = " + uri.getPathSegments().get(1));
		}
		       
		Cursor c = sqlBuilder.query(
		         db, 
		         projection, 
		         selection, 
		         selectionArgs, 
		         null, 
		         null, 
		         null);
		   
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}
	
	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		int count=0;
		String id = null;
		String table = null;
		switch (uriMatcher.match(uri)) {
	    	case CONTACTS:
	    		table = OpenHelper.KEYS_TABLE;
	    		break;
	        case CONTACT_ID:
	            id = uri.getPathSegments().get(1);
	    		table = OpenHelper.KEYS_TABLE;
	            break;
	    	case PDUS:
	    		table = OpenHelper.SMS_TABLE;
	    		break;
	        case PDU_ID:
	            id = uri.getPathSegments().get(1);
	    		table = OpenHelper.SMS_TABLE;
	            break;
	        default: throw new IllegalArgumentException(
	            "Unknown URI " + uri);    
		}     
		
		if(id == null) {
    		count = db.update(table, values, selection, selectionArgs);	
		}
		else {
            id = uri.getPathSegments().get(1);
            count = db.update(
            		table,
            		values,
            		OpenHelper.ID + " = " + id + 
            		(!TextUtils.isEmpty(selection) ? " AND (" + 
            		selection + ')' : ""), selectionArgs);		
		}
		
		getContext().getContentResolver().notifyChange(uri, null);
		return count;      
	}
 }