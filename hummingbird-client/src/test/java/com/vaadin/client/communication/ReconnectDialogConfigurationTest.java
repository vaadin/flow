/*
 * Copyright 2000-2016 Vaadin Ltd.
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

import static com.vaadin.hummingbird.namespace.ReconnectDialogConfigurationNamespace.DIALOG_GRACE_PERIOD_DEFAULT;
import static com.vaadin.hummingbird.namespace.ReconnectDialogConfigurationNamespace.DIALOG_GRACE_PERIOD_KEY;
import static com.vaadin.hummingbird.namespace.ReconnectDialogConfigurationNamespace.DIALOG_MODAL_DEFAULT;
import static com.vaadin.hummingbird.namespace.ReconnectDialogConfigurationNamespace.DIALOG_MODAL_KEY;
import static com.vaadin.hummingbird.namespace.ReconnectDialogConfigurationNamespace.DIALOG_TEXT_DEFAULT;
import static com.vaadin.hummingbird.namespace.ReconnectDialogConfigurationNamespace.DIALOG_TEXT_GAVE_UP_DEFAULT;
import static com.vaadin.hummingbird.namespace.ReconnectDialogConfigurationNamespace.DIALOG_TEXT_GAVE_UP_KEY;
import static com.vaadin.hummingbird.namespace.ReconnectDialogConfigurationNamespace.DIALOG_TEXT_KEY;
import static com.vaadin.hummingbird.namespace.ReconnectDialogConfigurationNamespace.RECONNECT_ATTEMPTS_DEFAULT;
import static com.vaadin.hummingbird.namespace.ReconnectDialogConfigurationNamespace.RECONNECT_ATTEMPTS_KEY;
import static com.vaadin.hummingbird.namespace.ReconnectDialogConfigurationNamespace.RECONNECT_INTERVAL_DEFAULT;
import static com.vaadin.hummingbird.namespace.ReconnectDialogConfigurationNamespace.RECONNECT_INTERVAL_KEY;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.vaadin.client.Registry;
import com.vaadin.client.UILifecycle;
import com.vaadin.client.communication.ConnectionStateHandler;
import com.vaadin.client.communication.ReconnectDialogConfiguration;
import com.vaadin.client.hummingbird.StateTree;
import com.vaadin.client.hummingbird.namespace.MapProperty;
import com.vaadin.client.hummingbird.reactive.Reactive;
import com.vaadin.hummingbird.shared.Namespaces;

public class ReconnectDialogConfigurationTest extends AbstractConfigurationTest {

    private StateTree stateTree;
    private ReconnectDialogConfiguration namespace;
    private AtomicInteger configurationUpdatedCalled = new AtomicInteger(0);

    {
        new Registry() {

            {
                set(UILifecycle.class, new UILifecycle());
                stateTree = new StateTree(this);
                set(StateTree.class, stateTree);
                // Binds to the root node
                namespace = new ReconnectDialogConfiguration(this);
                ConnectionStateHandler connectionStateHandler = Mockito
                        .mock(ConnectionStateHandler.class);
                Mockito.doAnswer(new Answer<Void>() {
                    @Override
                    public Void answer(InvocationOnMock invocation)
                            throws Throwable {
                        // Read some values to be able to test that the
                        // reactive computation works properly
                        namespace.isDialogModal();
                        namespace.getDialogText();
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
        Assert.assertEquals(DIALOG_TEXT_DEFAULT, namespace.getDialogText());
        Assert.assertEquals(DIALOG_TEXT_GAVE_UP_DEFAULT,
                namespace.getDialogTextGaveUp());
        Assert.assertEquals(RECONNECT_ATTEMPTS_DEFAULT,
                namespace.getReconnectAttempts());
        Assert.assertEquals(RECONNECT_INTERVAL_DEFAULT,
                namespace.getReconnectInterval());
        Assert.assertEquals(DIALOG_GRACE_PERIOD_DEFAULT,
                namespace.getDialogGracePeriod());
        Assert.assertEquals(DIALOG_MODAL_DEFAULT, namespace.isDialogModal());
    }

    @Override
    protected MapProperty getProperty(String key) {
        return stateTree.getRootNode()
                .getMapNamespace(Namespaces.RECONNECT_DIALOG_CONFIGURATION)
                .getProperty(key);
    }

    @Test
    public void setGetDialogText() {
        testString(DIALOG_TEXT_KEY, namespace::getDialogText);
    }

    @Test
    public void setGetDialogTextGaveUp() {
        testString(DIALOG_TEXT_GAVE_UP_KEY, namespace::getDialogTextGaveUp);
    }

    @Test
    public void setGetDialogGracePeriod() {
        testInt(DIALOG_GRACE_PERIOD_KEY, namespace::getDialogGracePeriod);
    }

    @Test
    public void setGetReconnectAttempts() {
        testInt(RECONNECT_ATTEMPTS_KEY, namespace::getReconnectAttempts);
    }

    @Test
    public void setGetReconnectInterval() {
        testInt(RECONNECT_INTERVAL_KEY, namespace::getReconnectInterval);
    }

    @Test
    public void setGetDialogModal() {
        testBoolean(DIALOG_MODAL_KEY, namespace::isDialogModal);
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
