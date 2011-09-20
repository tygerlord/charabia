/*
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
import java.util.Locale;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;

public class WebViewActivity extends Activity 
{

	private static final String defaultUrl = "file:///android_asset/html/help/index.html";
	
	private WebView webView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		webView = new WebView(this);
		setContentView(webView);

		WebSettings webSettings = webView.getSettings();
		webSettings.setSavePassword(false);
		webSettings.setSaveFormData(false);
		webSettings.setJavaScriptEnabled(false);
		webSettings.setSupportZoom(false);

		webView.setWebChromeClient(new WebChromeClient());
		
		Intent intent = getIntent();
		String action = intent == null ? null : intent.getAction();
		
		String url = defaultUrl;
		if (intent != null && action != null) 
		{
			if (action.equals(Intent.ACTION_VIEW)) 
			{
				Uri uri = intent.getData();
				if(uri != null)
				{
					url = uri.toString();
				}
			}
		}
		
		webView.loadUrl(url);
	}
	
	/*
	 * @brief Return url based on locale language
	 */
	public static String getBaseUrl(Context context, String path, String filename) {
		AssetManager am = context.getAssets();

		Locale locale = Locale.getDefault();

		String[] filelist = null;
		
		try {
			filelist = am.list("html-"+locale.getLanguage()+path);
			for(int i = 0; filelist != null && i < filelist.length; i++) {
				if(filename.equals(filelist[i])) {
					return "file:///android_asset/html-"+locale.getLanguage()+path+"/"+filename;
				}
			}
			
		} catch (IOException e) {
		}

		Log.v("CHARABIA:getBaseUrl", "html-"+locale.getLanguage()+path+"/"+filename+" not found");
		
		return "file:///android_asset/html"+path+"/"+filename;
	}
}
