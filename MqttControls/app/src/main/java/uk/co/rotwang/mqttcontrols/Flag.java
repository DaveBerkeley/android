package uk.co.rotwang.mqttcontrols;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

    /*
     *  On Flag Change handler
     */

interface OnFlag<T> {
    public void onFlag(T state);
}

class Flag<T> {

    private T state;
    private List<OnFlag<T>> handlers;

    public Flag(T s)
    {
        state = s;
        handlers = new ArrayList<OnFlag<T>>();
    }

    public void register(OnFlag<T> handler) {
        handlers.add(handler);
    }

    public void remove(OnFlag<T> handler) {
        handlers.remove(handler);
    }

    public void set(T s) {
        state = s;
        for (OnFlag handler : handlers) {
            handler.onFlag(state);
        }
    }

    public T get()
    {
        return state;
    }

    private static HashMap<String, Flag> flags;

    private static void add(String name, Flag flag) {
        if (flags == null) {
            flags = new HashMap<String, Flag>();
        }

        flags.put(name, flag);
    }

    public static void add(String name, boolean state) {
        Flag flag = new Flag<Boolean>(state);
        add(name, flag);
    }

    public static void add(String name, String state) {
        Flag flag = new Flag<String>(state);
        add(name, flag);
    }

    @SuppressWarnings("unchecked")
    public static Flag get(String name) {
        return flags.get(name);
    }
}

//  FIN