/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.client.communication;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.impl.SchedulerImpl;

import com.vaadin.client.ApplicationConfiguration;
import com.vaadin.client.ClientEngineTestBase;
import com.vaadin.client.CustomScheduler;
import com.vaadin.client.Registry;
import com.vaadin.client.UILifecycle;
import com.vaadin.client.URIResolver;
import com.vaadin.client.communication.AtmospherePushConnection.AtmosphereResponse;
import com.vaadin.client.flow.ConstantPool;
import com.vaadin.client.flow.StateTree;
import com.vaadin.client.flow.collection.JsCollections;
import com.vaadin.client.flow.collection.JsMap;

/**
 * @author Vaadin Ltd
 *
 */
public class GwtAtmoshperePushConnectionTest extends ClientEngineTestBase {

    private Registry registry;

    @Override
    protected void gwtSetUp() throws Exception {
        super.gwtSetUp();

        initScheduler(new CustomScheduler());

        registry = new Registry() {
            {
                set(ConstantPool.class, new ConstantPool());
                set(StateTree.class, new StateTree(this));
                set(URIResolver.class, new URIResolver(this));
                set(UILifecycle.class, new UILifecycle());
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

    public void testDicsonnect_disconnectUrlIsSameAsInConnect() {
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
        assertTrue(getUnsubscriveUri().startsWith("bar/"));
        assertEquals(pushUri, getUnsubscriveUri());
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

    private native String getUnsubscriveUri()/*-{
                                             return $wnd.unsubscribeUri ;
                                             }-*/;

    private native void initScheduler(SchedulerImpl scheduler)
    /*-{
       @com.google.gwt.core.client.impl.SchedulerImpl::INSTANCE = scheduler;
    }-*/;
}
