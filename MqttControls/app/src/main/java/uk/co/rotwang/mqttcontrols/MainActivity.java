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

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.HashMap;
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
    Map<String, mqttHandler> map;
    Activity activity;

    public callBackHandler(Activity ctx, MqttClient mclient)
    {
        activity = ctx;
        client = mclient;
        map = new HashMap<String, mqttHandler>();
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
            mqttHandler handler = map.get(topic);
            if (handler != null) {
                handler.onMessage(topic, message);
            } else {
                Log.d(getClass().getCanonicalName(), "no handler:" + topic + ":" + message.toString());
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
        map.put(topic, handler);
        try {
            client.subscribe(topic);
            Log.d(getClass().getCanonicalName(), "Set handler:" + topic + " " + handler);
        }
        catch (MqttException e) {
            Log.d(getClass().getCanonicalName(), "Subscribe Error:" + e.getCause());
        }
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

    MqttButton(Context ctx, String label, callBackHandler handler, String wr_topic) {
        super(ctx);
        setText(label);
        mqttHandler = handler;
        topic = wr_topic;
        setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        Log.d(getClass().getCanonicalName(), "on click");
        mqttHandler.sendMessage(topic, "1");
    }
};

    /*
     *  Progress Bar
     */

class MqttProgressBar extends ProgressBar implements mqttHandler {

    float min, max;

    MqttProgressBar(Context ctx, float fmin, float fmax, callBackHandler handler, String rd_topic) {
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
        Log.d(getClass().getCanonicalName(), "Progress:" + topic + ":" + msg.toString());

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

    Activity activity;

    public MqttCheckBox(Activity ctx, callBackHandler handler, String topic) {

        super(ctx);
        activity = ctx;
        handler.addHandler(topic, this);
    }

    @Override
    public void onMessage(String topic, MqttMessage msg)
    {
        Log.d(getClass().getCanonicalName(), "CheckBox:" + topic + ":" + msg.toString());

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
        }
        catch (MqttException ex) {
            ex.printStackTrace();
        }

        MqttConnectOptions options = new MqttConnectOptions();
        try
        {
            client.connect(options);
        }
        catch (MqttException e) {
            Log.d(getClass().getCanonicalName(), "Connection attempt failed with reason code = " + e.getReasonCode() + ":" + e.getCause());
        }

        LinearLayout layout = (LinearLayout) findViewById(R.id.main_layout);

        ProgressBar pb = new MqttProgressBar(this, 0, 15, handler, "node/jeenet/11/voltage");
        layout.addView(pb);

        Button bt = new MqttButton(this, "A button", handler, "uif/button");
        layout.addView(bt);

        pb = new MqttProgressBar(this, 0, 50, handler, "node/jeenet/8/voltage");
        layout.addView(pb);

        CheckBox cb = new MqttCheckBox(this, handler, "node/jeenet/7/state");
        layout.addView(cb);
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