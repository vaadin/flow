/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
