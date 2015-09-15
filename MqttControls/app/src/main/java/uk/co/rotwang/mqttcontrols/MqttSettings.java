package uk.co.rotwang.mqttcontrols;

import android.content.Context;
import android.content.SharedPreferences;

    /*
     *  Data store for settings
     */


public class MqttSettings {

    final String PREFS_NAME = "mqtt_settings";

    String server;
    int port;
    String url;
    boolean allow_location;
    boolean mute;
    String ident;
    String username;
    String password;

    public void read(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        server = sp.getString("server", "");
        port = sp.getInt("port", 1883);
        url = sp.getString("url", "");
        allow_location = sp.getBoolean("location", false);
        mute = sp.getBoolean("mute", false);
        ident = sp.getString("ident", "");
        password = sp.getString("password", "");
        username = sp.getString("username", "");
    }

    public void save(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = sp.edit();
        edit.putString("server", server);
        edit.putInt("port", port);
        edit.putString("url", url);
        edit.putBoolean("location", allow_location);
        edit.putBoolean("mute", mute);
        edit.putString("ident", ident);
        edit.putString("password", password);
        edit.putString("username", username);
        edit.commit();
    }

    public String getUrl() {
        return "tcp://" + server + ":" + Integer.toString(port);
    }
};

