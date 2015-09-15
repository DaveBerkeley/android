package uk.co.rotwang.mqttcontrols;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.content.Intent;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

    /*
     *  Activity
     */

public class MainActivity extends AppCompatActivity implements OnUrl {

    private MqttClient client;
    private CallBackHandler handler = null;
    private int page_num;
    private int max_page;
    private GestureDetector fling;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fling = new GestureDetector(this, new FlingDetector());

        MqttSettings conf = new MqttSettings();
        conf.read(this);

        try {
            client = new MqttClient(conf.getUrl(), MqttClient.generateClientId(), null);
            handler = new CallBackHandler(this, client);
            client.setCallback(handler);
        } catch (MqttException ex) {
            ex.printStackTrace();
        }

        MqttConnectOptions options = new MqttConnectOptions();
        if (!conf.username.isEmpty()) {
            options.setUserName(conf.username);
            options.setPassword(conf.password.toCharArray());
        }
        try {
            client.connect(options);
        } catch (MqttException e) {
            Log.d(getClass().getCanonicalName(), "Connection attempt failed with reason code = " + e.getReasonCode() + ":" + e.getCause());
        }

        Flag.add("mute", conf.mute);
        Flag.add("location", conf.allow_location);
        Flag.add("ident", conf.ident);

        page_num = 0;
        reload();
    }

    /*
     * FlingListener
     */

    public class FlingDetector extends SimpleOnGestureListener {

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            final float dx = e1.getX() - e2.getX();
            //final float dy = e1.getY() - e2.getY();
            //Log.d(getClass().getCanonicalName(), "fling:" + dx + "," + dy);

            if (dx > 100) {
                if (page_num < max_page) {
                    page_num += 1;
                    redraw();
                }
            } else if (dx < 100) {
                if (page_num > 0) {
                    page_num -= 1;
                    redraw();
                }
            }

            return false;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (fling.onTouchEvent(event)) {
            return true;
        }
        return super.onTouchEvent(event);
    }

    /*
     * ViewGroup wrapper to allow Grid subviews
     */

    class Table extends TableLayout implements MqttControl {
        final int cols;
        int row;
        Activity context;
        TableRow tr;

        public Table(Activity ctx, MqttControl parent, JSONObject json, int r, int c)
        {
            super(ctx);
            context = ctx;
            cols = c;
            row = 0;
            style = new MqttStyle(parent, json);
        }

        public void addView(View view)
        {
            if (row == 0) {
                //  Added new row
                tr = new TableRow(context);
                super.addView(tr);
            }

            row += 1;
            if (row == cols) {
                row = 0;
            }

            // Set weight so all colums are equal.
            TableRow.LayoutParams layout = new TableRow.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
            layout.weight = (float) (1.0 / cols);
            view.setLayoutParams(layout);
            tr.addView(view);
        }

        private MqttStyle style;

        @Override
        public MqttStyle getStyle() {
            return style;
        }
    };

    private MqttControl getParent(View view) {
        try {
            return (MqttControl) (Object) view;
        }
        catch (ClassCastException ex){
            return null;
        }
    }

    /*
     *  Create View controls from JSON config file,
     */

    private void loadControls(ViewGroup group, JSONArray reader) throws JSONException {
        // iterate through controls
        for (int i = 0; i < reader.length(); ++i) {
            JSONArray item = reader.getJSONArray(i);
            if (item.length() != 2) {
                // should always have ["name",{ dict },] pairs
                throw new JSONException("bad data");
            }
            String type = item.getString(0);
            JSONObject dict = item.getJSONObject(1);
            Log.d(getClass().getCanonicalName(), "Create " + type + " : " + dict);

            if (type.equals("GridView")) {
                final int rows = dict.getInt("rows");
                final int cols = dict.getInt("cols");
                Table grid = new Table(this, getParent(group), dict, rows, cols);

                group.addView(grid);

                // recurse to fill the grid
                JSONArray elements = dict.getJSONArray("elements");
                loadControls(grid, elements);
                continue;
            }

            View view = MqttFactory.create(this, getParent(group), handler, type, dict);
            if (view != null) {
                group.addView(view);
            } else {
                Log.d(getClass().getCanonicalName(), "Error creating object");
            }
        }
    }

    public void setTitle(CharSequence title)
    {
        String app_name = getString(R.string.app_name);
        super.setTitle(app_name + " : " + title);
    }

    private JSONArray readPage(JSONArray array, int idx) throws JSONException {
        JSONArray page = array.getJSONArray(idx);
        String type = page.getString(0);
        assert type.equals("Page");
        JSONObject dict = page.getJSONObject(1);
        JSONArray elements = dict.getJSONArray("elements");
        String title = dict.getString("title");
        Log.d(getClass().getCanonicalName(), "Reading page:" + title);
        setTitle(title);
        return elements;
    }

    private boolean loadControls(String conf)
    {
        // remove all current MQTT subscribe
        handler.unsubscribe();

        LinearLayout layout = (LinearLayout) findViewById(R.id.main_layout);

        // Remove existing views
        if (layout.getChildCount() > 0) {
            layout.removeAllViews();
        }

        if (conf == null)
            return false;

        try {
            JSONArray reader = new JSONArray(conf);
            max_page = reader.length() - 1;
            if (page_num > max_page)
                page_num = max_page;
            JSONArray page = readPage(reader, page_num);
            loadControls(layout, page);
        } catch (JSONException e) {
            e.printStackTrace();
            toast(getString(R.string.json_error) + " : " + e.getCause());
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            toast(getString(R.string.config_read_error) + " : " + e.getCause());
            return false;
        }
        return true;
    }

    private void redraw() {
        String config = loadConfigFromCache();
        loadControls(config);
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
        switch (item.getItemId())
        {
            case R.id.action_settings : {
                actionSettings(null);
                return true;
            }
            case R.id.reload : {
                reload();
                return true;
            }
            case R.id.exit : {
                toast(getString(R.string.mqtt_disconnect));
                finish();
                return true;
            }
            case R.id.about : {
                // Goto About page
                String url = getString(R.string.about_url);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                startActivity(intent);

            }
            default : return super.onOptionsItemSelected(item);
        }
    }

    /** Called when the user clicks the Settings button */
    public void actionSettings(View view) {
        Intent intent = new Intent(this, MqttSettingsActivity.class);
        startActivity(intent);
    }

    private void toast(CharSequence text) {
        // Show Toast
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(this, text, duration);
        toast.show();
    }

        /*
         *  Save / Load config data to cache
         *  so we have a default when the source URI is not readable.
         */

    final private String cachePath = "datacahe.txt";
    final private String encoding = "UTF-8";

    private boolean saveConfigToCache(String data) {
        try {
            FileOutputStream file = openFileOutput(cachePath, MODE_PRIVATE);
            file.write(data.getBytes(Charset.forName(encoding)));
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private String loadConfigFromCache() {
        try {
            FileInputStream file = openFileInput(cachePath);
            Reader r = new InputStreamReader(file, encoding);
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[1024];
            int amt = r.read(buf);
            while(amt > 0) {
                sb.append(buf, 0, amt);
                amt = r.read(buf);
            }
            return sb.toString();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Callback when data is fetched from URL
    @Override
    public void onUrl(String data) {
        //Log.d(getClass().getCanonicalName(), "Got data:" + data);

        if (data == null) {
            // try to read from cache
            toast("Read data from cache");
            data = loadConfigFromCache();
        }

        if (data == null) {
            toast("Unable to read config");
            return;
        }

        if (loadControls(data)) {
            saveConfigToCache(data);
        }
    }

    private void reload() {
        MqttSettings conf = new MqttSettings();
        conf.read(this);
        //  Fetch and load the controls config
        UrlFetcher fetcher = new UrlFetcher(this, conf.url, this);
        fetcher.start();
    }

    @Override
    protected void onDestroy() {
        handler.unsubscribe();
        super.onDestroy();
        //GpsLocation.close();
    }
}

// FIN