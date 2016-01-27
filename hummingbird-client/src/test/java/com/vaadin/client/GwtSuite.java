package com.vaadin.client;

import com.google.gwt.junit.tools.GWTTestSuite;
import com.vaadin.client.hummingbird.collection.GwtCollectionTest;

import junit.framework.Test;
import junit.framework.TestSuite;

public class GwtSuite extends GWTTestSuite {
    public static Test suite() {
        /*
         * List all Gwt unit test classes here so that the test runner can
         * compile them all into one JS module instead of creating a separate
         * module for each test class.
         */
        TestSuite suite = new TestSuite("Hummingbird GWT tests");
        suite.addTestSuite(GwtCollectionTest.class);
        return suite;
    }
}
