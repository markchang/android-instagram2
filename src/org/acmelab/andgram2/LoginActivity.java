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
import android.content.SharedPreferences;
import android.net.Uri;
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
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import javax.net.ssl.SSLException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: mchang
 * Date: 4/16/11
 * Time: 12:54 AM
 * Prompts the user to login. Discusses OAuth.
 */
public class LoginActivity extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_layout);

        Log.i(Constants.TAG, "LoginActivity onCreate");
    }

    public void doAuth(View view) {
        Log.i(Constants.TAG, "LoginActivity doAuth");
        // == STEP ONE: DIRECT YOUR USER TO OUR AUTHORIZATION URL
        // TODO: use a webview rather than the browser
        Intent webAuthIntent = new Intent(Intent.ACTION_VIEW);
        webAuthIntent.setData(Uri.parse(getAuthorizationUrl()));
        startActivity(webAuthIntent);
    }

    private String getAuthorizationUrl() {
        StringBuilder authorizationUrl = new StringBuilder();
        authorizationUrl.append(Constants.AUTHORIZATION_URL);
        authorizationUrl.append("?client_id=" + Credentials.CLIENT_ID);
        authorizationUrl.append("&redirect_uri="+ Constants.REDIRECT_URI);
        authorizationUrl.append("&response_type=code");
        authorizationUrl.append("&display=touch");
        authorizationUrl.append("&scope=likes+comments+relationships");

        return authorizationUrl.toString();
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.i(Constants.TAG, "LoginActivity onResume");

        Uri uri = this.getIntent().getData();
        if( uri != null ) {
            // oauth callback, so we update or create our credentials
            String access_code = retrieveAccessCode(uri);
            Map<String,String> oauthMap = requestAccessToken(access_code);

            if( oauthMap != null ) {
                SharedPreferences sharedPreferences = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.clear();

                // stash in preferences
                Iterator it = oauthMap.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry pairs = (Map.Entry)it.next();
                    editor.putString((String)pairs.getKey(), (String)pairs.getValue());
                    System.out.println(pairs.getKey() + " = " + pairs.getValue());
                }
                editor.commit();

                // TODO: fix this
                Toast.makeText(this, "Login successful!", Toast.LENGTH_LONG).show();

                // start main activity
                Intent homeIntent = new Intent(LoginActivity.this, HomeActivity.class);
                homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(homeIntent);

            } else {
                // TODO: better error message
            }
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
                Log.e(Constants.TAG, "OAuth access code error.");
                Log.e(Constants.TAG, error);
                Log.e(Constants.TAG, error_reason);
                Log.e(Constants.TAG, error_description);
                return null;
            } else {
                return access_code;
            }
        } else {
            return null;
        }
    }

    private Map<String,String> requestAccessToken(String access_code) {
        HttpResponse httpResponse;
        HashMap<String,String> oauthMap = new HashMap<String,String>();

        HttpClient httpClient =  new MyHttpClient(getApplicationContext());
        HttpPost httpPost = new HttpPost(Constants.ACCESS_TOKEN_ENDPOINT);

        List<NameValuePair> postParams = new ArrayList<NameValuePair>();
        postParams.add(new BasicNameValuePair("client_id", Credentials.CLIENT_ID));
        postParams.add(new BasicNameValuePair("client_secret", Credentials.CLIENT_SECRET));
        postParams.add(new BasicNameValuePair("grant_type", "authorization_code"));
        postParams.add(new BasicNameValuePair("redirect_uri", Constants.REDIRECT_URI));
        postParams.add(new BasicNameValuePair("code", access_code));

        // post it
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(postParams));
            httpResponse = httpClient.execute(httpPost);

            // test result code
            if( httpResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK ) {
                Toast.makeText(this, "Authorization error", Toast.LENGTH_SHORT).show();
                Log.e(Constants.TAG, "Error requesting oauth access token.");
                return null;
            }

        } catch( SSLException sslException ) {
            Toast.makeText(this, "SSL exception.\nMost times, you can simply try again.", Toast.LENGTH_SHORT).show();
            Log.e(Constants.TAG, "SSL exception.");
            return null;
        } catch (IOException ioException) {
            Toast.makeText(this, "Authorization error", Toast.LENGTH_SHORT).show();
            Log.e(Constants.TAG, "Error requesting oauth access token.");
            return null;
        }

        Log.i(Constants.TAG, "Got access token response!");

        // parse JSON response
        try {
            HttpEntity httpEntity = httpResponse.getEntity();
            if( httpEntity != null ) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(httpEntity.getContent(), "UTF-8"));
                String json = reader.readLine();
                JSONTokener jsonTokener = new JSONTokener(json);
                JSONObject jsonObject = new JSONObject(jsonTokener);
                JSONObject userObject = jsonObject.getJSONObject("user");

                // stash it in the return hashmap
                oauthMap.put("access_token", jsonObject.getString("access_token"));
                oauthMap.put("user_id", userObject.getString("id"));
                oauthMap.put("username", userObject.getString("username"));
                oauthMap.put("full_name", userObject.getString("full_name"));
                oauthMap.put("profile_picture_url", userObject.getString("profile_picture"));

                return oauthMap;

            }
        } catch( JSONException jsonException ) {
            Toast.makeText(this, "Error parsing authorization response.", Toast.LENGTH_SHORT).show();
            Log.e(Constants.TAG, "Error parsing oauth access token JSON response.");
            return null;
        } catch( IOException ioException ) {
            Toast.makeText(this, "I/O error parsing authorization response.", Toast.LENGTH_SHORT).show();
            Log.e(Constants.TAG, "I/O error parsing oauth access token JSON response.");
            return null;
        }

        return null;
    }


}