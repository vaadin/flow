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
package com.vaadin.flow.dom;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.NodeVisitor.ElementType;
import com.vaadin.flow.dom.impl.ShadowRootStateProvider;
import com.vaadin.flow.internal.NullOwner;
import com.vaadin.flow.internal.nodefeature.NodeProperties;
import com.vaadin.flow.shared.Registration;

public class ShadowRootTest extends AbstractNodeTest {

    @Test
    public void publicElementMethodsShouldReturnElement() {
        Set<String> ignore = new HashSet<>();
        ignore.add("toString");
        ignore.add("hashCode");
        ignore.add("equals");

        // Returns index of child element
        ignore.add("indexOfChild");

        assertMethodsReturnType(ShadowRoot.class, ignore);
    }

    @Test
    public void insertAtCurrentPositionNoOp() {
        // Must have an UI to get attach events
        UI ui = new UI();
        ShadowRoot parent = ui.getElement().attachShadow();
        Element child = ElementFactory.createDiv();

        parent.appendChild(child);

        child.addDetachListener(
                e -> Assert.fail("Child should not be detached"));
        parent.insertChild(0, child);
    }

    @Test
    public void equalsSelf() {
        ShadowRoot root = createParentNode();
        Assert.assertTrue(root.equals(root));
    }

    @Test
    public void notEqualsNull() {
        ShadowRoot root = createParentNode();
        Assert.assertFalse(root.equals(null));
    }

    @Test
    public void attachListener_parentAttach_childListenersTriggered() {
        Element body = new UI().getElement();
        Element parent = ElementFactory.createDiv();
        Element child = ElementFactory.createDiv();
        Element grandChild = ElementFactory.createDiv();

        AtomicInteger childTriggered = new AtomicInteger();
        AtomicInteger grandChildTriggered = new AtomicInteger();

        Registration registrationHandle = child.addAttachListener(event -> {
            childTriggered.addAndGet(1);
        });
        child.addAttachListener(event -> {
            Assert.assertEquals(child, event.getSource());
        });
        grandChild.addAttachListener(event -> {
            grandChildTriggered.addAndGet(1);
        });
        grandChild.addAttachListener(event -> {
            Assert.assertEquals(grandChild, event.getSource());
        });

        parent.attachShadow().appendChild(child);
        child.appendChild(grandChild);

        Assert.assertEquals(childTriggered.get(), 0);
        Assert.assertEquals(grandChildTriggered.get(), 0);

        body.appendChild(parent);

        Assert.assertEquals(childTriggered.get(), 1);
        Assert.assertEquals(grandChildTriggered.get(), 1);

        body.removeAllChildren();
        parent.getShadowRoot().get().removeAllChildren();

        body.appendChild(parent);
        parent.getShadowRoot().get().appendChild(child);

        Assert.assertEquals(childTriggered.get(), 2);
        Assert.assertEquals(grandChildTriggered.get(), 2);

        registrationHandle.remove();

        body.removeAllChildren();
        body.appendChild(child);

        Assert.assertEquals(childTriggered.get(), 2);
        Assert.assertEquals(grandChildTriggered.get(), 3);
    }

    @Test
    public void detachListener_parentDetach_childListenersTriggered() {
        Element body = new UI().getElement();
        Element parent = ElementFactory.createDiv();
        Element child = ElementFactory.createDiv();
        Element grandChild = ElementFactory.createDiv();

        AtomicInteger triggered = new AtomicInteger();

        Registration registrationHandle = child.addDetachListener(event -> {
            triggered.addAndGet(1);
            Assert.assertEquals(child, event.getSource());
        });

        grandChild.addDetachListener(event -> {
            triggered.addAndGet(1);
            Assert.assertEquals(grandChild, event.getSource());
        });

        child.appendChild(grandChild);
        parent.attachShadow().appendChild(child);
        body.appendChild(parent);

        Assert.assertEquals(triggered.get(), 0);

        body.removeAllChildren();
        Assert.assertEquals(triggered.get(), 2);

        body.appendChild(parent);
        body.removeAllChildren();

        Assert.assertEquals(triggered.get(), 4);

        body.appendChild(parent);
        registrationHandle.remove();

        body.removeAllChildren();

        Assert.assertEquals(triggered.get(), 5);
    }

    @Test
    public void attachListener_eventOrder_childFirst() {
        Element body = new UI().getElement();
        Element parent = ElementFactory.createDiv();
        Element child = ElementFactory.createDiv();
        parent.attachShadow().appendChild(child);

        AtomicBoolean parentAttached = new AtomicBoolean();
        AtomicBoolean childAttached = new AtomicBoolean();

        child.addAttachListener(event -> {
            childAttached.set(true);
            Assert.assertFalse(parentAttached.get());
        });
        parent.addAttachListener(event -> {
            parentAttached.set(true);
            Assert.assertTrue(childAttached.get());
        });

        body.appendChild(parent);

        Assert.assertTrue(parentAttached.get());
        Assert.assertTrue(childAttached.get());
    }

    @Test
    public void detachListener_eventOrder_childFirst() {
        Element body = new UI().getElement();
        Element parent = ElementFactory.createDiv();
        Element child = ElementFactory.createDiv();
        parent.attachShadow().appendChild(child);
        body.appendChild(parent);

        AtomicBoolean parentDetached = new AtomicBoolean();
        AtomicBoolean childDetached = new AtomicBoolean();

        child.addDetachListener(event -> {
            childDetached.set(true);
            Assert.assertFalse(parentDetached.get());
        });
        parent.addDetachListener(event -> {
            parentDetached.set(true);
            Assert.assertTrue(childDetached.get());
        });

        body.removeAllChildren();

        Assert.assertTrue(parentDetached.get());
        Assert.assertTrue(childDetached.get());
    }

    @Test
    public void attachDetach_elementMoved_bothEventsTriggered() {
        ShadowRoot bodyShadow = new UI().getElement().attachShadow();
        Element parent = ElementFactory.createDiv();
        Element child = ElementFactory.createDiv();

        parent.appendChild(child);
        bodyShadow.appendChild(parent);

        AtomicBoolean attached = new AtomicBoolean();
        AtomicBoolean detached = new AtomicBoolean();

        child.addAttachListener(event -> {
            attached.set(true);
            Assert.assertTrue(detached.get());
        });
        child.addDetachListener(event -> {
            detached.set(true);
            Assert.assertFalse(attached.get());
        });

        bodyShadow.appendChild(child);

        Assert.assertTrue(attached.get());
        Assert.assertTrue(detached.get());
    }

    @Test
    public void attachEvent_stateTreeCanFound() {
        ShadowRoot bodyShadow = new UI().getElement().attachShadow();
        Element child = ElementFactory.createDiv();

        AtomicInteger attached = new AtomicInteger();

        child.addAttachListener(event -> {
            Assert.assertNotNull(event.getSource().getNode().getOwner());
            Assert.assertNotEquals(NullOwner.get(),
                    event.getSource().getNode().getOwner());
        });
        child.addAttachListener(event -> attached.incrementAndGet());

        bodyShadow.appendChild(child);
        Assert.assertEquals(1, attached.get());
    }

    @Test
    public void detachEvent_stateTreeCanFound() {
        ShadowRoot bodyShadow = new UI().getElement().attachShadow();
        Element child = ElementFactory.createDiv();
        bodyShadow.appendChild(child);

        AtomicInteger detached = new AtomicInteger();

        child.addDetachListener(event -> {
            Assert.assertNotNull(event.getSource().getNode().getOwner());
            Assert.assertNotEquals(NullOwner.get(),
                    event.getSource().getNode().getOwner());
        });
        child.addDetachListener(event -> detached.incrementAndGet());

        bodyShadow.removeAllChildren();

        Assert.assertEquals(1, detached.get());
    }

    @Test
    public void getParentNode_parentNodeIsNull() {
        ShadowRoot root = createParentNode();
        Assert.assertNull(root.getParentNode());
    }

    @Test
    public void visitOnlyNode_hasDescendants_nodeVisitedAndNoDescendantsVisited() {
        TestNodeVisitor visitor = new TestNodeVisitor(false);

        Map<Node<?>, ElementType> map = new HashMap<>();

        ShadowRoot subject = createHierarchy(map);

        ShadowRootStateProvider.get().visit(subject.getNode(), visitor);

        Assert.assertEquals(1, visitor.getVisited().size());
        Assert.assertEquals(subject,
                visitor.getVisited().keySet().iterator().next());
        Assert.assertEquals(null,
                visitor.getVisited().values().iterator().next());
    }

    @Test
    public void visitOnlyNode_hasDescendants_nodeAndDescendatnsAreVisited() {
        TestNodeVisitor visitor = new TestNodeVisitor(true);

        Map<Node<?>, ElementType> map = new HashMap<>();

        ShadowRoot subject = createHierarchy(map);

        ShadowRootStateProvider.get().visit(subject.getNode(), visitor);

        Assert.assertTrue(map.size() > 1);

        Assert.assertEquals(
                "The collected descendants doesn't match expected descendatns",
                map, visitor.getVisited());
    }

    private ShadowRoot createHierarchy(Map<Node<?>, ElementType> map) {
        Element root = ElementFactory.createDiv();

        ShadowRoot shadowRoot = root.attachShadow();

        map.put(shadowRoot, null);

        Element shadowChild = ElementFactory.createAnchor();
        Element shadowVirtualChild = ElementFactory.createBr();
        shadowRoot.appendChild(shadowChild);
        shadowRoot.appendVirtualChild(shadowVirtualChild);

        map.put(shadowChild, ElementType.REGULAR);
        map.put(shadowVirtualChild, ElementType.VIRTUAL);

        Element virtualGrandChild = ElementFactory.createDiv();

        shadowChild.getStateProvider().appendVirtualChild(shadowChild.getNode(),
                virtualGrandChild, NodeProperties.INJECT_BY_ID, "id");

        map.put(virtualGrandChild, ElementType.VIRTUAL_ATTACHED);

        return shadowRoot;
    }

    @Override
    protected ShadowRoot createParentNode() {
        return ElementFactory.createDiv().attachShadow();
    }
}
