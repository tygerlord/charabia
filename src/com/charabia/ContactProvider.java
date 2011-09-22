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


public class ContactProvider extends ContentProvider 
{
	public static final String PROVIDER_NAME = 
	     "com.charabia.provider.Contacts";
	  
	public static final Uri CONTENT_URI = 
	     Uri.parse("content://"+ PROVIDER_NAME + "/contacts");
	  
	private static final int CONTACTS = 1;
	private static final int CONTACT_ID = 2;   
	     
	private static final UriMatcher uriMatcher;
	static{
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	    uriMatcher.addURI(PROVIDER_NAME, "contacts", CONTACTS);
	    uriMatcher.addURI(PROVIDER_NAME, "contacts/#", CONTACT_ID);      
	}
    
	private SQLiteDatabase db = null;
      
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int count=0;
		switch (uriMatcher.match(uri)) {
	    	case CONTACTS:
	    		count = db.delete(OpenHelper.KEYS_TABLE, selection, selectionArgs);
	    		break;
	        case CONTACT_ID:
	            String id = uri.getPathSegments().get(1);
	            count = db.delete(
	            		OpenHelper.KEYS_TABLE,                        
	            		OpenHelper.ID + " = " + id + 
	            		(!TextUtils.isEmpty(selection) ? " AND (" + 
	            		selection + ')' : ""), selectionArgs);
	            break;
	        default: throw new IllegalArgumentException(
	            "Unknown URI " + uri);    
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
				return "vnd.android.cursor.item/vnd.charabia.contacts";
			default:
				throw new IllegalArgumentException("Unsupported URI: " + uri);        
		}   
	}
	
	@Override
	public Uri insert(Uri uri, ContentValues values) {
	      long rowID = db.insert(
	         OpenHelper.KEYS_TABLE, "", values);
	           
	      if (rowID>0)
	      {
	         Uri _uri = ContentUris.withAppendedId(CONTENT_URI, rowID);
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
		sqlBuilder.setTables(OpenHelper.KEYS_TABLE);
		       
		if (uriMatcher.match(uri) == CONTACT_ID) {
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
		switch (uriMatcher.match(uri)) {
	    	case CONTACTS:
	    		count = db.update(OpenHelper.KEYS_TABLE, values, selection, selectionArgs);
	    		break;
	        case CONTACT_ID:
	            String id = uri.getPathSegments().get(1);
	            count = db.update(
	            		OpenHelper.KEYS_TABLE,
	            		values,
	            		OpenHelper.ID + " = " + id + 
	            		(!TextUtils.isEmpty(selection) ? " AND (" + 
	            		selection + ')' : ""), selectionArgs);
	            break;
	        default: throw new IllegalArgumentException(
	            "Unknown URI " + uri);    
		}       
		getContext().getContentResolver().notifyChange(uri, null);
		return count;      
	}
 }