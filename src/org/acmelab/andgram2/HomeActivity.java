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
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class HomeActivity extends Activity
{
    String access_token = null;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Log.i(Constants.TAG, "onCreate");
        setContentView(R.layout.home_layout);

        // if no login data, prompt for login
        access_token = Utils.getAccessToken(this);

        if( access_token == null ) {
            openLoginIntent(null);
        }
    }

    public void setAccessToken() {
        SharedPreferences sharedPreferences = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        access_token = sharedPreferences.getString("access_token", null);
    }


    public void openPopularGridIntent(View view) {
        // if no login data, prompt for login
        access_token = Utils.getAccessToken(this);

        if( access_token == null )
            openLoginIntent(null);
        else {
            String popular_endpoint = Utils.decorateEndpoint(Constants.POPULAR_ENDPOINT, access_token);
            Intent feedIntent = new Intent(HomeActivity.this, PopularActivity.class);
            feedIntent.putExtra("endpoint", popular_endpoint);
            feedIntent.putExtra("title", R.string.popular);
            startActivity(feedIntent);
        }
    }

    public void openFeedIntent(View view) {
        access_token = Utils.getAccessToken(this);

        if( access_token == null )
            openLoginIntent(null);
        else {
            String feed_endpoint = Utils.decorateEndpoint(Constants.USER_FEED_ENDPOINT, access_token);
            Intent feedIntent = new Intent(HomeActivity.this, FeedActivity.class);
            feedIntent.putExtra("endpoint", feed_endpoint);
            feedIntent.putExtra("title", R.string.feed);
            startActivity(feedIntent);
        }
    }


    public void openLoginIntent(View view) {
        // clear all login data
        SharedPreferences sharedPreferences = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.commit();

        Intent loginIntent = new Intent(HomeActivity.this, LoginActivity.class);
        loginIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(loginIntent);
    }

}
