package com.vaadin.client;

import com.google.gwt.junit.client.GWTTestCase;

public abstract class ClientEngineTestBase extends GWTTestCase {
    @Override
    public String getModuleName() {
        // Change to com.vaadin.ClientEngineXSI when introducing new linker
        return "com.vaadin.ClientEngine";
    }
}