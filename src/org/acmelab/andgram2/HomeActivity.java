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
import android.content.Intent;
import android.net.*;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends Activity
{
    private static final String AUTHORIZATION_URL = "https://api.instagram.com/oauth/authorize/";
    private static final String ACCESS_TOKEN_ENDPOINT = "https://api.instagram.com/oauth/access_token";
    private static final String REDIRECT_URI = "andgram://";
    private static final String TAG = "ANDGRAM2";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        setContentView(R.layout.main);
    }

    public void doAuth(View view) {
        Log.i(TAG, "doAuth");
        // == STEP ONE: DIRECT YOUR USER TO OUR AUTHORIZATION URL
        Intent webAuthIntent = new Intent(Intent.ACTION_VIEW);
        webAuthIntent.setData(Uri.parse(getAuthorizationUrl()));
        startActivity(webAuthIntent);
    }

    private String getAuthorizationUrl() {
        StringBuilder authorizationUrl = new StringBuilder();
        authorizationUrl.append(AUTHORIZATION_URL);
        authorizationUrl.append("?client_id=" + Credentials.CLIENT_ID);
        authorizationUrl.append("&redirect_uri="+ REDIRECT_URI);
        authorizationUrl.append("&response_type=code");
        authorizationUrl.append("&display=touch");

        return authorizationUrl.toString();
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.i(TAG, "onResume");

        Uri uri = this.getIntent().getData();
        if( uri != null ) {
            String access_code = retrieveAccessCode(uri);
            String access_token = requestAccessToken(access_code);

            Toast.makeText(this, "Got access token: " + access_token, Toast.LENGTH_LONG).show();
            Log.i(TAG, "Got access token: " + access_token);
        }
    }

    private String retrieveAccessCode(Uri uri) {
        if(uri != null) {
            String access_code = uri.getQueryParameter("code");
            String error = uri.getQueryParameter("error");
            String error_reason = uri.getQueryParameter("error_reason");
            String error_description = uri.getQueryParameter("error_description");

            if( error != null ) {
                // didn't actually work
                Toast.makeText(this, "Authorization error!\n" + error_reason + "\n" +
                        error_description , Toast.LENGTH_LONG).show();
                Log.e(TAG, "OAuth access code error.");
                Log.e(TAG, error);
                Log.e(TAG, error_reason);
                Log.e(TAG, error_description);
                return null;
            } else {
                return access_code;
            }
        } else {
            return null;
        }
    }

    private String requestAccessToken(String access_code) {
        HttpResponse httpResponse;

        HttpClient httpClient =  new MyHttpClient(getApplicationContext());
        HttpPost httpPost = new HttpPost(ACCESS_TOKEN_ENDPOINT);

        List<NameValuePair> postParams = new ArrayList<NameValuePair>();
        postParams.add(new BasicNameValuePair("client_id", Credentials.CLIENT_ID));
        postParams.add(new BasicNameValuePair("client_secret", Credentials.CLIENT_SECRET));
        postParams.add(new BasicNameValuePair("grant_type", "authorization_code"));
        postParams.add(new BasicNameValuePair("redirect_uri", REDIRECT_URI));
        postParams.add(new BasicNameValuePair("code", access_code));

        // post it
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(postParams));
            httpResponse = httpClient.execute(httpPost);

            // test result code
            if( httpResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK ) {
                Toast.makeText(this, "Authorization error", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error requesting oauth access token.");
                return null;
            }

        } catch (IOException ioException) {
            Toast.makeText(this, "Authorization error", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error requesting oauth access token.");
            ioException.printStackTrace();
            return null;
        }

        Log.i(TAG, "Got access token response!");

        // parse JSON response
        try {
            HttpEntity httpEntity = httpResponse.getEntity();
            if( httpEntity != null ) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(httpEntity.getContent(), "UTF-8"));
                String json = reader.readLine();
                JSONTokener jsonTokener = new JSONTokener(json);
                JSONObject jsonObject = new JSONObject(jsonTokener);
                Log.i(TAG,"JSON: " + jsonObject.toString());

                String access_token = jsonObject.getString("access_token");
                JSONObject userObject = jsonObject.getJSONObject("user");
                String user_id = userObject.getString("id");
                String username = userObject.getString("username");
                String full_name = userObject.getString("full_name");
                String profile_picture_url = userObject.getString("profile_picture");

                return access_token;

            }
        } catch( JSONException jsonException ) {
            Toast.makeText(this, "Error parsing authorization response.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error parsing oauth access token JSON response.");
            return null;
        } catch( IOException ioException ) {
            Toast.makeText(this, "I/O error parsing authorization response.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "I/O error parsing oauth access token JSON response.");
            return null;
        }

        return null;
    }

}
