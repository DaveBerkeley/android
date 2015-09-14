package uk.co.rotwang.mqttcontrols;

import android.app.Activity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

    /*
     *  Fetch data from URL in a thread
     */

interface OnUrl
{
    public void onUrl(String data);
}

class UrlFetcher implements Runnable {

    private String url;
    private OnUrl handler;
    private Activity activity;

    public UrlFetcher(Activity ctx, String u, OnUrl callback)
    {
        activity = ctx;
        url = u;
        handler = callback;
    }

    public void start()
    {
        Thread thread = new Thread(this);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    private String data = null;

    @Override
    public void run() {
        try {
            data = openHttpConnection(url);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Make sure the callback is run on the UI thread
        Runnable runner = new Runnable() {
            @Override
            public void run() {
                handler.onUrl(data);
            }
        };
        activity.runOnUiThread(runner);
    }

    private String openHttpConnection(String urlString) throws IOException
    {
        InputStream in = null;

        URL url = new URL(urlString);
        URLConnection conn = url.openConnection();

        if (!(conn instanceof HttpURLConnection))
            throw new IOException("Not an HTTP connection");

        try{
            HttpURLConnection httpConn = (HttpURLConnection) conn;
            httpConn.setAllowUserInteraction(false);
            httpConn.setInstanceFollowRedirects(true);
            httpConn.setRequestMethod("GET");
            httpConn.connect();

            int response = httpConn.getResponseCode();
            if (response == HttpURLConnection.HTTP_OK) {
                in = httpConn.getInputStream();
            }
        }
        catch (Exception ex)
        {
            throw new IOException("Error connecting:" + ex.toString());
        }

        if (in == null) {
            throw new IOException("Error opening url");
        }

        //  Read from stream, convert to string
        ByteArrayOutputStream writer = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length = 0;
        while ((length = in.read(buffer)) != -1) {
            writer.write(buffer, 0, length);
        }
        return new String(writer.toByteArray());
    }
}

// FIN