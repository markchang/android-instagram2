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
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import com.markupartist.android.widget.ActionBar;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.message.BasicNameValuePair;
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
import java.util.List;

public class FeedActivity extends Activity {
    private static final boolean debug = true;
    private String sourceUrl;

    ListView feedList;
    LazyListAdapter adapter;
    ArrayList<InstagramImage> instagramImageList;
    ActionBar actionBar;
    MyHttpClient httpClient;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(debug) Log.i(Constants.TAG, "FeedActivity onCreate");
        setContentView(R.layout.feed_layout);

        Bundle extras = getIntent().getExtras();
        sourceUrl = extras.getString("endpoint");
        int titleId = extras.getInt("title");
        String title = getResources().getString(titleId);

        actionBar = (ActionBar) findViewById(R.id.feedActionbar);
        actionBar.setTitle(title);

        Intent homeIntent = new Intent(getApplicationContext(), HomeActivity.class);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        actionBar.addAction(new RefreshAction());
        final ActionBar.Action goHomeAction = new ActionBar.IntentAction(this,
                homeIntent, R.drawable.ic_title_home);
        actionBar.addAction(goHomeAction);

        feedList = (ListView)findViewById(R.id.feedList);
        feedList.setOnItemClickListener(itemClickListener);

        // set that list to background downloader
        instagramImageList = new ArrayList<InstagramImage>();
        adapter = new LazyListAdapter(this, instagramImageList);
        feedList.setAdapter(adapter);
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
        feedList.setAdapter(null);
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
            Toast.makeText(FeedActivity.this, toastText[0], Toast.LENGTH_SHORT).show();
            if(debug) Log.e(Constants.TAG, toastText[0]);
        }

        protected Boolean doInBackground(Void... voids) {

            if(debug) Log.i(Constants.TAG, "PopularActivity FETCH");

            HttpEntity httpEntity = null;

            if( Utils.isOnline(getApplicationContext()) == false ) {
                publishProgress("No connection to Internet.\nTry again later");
                return false;
            }

            boolean success = false;
            int fail_count = 0;
            while( success == false  ) {
                try {
                    httpClient = new MyHttpClient(getApplicationContext());
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
                    if(debug) Log.e(Constants.TAG, "SSL Exception: " + fail_count);
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
                        JSONObject images = image.getJSONObject("images");
                        JSONObject thumbnailImage = images.getJSONObject("thumbnail");
                        JSONObject lowResolutionImage = images.getJSONObject("low_resolution");
                        JSONObject standardResolutionImage = images.getJSONObject("standard_resolution");
                        instagramImage.id = image.getString("id");
                        instagramImage.permalink = image.getString("link");

                        instagramImage.user_has_liked = image.getBoolean("user_has_liked");

                        // permalinks
                        instagramImage.thumbnail = thumbnailImage.getString("url");
                        instagramImage.low_resolution = lowResolutionImage.getString("url");
                        instagramImage.standard_resolution = standardResolutionImage.getString("url");

                        // user
                        JSONObject user = image.getJSONObject("user");
                        instagramImage.username = user.getString("username");
                        instagramImage.user_id = user.getString("id");
                        instagramImage.full_name = user.getString("full_name");

                        // date taken_at
                        Long dateLong = image.getLong("created_time");
                        SimpleDateFormat formatter = new SimpleDateFormat("MMMM d, yyyy HH:mm");
                        instagramImage.taken_at = formatter.format(new Date(dateLong * 1000L));
                        instagramImage.taken_time = dateLong * 1000L;

                        // comments
                        instagramImage.comment_count = image.getJSONObject("comments").getInt("count");
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
                            instagramImage.liker_count = image.getJSONObject("likes").getInt("count");
                            JSONArray likes = image.getJSONObject("likes").getJSONArray("data");
                            if( likes != null ) {
                                ArrayList<String> likerList = new ArrayList<String>();
                                if( likes.length() > 0 ) {
                                    for( int l=0; l < likes.length(); l++ ) {
                                        JSONObject like = likes.getJSONObject(l);
                                        likerList.add(like.getString("username"));
                                    }
                                    instagramImage.liker_list = likerList;
                                }
                            }
                        } catch( JSONException j ) {}

                        instagramImageList.add(instagramImage);
                    }

                    return true;
                } else {
                    publishProgress("Improper data returned from Instagram");
                    if(debug) Log.e(Constants.TAG, "instagram returned bad data");
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    public AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            final InstagramImage instagramImage = (InstagramImage)adapter.getItem(i);

            // build dialog
            List<String> dialogItems = new ArrayList<String>();

            // 0: like/unlike
            if( instagramImage.user_has_liked == true ) {
                dialogItems.add("Unlike");
            } else {
                dialogItems.add("Like");
            }

            // 1: comment
            dialogItems.add("Comment");

            // 2: share
            dialogItems.add("Share");

            final CharSequence[] items = dialogItems.toArray(new String[dialogItems.size()]);

            AlertDialog.Builder builder = new AlertDialog.Builder(FeedActivity.this);
            builder.setTitle("Choose your action");
            builder.setItems(items, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    switch(item) {
                        case 0:
                            if (instagramImage.user_has_liked == true) {
                                unlike(instagramImage);
                            } else {
                                like(instagramImage);
                            }
                            break;
                        case 1:
                            showCommentDialog(instagramImage);
                            break;
                        case 2:
                            showShareDialog(instagramImage);
                            break;
                        default:
                            break;
                    }
                }
            });
            AlertDialog alert = builder.create();
            alert.show();

        }
    };

    public void showShareDialog(InstagramImage image) {
        // shoot the intent
        // will default to "messaging / sms" if nothing else is installed
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        //Text seems to be necessary for Facebook and Twitter
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(Intent.EXTRA_TEXT, image.caption + " " + image.permalink);
        startActivity(Intent.createChooser(sharingIntent,"Share using"));
    }

    public void showCommentDialog(InstagramImage image) {
        final InstagramImage finalImage = image;

        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Comment");

        // Set an EditText view to get user input
        final EditText input = new EditText(this);
        alert.setView(input);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String comment = input.getText().toString();
                postComment(comment, finalImage);
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });

        alert.show();
    }

    public void postComment(String comment, InstagramImage image) {
        List<NameValuePair> postParams = new ArrayList<NameValuePair>();
        postParams.add(new BasicNameValuePair("text", comment));
        postParams.add(new BasicNameValuePair("access_token", Utils.getAccessToken(getApplicationContext())));
        String url = Constants.MEDIA_ENDPOINT + image.id + Constants.COMMENT_MEDIA_ENDPOINT;


        JSONObject jsonResponse = Utils.doRestfulPut(httpClient,
                url,
                postParams,
                this);
        if( jsonResponse != null ) {
            image.comment_list.add(new Comment(Utils.getUsername(getApplicationContext()),comment));
            adapter.notifyDataSetChanged();
        } else {
            Toast.makeText(this,
                    "Comment failed", Toast.LENGTH_SHORT).show();
        }
    }


    public void like(InstagramImage image) {
        List<NameValuePair> postParams = new ArrayList<NameValuePair>();
        postParams.add(new BasicNameValuePair("access_token", Utils.getAccessToken(getApplicationContext())));

        String url = Constants.MEDIA_ENDPOINT + image.id + Constants.LIKE_MEDIA_ENDPOINT;

        JSONObject jsonResponse = Utils.doRestfulPut(httpClient,
                url,
                postParams,
                this);

        if( jsonResponse != null ) {
            if( image.liker_list == null ) image.liker_list = new ArrayList<String>();
            image.liker_list.add(Utils.getUsername(getApplicationContext()));
            image.liker_count++;
            image.user_has_liked = true;
            adapter.notifyDataSetChanged();
        }
    }

    public void unlike(InstagramImage image) {
        List<NameValuePair> postParams = new ArrayList<NameValuePair>();
        postParams.add(new BasicNameValuePair("access_token", Utils.getAccessToken(getApplicationContext())));

        String url = Constants.MEDIA_ENDPOINT + image.id + Constants.LIKE_MEDIA_ENDPOINT;
        String access_url = Utils.decorateEndpoint(url, Utils.getAccessToken(getApplicationContext()));

        JSONObject jsonResponse = Utils.doRestfulDelete(httpClient, access_url, this);

        if( jsonResponse != null ) {
            image.liker_list.remove(Utils.getUsername(getApplicationContext()));
            image.liker_count--;
            image.user_has_liked = false;
            adapter.notifyDataSetChanged();
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