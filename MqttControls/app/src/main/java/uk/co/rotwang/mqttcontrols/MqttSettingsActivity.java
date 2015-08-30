package uk.co.rotwang.mqttcontrols;

import android.content.Context;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.EditText;
import android.widget.Toast;

    /*
     *  Data store for settings
     */

class MqttSettings {

    final String PREFS_NAME = "mqtt_settings";

    String server;
    int port;

    public void read(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_WORLD_READABLE);
        server = sp.getString("server", "");
        port = sp.getInt("port", 1888);
    }

    public void save(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_WORLD_READABLE);
        SharedPreferences.Editor edit = sp.edit();
        edit.putString("server", server);
        edit.putInt("port", port);
    }
};

    /*
    *   Activity
    */

public class MqttSettingsActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mqtt_settings);

        MqttSettings s = new MqttSettings();
        s.read(this);

        EditText edit = (EditText) findViewById(R.id.server_edit);
        edit.setText(s.server);
        edit = (EditText) findViewById(R.id.port_edit);
        edit.setText(Integer.toString(s.port));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_mqtt_settings, menu);
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /** Called when the user clicks the Settings button */
    public void saveMqttSettings(View view) {
        MqttSettings s = new MqttSettings();
        EditText edit = (EditText) findViewById(R.id.server_edit);
        s.server = edit.getText().toString();
        edit = (EditText) findViewById(R.id.port_edit);
        String port = edit.getText().toString();
        s.port = Integer.parseInt(port);

        // Show Toast
        CharSequence text = "Saving " + s.server + ":" + port;
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(this, text, duration);
        toast.show();

        s.save(this);
    }
}
