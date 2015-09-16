package uk.co.rotwang.mqttcontrols;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
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

import java.text.SimpleDateFormat;
import java.util.Date;

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
}

    /*
     *  Topic handler
     */

class Topic
{
    private static Flag<String> flag;
    private String topic;

    public Topic(String t) {
        topic = t;

        if (flag == null) {
            if (topic.contains("%I")) {
                flag = Flag.get("ident");
            }
        }
    }

    public String get() {
        if (flag != null) {
            return topic.replace("%I", flag.get());
        }
        return topic;
    }
}

    /*
     *  Style
     */

class MqttStyle {
    private MqttControl parent;

    private Integer fontsize;
    private Integer textcolor;
    private Integer backcolor;

    public MqttStyle(MqttControl p, JSONObject json) {
        parent = p;
        //fontsize = getInt(json, "fontsize");
        StyleFontSize sfs = new StyleFontSize();
        sfs.parse(this, json);
        textcolor = getColor(json, "textcolor");
        backcolor = getColor(json, "backcolor");
    }

        /*
         *  Get attribute from JSON Object.
         */

    private Integer getColor(JSONObject json, String field) {
        String s = getString(json, field);
        if (s == null)
            return null;
        return Color.parseColor(s);
    }

    private Integer getInt(JSONObject json, String field) {
        try {
            return json.getInt(field);
        } catch (JSONException e) {
            return null;
        }
    }

    private String getString(JSONObject json, String field) {
        try {
            return json.getString(field);
        } catch (JSONException e) {
            return null;
        }
    }

    //  Recursively search the style list for the first setting

    interface Getter<T> {
        T getField(MqttStyle s);
    }

    public <T extends Object> T cascade(Getter<T> getter) {
        MqttStyle style = this;
        while (style != null) {
            if (getter.getField(style) != null)
                return getter.getField(style);
            if (style.parent == null)
                return null;
            style = style.parent.getStyle();
        }
        return null;
    }

        /*
         *  Cascading access functions
         */

    interface Attrib<T> {
        void parse(MqttStyle style, JSONObject json);
        void apply(MqttStyle style, TextView view);
    }

    abstract class StyleAttrib<T> implements Attrib<T>, Getter<T> {
        T getAttrib(MqttStyle s, Getter<T> getter) {
            return cascade(getter);
        }
    }

    class StyleFontSize extends StyleAttrib<Integer> implements Getter<Integer> {

        @Override
        public void parse(MqttStyle style, JSONObject json) {
            style.fontsize = getInt(json, "fontsize");
        }

        @Override
        public void apply(MqttStyle style, TextView view) {
            Integer i = getAttrib(style, this);
            if (i != null) {
                view.setTextSize((float) i);
            }
        }

        @Override
        public Integer getField(MqttStyle s) {
            return s.fontsize;
        }
    }

    public Integer gettextcolor() {
        return cascade(new Getter<Integer>() {
            @Override
            public Integer getField(MqttStyle s) {
                return s.textcolor;
            }
        });
    }

    public Integer getbackcolor() {
        return cascade(new Getter<Integer>() {
            @Override
            public Integer getField(MqttStyle s) {
                return s.backcolor;
            }
        });
    }

    //  Apply Style to View

    public void apply(TextView view) {
        StyleFontSize sfs = new StyleFontSize();
        sfs.apply(this, view);

        Integer i = gettextcolor();
        if (i != null) {
            view.setTextColor(i);
        }
        i = getbackcolor();
        if (i != null) {
            view.setBackgroundColor(i);
        }
    }
}

interface MqttControl {
    MqttStyle getStyle();
}

    /*
     *  Button wrapper
     */

class MqttButton extends Button implements View.OnClickListener, MqttControl {
    CallBackHandler mqttHandler;
    Topic topic;
    String data;

    private MqttButton(Context ctx, MqttStyle s, CallBackHandler handler, String label, String wr_topic, String send) {
        super(ctx);
        setText(label);
        mqttHandler = handler;
        topic = new Topic(wr_topic);
        setOnClickListener(this);
        data = send;
        style = s;
        s.apply(this);
    }

    @Override
    public void onClick(View view) {
        //Log.d(getClass().getCanonicalName(), "on click");
        mqttHandler.sendMessage(topic.get(), data);
    }

    private MqttStyle style;

    @Override
    public MqttStyle getStyle() {
        return style;
    }

    static public View create(Activity ctx, MqttControl parent, CallBackHandler handler, JSONObject obj) throws JSONException
    {
        String text = obj.getString("text");
        String topic = obj.getString("topic");
        String send = obj.getString("send");
        MqttStyle style = new MqttStyle(parent, obj);
        return new MqttButton(ctx, style, handler, text, topic, send);
    }
};

    /*
     *  Progress Bar
     */

class MqttProgressBar extends ProgressBar implements MqttHandler, MqttControl {

    double min, max;
    Picker picker;

    private MqttProgressBar(Context ctx, MqttStyle s, CallBackHandler handler, double fmin, double fmax, String t, String field) {
        super(ctx, null, android.R.attr.progressBarStyleHorizontal);
        Topic topic = new Topic(t);
        handler.addHandler(topic.get(), this);
        min = fmin;
        max = fmax;
        picker = new Picker(field);
        style = s;
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

    private MqttStyle style;

    @Override
    public MqttStyle getStyle() {
        return style;
    }

    static public View create(Activity ctx, MqttControl parent, CallBackHandler handler, JSONObject obj) throws JSONException
    {
        double min = obj.getDouble("min");
        double max = obj.getDouble("max");
        String topic = obj.getString("topic");
        String field = obj.getString("field");
        MqttStyle style = new MqttStyle(parent, obj);
        return new MqttProgressBar(ctx, style, handler, min, max, topic, field);
    }
};

    /*
     *  Seek Bar
     */

class MqttSeekBar extends SeekBar implements SeekBar.OnSeekBarChangeListener {

    double min, max;
    CallBackHandler mqtt;
    Topic topic;

    MqttSeekBar(Context ctx, CallBackHandler handler, double fmin, double fmax, String wr_topic) {
        super(ctx);
        min = fmin;
        max = fmax;
        topic = new Topic(wr_topic);
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
        mqtt.sendMessage(topic.get(), "" + translate(progress));
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

class MqttCheckBox extends CheckBox implements MqttHandler, MqttControl {

    Picker picker;

    public MqttCheckBox(Context ctx, MqttStyle s, CallBackHandler handler, String t, String field) {

        super(ctx);
        setEnabled(false);
        Topic topic = new Topic(t);
        handler.addHandler(topic.get(), this);
        picker = new Picker(field);
        style = s;
        s.apply(this);
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

    private MqttStyle style;
    @Override
    public MqttStyle getStyle() {
        return style;
    }

    static public View create(Activity ctx, MqttControl parent, CallBackHandler handler, JSONObject obj) throws JSONException
    {
        String topic = obj.getString("topic");
        String field = obj.getString("field");
        MqttStyle style = new MqttStyle(parent, obj);
        return new MqttCheckBox(ctx, style, handler, topic, field);
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
    Flag<Boolean> muted;

    public MqttBell(Context ctx, CallBackHandler handler, String t, String field) {

        super(ctx);
        context = ctx;
        Topic topic = new Topic(t);
        handler.addHandler(topic.get(), this);
        picker = new Picker(field);
        muted = Flag.get("mute");
    }

    @Override
    public void onMessage(String topic, MqttMessage msg)
    {
        if (muted.get())
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

        if (first) {
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
};

    /*
     *  TextView
     */

class MqttTextView extends TextView implements MqttHandler, MqttControl {

    Picker picker;
    String pre_text;
    String post_text;

    public MqttTextView(Activity ctx, MqttStyle s, CallBackHandler handler, String t, String field, String pre, String post) {
        super(ctx);
        pre_text = (pre == null) ? "" : pre;
        post_text = (post == null) ? "" : post;
        Topic topic = new Topic(t);
        handler.addHandler(topic.get(), this);
        picker = new Picker(field);
        style = s;

        s.apply(this);
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

    private MqttStyle style;

    @Override
    public MqttStyle getStyle() {
        return style;
    }

    static public View create(Activity ctx, MqttControl parent, CallBackHandler handler, JSONObject obj) throws JSONException
    {
        String topic = obj.getString("topic");
        String field = obj.getString("field");
        String pre = getString(obj, "pre");
        String post = getString(obj, "post");
        MqttStyle style = new MqttStyle(parent, obj);
        return new MqttTextView(ctx, style, handler, topic, field, pre, post);
    }
};

    /*
     *  Url link
     */

class MqttUrl extends TextView implements MqttHandler, View.OnClickListener, MqttControl {

    Picker text_picker;
    Picker url_picker;
    String url;
    Activity activity;

    public MqttUrl(Activity ctx, MqttStyle s, CallBackHandler handler, String t, String text_field, String url_field) {
        super(ctx);
        Topic topic = new Topic(t);
        handler.addHandler(topic.get(), this);
        text_picker = new Picker(text_field);
        url_picker = new Picker(url_field);
        url = null;
        activity = ctx;
        setOnClickListener(this);
        setTextColor(Color.BLUE);
        style = s;

        s.apply(this);
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
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            activity.startActivity(intent);
        }
        catch (ActivityNotFoundException e) {
            Log.d(getClass().getCanonicalName(), "URL Error:" + e.getCause());
        }
    }

    private MqttStyle style;

    @Override
    public MqttStyle getStyle() {
        return style;
    }

    static public View create(Activity ctx, MqttControl parent, CallBackHandler handler, JSONObject obj) throws JSONException
    {
        String topic = obj.getString("topic");
        String text_field = obj.getString("text");
        String url_field = obj.getString("url");
        MqttStyle style = new MqttStyle(parent, obj);
        return new MqttUrl(ctx, style, handler, topic, text_field, url_field);
    }
};

    /*
     *  Label
     */

class MqttLabel extends TextView implements MqttControl {

    MqttLabel(Context ctx) {
        super(ctx);
    }

    public MqttLabel(Activity ctx, MqttStyle s, String text) {
        super(ctx);
        setText(text);
        style = s;

        s.apply(this);
    }

    private MqttStyle style;
    @Override
    public MqttStyle getStyle() {
        return style;
    }

    static public View create(Activity ctx, MqttControl parent, CallBackHandler handler, JSONObject obj) throws JSONException
    {
        String text = obj.getString("text");
        MqttStyle style = new MqttStyle(parent, obj);
        return new MqttLabel(ctx, style, text);
    }
};

    /*
     *  GPS
     */

// TODO : needs to look like a GPS icon ...
class MqttGps extends TextView implements OnLocation {

    private Topic topic;
    private CallBackHandler handler;

    private MqttGps(Activity ctx, CallBackHandler h, String t) {
        super(ctx);
        handler = h;
        topic = new Topic(t);

        Flag location_flag = Flag.get("location");
        GpsLocation gps = GpsLocation.get(ctx, handler, location_flag);
        gps.register(this);
    }

    private JSONObject toJson(Location location)
    {
        JSONObject json = new JSONObject();
        try {
            json.accumulate("lon", location.getLongitude());
            json.accumulate("lat", location.getLatitude());
            json.accumulate("alt", location.getAltitude());
            json.accumulate("v", location.getSpeed());
            json.accumulate("az", location.getBearing());

            final long time = location.getTime();
            Date date = new Date(time);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm Z");
            String s = sdf.format(date);
            json.accumulate("time", s);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return json;
    }

    // implement OnLocation.
    @Override
    public void onEvent(Location location) {
        //Log.d(getClass().getCanonicalName(), topic + ":" + location);
        JSONObject json = toJson(location);
        handler.sendMessage(topic.get(), json.toString(), true);
    }

    static public View create(Activity ctx, CallBackHandler handler, JSONObject obj) throws JSONException
    {
        String t = obj.getString("topic");
        return new MqttGps(ctx, handler, t);
    }
};

    /*
     *  Text Entry
     */

class MqttEditText extends EditText implements View.OnKeyListener, MqttControl {

    Topic topic;
    CallBackHandler handler;

    public MqttEditText(Activity ctx, MqttStyle s, CallBackHandler h, String t) {
        super(ctx);
        handler = h;
        topic = new Topic(t);

        setSingleLine(true);
        setImeOptions(EditorInfo.IME_ACTION_SEND);
        setOnKeyListener(this);

        style = s;
        s.apply(this);
    }

    public void send() {
        String text = getText().toString();
        handler.sendMessage(topic.get(), text);
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

    private MqttStyle style;
    @Override
    public MqttStyle getStyle() {
        return style;
    }

    static public View create(Activity ctx, MqttControl parent, CallBackHandler handler, JSONObject obj) throws JSONException
    {
        String topic = obj.getString("topic");
        MqttStyle style = new MqttStyle(parent, obj);
        return new MqttEditText(ctx, style, handler, topic);
    }
}

    /*
     *  Factory method to construct View classes
     */

class MqttFactory {
    static public View create(Activity ctx, MqttControl parent, CallBackHandler handler, String type, JSONObject obj) throws JSONException
    {
        if (type.equals("TextLabel")) {
            return MqttLabel.create(ctx, parent, handler, obj);
        }
        if (type.equals("TextView")) {
            return MqttTextView.create(ctx, parent, handler, obj);
        }
        if (type.equals("CheckBox")) {
            return MqttCheckBox.create(ctx, parent, handler, obj);
        }
        if (type.equals("ProgressBar")) {
            return MqttProgressBar.create(ctx, parent, handler, obj);
        }
        if (type.equals("Button")) {
            return MqttButton.create(ctx, parent, handler, obj);
        }
        if (type.equals("SeekBar")) {
            return MqttSeekBar.create(ctx, handler, obj);
        }
        if (type.equals("Bell")) {
            return MqttBell.create(ctx, handler, obj);
        }
        if (type.equals("Url")) {
            return MqttUrl.create(ctx, parent, handler, obj);
        }
        if (type.equals("EditText")) {
            return MqttEditText.create(ctx, parent, handler, obj);
        }
        if (type.equals("GPS")) {
            return MqttGps.create(ctx, handler, obj);
        }
        return null;
    }
}

// FIN