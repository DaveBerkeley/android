package uk.co.rotwang.mqttcontrols;

import android.app.Activity;
import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

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

class MqttProgressBar extends ProgressBar implements MqttHandler {

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
     *  Seek Bar
     */

class MqttSeekBar extends SeekBar implements SeekBar.OnSeekBarChangeListener {

    double min, max;
    CallBackHandler mqtt;
    String topic;

    MqttSeekBar(Context ctx, CallBackHandler handler, double fmin, double fmax, String wr_topic) {
        super(ctx);
        min = fmin;
        max = fmax;
        topic = wr_topic;
        mqtt = handler;
        setOnSeekBarChangeListener(this);
        setMax(100);
    }

    static public View create(Activity ctx, CallBackHandler handler, JSONObject obj) throws JSONException
    {
        double min = obj.getDouble("min");
        double max = obj.getDouble("max");
        String topic = obj.getString("topic");
        return new MqttSeekBar(ctx, handler, min, max, topic);
    }

    private double translate(int n)
    {
        return ((n / 100.0) * (max - min)) + min;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        //Log.d(getClass().getCanonicalName(), "" + progress);
        mqtt.sendMessage(topic, "" + translate(progress));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }
};

    /*
     *  CheckBox
     */

class MqttCheckBox extends CheckBox implements MqttHandler {

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
     *  Bell alarm.
     */

class MqttBell extends View implements MqttHandler {

    Picker picker;
    Context context;
    int last_value;
    boolean first = true;

    public MqttBell(Context ctx, CallBackHandler handler, String topic, String field) {

        super(ctx);
        context = ctx;
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

            if (first == true) {
                last_value = i;
                first = false;
                return;
            }

            if (i != last_value) {
                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Ringtone r = RingtoneManager.getRingtone(context, notification);
                r.play();
            }

            last_value = i;
        }
        catch (NumberFormatException ex) {
            Log.d(getClass().getCanonicalName(), "Bad number:" + msg.toString() + " " + ex.getCause());
        }
    }

    static public View create(Activity ctx, CallBackHandler handler, JSONObject obj) throws JSONException
    {
        String topic = obj.getString("topic");
        String field = obj.getString("field");
        return new MqttBell(ctx, handler, topic, field);
    }
};

    /*
     *  TextView
     */

class MqttTextView extends TextView implements MqttHandler {

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
        if (type.equals("SeekBar")) {
            return MqttSeekBar.create(ctx, handler, obj);
        }
        if (type.equals("Bell")) {
            return MqttBell.create(ctx, handler, obj);
        }
        return null;
    }
}
