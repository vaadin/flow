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

import static com.vaadin.flow.internal.nodefeature.ReconnectDialogConfigurationMap.DIALOG_TEXT_GAVE_UP_KEY;
import static com.vaadin.flow.internal.nodefeature.ReconnectDialogConfigurationMap.DIALOG_TEXT_KEY;
import static com.vaadin.flow.internal.nodefeature.ReconnectDialogConfigurationMap.RECONNECT_ATTEMPTS_DEFAULT;
import static com.vaadin.flow.internal.nodefeature.ReconnectDialogConfigurationMap.RECONNECT_ATTEMPTS_KEY;
import static com.vaadin.flow.internal.nodefeature.ReconnectDialogConfigurationMap.RECONNECT_INTERVAL_DEFAULT;
import static com.vaadin.flow.internal.nodefeature.ReconnectDialogConfigurationMap.RECONNECT_INTERVAL_KEY;

public class ReconnectConfigurationTest extends AbstractConfigurationTest {

    private StateTree stateTree;
    private ReconnectConfiguration configuration;
    private AtomicInteger configurationUpdatedCalled = new AtomicInteger(0);

    {
        new Registry() {
            {
                set(UILifecycle.class, new UILifecycle());
                stateTree = new StateTree(this);
                set(StateTree.class, stateTree);
                // Binds to the root node
                configuration = new ReconnectConfiguration(this);
                ConnectionStateHandler connectionStateHandler = Mockito
                        .mock(ConnectionStateHandler.class);
                Mockito.doAnswer(new Answer<Void>() {
                    @Override
                    public Void answer(InvocationOnMock invocation)
                            throws Throwable {
                        // Read some values to be able to test that the
                        // reactive computation works properly
                        configuration.getDialogText();
                        configurationUpdatedCalled.incrementAndGet();
                        return null;
                    }
                }).when(connectionStateHandler).configurationUpdated();

                ReconnectConfiguration.bind(connectionStateHandler);
                set(ConnectionStateHandler.class, connectionStateHandler);
            }
        };
    }

    @Test
    public void defaults() {
        // Defaults for dialog properties moved to ConnectionIndicator.ts
        Assert.assertEquals(null, configuration.getDialogText());
        Assert.assertEquals(null, configuration.getDialogTextGaveUp());
        Assert.assertEquals(RECONNECT_ATTEMPTS_DEFAULT,
                configuration.getReconnectAttempts());
        Assert.assertEquals(RECONNECT_INTERVAL_DEFAULT,
                configuration.getReconnectInterval());
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
    public void setGetReconnectAttempts() {
        testInt(RECONNECT_ATTEMPTS_KEY, configuration::getReconnectAttempts);
    }

    @Test
    public void setGetReconnectInterval() {
        testInt(RECONNECT_INTERVAL_KEY, configuration::getReconnectInterval);
    }

    @Test
    public void reactsToChanges() {
        configurationUpdatedCalled.set(0);
        getProperty(DIALOG_TEXT_GAVE_UP_KEY).setValue("bar");
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
        getProperty(DIALOG_TEXT_KEY).setValue("abc");
        getProperty(DIALOG_TEXT_GAVE_UP_KEY).setValue("def");
        Assert.assertEquals(0, configurationUpdatedCalled.get());
        Reactive.flush();
        Assert.assertEquals(1, configurationUpdatedCalled.get());
    }

}
