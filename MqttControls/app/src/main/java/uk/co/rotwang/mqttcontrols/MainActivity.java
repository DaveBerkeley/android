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

class callBackHandler implements MqttCallback
{
    MqttClient client;
    Map<String, List<mqttHandler>> map;
    Activity activity;

    public callBackHandler(Activity ctx, MqttClient mclient)
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
     *  Button wrapper
     */

class MqttButton extends Button implements View.OnClickListener {
    callBackHandler mqttHandler;
    String topic;
    String data;

    MqttButton(Context ctx, callBackHandler handler, String label, String wr_topic, String tx_data) {
        super(ctx);
        setText(label);
        mqttHandler = handler;
        topic = wr_topic;
        setOnClickListener(this);
        data = tx_data;
    }

    @Override
    public void onClick(View view) {
        Log.d(getClass().getCanonicalName(), "on click");
        mqttHandler.sendMessage(topic, data);
    }
};

    /*
     *  Progress Bar
     */

class MqttProgressBar extends ProgressBar implements mqttHandler {

    double min, max;

    MqttProgressBar(Context ctx, callBackHandler handler, double fmin, double fmax, String rd_topic) {
        super(ctx, null, android.R.attr.progressBarStyleHorizontal);
        handler.addHandler(rd_topic, this);
        min = fmin;
        max = fmax;
    }

    private int translate(float n)
    {
        final int i = (int) (100 * ((n - min) / (max - min)));
        //Log.d(getClass().getCanonicalName(), n + "->" + i);
        return i;
    }

    @Override
    public void onMessage(String topic, MqttMessage msg)
    {
        Log.d(getClass().getCanonicalName(), topic + ":" + msg.toString());

        try {
            final float f = Float.parseFloat(msg.toString());
            setProgress(translate(f));
        }
        catch (NumberFormatException ex) {
            Log.d(getClass().getCanonicalName(), "Bad number:" + msg.toString());
        }
    }
};

    /*
     *  CheckBox
     */

class MqttCheckBox extends CheckBox implements mqttHandler {

    public MqttCheckBox(Context ctx, callBackHandler handler, String topic) {

        super(ctx);
        handler.addHandler(topic, this);
    }

    @Override
    public void onMessage(String topic, MqttMessage msg)
    {
        Log.d(getClass().getCanonicalName(), topic + ":" + msg.toString());

        try {
            int i = (int) Float.parseFloat(msg.toString());
            Log.d(getClass().getCanonicalName(), "Got number:" + i);
            setChecked(i != 0);
        }
        catch (NumberFormatException ex) {
            Log.d(getClass().getCanonicalName(), "Bad number:" + msg.toString());
        }
    }
};

    /*
     *  TextView
     */

class MqttTextView extends TextView implements mqttHandler {

    public MqttTextView(Activity ctx, callBackHandler handler, String topic) {
        super(ctx);
        handler.addHandler(topic, this);
    }

    @Override
    public void onMessage(String topic, MqttMessage msg)
    {
        Log.d(getClass().getCanonicalName(), topic + ":" + msg.toString());
        setText(msg.toString());
    }
};

    /*
     *  TextView
     */

class MqttLabel extends TextView {

    public MqttLabel(Activity ctx, String text) {
        super(ctx);
        setText(text);
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

        callBackHandler handler = null;
        try {
            client = new MqttClient(conf.getUrl(), MqttClient.generateClientId(), null);
            handler = new callBackHandler(this, client);
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

        loadControls(handler);
    }

    private View viewFactory(callBackHandler handler, String type, String params) {
        if (type == "Button") {
            String[] args = params.split(";");
            return new MqttButton(this, handler, args[0], args[1], args[2]);
        }
        if (type == "ProgressBar") {
            String[] args = params.split(";");
            double min = Double.parseDouble(args[0]);
            double max = Double.parseDouble(args[1]);
            return new MqttProgressBar(this, handler, min, max, args[2]);
        }
        if (type == "CheckBox") {
            String[] args = params.split(";");
            return new MqttCheckBox(this, handler, args[0]);
        }
        if (type == "TextView") {
            String[] args = params.split(";");
            return new MqttTextView(this, handler, args[0]);
        }
        if (type == "TextLabel") {
            String[] args = params.split(";");
            return new MqttLabel(this, args[0]);
        }
        return null;
    }

    private void loadControls(callBackHandler handler)
    {
        LinearLayout layout = (LinearLayout) findViewById(R.id.main_layout);

        View view = null;

        view = viewFactory(handler, "ProgressBar", "11;13;node/jeenet/11/voltage");
        layout.addView(view);

        view = viewFactory(handler, "Button", "Radio;uif/button/1;1");
        layout.addView(view);

        view = viewFactory(handler, "ProgressBar", "0;50;node/jeenet/8/voltage");
        layout.addView(view);

        view = viewFactory(handler, "CheckBox", "node/jeenet/7/state");
        layout.addView(view);

        view = viewFactory(handler, "Button", "Relay;uif/button/2;1");
        layout.addView(view);

        view = viewFactory(handler, "TextView", "node/jeenet/11/voltage");
        layout.addView(view);

        view = viewFactory(handler, "TextLabel", "Charlotte V");
        layout.addView(view);
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