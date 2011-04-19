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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.ImageView;

import java.io.*;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.*;

public class ImageLoader {
    private static final boolean debug = false;

    // the simplest in-memory cache implementation.
    // This should be replaced with something like SoftReference or
    // BitmapOptions.inPurgeable(since 1.6)
    private HashMap<String, SoftReference<Bitmap>> cache=new HashMap<String, SoftReference<Bitmap>>();

    final int stub_id = R.drawable.stub;
    private File cacheDir;

    public ImageLoader(Context context){
        // Make the background thread low priority. This way it will not affect the UI performance
        photoLoaderThread.setPriority(Thread.NORM_PRIORITY-1);

        //Find the dir to save cached images
        if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
            cacheDir=new File(android.os.Environment.getExternalStorageDirectory(),Constants.OUTPUT_DIR);
        else
            cacheDir=context.getCacheDir();
        if(!cacheDir.exists())
            cacheDir.mkdirs();
    }

    public void DisplayImage(String url, Activity activity, ImageView imageView)
    {
        if(debug) Log.i(Constants.TAG, "Displaying image: " + url);
        if(cache.containsKey(url)) {
            if(debug) Log.i(Constants.TAG, "Found cached image " + url);
            SoftReference<Bitmap> softRef = cache.get(url);
            Bitmap bitmap = softRef.get();
            if( bitmap == null ) {
                if(debug) Log.i(Constants.TAG, "But re-queuing GC'ed image: " + url);
                // maybe? : cache.remove(softRef);
                queuePhoto(url, activity, imageView);
                imageView.setImageResource(stub_id);
            } else {
                if(debug) Log.i(Constants.TAG, "Setting image bitmap");
                imageView.setImageBitmap(softRef.get());
                if( softRef.get() == null ) {
                    if(debug) Log.e(Constants.TAG, "Null bitmap: " + url);
                }
            }
        }
        else
        {
            if(debug) Log.i(Constants.TAG, "Not in cache, queueing "  + url);
            queuePhoto(url, activity, imageView);
            imageView.setImageResource(stub_id);
        }
    }

    private void queuePhoto(String url, Activity activity, ImageView imageView)
    {
        if(debug) Log.i(Constants.TAG, "Queueing: " + url);

        // This ImageView may be used for other images before.
        // So there may be some old tasks in the queue. We need to discard them.
        photosQueue.Clean(imageView);
        PhotoToLoad p=new PhotoToLoad(url, imageView);
        synchronized( photosQueue.photosToLoad ){
            photosQueue.photosToLoad.add(p);
            photosQueue.photosToLoad.notifyAll();
        }

        //start thread if it's not started yet
        if(photoLoaderThread.getState()==Thread.State.NEW)
            photoLoaderThread.start();
    }

    private Bitmap getBitmap(String url)
    {
        // I identify images by hashcode. Not a perfect solution, good for the demo.
        if(debug) Log.i(Constants.TAG, "Getting image in thread: " + url);
        String filename=String.valueOf(url.hashCode());
        File f=new File(cacheDir, filename);

        //from SD cache
        Bitmap b = decodeFile(f);
        if(b != null) {
            if(debug) Log.i(Constants.TAG, "Found in cache." );
            return b;
        }


        //from web
        try {
            if(debug) Log.i(Constants.TAG, "Downloading from web");
            Bitmap bitmap = null;
            InputStream is = new URL(url).openStream();
            OutputStream os = new FileOutputStream(f);
            Utils.CopyStream(is, os);
            os.close();
            is.close();
            bitmap = decodeFile(f);
            return bitmap;
        } catch (Exception ex){
           ex.printStackTrace();
           return null;
        }
    }

    //decodes image and scales it to reduce memory consumption
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

    // Task for the queue
    private class PhotoToLoad
    {
        public String url;
        public ImageView imageView;
        public PhotoToLoad(String u, ImageView i){
            url = u;
            imageView = i;
        }
    }

    PhotosQueue photosQueue = new PhotosQueue();

    public void stopThread()
    {
        photoLoaderThread.interrupt();
    }

    // stores feedList of photos to download
    class PhotosQueue
    {
        // private Stack<PhotoToLoad> photosToLoad = new Stack<PhotoToLoad>();
        private LinkedList<PhotoToLoad> photosToLoad = new LinkedList<PhotoToLoad>();

        //removes all instances of this ImageView
        public void Clean(ImageView image)
        {
            for(int j=0 ;j<photosToLoad.size();){
                if(photosToLoad.get(j).imageView == image)
                    photosToLoad.remove(j);
                else
                    ++j;
            }
        }
    }

    class PhotosLoader extends Thread {
        public void run() {
            try {
                while(true)
                {
                    // thread waits until there are any images to load in the queue
                    if(photosQueue.photosToLoad.size() == 0) {
                        synchronized(photosQueue.photosToLoad){
                            photosQueue.photosToLoad.wait();
                        }
                    }
                    if(photosQueue.photosToLoad.size() != 0)
                    {
                        PhotoToLoad photoToLoad;
                        synchronized(photosQueue.photosToLoad){
                            photoToLoad=photosQueue.photosToLoad.remove();
                        }
                        Bitmap bmp = getBitmap(photoToLoad.url);
                        if(bmp == null) {
                            if(debug) Log.e(Constants.TAG, "Bitmap loaded as null!");
                        } else {
                            cache.put(photoToLoad.url, new SoftReference<Bitmap>(bmp));
                            Object tag = photoToLoad.imageView.getTag();
                            if(tag != null && ((String)tag).equals(photoToLoad.url)){
                                BitmapDisplayer bd = new BitmapDisplayer(bmp, photoToLoad.imageView);
                                Activity a = (Activity)photoToLoad.imageView.getContext();
                                a.runOnUiThread(bd);
                            } else {
                                if(debug) Log.i(Constants.TAG, "Got image w/o tag!");
                            }
                        }
                    }
                    if(Thread.interrupted())
                        break;
                }
            } catch (InterruptedException e) {
                //allow thread to exit
            }
        }
    }

    PhotosLoader photoLoaderThread = new PhotosLoader();

    // Used to display bitmap in the UI thread
    class BitmapDisplayer implements Runnable
    {
        Bitmap bitmap;
        ImageView imageView;

        public BitmapDisplayer(Bitmap b, ImageView i) {
            bitmap = b;
            imageView = i;
        }

        public void run()
        {
            if(bitmap != null)
                imageView.setImageBitmap(bitmap);
            else
                imageView.setImageResource(stub_id);
        }
    }

    public void clearCache() {
        // clear memory cache
        cache.clear();

        //clear SD cache
        File[] files=cacheDir.listFiles();
        for(File f:files)
            f.delete();
    }

}
