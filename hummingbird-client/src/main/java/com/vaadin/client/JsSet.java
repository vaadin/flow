package com.vaadin.client;

import com.google.gwt.core.client.js.JsType;
import com.vaadin.client.JsSet.Visitor;

@JsType
public interface JsSet<T> {

    public interface Visitor<T> {
        public void accept(T value);
    }

    public JsSet<T> add(T value);

    public void clear();

    public boolean delete(T value);

    public boolean has(T value);

    // There's probably some jsinterop annotation to make this work without a
    // static method
    public static <T> void forEach(JsSet<T> set, Visitor<T> something) {
        StaticHelper.forEach(set, something);
    }

    public static <T> JsSet<T> create() {
        return StaticHelper.create();
    }
}

// Hack since static interface method can't be native
class StaticHelper {
    static native <T> JsSet<T> create()
    /*-{
      return new $wnd.Set();
    }-*/;

    // There must be an easier way
    static native <T> void forEach(JsSet<T> set, Visitor<T> visitor)
    /*-{
      set.forEach(function(value) {visitor.@Visitor::accept(*)(value)});
    }-*/;
}
