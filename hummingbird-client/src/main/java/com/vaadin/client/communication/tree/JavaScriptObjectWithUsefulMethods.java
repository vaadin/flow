package com.vaadin.client.communication.tree;

import com.google.gwt.core.client.JavaScriptObject;

public class JavaScriptObjectWithUsefulMethods extends JavaScriptObject {

    protected JavaScriptObjectWithUsefulMethods() {

    }

    public final native void put(String key, JavaScriptObject value)
    /*-{
         this[key] = value;
    }-*/;

}
