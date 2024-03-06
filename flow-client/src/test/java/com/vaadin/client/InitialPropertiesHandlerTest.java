/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.client;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.client.flow.StateNode;
import com.vaadin.client.flow.StateTree;
import com.vaadin.client.flow.nodefeature.MapProperty;
import com.vaadin.client.flow.nodefeature.NodeMap;
import com.vaadin.client.flow.reactive.FlushListener;
import com.vaadin.client.flow.reactive.Reactive;
import com.vaadin.flow.internal.nodefeature.NodeFeatures;

public class InitialPropertiesHandlerTest {

    private Registry registry = Mockito.mock(Registry.class);
    private StateTree tree = Mockito.mock(StateTree.class);

    private InitialPropertiesHandler handler = new InitialPropertiesHandler(
            registry);

    @Before
    public void setUp() {
        Mockito.when(tree.getRegistry()).thenReturn(registry);
        Mockito.when(registry.getStateTree()).thenReturn(tree);
    }

    @Test
    public void flushPropertyUpdates_updateInProgress_noInteractions() {
        Mockito.when(tree.isUpdateInProgress()).thenReturn(true);
        handler.flushPropertyUpdates();

        Reactive.flush();

        Mockito.verify(tree).isUpdateInProgress();
        Mockito.verifyNoMoreInteractions(tree);
    }

    @Test
    public void flushPropertyUpdates_updateIsNotInProgress_collectInitialProperties() {
        Mockito.when(tree.isUpdateInProgress()).thenReturn(false);
        StateNode node = new StateNode(1, tree);

        NodeMap properties = node.getMap(NodeFeatures.ELEMENT_PROPERTIES);
        MapProperty serverSidePropertyUpdatedByClient = properties
                .getProperty("foo");
        serverSidePropertyUpdatedByClient.setValue("bar");

        MapProperty serverSideProperty = properties.getProperty("other");
        serverSideProperty.setValue("value");

        handler.nodeRegistered(node);

        Mockito.when(tree.getNode(node.getId())).thenReturn(node);

        handler.flushPropertyUpdates();

        serverSidePropertyUpdatedByClient.setValue("updated");
        MapProperty clientSideProperty = properties.getProperty("client");
        clientSideProperty.setValue("baz");

        handler.handlePropertyUpdate(serverSidePropertyUpdatedByClient);
        handler.handlePropertyUpdate(clientSideProperty);

        Reactive.flush();

        Assert.assertEquals("bar", properties.getProperty("foo").getValue());
        Assert.assertEquals("value",
                properties.getProperty("other").getValue());
        Assert.assertEquals("baz", properties.getProperty("client").getValue());

        Mockito.verify(tree, Mockito.times(0)).sendNodePropertySyncToServer(
                serverSidePropertyUpdatedByClient);
        Mockito.verify(tree, Mockito.times(0))
                .sendNodePropertySyncToServer(serverSideProperty);
        Mockito.verify(tree).sendNodePropertySyncToServer(clientSideProperty);
    }

    @Test
    public void flushPropertyUpdates_updateIsNotInProgress_flushForEechProperty() {
        Mockito.when(tree.isUpdateInProgress()).thenReturn(false);
        StateNode node = new StateNode(1, tree);

        NodeMap properties = node.getMap(NodeFeatures.ELEMENT_PROPERTIES);
        MapProperty property1 = properties.getProperty("foo");
        property1.setValue("bar");

        MapProperty property2 = properties.getProperty("other");
        property2.setValue("value");

        handler.nodeRegistered(node);

        Mockito.when(tree.getNode(node.getId())).thenReturn(node);

        handler.flushPropertyUpdates();

        property1.setValue("baz");
        property2.setValue("foo");

        handler.handlePropertyUpdate(property1);
        handler.handlePropertyUpdate(property2);

        AtomicInteger count = new AtomicInteger();
        FlushListener listener = () -> count.incrementAndGet();
        property1.addChangeListener(
                event -> Reactive.addFlushListener(listener));
        property2.addChangeListener(
                event -> Reactive.addFlushListener(listener));

        Reactive.flush();

        Assert.assertEquals(2, count.get());
    }

}
