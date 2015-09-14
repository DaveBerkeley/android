package uk.co.rotwang.mqttcontrols;

import android.content.Context;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;



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
        edit = (EditText) findViewById(R.id.url_edit);
        edit.setText(s.url);

        CheckBox cb = (CheckBox) findViewById(R.id.allow_location);
        cb.setChecked(s.allow_location);

        cb = (CheckBox) findViewById(R.id.mute);
        cb.setChecked(s.mute);
        // Disable for now ...
        cb.setEnabled(false);
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

    private void toast(CharSequence text) {
        // Show Toast
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(this, text, duration);
        toast.show();
    }

    /** Called when the user clicks the Settings button */
    public void saveMqttSettings(View view) {
        MqttSettings s = new MqttSettings();
        EditText edit = (EditText) findViewById(R.id.server_edit);
        s.server = edit.getText().toString();
        edit = (EditText) findViewById(R.id.port_edit);
        String port = edit.getText().toString();
        s.port = Integer.parseInt(port);
        edit = (EditText) findViewById(R.id.url_edit);
        s.url = edit.getText().toString();

        CheckBox cb = (CheckBox) findViewById(R.id.allow_location);
        s.allow_location = cb.isChecked();
        cb = (CheckBox) findViewById(R.id.mute);
        s.mute = cb.isChecked();

        toast("Saving " + s.server + ":" + port);

        s.save(this);
        finish();
    }
}
