package com.vaadin.client;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.google.gwt.junit.tools.GWTTestSuite;

import com.vaadin.client.communication.GwtDefaultReconnectDialogTest;
import com.vaadin.client.flow.GwtBasicElementBinderTest;
import com.vaadin.client.flow.GwtEventHandlerTest;
import com.vaadin.client.flow.GwtMultipleBindingTest;
import com.vaadin.client.flow.GwtPolymerModelTest;
import com.vaadin.client.flow.GwtRouterLinkHandlerTest;
import com.vaadin.client.flow.GwtStateNodeTest;
import com.vaadin.client.flow.GwtStateTreeTest;
import com.vaadin.client.flow.GwtTreeChangeProcessorTest;
import com.vaadin.client.flow.collection.GwtJsArrayTest;
import com.vaadin.client.flow.collection.GwtJsMapTest;
import com.vaadin.client.flow.collection.GwtJsSetTest;
import com.vaadin.client.flow.collection.GwtJsWeakMapTest;
import com.vaadin.client.flow.dom.GwtDomApiTest;
import com.vaadin.client.flow.dom.GwtPolymerApiImplTest;
import com.vaadin.client.flow.util.GwtNativeFunctionTest;

public class GwtSuite extends GWTTestSuite {
    public static Test suite() {
        /*
         * List all Gwt unit test classes here so that the test runner can
         * compile them all into one JS module instead of creating a separate
         * module for each test class.
         */
        TestSuite suite = new TestSuite("Flow GWT tests");
        suite.addTestSuite(GwtApplicationConnectionTest.class);
        suite.addTestSuite(GwtJsArrayTest.class);
        suite.addTestSuite(GwtJsMapTest.class);
        suite.addTestSuite(GwtJsWeakMapTest.class);
        suite.addTestSuite(GwtJsSetTest.class);
        suite.addTestSuite(GwtBasicElementBinderTest.class);
        suite.addTestSuite(GwtPolymerModelTest.class);
        suite.addTestSuite(GwtEventHandlerTest.class);
        suite.addTestSuite(GwtTreeChangeProcessorTest.class);
        suite.addTestSuite(GwtNativeFunctionTest.class);
        suite.addTestSuite(GwtRouterLinkHandlerTest.class);
        suite.addTestSuite(GwtDefaultReconnectDialogTest.class);
        suite.addTestSuite(GwtStateNodeTest.class);
        suite.addTestSuite(GwtStateTreeTest.class);
        suite.addTestSuite(GwtDomApiTest.class);
        suite.addTestSuite(GwtPolymerApiImplTest.class);
        suite.addTestSuite(GwtWidgetUtilTest.class);
        suite.addTestSuite(GwtExecuteJavaScriptElementUtilsTest.class);
        suite.addTestSuite(GwtDependencyLoaderTest.class);
        suite.addTestSuite(GwtMessageHandlerTest.class);
        suite.addTestSuite(GwtMultipleBindingTest.class);
        return suite;
    }
}
