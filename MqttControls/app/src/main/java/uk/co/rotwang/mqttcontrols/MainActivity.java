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

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
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
     *  Parser : optionaly extracts fields from JSON string
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
            Log.d(getClass().getCanonicalName(), "JSON Error:" + ex.getCause());
        }
        return "";
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
            Log.d(getClass().getCanonicalName(), "Bad number:" + msg.toString());
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
            Log.d(getClass().getCanonicalName(), "Bad number:" + msg.toString());
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

    public MqttTextView(Activity ctx, CallBackHandler handler, String topic, String field) {
        super(ctx);
        handler.addHandler(topic, this);
        picker = new Picker(field);
    }

    @Override
    public void onMessage(String topic, MqttMessage msg)
    {
        //Log.d(getClass().getCanonicalName(), topic + ":" + msg.toString());
        final String s = picker.pick(msg.toString());
        setText(s);
    }

    static public View create(Activity ctx, CallBackHandler handler, JSONObject obj) throws JSONException
    {
        String topic = obj.getString("topic");
        String field = obj.getString("field");
        return new MqttTextView(ctx, handler, topic, field);
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
     *  Activity
     */

public class MainActivity extends ActionBarActivity {

    MqttClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MqttSettings conf = new MqttSettings();
        conf.read(this);

        CallBackHandler handler = null;
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

        loadControls(handler, config);
    }

    private String config = "[[\"TextLabel\", {\"text\": \"Charlotte Battery Voltage\"}], [\"ProgressBar\", {\"topic\": \"home/jeenet/voltagedev_11\", \"field\": \"voltage\", \"max\": 13.0, \"min\": 11.0}], [\"TextView\", {\"topic\": \"home/jeenet/voltagedev_11\", \"field\": \"voltage\"}], [\"Button\", {\"topic\": \"uif/button/1\", \"text\": \"Radio Relay\", \"send\": \"1\"}], [\"CheckBox\", {\"topic\": \"home/jeenet/relaydev_7\", \"field\": \"state\"}], [\"TextLabel\", {\"text\": \"Street Signal (random)\"}], [\"ProgressBar\", {\"topic\": \"node/jeenet/8/voltage\", \"field\": null, \"max\": 50.0, \"min\": 0.0}], [\"Button\", {\"topic\": \"uif/button/2\", \"text\": \"Relay\", \"send\": \"1\"}], [\"TextLabel\", {\"text\": \"Gas Meter (sector)\"}], [\"ProgressBar\", {\"topic\": \"node/gas/sector\", \"field\": null, \"max\": 0.0, \"min\": 63.0}], [\"TextLabel\", {\"text\": \"Export\"}], [\"ProgressBar\", {\"topic\": \"home/power\", \"field\": \"power\", \"max\": -3000.0, \"min\": 0.0}], [\"TextLabel\", {\"text\": \"Import\"}], [\"ProgressBar\", {\"topic\": \"home/power\", \"field\": \"power\", \"max\": 3000.0, \"min\": 0.0}], [\"TextView\", {\"topic\": \"home/power\", \"field\": \"power\"}]]";

    private void loadControls(CallBackHandler handler, String conf)
    {
        LinearLayout layout = (LinearLayout) findViewById(R.id.main_layout);

        // Remove existing views
        if (layout.getChildCount() > 0) {
            layout.removeAllViews();
        }

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
            return;
        }
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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            actionSettings(null);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /** Called when the user clicks the Settings button */
    public void actionSettings(View view) {
        Intent intent = new Intent(this, MqttSettingsActivity.class);
        startActivity(intent);
    }
}

// FIN