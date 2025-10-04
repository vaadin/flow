/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.client.communication;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.impl.SchedulerImpl;

import com.vaadin.client.ApplicationConfiguration;
import com.vaadin.client.ClientEngineTestBase;
import com.vaadin.client.CustomScheduler;
import com.vaadin.client.Registry;
import com.vaadin.client.UILifecycle;
import com.vaadin.client.communication.AtmospherePushConnection.AtmosphereResponse;
import com.vaadin.client.flow.ConstantPool;
import com.vaadin.client.flow.StateTree;
import com.vaadin.client.flow.collection.JsCollections;
import com.vaadin.client.flow.collection.JsMap;

/**
 * @author Vaadin Ltd
 *
 */
public class GwtAtmospherePushConnectionTest extends ClientEngineTestBase {

    private Registry registry;

    @Override
    protected void gwtSetUp() throws Exception {
        super.gwtSetUp();

        initScheduler(new CustomScheduler());

        registry = new Registry() {
            {
                set(ConstantPool.class, new ConstantPool());
                set(StateTree.class, new StateTree(this));
                set(UILifecycle.class, new UILifecycle());
                set(RequestResponseTracker.class, new RequestResponseTracker(this));
                set(ApplicationConfiguration.class,
                        new ApplicationConfiguration());
                set(MessageHandler.class, new MessageHandler(this));
                set(PushConfiguration.class, new PushConfiguration(this) {
                    @Override
                    public JsMap<String, String> getParameters() {
                        return JsCollections.map();
                    }
                });
                set(ConnectionStateHandler.class,
                        new DefaultConnectionStateHandler(this));
            }
        };

    }

    public void testDisconnect_disconnectUrlIsSameAsInConnect() {
        setUpAtmosphere();

        registry.getApplicationConfiguration().setServiceUrl("context://foo");
        registry.getApplicationConfiguration().setContextRootUrl("bar/");

        AtmospherePushConnection connection = new AtmospherePushConnection(
                registry);
        String pushUri = getPushUri();

        AtmosphereResponse response = (AtmosphereResponse) JavaScriptObject
                .createObject();
        connection.onConnect(response);
        connection.disconnect(() -> {
        });
        assertTrue(getUnsubscribeUri().startsWith("bar/"));
        assertEquals(pushUri, getUnsubscribeUri());
    }

    private native void setUpAtmosphere()/*-{
                                         $wnd.vaadinPush={};
                                         $wnd.vaadinPush.atmosphere ={};
                                         $wnd.vaadinPush.atmosphere.subscribe = function(config){
                                             $wnd.subscribeUrl =  config.url;
                                         };
                                         $wnd.vaadinPush.atmosphere.unsubscribeUrl = function(uri){
                                             $wnd.unsubscribeUri =  uri;
                                         };
                                         }-*/;

    private native String getPushUri()/*-{
                                      return $wnd.subscribeUrl ;
                                      }-*/;

    private native String getUnsubscribeUri()/*-{
                                             return $wnd.unsubscribeUri ;
                                             }-*/;

    private native void initScheduler(SchedulerImpl scheduler)
    /*-{
       @com.google.gwt.core.client.impl.SchedulerImpl::INSTANCE = scheduler;
    }-*/;
}
