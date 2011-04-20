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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
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
import java.io.*;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: mchang
 * Date: 4/19/11
 * Time: 1:06 AM
 * Shows a single image
 */

// TODO: make image clickable for comment/like/unlike/share
public class ImageDetailActivity extends Activity {

    private static final boolean debug = true;
    ActionBar actionBar;
    String id;
    MyHttpClient httpClient;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detail_layout);

        Bundle extras = getIntent().getExtras();
        id = extras.getString("id");

        actionBar = (ActionBar) findViewById(R.id.detail_actionbar);
        actionBar.setTitle(R.string.image_detail);

        Intent homeIntent = new Intent(getApplicationContext(), HomeActivity.class);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        final ActionBar.Action goHomeAction = new ActionBar.IntentAction(this,
                homeIntent, R.drawable.ic_title_home);
        actionBar.addAction(goHomeAction);

        httpClient = new MyHttpClient(this);

        new FetchImage().execute(id);
    }

    private void drawImageDetails(InstagramImage image) {
        // get handle to UI elements
        TextView username = (TextView) findViewById(R.id.detail_username);
        ImageView imageView = (ImageView) findViewById(R.id.detail_image);
        TextView caption = (TextView) findViewById(R.id.detail_caption);
        TextView comments = (TextView) findViewById(R.id.detail_comments);

        // render it
        imageView.setTag(image.standard_resolution);
        username.setText(Html.fromHtml("<b>" + image.username + "</b> ") +
                image.taken_at);
        caption.setText(Html.fromHtml("<b>" + image.username + "</b> " + image.caption));

        // comments hold likes and comments
        StringBuilder likerString = new StringBuilder();

        if (image.liker_list != null) {
            if (image.liker_list.size() > 0) {
                likerString.append("Liked by <b>");
                for (String liker : image.liker_list) {
                    likerString.append(" " + liker);
                }
                likerString.append("</b>");
                if (image.liker_list.size() < image.liker_count) {
                    int others_count = image.liker_count - image.liker_list.size();
                    likerString.append(" and " + Integer.toString(others_count) + " others");
                }
                likerString.append("</b><br />");
            }
        }

        // iterate over comments
        if (image.comment_list != null) {
            if (image.comment_list.size() > 0) {
                for (Comment comment : image.comment_list) {
                    likerString.append("<b>" + comment.username + "</b> ");
                    likerString.append(comment.comment + "<br />");
                }
            }
        }

        comments.setText(Html.fromHtml(likerString.toString()));
    }

    private void drawImageBitmap(Bitmap bitmap) {
        ImageView imageView = (ImageView) findViewById(R.id.detail_image);
        if( bitmap != null ) {
            imageView.setImageBitmap(bitmap);
        } else {
            Toast.makeText(this,"Error fetching photo!", Toast.LENGTH_SHORT).show();
            return;
        }
    }


    private Bitmap decodeFile(File f){
        try {
            if(debug) Log.i(Constants.TAG, "Decoding image: " + f.toString());
            Bitmap b = BitmapFactory.decodeStream(new FileInputStream(f));
            if(debug) Log.i(Constants.TAG, "decoded a real bitmap!");
            return(b);
        } catch (FileNotFoundException e) {
            if(debug) Log.i(Constants.TAG, "File not found");
            return null;
        } catch (Exception ex) {
            if(debug) Log.e(Constants.TAG, "Some other shit failed in decodeFile");
            ex.printStackTrace();
            return null;
        }
    }



    private class FetchImage extends AsyncTask<String, String, InstagramImage> {
        protected void onPreExecute() {
            actionBar.setProgressBarVisibility(View.VISIBLE);
            TextView caption = (TextView) findViewById(R.id.detail_username);
            caption.setText("Loading...");
        }

        protected void onPostExecute(InstagramImage image) {
            actionBar.setProgressBarVisibility(View.GONE);
            drawImageDetails(image);
            new FetchBitmap().execute(image.standard_resolution);
        }

        protected void onProgressUpdate(String... toastText) {
            Toast.makeText(ImageDetailActivity.this, toastText[0], Toast.LENGTH_SHORT).show();
            if(debug) Log.e(Constants.TAG, toastText[0]);
        }

        protected InstagramImage doInBackground(String... id) {
            if (debug) Log.i(Constants.TAG, "Fetching single image");

            HttpEntity httpEntity = null;

            // construct url
            String url = Utils.decorateEndpoint(Constants.MEDIA_ENDPOINT + id[0], Utils.getAccessToken(getApplicationContext()));


            if (Utils.isOnline(getApplicationContext()) == false) {
                publishProgress("No connection to Internet.\nTry again later");
                return null;
            }

            boolean success = false;
            int fail_count = 0;
            while (success == false) {
                try {
                    httpClient = new MyHttpClient(getApplicationContext());
                    HttpGet httpGet = new HttpGet(url);
                    HttpResponse httpResponse = httpClient.execute(httpGet);

                    // test result code
                    if (httpResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                        publishProgress("Login failed.");
                        return null;
                    }

                    httpEntity = httpResponse.getEntity();
                    success = true;
                } catch (SSLException sslException) {
                    if (debug) Log.e(Constants.TAG, "SSL Exception: " + fail_count);
                    success = false;
                    fail_count++;
                    if (fail_count > 10) {
                        publishProgress("SSL exception.\nMost times, you can simply try again.");
                        return null;
                    }
                } catch (IOException ioException) {
                    publishProgress("Authorization error");
                    return null;
                }
            }

            try {
                if (httpEntity != null) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(httpEntity.getContent(), "UTF-8"));
                    String json = reader.readLine();
                    JSONTokener jsonTokener = new JSONTokener(json);
                    JSONObject jsonObject = new JSONObject(jsonTokener);

                    // create a new instance
                    InstagramImage instagramImage = new InstagramImage();

                    // parse the activity feed
                    JSONObject image = jsonObject.getJSONObject("data");

                    // image
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

                    // comments
                    instagramImage.comment_count = image.getJSONObject("comments").getInt("count");
                    JSONArray comments = image.getJSONObject("comments").getJSONArray("data");
                    if (comments != null) {
                        ArrayList<Comment> commentList = new ArrayList<Comment>();
                        for (int c = 0; c < comments.length(); c++) {
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
                        if (caption != null) {
                            instagramImage.caption = caption.getString("text");
                        }
                    } catch (JSONException e) {
                    }

                    // likers
                    try {
                        instagramImage.liker_count = image.getJSONObject("likes").getInt("count");
                        JSONArray likes = image.getJSONObject("likes").getJSONArray("data");
                        if (likes != null) {
                            ArrayList<String> likerList = new ArrayList<String>();
                            if (likes.length() > 0) {
                                for (int l = 0; l < likes.length(); l++) {
                                    JSONObject like = likes.getJSONObject(l);
                                    likerList.add(like.getString("username"));
                                }
                                instagramImage.liker_list = likerList;
                            }
                        }
                    } catch (JSONException j) {
                    }

                    // got it!
                    return instagramImage;

                } else {
                    publishProgress("Improper data returned from Instagram");
                    if (debug) Log.e(Constants.TAG, "instagram returned bad data");
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    private class FetchBitmap extends AsyncTask<String, String, Bitmap> {
        protected void onPreExecute() {
            actionBar.setProgressBarVisibility(View.VISIBLE);
        }

        protected void onPostExecute(Bitmap bitmap) {
            actionBar.setProgressBarVisibility(View.GONE);
            drawImageBitmap(bitmap);
        }

        protected void onProgressUpdate(String... toastText) {
            Toast.makeText(ImageDetailActivity.this, toastText[0], Toast.LENGTH_SHORT).show();
            if(debug) Log.e(Constants.TAG, toastText[0]);
        }

        protected Bitmap doInBackground(String... url) {

            if(debug) Log.i(Constants.TAG, "Fetching bitmap");

            File downloadDir;

            //Find the dir to save cached images
            if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
                downloadDir = new File(android.os.Environment.getExternalStorageDirectory(), Constants.OUTPUT_DIR);
            else {
                publishProgress("You need a SD card!");
                return null;
            }

            if (!downloadDir.exists())
                downloadDir.mkdirs();

            String filename = String.valueOf(url.hashCode());

            try {
                File f = new File(downloadDir, filename);
                if (debug) Log.i(Constants.TAG, "Downloading from web");
                Bitmap bitmap = null;
                InputStream is = new URL(url[0]).openStream();
                OutputStream os = new FileOutputStream(f);
                Utils.CopyStream(is, os);
                os.close();
                is.close();
                bitmap = decodeFile(f);
                return bitmap;
            } catch (Exception ex) {
                ex.printStackTrace();
                return null;
            }
        }
    }
}

