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
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.GridView;
import android.widget.Toast;
import com.markupartist.android.widget.ActionBar;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import javax.net.ssl.SSLException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class PopularActivity extends Activity {

    private String sourceUrl;

    GridView grid;
    LazyGridAdapter adapter;
    ArrayList<InstagramImage> instagramImageList;
    ActionBar actionBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(Constants.TAG, "PopularActivity onCreate");
        setContentView(R.layout.popular_layout);

        Bundle extras = getIntent().getExtras();
        sourceUrl = extras.getString("endpoint");
        grid = (GridView)findViewById(R.id.gridview);
        int titleId = extras.getInt("title");
        String title = getResources().getString(titleId);

        actionBar = (ActionBar) findViewById(R.id.actionbar);
        actionBar.setTitle(title);

        Intent homeIntent = new Intent(getApplicationContext(), HomeActivity.class);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        actionBar.addAction(new RefreshAction());
        final ActionBar.Action goHomeAction = new ActionBar.IntentAction(this,
                homeIntent, R.drawable.ic_title_home);
        actionBar.addAction(goHomeAction);

        // set that list to background downloader
        instagramImageList = new ArrayList<InstagramImage>();
        adapter = new LazyGridAdapter(this, instagramImageList);
        grid.setAdapter(adapter);
        new FetchActivity().execute();
    }

    private void refresh() {
        instagramImageList.clear();
        adapter.notifyDataSetChanged();
        new FetchActivity().execute();
    }


    @Override
    public void onDestroy()
    {
        adapter.imageLoader.stopThread();
        grid.setAdapter(null);
        super.onDestroy();
    }

    public void clearCache(View view) {
        adapter.imageLoader.clearCache();
        adapter.notifyDataSetChanged();
    }

    private class FetchActivity extends AsyncTask<Void, String, Boolean> {
        protected void onPreExecute() {
            actionBar.setProgressBarVisibility(View.VISIBLE);
        }

        protected void onPostExecute(Boolean result) {
            actionBar.setProgressBarVisibility(View.GONE);

            if(result) {
                adapter.notifyDataSetChanged();
            }
        }

        protected void onProgressUpdate(String... toastText) {
            Toast.makeText(PopularActivity.this, toastText[0], Toast.LENGTH_SHORT).show();
            Log.e(Constants.TAG, toastText[0]);
        }

        protected Boolean doInBackground(Void... voids) {

            Log.i(Constants.TAG, "PopularActivity FETCH");

            HttpEntity httpEntity = null;

            if( Utils.isOnline(getApplicationContext()) == false ) {
                publishProgress("No connection to Internet.\nTry again later");
                return false;
            }

            boolean success = false;
            int fail_count = 0;
            while( success == false  ) {
                try {
                    MyHttpClient httpClient = new MyHttpClient(getApplicationContext());
                    HttpGet httpGet = new HttpGet(sourceUrl);
                    HttpResponse httpResponse = httpClient.execute(httpGet);

                    // test result code
                    if( httpResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK ) {
                        publishProgress("Login failed.");
                        return false;
                    }

                    httpEntity = httpResponse.getEntity();
                    success = true;
                } catch( SSLException sslException ) {
                    Log.e(Constants.TAG, "SSL Exception: " + fail_count);
                    success = false;
                    fail_count++;
                    if( fail_count > 10 ) {
                        publishProgress("SSL exception.\nMost times, you can simply try again.");
                        return false;
                    }
                } catch (IOException ioException) {
                    publishProgress("Authorization error");
                    return false;
                }
            }

            try {
                if( httpEntity != null ) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(httpEntity.getContent(), "UTF-8"));
                    String json = reader.readLine();
                    JSONTokener jsonTokener = new JSONTokener(json);
                    JSONObject jsonObject = new JSONObject(jsonTokener);

                    // parse the activity feed
                    JSONArray data = jsonObject.getJSONArray("data");

                    // get image URLs and commentary
                    for( int i=0; i< data.length(); i++ ) {
                        // create a new instance
                        InstagramImage instagramImage = new InstagramImage();

                        // image
                        JSONObject image = (JSONObject)data.get(i);
                        JSONObject thumbnailImage = image.getJSONObject("images").getJSONObject("thumbnail");
                        instagramImage.url = thumbnailImage.getString("url");
                        instagramImage.id = image.getString("id");

                        // user
                        JSONObject user = image.getJSONObject("user");
                        instagramImage.username = user.getString("username");
                        instagramImage.user_id = user.getString("id");
                        instagramImage.full_name = user.getString("full_name");

                        // date taken_at
                        Long dateLong = image.getLong("created_time");
                        SimpleDateFormat formatter = new SimpleDateFormat("MMMM d, yyyy HH:mm");
                        instagramImage.taken_at = formatter.format(new Date(dateLong * 1000L));

                        // comments
                        JSONArray comments = image.getJSONObject("comments").getJSONArray("data");
                        if( comments != null ) {
                            ArrayList<Comment> commentList = new ArrayList<Comment>();
                            for( int c=0; c < comments.length(); c++ ) {
                                JSONObject comment = comments.getJSONObject(c);
                                JSONObject from = comment.getJSONObject("from");
                                commentList.add(new Comment(from.getString("username"),
                                        comment.getString("text")));
                            }
                            instagramImage.comment_list = commentList;
                        }

                        // caption

                        try {
                            JSONObject caption = image.getJSONObject("caption");
                            if( caption != null ) {
                                instagramImage.caption = caption.getString("text");
                            }
                        } catch (JSONException e) {}

                        // likers
                        try {
                            JSONArray likes = image.getJSONObject("likes").getJSONArray("data");
                            if( likes != null ) {
                                ArrayList<String> likerList = new ArrayList<String>();
                                StringBuilder likerString = new StringBuilder();
                                if( likes.length() > 0 ) {
                                    likerString.append("Liked by: <b>");
                                    for( int l=0; l < likes.length(); l++ ) {
                                        JSONObject like = likes.getJSONObject(l);
                                        likerString.append(like.getString("username") + " ");
                                        likerList.add(like.getString("username"));
                                    }
                                    likerString.append("</b>");
                                    instagramImage.liker_list = likerList;
                                    instagramImage.liker_list_is_count = false;
                                }
                            }
                        } catch( JSONException j ) {}

                        instagramImageList.add(instagramImage);
                    }

                    return true;
                } else {
                    publishProgress("Improper data returned from Instagram");
                    Log.e(Constants.TAG, "instagram returned bad data");
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    private class RefreshAction implements ActionBar.Action {

        public int getDrawable() {
            return R.drawable.ic_title_refresh;
        }

        public void performAction(View view) {
            refresh();
        }
    }
}