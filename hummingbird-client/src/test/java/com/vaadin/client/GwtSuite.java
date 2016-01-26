package com.vaadin.client;

import com.google.gwt.junit.tools.GWTTestSuite;

import junit.framework.Test;
import junit.framework.TestSuite;

public class GwtSuite extends GWTTestSuite {
    public static Test suite() {
        TestSuite suite = new TestSuite("Hummingbird GWT tests");
        suite.addTestSuite(GwtExampleTest.class);
        return suite;
    }
}
