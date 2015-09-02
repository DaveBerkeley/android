package uk.co.rotwang.mqttcontrols;

import android.content.Context;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.content.Intent;
import android.widget.Button;
import android.widget.LinearLayout;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.HashMap;
import java.util.Map;

interface mqttHandler {
  public void onMessage(String topic, MqttMessage msg);
};

class callBackHandler implements MqttCallback
{
    Map<String, mqttHandler> map;

    public callBackHandler()
    {
        map = new HashMap<String, mqttHandler>();
    }

    public void messageArrived(String topic, MqttMessage message)
    {
        mqttHandler handler = map.get(topic);
        if (handler != null) {
            handler.onMessage(topic, message);
        } else {
            Log.d(getClass().getCanonicalName(), "no handler:" + topic + ":" + message.toString());
        }
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
    }
};

    /*
     *  Button wrapper
     */

class MqttButton extends Button implements mqttHandler, View.OnClickListener {
    MqttClient client;
    String topic;

    MqttButton(Context ctx, MqttClient mclient, callBackHandler handler, String rd_topic, String wr_topic) {
        super(ctx);
        client = mclient;
        topic = wr_topic;

        if (rd_topic != null) {
            handler.addHandler(rd_topic, this);
        }
        
        setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        Log.d(getClass().getCanonicalName(), "on click" + client);
        this.sendMessage("1");
    }

    @Override
    public void onMessage(String topic, MqttMessage msg)
    {
        Log.d(getClass().getCanonicalName(), "Button:" + topic + ":" + msg.toString());
    }

    public void sendMessage(String msg)
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

        callBackHandler handler = new callBackHandler();
        try {
            client = new MqttClient(conf.getUrl(), MqttClient.generateClientId(), null);
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

        try
        {
            client.subscribe("node/jeenet/1/temp");
            client.subscribe("node/jeenet/11/voltage");
        }
        catch (MqttException e)
        {
            Log.d(getClass().getCanonicalName(), "Subscribe failed with reason code = " + e.getReasonCode());
        }

        LinearLayout layout = (LinearLayout) findViewById(R.id.main_layout);
        Button bt = new MqttButton(this, client, handler, "node/jeenet/1/temp", "uif/button");
        bt.setText("A Button");
        //bt.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
        //        LayoutParams.WRAP_CONTENT));
        layout.addView(bt);

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