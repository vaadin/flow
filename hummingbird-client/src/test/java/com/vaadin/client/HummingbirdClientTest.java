package com.vaadin.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;
import com.vaadin.client.communication.DomApiImpl;
import com.vaadin.client.communication.PlainDomApiImpl;

public class HummingbirdClientTest extends GWTTestCase {
    static {
        if (GWT.isClient()) {
            useDomImpl(new PlainDomApiImpl());
        }
    }

    @Override
    public String getModuleName() {
        return "com.vaadin.ClientEngine";
    }

    private static native void useDomImpl(DomApiImpl impl)
    /*-{
        @com.vaadin.client.communication.DomApi::impl = impl;
    }-*/;

}
