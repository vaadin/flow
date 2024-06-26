/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.client.communication;

import static com.vaadin.flow.internal.nodefeature.ReconnectDialogConfigurationMap.DIALOG_GRACE_PERIOD_DEFAULT;
import static com.vaadin.flow.internal.nodefeature.ReconnectDialogConfigurationMap.DIALOG_GRACE_PERIOD_KEY;
import static com.vaadin.flow.internal.nodefeature.ReconnectDialogConfigurationMap.DIALOG_MODAL_DEFAULT;
import static com.vaadin.flow.internal.nodefeature.ReconnectDialogConfigurationMap.DIALOG_MODAL_KEY;
import static com.vaadin.flow.internal.nodefeature.ReconnectDialogConfigurationMap.DIALOG_TEXT_DEFAULT;
import static com.vaadin.flow.internal.nodefeature.ReconnectDialogConfigurationMap.DIALOG_TEXT_GAVE_UP_DEFAULT;
import static com.vaadin.flow.internal.nodefeature.ReconnectDialogConfigurationMap.DIALOG_TEXT_GAVE_UP_KEY;
import static com.vaadin.flow.internal.nodefeature.ReconnectDialogConfigurationMap.DIALOG_TEXT_KEY;
import static com.vaadin.flow.internal.nodefeature.ReconnectDialogConfigurationMap.RECONNECT_ATTEMPTS_DEFAULT;
import static com.vaadin.flow.internal.nodefeature.ReconnectDialogConfigurationMap.RECONNECT_ATTEMPTS_KEY;
import static com.vaadin.flow.internal.nodefeature.ReconnectDialogConfigurationMap.RECONNECT_INTERVAL_DEFAULT;
import static com.vaadin.flow.internal.nodefeature.ReconnectDialogConfigurationMap.RECONNECT_INTERVAL_KEY;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.vaadin.client.Registry;
import com.vaadin.client.UILifecycle;
import com.vaadin.client.flow.StateTree;
import com.vaadin.client.flow.nodefeature.MapProperty;
import com.vaadin.client.flow.reactive.Reactive;
import com.vaadin.flow.internal.nodefeature.NodeFeatures;

public class ReconnectDialogConfigurationTest
        extends AbstractConfigurationTest {

    private StateTree stateTree;
    private ReconnectDialogConfiguration configuration;
    private AtomicInteger configurationUpdatedCalled = new AtomicInteger(0);

    {
        new Registry() {
            {
                set(UILifecycle.class, new UILifecycle());
                stateTree = new StateTree(this);
                set(StateTree.class, stateTree);
                // Binds to the root node
                configuration = new ReconnectDialogConfiguration(this);
                ConnectionStateHandler connectionStateHandler = Mockito
                        .mock(ConnectionStateHandler.class);
                Mockito.doAnswer(new Answer<Void>() {
                    @Override
                    public Void answer(InvocationOnMock invocation)
                            throws Throwable {
                        // Read some values to be able to test that the
                        // reactive computation works properly
                        configuration.isDialogModal();
                        configuration.getDialogText();
                        configurationUpdatedCalled.incrementAndGet();
                        return null;
                    }
                }).when(connectionStateHandler).configurationUpdated();

                ReconnectDialogConfiguration.bind(connectionStateHandler);
                set(ConnectionStateHandler.class, connectionStateHandler);
            }
        };
    }

    @Test
    public void defaults() {
        Assert.assertEquals(DIALOG_TEXT_DEFAULT, configuration.getDialogText());
        Assert.assertEquals(DIALOG_TEXT_GAVE_UP_DEFAULT,
                configuration.getDialogTextGaveUp());
        Assert.assertEquals(RECONNECT_ATTEMPTS_DEFAULT,
                configuration.getReconnectAttempts());
        Assert.assertEquals(RECONNECT_INTERVAL_DEFAULT,
                configuration.getReconnectInterval());
        Assert.assertEquals(DIALOG_GRACE_PERIOD_DEFAULT,
                configuration.getDialogGracePeriod());
        Assert.assertEquals(DIALOG_MODAL_DEFAULT,
                configuration.isDialogModal());
    }

    @Override
    protected MapProperty getProperty(String key) {
        return stateTree.getRootNode()
                .getMap(NodeFeatures.RECONNECT_DIALOG_CONFIGURATION)
                .getProperty(key);
    }

    @Test
    public void setGetDialogText() {
        testString(DIALOG_TEXT_KEY, configuration::getDialogText);
    }

    @Test
    public void setGetDialogTextGaveUp() {
        testString(DIALOG_TEXT_GAVE_UP_KEY, configuration::getDialogTextGaveUp);
    }

    @Test
    public void setGetDialogGracePeriod() {
        testInt(DIALOG_GRACE_PERIOD_KEY, configuration::getDialogGracePeriod);
    }

    @Test
    public void setGetReconnectAttempts() {
        testInt(RECONNECT_ATTEMPTS_KEY, configuration::getReconnectAttempts);
    }

    @Test
    public void setGetReconnectInterval() {
        testInt(RECONNECT_INTERVAL_KEY, configuration::getReconnectInterval);
    }

    @Test
    public void setGetDialogModal() {
        testBoolean(DIALOG_MODAL_KEY, configuration::isDialogModal);
    }

    @Test
    public void reactsToChanges() {
        configurationUpdatedCalled.set(0);
        getProperty(DIALOG_MODAL_KEY).setValue(true);
        Reactive.flush();
        Assert.assertEquals(1, configurationUpdatedCalled.get());
        getProperty(DIALOG_TEXT_KEY).setValue("foo");
        Reactive.flush();
        Assert.assertEquals(2, configurationUpdatedCalled.get());
    }

    @Test
    public void changesReportedInOneBatch() {
        configurationUpdatedCalled.set(0);
        getProperty(RECONNECT_INTERVAL_KEY).setValue(13);
        getProperty(RECONNECT_ATTEMPTS_KEY).setValue(13);
        getProperty(DIALOG_GRACE_PERIOD_KEY).setValue(13);
        getProperty(DIALOG_MODAL_KEY).setValue(true);
        getProperty(DIALOG_TEXT_KEY).setValue("abc");
        getProperty(DIALOG_TEXT_GAVE_UP_KEY).setValue("def");
        Assert.assertEquals(0, configurationUpdatedCalled.get());
        Reactive.flush();
        Assert.assertEquals(1, configurationUpdatedCalled.get());
    }

}
