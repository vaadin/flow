/*
 * Copyright 2000-2019 Vaadin Ltd.
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
import com.vaadin.flow.internal.nodefeature.AttachExistingElementFeature;

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
