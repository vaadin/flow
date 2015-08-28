package com.vaadin.client.communication.tree;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.js.JsFunction;

@JsFunction
public interface DomListener {
    public void handleEvent(JavaScriptObject event);
}