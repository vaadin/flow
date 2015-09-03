package com.vaadin.client;

import com.google.gwt.junit.tools.GWTTestSuite;
import com.vaadin.client.communication.tree.TestBasicTreeOperations;

import junit.framework.Test;
import junit.framework.TestSuite;

public class GwtHummingbirdSuite extends GWTTestSuite {
    public static Test suite() {
        TestSuite suite = new TestSuite("Hummingbird client-side tests");
        suite.addTestSuite(TestBasicTreeOperations.class);
        return suite;
    }
}
