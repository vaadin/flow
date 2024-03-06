/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.client;

import com.google.gwt.user.client.Timer;

import com.vaadin.client.bootstrap.Bootstrapper;

import elemental.client.Browser;
import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * Gwt unit tests for the ApplicationConnection class.
 */
public class GwtApplicationConnectionTest extends ClientEngineTestBase {

    public void test_should_addNavigationEvents_byDefault() {
        mockFlowBootstrapScript(false);

        JsonObject windowEvents = Json.createObject();
        addEventsObserver(Browser.getWindow(), windowEvents);

        JsonObject bodyEvents = Json.createObject();
        addEventsObserver(Browser.getDocument().getBody(), bodyEvents);

        new Bootstrapper().onModuleLoad();

        delayTestFinish(500);
        new Timer() {
            @Override
            public void run() {
                assertTrue(windowEvents.hasKey("popstate"));
                assertTrue(bodyEvents.hasKey("click"));
                finishTest();
            }
        }.schedule(100);
    }

    public void test_should_not_addNavigationEvents_forWebComponents() {
        mockFlowBootstrapScript(true);

        JsonObject windowEvents = Json.createObject();
        addEventsObserver(Browser.getWindow(), windowEvents);

        JsonObject bodyEvents = Json.createObject();
        addEventsObserver(Browser.getDocument().getBody(), bodyEvents);

        new Bootstrapper().onModuleLoad();

        delayTestFinish(500);
        new Timer() {
            @Override
            public void run() {
                assertFalse(windowEvents.hasKey("popstate"));
                assertFalse(bodyEvents.hasKey("click"));
                finishTest();
            }
        }.schedule(100);
    }

    // monkey-patch the addEventListener method in the element in order
    // to store calls to the method in the addedEvents object
    private native void addEventsObserver(Object elm, Object addedEvents) /*-{
        elm._addEventListener = elm._addEventListener || elm.addEventListener;
        elm.addEventListener = function(a, b, c) {
            elm._addEventListener(a, b, c);
            addedEvents[a] = b;
        }
    }-*/;

    // create a mock version of the BootstrapHandler.js script with the
    // callbacks and configuration needed for testing gwt client code.
    private native void mockFlowBootstrapScript(boolean webComponentMode) /*-{
        var mockCfg = {
            'heartbeatInterval' : 300,
            'maxMessageSuspendTimeout': 5000,
            'contextRootUrl' : '../',
            'debug' : true,
            'v-uiId' : 0,
            'serviceUrl' : '//localhost:8080/flow/',
            'webComponentMode' : webComponentMode,
        };
        $wnd.Vaadin = {
            Flow : {
                clients : {},
                registerWidgetset : function(name, callback) {
                    callback(name);
                },
                getApp : function() {
                    return {
                        getConfig : function(key) {
                            return mockCfg[key];
                        }
                    }
                }
            },
            connectionState: {
                setState: function(state) {
                    // NOP
                }
            }
        };
    }-*/;

}
