package uk.co.rotwang.mqttcontrols;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
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

interface AudioDevice {
    boolean isMuted();
    void mute(boolean off);
}

class MqttBell extends View implements MqttHandler, AudioDevice {

    Picker picker;
    Context context;
    int last_value;
    boolean first = true;
    boolean muted = false;

    public MqttBell(Context ctx, CallBackHandler handler, String topic, String field) {

        super(ctx);
        context = ctx;
        handler.addHandler(topic, this);
        picker = new Picker(field);
    }

    @Override
    public void onMessage(String topic, MqttMessage msg)
    {
        if (muted)
            return;
        //Log.d(getClass().getCanonicalName(), topic + ":" + msg.toString());

        int i = 0;
        final String s = picker.pick(msg.toString());
        try {
            i = (int) Float.parseFloat(s);
            //Log.d(getClass().getCanonicalName(), "Got number:" + i);
        }
        catch (NumberFormatException ex) {
            Log.d(getClass().getCanonicalName(), "Bad number:" + msg.toString() + " " + ex.getCause());
            last_value = i + 1; // to force a notification
        }

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

    static public View create(Activity ctx, CallBackHandler handler, JSONObject obj) throws JSONException
    {
        String topic = obj.getString("topic");
        String field = obj.getString("field");
        return new MqttBell(ctx, handler, topic, field);
    }

    @Override
    public boolean isMuted() {
        return muted;
    }

    @Override
    public void mute(boolean off) {
        muted = off;
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
     *  Url link
     */

class MqttUrl extends TextView implements MqttHandler, View.OnClickListener {

    Picker text_picker;
    Picker url_picker;
    String url;
    Activity activity;

    public MqttUrl(Activity ctx, CallBackHandler handler, String topic, String text_field, String url_field) {
        super(ctx);
        handler.addHandler(topic, this);
        text_picker = new Picker(text_field);
        url_picker = new Picker(url_field);
        url = null;
        activity = ctx;
        setOnClickListener(this);
        setTextColor(Color.BLUE);
    }

    @Override
    public void onMessage(String topic, MqttMessage msg)
    {
        //Log.d(getClass().getCanonicalName(), topic + ":" + msg.toString());
        final String s = msg.toString();
        final String text = text_picker.pick(s);
        url = url_picker.pick(s);
        setText(text);
    }

    static String getString(JSONObject obj, String key) throws JSONException
    {
        if (obj.has(key)) {
            return obj.getString(key);
        }
        return null;
    }

    @Override
    public void onClick(View v) {
        if (url == null)
            return;

        // Goto Url
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        activity.startActivity(intent);
    }

    static public View create(Activity ctx, CallBackHandler handler, JSONObject obj) throws JSONException
    {
        String topic = obj.getString("topic");
        String text_field = obj.getString("text");
        String url_field = obj.getString("url");
        return new MqttUrl(ctx, handler, topic, text_field, url_field);
    }
};

    /*
     *  Label
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
     *  Text Entry
     */

class MqttEditText extends EditText implements View.OnKeyListener {

    String topic;
    CallBackHandler handler;

    public MqttEditText(Activity ctx, CallBackHandler handler0, String topic0) {
        super(ctx);
        handler = handler0;
        topic = topic0;

        setSingleLine(true);
        setImeOptions(EditorInfo.IME_ACTION_SEND);
        setOnKeyListener(this);
    }

    public void send() {
        String text = getText().toString();
        handler.sendMessage(topic, text);
        setText("");
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getKeyCode() != KeyEvent.KEYCODE_ENTER)
            return false;
        if (event.isShiftPressed())
            return false;
        if (event.getAction() != KeyEvent.ACTION_DOWN)
            return false;

        send();
        return true;
    }

    static public View create(Activity ctx, CallBackHandler handler, JSONObject obj) throws JSONException
    {
        String topic = obj.getString("topic");
        return new MqttEditText(ctx, handler, topic);
    }
}

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
        if (type.equals("Url")) {
            return MqttUrl.create(ctx, handler, obj);
        }
        if (type.equals("EditText")) {
            return MqttEditText.create(ctx, handler, obj);
        }
        return null;
    }
}
