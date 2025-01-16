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

import com.vaadin.client.Registry;
import com.vaadin.client.UILifecycle;
import com.vaadin.client.flow.StateTree;
import com.vaadin.client.flow.nodefeature.MapProperty;
import com.vaadin.flow.internal.nodefeature.NodeFeatures;
import com.vaadin.flow.internal.nodefeature.PollConfigurationMap;

public class PollConfiguratorTest {

    StateTree stateTree;
    private AtomicInteger pollerInterval = new AtomicInteger(-2);
    private AtomicInteger pollerSetIntervalCalled = new AtomicInteger(0);

    private final Registry registry = new Registry() {
        {
            set(UILifecycle.class, new UILifecycle());
            stateTree = new StateTree(this);
            set(StateTree.class, stateTree);
        }
    };

    @Test
    public void listensToProperty() {
        PollConfigurator.observe(stateTree.getRootNode(), new Poller(registry) {
            @Override
            public void setInterval(int interval) {
                pollerSetIntervalCalled.incrementAndGet();
                pollerInterval.set(interval);
            };
        });

        Assert.assertEquals(-2, pollerInterval.get());
        Assert.assertEquals(0, pollerSetIntervalCalled.get());
        MapProperty pollIntervalProperty = stateTree.getRootNode()
                .getMap(NodeFeatures.POLL_CONFIGURATION)
                .getProperty(PollConfigurationMap.POLL_INTERVAL_KEY);

        pollIntervalProperty.setValue(100.0);
        Assert.assertEquals(100, pollerInterval.get());
        Assert.assertEquals(1, pollerSetIntervalCalled.get());

        pollIntervalProperty.setValue(-1.0);
        Assert.assertEquals(-1, pollerInterval.get());
        Assert.assertEquals(2, pollerSetIntervalCalled.get());

    }
}
