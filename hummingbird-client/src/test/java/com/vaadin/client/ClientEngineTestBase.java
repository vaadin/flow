package com.vaadin.client;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Base class for all unit tests that run as JavaScript compiled by GWT. The
 * name of any non-abstract subclass must begin with "Gwt" to prevent the class
 * from being run as a regular JVM unit test.
 */
public abstract class ClientEngineTestBase extends GWTTestCase {
    @Override
    public String getModuleName() {
        // Change to com.vaadin.ClientEngineXSI when introducing new linker
        return "com.vaadin.ClientEngine";
    }
}