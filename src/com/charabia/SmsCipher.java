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

import android.util.Base64;

import android.widget.Toast;

import android.content.Context;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;

public class SmsCipher
{

	public static final String KEYWORD = "itscharabia:";

	public static final String CIPHER_ALGO = "AES/CBC/PKCS5Padding";

	public static final byte[] demo_key = new byte[] { 
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 ,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 
		};

	private Context context = null;
	
	public SmsCipher(Context context) {
		this.context = context;
	}
	
	public SecretKey generateKeyAES(int size) {
		try {
			KeyGenerator keyGen = KeyGenerator.getInstance("AES");
			keyGen.init(size);
			SecretKey key = keyGen.generateKey();
			
			// key.getEncoded to have byte[]
			return key;
		}
		catch(Exception e) {
			Toast.makeText(context, context.getString(R.string.unexpected_error) + "\n" + e.toString(), Toast.LENGTH_LONG).show();
		}

		return null;
	}

	public SecretKey generateKeyAES() {
		return generateKeyAES(256);
	}
	
	public String decryptTexte(byte[] key_data, String texte) {
		
		try {
			byte[] data = Base64.decode(texte.substring(KEYWORD.length()), Base64.DEFAULT);
			
			Cipher c = Cipher.getInstance(CIPHER_ALGO);
		
			SecretKey key = new SecretKeySpec(key_data, "AES");
		
			c.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(data, 0, 16));

			String result = new String(c.doFinal(data, 16, data.length-16));
			
			return result;
		}
		catch(Exception e) {
			Toast.makeText(context, context.getString(R.string.unexpected_error) + "\n" + e.toString(), Toast.LENGTH_LONG).show();
		}

		return context.getString(R.string.unexpected_error);
	}

	public String encryptToTexte(byte[] key_data, String texte) {
		try {
			Cipher c = Cipher.getInstance(CIPHER_ALGO);

			SecretKey key = new SecretKeySpec(key_data, "AES");

			c.init(Cipher.ENCRYPT_MODE, key);

			byte[] bIV = c.getIV();
			byte[] cryptedTexte = c.doFinal(texte.getBytes());
			byte[] data = new byte[cryptedTexte.length+bIV.length];

			System.arraycopy(bIV, 0, data, 0, bIV.length);
			System.arraycopy(cryptedTexte, 0, data, bIV.length, cryptedTexte.length);

			return KEYWORD + Base64.encodeToString(data, Base64.DEFAULT);
		}
		catch(Exception e) {
			Toast.makeText(context, context.getString(R.string.unexpected_error) + "\n" + e.toString(), Toast.LENGTH_LONG).show();
		}
		
		return null;
	}

	public String decrypt(byte[] key_data, byte[] data) {
		
		try {
			Cipher c = Cipher.getInstance(CIPHER_ALGO);
		
			SecretKey key = new SecretKeySpec(key_data, "AES");
		
			c.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(data, Tools.magic.length, 16));

			String result = new String(c.doFinal(data, Tools.magic.length+16, data.length-16-Tools.magic.length));
			
			return result;
		}
		catch(Exception e) {
			Toast.makeText(context, context.getString(R.string.unexpected_error) + "\n" + e.toString(), Toast.LENGTH_LONG).show();
		}

		return context.getString(R.string.unexpected_error);
	}

	public byte[] encrypt(byte[] key_data, String texte) {
		try {
			Cipher c = Cipher.getInstance(CIPHER_ALGO);

			SecretKey key = new SecretKeySpec(key_data, "AES");

			c.init(Cipher.ENCRYPT_MODE, key);

			byte[] bIV = c.getIV();
			byte[] cryptedTexte = c.doFinal(texte.getBytes());
			byte[] data = new byte[Tools.magic.length+cryptedTexte.length+bIV.length];

			System.arraycopy(Tools.magic, 0, data, 0, Tools.magic.length);
			System.arraycopy(bIV, 0, data, Tools.magic.length, bIV.length);
			System.arraycopy(cryptedTexte, 0, data, Tools.magic.length+bIV.length, cryptedTexte.length);

			return data;
		}
		catch(Exception e) {
			Toast.makeText(context, context.getString(R.string.unexpected_error) + "\n" + e.toString(), Toast.LENGTH_LONG).show();
		}
		
		return null;
	}
}
