package uk.co.rotwang.mqttcontrols;

import android.app.Activity;
import android.content.Context;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

    /*
     *  GPS / Location listener
     */

interface OnLocation {
    public void onEvent(android.location.Location location);
}

class GpsLocation implements LocationListener, OnFlag {

    private Activity activity;
    private CallBackHandler handler;
    private Flag location_flag;
    private List<OnLocation> handlers;

    private GpsLocation(Activity ctx, CallBackHandler h, Flag flag) {
        activity = ctx;
        handler = h;
        location_flag = flag;
        handlers = new ArrayList<OnLocation>();
        flag.register(this);
        connect(flag.get());
    }

    public void register(OnLocation handler) {
        handlers.add(handler);
    }

    private void connect(boolean on)
    {
        LocationManager man = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);

        if (on) {
            man.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, this);
        } else {
            man.removeUpdates(this);
        }
    }

    //  LocationListener interface.

    @Override
    public void onLocationChanged(android.location.Location location) {
        Log.d(getClass().getCanonicalName(), "Location : " + location);

        for (OnLocation handler : handlers) {
            handler.onEvent(location);
        }
    }

    @Override public void onStatusChanged(String provider, int status, Bundle extras) { }
    @Override public void onProviderEnabled(String provider) { }
    @Override public void onProviderDisabled(String provider) { }

    //  Implement OnFlag

    @Override
    public void onFlag(boolean state) {
        connect(location_flag.get());
    }

    /*
     *  Singleton
     */

    private static GpsLocation instance;

    public static GpsLocation get(Activity ctx, CallBackHandler h, Flag flag)
    {
        if (instance == null) {
            instance = new GpsLocation(ctx, h, flag);
        }

        return instance;
    }
}

//  FIN