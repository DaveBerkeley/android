package uk.co.rotwang.mqttcontrols;

import android.app.Activity;
import android.content.Context;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.content.Intent;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

    /*
     *  Handler for MQTT messages
     */

interface mqttHandler {
  public void onMessage(String topic, MqttMessage msg);
};

    /*
     *  MQTT Callback implementation
     */

class CallBackHandler implements MqttCallback
{
    MqttClient client;
    Map<String, List<mqttHandler>> map;
    Activity activity;

    public CallBackHandler(Activity ctx, MqttClient mclient)
    {
        activity = ctx;
        client = mclient;
        map = new HashMap<String, List<mqttHandler>>();
    }

    public void unsubscribe()
    {
        Iterator it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            String topic = (String) pair.getKey();
            //Log.d(getClass().getCanonicalName(), "Unsubscribe:" + topic);
            try {
                client.unsubscribe(topic);
            } catch (MqttException e) {
                Log.d(getClass().getCanonicalName(), "Unsubscribe Error:" + topic + " " + e.getCause());
            }
        }
    }

    class Runner implements Runnable {
        String topic;
        MqttMessage message;

        public Runner(String t, MqttMessage m) {
            topic = t;
            message = m;
        }
        @Override
        public void run() {
            List<mqttHandler> handlers = map.get(topic);
            if (handlers != null) {
                for (int i = 0; i < handlers.size(); ++i) {
                    mqttHandler handler = handlers.get(i);
                    handler.onMessage(topic, message);
                }
            }
        }
    };

    public void messageArrived(String topic, MqttMessage message)
    {
        Runner runner = new Runner(topic, message);
        activity.runOnUiThread(runner);
    }

    private void dispatch(String topic, MqttMessage message) {
    }

    public void connectionLost(Throwable cause)
    {
        Log.d(getClass().getCanonicalName(), "MQTT Server connection lost");
    }
    public void deliveryComplete(IMqttDeliveryToken token)
    {
        Log.d(getClass().getCanonicalName(), "Delivery complete");
    }

    public void addHandler(String topic, mqttHandler handler)
    {
        Log.d(getClass().getCanonicalName(), "Add handler " + topic + ":" + handler);
        List<mqttHandler> handlers = map.get(topic);

        if (handlers == null) {
            handlers = new ArrayList<mqttHandler>();
            map.put(topic, handlers);

            try {
                client.subscribe(topic);
                Log.d(getClass().getCanonicalName(), "Set handler:" + topic + " " + handler);
            }
            catch (MqttException e) {
                Log.d(getClass().getCanonicalName(), "Subscribe Error:" + e.getCause());
            }
        }

        handlers.add(handler);
    }

    public void sendMessage(String topic, String msg)
    {
        try
        {
            MqttMessage message = new MqttMessage();
            message.setPayload(msg.getBytes());
            client.publish(topic, message);
        }
        catch (MqttException e)
        {
            Log.d(getClass().getCanonicalName(), "Publish failed with reason code = " + e.getReasonCode());
        }
    }
};

    /*
     *  Parser : optionally extracts fields from JSON string
     */

class Picker {

    String field;

    public Picker(String pfield) {
        field = pfield;
    }

    public String pick(String text) {
        if (field == null) {
            return text;
        }

        try {
            JSONObject reader = new JSONObject(text);
            return reader.getString(field);
        }
        catch (JSONException ex) {
            Log.d(getClass().getCanonicalName(), ex.toString() + ":" + text);
        }
        return text;
    }
};

    /*
     *  Button wrapper
     */

class MqttButton extends Button implements View.OnClickListener {
    CallBackHandler mqttHandler;
    String topic;
    String data;

    MqttButton(Context ctx, CallBackHandler handler, String label, String wr_topic, String send) {
        super(ctx);
        setText(label);
        mqttHandler = handler;
        topic = wr_topic;
        setOnClickListener(this);
        data = send;
    }

    @Override
    public void onClick(View view) {
        //Log.d(getClass().getCanonicalName(), "on click");
        mqttHandler.sendMessage(topic, data);
    }

    static public View create(Activity ctx, CallBackHandler handler, JSONObject obj) throws JSONException
    {
        String text = obj.getString("text");
        String topic = obj.getString("topic");
        String send = obj.getString("send");
        return new MqttButton(ctx, handler, text, topic, send);
    }
};

    /*
     *  Progress Bar
     */

class MqttProgressBar extends ProgressBar implements mqttHandler {

    double min, max;
    Picker picker;

    MqttProgressBar(Context ctx, CallBackHandler handler, double fmin, double fmax, String topic, String field) {
        super(ctx, null, android.R.attr.progressBarStyleHorizontal);
        handler.addHandler(topic, this);
        min = fmin;
        max = fmax;
        picker = new Picker(field);
    }

    private int translate(double n)
    {
        return (int) (100 * ((n - min) / (max - min)));
    }

    @Override
    public void onMessage(String topic, MqttMessage msg)
    {
        //Log.d(getClass().getCanonicalName(), topic + ":" + msg.toString());

        try {
            final String s = picker.pick(msg.toString());
            final double f = Double.parseDouble(s);
            setProgress(translate(f));
        }
        catch (NumberFormatException ex) {
            Log.d(getClass().getCanonicalName(), "Bad number:" + msg.toString() + " " + ex.getCause());
        }
    }

    static public View create(Activity ctx, CallBackHandler handler, JSONObject obj) throws JSONException
    {
        double min = obj.getDouble("min");
        double max = obj.getDouble("max");
        String topic = obj.getString("topic");
        String field = obj.getString("field");
        return new MqttProgressBar(ctx, handler, min, max, topic, field);
    }
};

    /*
     *  CheckBox
     */

class MqttCheckBox extends CheckBox implements mqttHandler {

    Picker picker;

    public MqttCheckBox(Context ctx, CallBackHandler handler, String topic, String field) {

        super(ctx);
        setEnabled(false);
        handler.addHandler(topic, this);
        picker = new Picker(field);
    }

    @Override
    public void onMessage(String topic, MqttMessage msg)
    {
        //Log.d(getClass().getCanonicalName(), topic + ":" + msg.toString());

        try {
            final String s = picker.pick(msg.toString());
            final int i = (int) Float.parseFloat(s);
            //Log.d(getClass().getCanonicalName(), "Got number:" + i);
            setChecked(i != 0);
        }
        catch (NumberFormatException ex) {
            Log.d(getClass().getCanonicalName(), "Bad number:" + msg.toString() + " " + ex.getCause());
        }
    }

    static public View create(Activity ctx, CallBackHandler handler, JSONObject obj) throws JSONException
    {
        String topic = obj.getString("topic");
        String field = obj.getString("field");
        return new MqttCheckBox(ctx, handler, topic, field);
    }
};

    /*
     *  TextView
     */

class MqttTextView extends TextView implements mqttHandler {

    Picker picker;
    String pre_text;
    String post_text;

    public MqttTextView(Activity ctx, CallBackHandler handler, String topic, String field, String pre, String post) {
        super(ctx);
        pre_text = (pre == null) ? "" : pre;
        post_text = (post == null) ? "" : post;
        handler.addHandler(topic, this);
        picker = new Picker(field);
    }

    @Override
    public void onMessage(String topic, MqttMessage msg)
    {
        //Log.d(getClass().getCanonicalName(), topic + ":" + msg.toString());
        final String s = picker.pick(msg.toString());
        setText(pre_text + s + post_text);
    }

    static String getString(JSONObject obj, String key) throws JSONException
    {
        if (obj.has(key)) {
            return obj.getString(key);
        }
        return null;
    }

    static public View create(Activity ctx, CallBackHandler handler, JSONObject obj) throws JSONException
    {
        String topic = obj.getString("topic");
        String field = obj.getString("field");
        String pre = getString(obj, "pre");
        String post = getString(obj, "post");
        return new MqttTextView(ctx, handler, topic, field, pre, post);
    }
};

    /*
     *  TextView
     */

class MqttLabel extends TextView {

    MqttLabel(Context ctx) {
        super(ctx);
    }

    public MqttLabel(Activity ctx, String text) {
        super(ctx);
        setText(text);
    }

    static public View create(Activity ctx, CallBackHandler handler, JSONObject obj) throws JSONException
    {
        String text = obj.getString("text");
        return new MqttLabel(ctx, text);
    }
};

    /*
     *  Factory method to construct View classes
     */

class MqttFactory {
    static public View create(Activity ctx, CallBackHandler handler, String type, JSONObject obj) throws JSONException
    {
        if (type.equals("TextLabel")) {
            return MqttLabel.create(ctx, handler, obj);
        }
        if (type.equals("TextView")) {
            return MqttTextView.create(ctx, handler, obj);
        }
        if (type.equals("CheckBox")) {
            return MqttCheckBox.create(ctx, handler, obj);
        }
        if (type.equals("ProgressBar")) {
            return MqttProgressBar.create(ctx, handler, obj);
        }
        if (type.equals("Button")) {
            return MqttButton.create(ctx, handler, obj);
        }
        return null;
    }
};

    /*
     *  Fetch data from URL in a thread
     */

interface OnUrl
{
    public void onUrl(String data);
};

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
};

    /*
     *  Activity
     */

public class MainActivity extends ActionBarActivity implements OnUrl {

    MqttClient client;
    CallBackHandler handler = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MqttSettings conf = new MqttSettings();
        conf.read(this);

        try {
            client = new MqttClient(conf.getUrl(), MqttClient.generateClientId(), null);
            handler = new CallBackHandler(this, client);
            client.setCallback(handler);
        } catch (MqttException ex) {
            ex.printStackTrace();
        }

        MqttConnectOptions options = new MqttConnectOptions();
        try {
            client.connect(options);
        } catch (MqttException e) {
            Log.d(getClass().getCanonicalName(), "Connection attempt failed with reason code = " + e.getReasonCode() + ":" + e.getCause());
        }

        reload();
    }

    private boolean loadControls(String conf)
    {
        LinearLayout layout = (LinearLayout) findViewById(R.id.main_layout);

        // Remove existing views
        if (layout.getChildCount() > 0) {
            layout.removeAllViews();
        }

        if (conf == null)
            return false;

        try {
            JSONArray reader = new JSONArray(conf);
            // iterate through controls
            for (int i = 0; i < reader.length(); ++i) {
                JSONArray item = reader.getJSONArray(i);
                assert(item.length() == 2);
                String type = item.getString(0);
                JSONObject dict = item.getJSONObject(1);
                Log.d(getClass().getCanonicalName(), "Create " + type + " : " + dict);

                View view = MqttFactory.create(this, handler, type, dict);
                if (view != null) {
                    layout.addView(view);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            // TODO : toast
            return false;
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            actionSettings(null);
            return true;
        }

        if (id == R.id.reload) {
            reload();
            return true;
        }

        if (id == R.id.exit) {
            toast("Disconnecting from MQTT feed");
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /** Called when the user clicks the Settings button */
    public void actionSettings(View view) {
        Intent intent = new Intent(this, MqttSettingsActivity.class);
        startActivity(intent);
    }

    private void toast(CharSequence text) {
        // Show Toast
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(this, text, duration);
        toast.show();
    }

        /*
         *  Save / Load config data to cache
         *  so we have a default when the source URI is not readable.
         */

    final private String cachePath = "datacahe.txt";
    final private String encoding = "UTF-8";

    private boolean saveConfigToCache(String data) {
        try {
            FileOutputStream file = openFileOutput(cachePath, MODE_PRIVATE);
            file.write(data.getBytes(Charset.forName(encoding)));
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private String loadConfigFromCache() {
        try {
            FileInputStream file = openFileInput(cachePath);
            Reader r = new InputStreamReader(file, encoding);
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[1024];
            int amt = r.read(buf);
            while(amt > 0) {
                sb.append(buf, 0, amt);
                amt = r.read(buf);
            }
            return sb.toString();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Callback when data is fetched from URL
    @Override
    public void onUrl(String data) {
        //Log.d(getClass().getCanonicalName(), "Got data:" + data);

        if (data == null) {
            // try to read from cache
            toast("Read data from cache");
            data = loadConfigFromCache();
        }

        if (data == null) {
            toast("Unable to read config");
            return;
        }

        if (loadControls(data)) {
            saveConfigToCache(data);
        }
    }

    private void reload() {
        MqttSettings conf = new MqttSettings();
        conf.read(this);
        //  Fetch and load the controls config
        UrlFetcher fetcher = new UrlFetcher(this, conf.url, this);
        fetcher.start();
    }

    @Override
    protected void onDestroy() {
        handler.unsubscribe();
        super.onDestroy();
    }
}

// FIN