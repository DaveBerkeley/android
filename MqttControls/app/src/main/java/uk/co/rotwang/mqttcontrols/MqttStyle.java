package uk.co.rotwang.mqttcontrols;

import android.graphics.Color;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

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
        parse(json);
    }

    /*
     *  Get attribute from JSON Object.
     */

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

    private Integer getColor(JSONObject json, String field) {
        String s = getString(json, field);
        if (s == null)
            return null;
        return Color.parseColor(s);
    }

        /*
         *  StyleAttrib class for each attribute.
         */

    interface Attrib<T> {
        void parse(JSONObject json);
        void set(T t, TextView view);
    }

    interface Getter<T> {
        T get(MqttStyle s);
    }

    //  search the style list for the nearest setting or parent setting.
    private <T extends Object> T cascade(Getter<T> getter) {
        MqttStyle style = this;
        while (style != null) {
            T t = getter.get(style);
            if (t != null)
                return t;
            if (style.parent == null)
                return null;
            style = style.parent.getStyle();
        }
        return null;
    }

    abstract class StyleAttrib<T> implements Attrib<T>, Getter<T> {
        void apply(TextView view) {
            T t = cascade(this);
            if (t != null) {
                set(t, view);
            }
        }
    }

    class StyleFontSize extends StyleAttrib<Integer> implements Getter<Integer> {

        @Override
        public void parse(JSONObject json) {
            fontsize = getInt(json, "fontsize");
        }

        @Override
        public void set(Integer i, TextView view) {
            view.setTextSize((float) i);
        }

        @Override
        public Integer get(MqttStyle s) {
            return s.fontsize;
        }
    }

    class StyleTextColor extends StyleAttrib<Integer> implements Getter<Integer> {

        @Override
        public void parse(JSONObject json) {
            textcolor = getColor(json, "textcolor");
        }

        @Override
        public void set(Integer i, TextView view) {
            view.setTextColor(i);
        }

        @Override
        public Integer get(MqttStyle s) {
            return s.textcolor;
        }
    }

    class StyleBackColor extends StyleAttrib<Integer> implements Getter<Integer> {

        @Override
        public void parse(JSONObject json) {
            backcolor = getColor(json, "backcolor");
        }

        @Override
        public void set(Integer i, TextView view) {
            view.setBackgroundColor(i);
        }

        @Override
        public Integer get(MqttStyle s) {
            return s.backcolor;
        }
    }

    //  Added StyleAttrib classes to this list

    private List<StyleAttrib> styles()
    {
        List<StyleAttrib> styles = new ArrayList<StyleAttrib>();
        // Add any new StyleAttrib to this list
        styles.add(new StyleFontSize());
        styles.add(new StyleTextColor());
        styles.add(new StyleBackColor());
        return styles;
    }

    //  Parse JSON

    private void parse(JSONObject json) {
        for (StyleAttrib style : styles()) {
            style.parse(json);
        }
    }

    //  Apply Style to View

    public void apply(TextView view) {
        for (StyleAttrib style : styles()) {
            style.apply(view);
        }
    }
}

//  FIN