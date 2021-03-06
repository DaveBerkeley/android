package uk.co.rotwang.mqttcontrols;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

    /*
    *   Activity
    */

public class MqttSettingsActivity extends AppCompatActivity {

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

        //  Connect flag to the location checkbox
        Flag<Boolean> flag = Flag.get("location");
        CheckboxListener cb_listener = new CheckboxListener(flag);
        CheckBox cb = (CheckBox) findViewById(R.id.allow_location);
        cb.setChecked(flag.get());
        cb.setOnCheckedChangeListener(cb_listener);

        //  Connect flag to the mute checkbox
        flag = Flag.get("mute");
        cb_listener = new CheckboxListener(flag);
        cb = (CheckBox) findViewById(R.id.mute);
        cb.setChecked(flag.get());
        cb.setOnCheckedChangeListener(cb_listener);

        // Connect flag to ident changes
        Flag<String> sflag = Flag.get("ident");
        EditTextListener et_listener = new EditTextListener(sflag);
        edit = (EditText) findViewById(R.id.ident_edit);
        edit.setText(sflag.get());
        edit.setOnEditorActionListener(et_listener);

        edit = (EditText) findViewById(R.id.username);
        edit.setText(s.username);
        edit = (EditText) findViewById(R.id.password);
        edit.setText(s.password);
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

    class CheckboxListener implements CompoundButton.OnCheckedChangeListener {

        private Flag<Boolean> flag;

        public CheckboxListener(Flag<Boolean> f) {
            flag = f;
        }

        //  Implement OnCheckedChangeListener
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            flag.set(isChecked);
        }
    }

    class EditTextListener implements TextView.OnEditorActionListener {

        private Flag<String> flag;

        public EditTextListener(Flag<String> f) { flag = f; }

        // Implements OnEditorActionListener
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                flag.set(v.getText().toString());
            }
            return false;
        }
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

        edit = (EditText) findViewById(R.id.ident_edit);
        s.ident = edit.getText().toString();

        edit = (EditText) findViewById(R.id.username);
        s.username = edit.getText().toString();
        edit = (EditText) findViewById(R.id.password);
        s.password = edit.getText().toString();

        toast("Saving " + s.server + ":" + port);

        s.save(this);
        finish();
    }
}
