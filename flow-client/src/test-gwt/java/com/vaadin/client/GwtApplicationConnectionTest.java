/*
 * Copyright 2000-2018 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
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
            }
        };
    }-*/;

}
