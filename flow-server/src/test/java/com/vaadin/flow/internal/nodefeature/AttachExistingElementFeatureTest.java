/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal.nodefeature;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.dom.ChildElementConsumer;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.Node;
import com.vaadin.flow.internal.StateNode;

public class AttachExistingElementFeatureTest {

    @Test
    public void register_dataIsAvailaleByNode() {
        StateNode node = new StateNode();
        AttachExistingElementFeature feature = new AttachExistingElementFeature(
                node);

        Element element = Mockito.mock(Element.class);
        StateNode child = Mockito.mock(StateNode.class);
        ChildElementConsumer callback = Mockito
                .mock(ChildElementConsumer.class);
        Node<?> parent = Mockito.mock(Node.class);
        feature.register(parent, element, child, callback);

        Mockito.verify(child).setParent(node);

        Assert.assertEquals(callback, feature.getCallback(child));
        Assert.assertEquals(parent, feature.getParent(child));
        Assert.assertEquals(element, feature.getPreviousSibling(child));
    }

    @Test
    public void forEachChild_register_registeredStatNodeIsAChild() {
        StateNode node = new StateNode();
        AttachExistingElementFeature feature = new AttachExistingElementFeature(
                node);

        Element element = Mockito.mock(Element.class);
        StateNode child = Mockito.mock(StateNode.class);
        ChildElementConsumer callback = Mockito
                .mock(ChildElementConsumer.class);
        Node<?> parent = Mockito.mock(Node.class);
        feature.register(parent, element, child, callback);

        List<StateNode> children = new ArrayList<>(1);
        feature.forEachChild(children::add);
        Assert.assertEquals(1, children.size());
        Assert.assertEquals(child, children.get(0));
    }

    @Test
    public void unregister_dataIsNotAvailaleByNode() {
        StateNode node = new StateNode();
        AttachExistingElementFeature feature = new AttachExistingElementFeature(
                node);

        Element element = Mockito.mock(Element.class);
        StateNode child = Mockito.mock(StateNode.class);
        ChildElementConsumer callback = Mockito
                .mock(ChildElementConsumer.class);
        Node<?> parent = Mockito.mock(Node.class);
        feature.register(parent, element, child, callback);

        feature.unregister(child);

        Assert.assertNull(feature.getCallback(child));
        Assert.assertNull(feature.getParent(child));
        Assert.assertNull(feature.getPreviousSibling(child));
    }
}
