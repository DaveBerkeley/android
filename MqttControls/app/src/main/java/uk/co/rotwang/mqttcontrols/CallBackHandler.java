package uk.co.rotwang.mqttcontrols;

import android.app.Activity;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

    /*
    *   MQTT Callback interface
    */

interface MqttHandler {
  public void onMessage(String topic, MqttMessage msg);
}
    /*
     *  MQTT Callback implementation
     */

class CallBackHandler implements MqttCallback
{
    MqttClient client;
    Map<String, List<MqttHandler>> map;
    Activity activity;

    public CallBackHandler(Activity ctx, MqttClient mclient)
    {
        activity = ctx;
        client = mclient;
        map = new HashMap<String, List<MqttHandler>>();
    }

    public void unsubscribe()
    {
        Iterator it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            String topic = (String) pair.getKey();
            try {
                client.unsubscribe(topic);
            } catch (MqttException e) {
                Log.d(getClass().getCanonicalName(), "Unsubscribe Error:" + topic + " " + e.getCause());
            }
        }

        // start a fresh map
        map = new HashMap<String, List<MqttHandler>>();
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
            List<MqttHandler> handlers = map.get(topic);
            if (handlers != null) {
                for (int i = 0; i < handlers.size(); ++i) {
                    MqttHandler handler = handlers.get(i);
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

    public void addHandler(String topic, MqttHandler handler)
    {
        Log.d(getClass().getCanonicalName(), "Add handler " + topic + ":" + handler);
        List<MqttHandler> handlers = map.get(topic);

        if (handlers == null) {
            handlers = new ArrayList<MqttHandler>();
            map.put(topic, handlers);

            try {
                client.subscribe(topic);
                Log.d(getClass().getCanonicalName(), "Subscribe:" + topic + " " + handler);
            }
            catch (MqttException e) {
                Log.d(getClass().getCanonicalName(), "Subscribe Error:" + e.getCause());
            }
        }

        handlers.add(handler);
    }

    public void sendMessage(String topic, String msg, boolean retain)
    {
        try
        {
            MqttMessage message = new MqttMessage();
            message.setPayload(msg.getBytes());
            message.setRetained(retain);
            client.publish(topic, message);
        }
        catch (MqttException e)
        {
            Log.d(getClass().getCanonicalName(), "Publish failed with reason code = " + e.getReasonCode());
        }
    }

    public void sendMessage(String topic, String msg)
    {
        sendMessage(topic, msg, false);
    }
}

//  FIN