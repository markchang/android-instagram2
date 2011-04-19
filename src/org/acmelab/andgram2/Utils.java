/*
 * Copyright 2011, Mark L. Chang <mark.chang@gmail.com>. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other
 *       materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY Mark L. Chang ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL MARK L. CHANG OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of Mark L. Chang.
 */

package org.acmelab.andgram2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.util.Log;
import android.widget.Toast;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Utils {

    public static String getAccessToken(Context ctx) {
        SharedPreferences sharedPreferences = ctx.getSharedPreferences(Constants.PREFS_NAME, Activity.MODE_PRIVATE);
        return(sharedPreferences.getString("access_token", null));
    }

    public static boolean isOnline(Context ctx) {
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if( cm.getActiveNetworkInfo() == null ) return false;
        return cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }

    public static String decorateEndpoint(String endpoint, String access_token) {
        return endpoint + "?access_token=" + access_token;
    }

    public static void CopyStream(InputStream is, OutputStream os)
    {
        final int buffer_size=1024;
        try
        {
            byte[] bytes=new byte[buffer_size];
            for(;;)
            {
              int count=is.read(bytes, 0, buffer_size);
              if(count==-1)
                  break;
              os.write(bytes, 0, count);
            }
        }
        catch(Exception ex){}
    }

    public static String getUsername(Context ctx) {
        SharedPreferences sharedPreferences = ctx.getSharedPreferences(Constants.PREFS_NAME, Activity.MODE_PRIVATE);
        return sharedPreferences.getString("username",null);
    }


    public static String doRestfulPut(MyHttpClient httpClient, String url,
                                     List<NameValuePair> postParams, Context ctx) {
        // create POST
        HttpPost httpPost = new HttpPost(url);

        try {
            httpPost.setEntity(new UrlEncodedFormEntity(postParams, HTTP.UTF_8));
            HttpResponse httpResponse = httpClient.execute(httpPost);

            // test result code
            if( httpResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK ) {
                Log.i(Constants.TAG, "Login HTTP status fail");
                return null;
            }

            // test json response
            HttpEntity httpEntity = httpResponse.getEntity();
            if( httpEntity != null ) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(httpEntity.getContent(), "UTF-8"));
                String json = reader.readLine();
                JSONTokener jsonTokener = new JSONTokener(json);
                JSONObject jsonObject = new JSONObject(jsonTokener);
                Log.i(Constants.TAG,"JSON: " + jsonObject.toString());

                String loginStatus = jsonObject.getString("status");

                if( !loginStatus.equals("ok") ) {
                    Log.e(Constants.TAG, "JSON status not ok: " + jsonObject.getString("status"));
                    return null;
                } else {
                    return json;
                }
            } else {
                return null;
            }
        } catch( IOException e ) {
            Log.e(Constants.TAG, "HttpPost error: " + e.toString());
            return null;
        } catch( JSONException e ) {
            Log.e(Constants.TAG, "JSON parse error: " + e.toString());
            return null;
        }
    }

    public static JSONObject doRestfulGet(MyHttpClient httpClient, String url, Context ctx) {
        if( Utils.isOnline(ctx) == false ) {
            Toast.makeText(ctx,"No connection to Internet.\nTry again later",Toast.LENGTH_SHORT).show();
            Log.i(Constants.TAG, "No internet!");
            return null;
        }

        // TODO: SSL retries
        try {
            HttpGet httpGet = new HttpGet(url);
            HttpResponse httpResponse = httpClient.execute(httpGet);

            // test result code
            if( httpResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK ) {
                Log.e(Constants.TAG, "Return status code bad.");
                return null;
            }

            HttpEntity httpEntity = httpResponse.getEntity();

            if( httpEntity != null ) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(httpEntity.getContent(), "UTF-8"));
                String json = reader.readLine();
                JSONTokener jsonTokener = new JSONTokener(json);
                JSONObject jsonObject = new JSONObject(jsonTokener);
                return jsonObject;
            } else {
                Log.e(Constants.TAG, "instagram returned bad data");
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}