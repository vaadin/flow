package com.vaadin.client;

import com.google.gwt.junit.tools.GWTTestSuite;
import com.vaadin.client.hummingbird.collection.GwtBasicElementBinderTest;
import com.vaadin.client.hummingbird.template.GwtTemplateBinderTest2;

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
        // suite.addTestSuite(GwtJsArrayTest.class);
        // suite.addTestSuite(GwtJsMapTest.class);
        // suite.addTestSuite(GwtJsWeakMapTest.class);
        // suite.addTestSuite(GwtJsSetTest.class);
        suite.addTestSuite(GwtBasicElementBinderTest.class);
        // suite.addTestSuite(GwtTreeChangeProcessorTest.class);
        // suite.addTestSuite(GwtNativeFunctionTest.class);
        // suite.addTestSuite(GwtRouterLinkHandlerTest.class);
        // suite.addTestSuite(GwtDefaultReconnectDialogTest.class);
        suite.addTestSuite(GwtTemplateBinderTest2.class);
        return suite;
    }
}
