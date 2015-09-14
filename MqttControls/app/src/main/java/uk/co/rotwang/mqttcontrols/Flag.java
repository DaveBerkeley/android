package uk.co.rotwang.mqttcontrols;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

    /*
     *  On Flag Change handler
     */

interface OnFlag {
    public void onFlag(boolean state);
}

class Flag {

    private boolean state;
    private List<OnFlag> handlers;

    public Flag(boolean s)
    {
        state = s;
        handlers = new ArrayList<OnFlag>();
    }

    public void register(OnFlag handler) {
        handlers.add(handler);
    }

    public void remove(OnFlag handler) {
        handlers.remove(handler);
    }

    public void set(boolean s) {
        state = s;
        for (OnFlag handler : handlers) {
            handler.onFlag(state);
        }
    }

    public boolean get()
    {
        return state;
    }

    private static HashMap<String, Flag> flags;

    public static Flag add(String name, boolean state) {
        if (flags == null) {
            flags = new HashMap<String, Flag>();
        }

        Flag flag = new Flag(state);
        flags.put(name, flag);
        return flag;
    }

    public static Flag get(String name) {
        return flags.get(name);
    }
}

//  FIN