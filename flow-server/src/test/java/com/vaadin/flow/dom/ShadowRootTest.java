/*
 * Copyright 2000-2017 Vaadin Ltd.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.vaadin.flow.NullOwner;
import com.vaadin.shared.Registration;
import com.vaadin.ui.UI;

public class ShadowRootTest extends AbstractNodeTest {

    @Test
    public void publicElementMethodsShouldReturnElement() {
        HashSet<String> ignore = new HashSet<>();
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
                e -> fail("Child should not be detached"));
        parent.insertChild(0, child);
    }

    @Test
    public void equalsSelf() {
        ShadowRoot root = createParentNode();
        assertTrue(root.equals(root));
    }

    @Test
    public void notEqualsNull() {
        ShadowRoot root = createParentNode();
        assertFalse(root.equals(null));
    }

    @Test
    public void attachListener_parentAttach_childListenersTriggered() {
        Element body = new UI().getElement();
        Element parent = ElementFactory.createDiv();
        Element child = ElementFactory.createDiv();
        Element grandChild = ElementFactory.createDiv();

        AtomicInteger childTriggered = new AtomicInteger();
        AtomicInteger grandChildTriggered = new AtomicInteger();

        Registration registrationHandle = child
                .addAttachListener(event -> {
                    childTriggered.addAndGet(1);
                });
        child.addAttachListener(event -> {
            assertEquals(child, event.getSource());
        });
        grandChild.addAttachListener(event -> {
            grandChildTriggered.addAndGet(1);
        });
        grandChild.addAttachListener(event -> {
            assertEquals(grandChild, event.getSource());
        });

        parent.attachShadow().appendChild(child);
        child.appendChild(grandChild);

        assertEquals(childTriggered.get(), 0);
        assertEquals(grandChildTriggered.get(), 0);

        body.appendChild(parent);

        assertEquals(childTriggered.get(), 1);
        assertEquals(grandChildTriggered.get(), 1);

        body.removeAllChildren();
        parent.getShadowRoot().get().removeAllChildren();

        body.appendChild(parent);
        parent.getShadowRoot().get().appendChild(child);

        assertEquals(childTriggered.get(), 2);
        assertEquals(grandChildTriggered.get(), 2);

        registrationHandle.remove();

        body.removeAllChildren();
        body.appendChild(child);

        assertEquals(childTriggered.get(), 2);
        assertEquals(grandChildTriggered.get(), 3);
    }

    @Test
    public void detachListener_parentDetach_childListenersTriggered() {
        Element body = new UI().getElement();
        Element parent = ElementFactory.createDiv();
        Element child = ElementFactory.createDiv();
        Element grandChild = ElementFactory.createDiv();

        AtomicInteger triggered = new AtomicInteger();

        Registration registrationHandle = child
                .addDetachListener(event -> {
                    triggered.addAndGet(1);
                    assertEquals(child, event.getSource());
                });

        grandChild.addDetachListener(event -> {
            triggered.addAndGet(1);
            assertEquals(grandChild, event.getSource());
        });

        child.appendChild(grandChild);
        parent.attachShadow().appendChild(child);
        body.appendChild(parent);

        assertEquals(triggered.get(), 0);

        body.removeAllChildren();
        assertEquals(triggered.get(), 2);

        body.appendChild(parent);
        body.removeAllChildren();

        assertEquals(triggered.get(), 4);

        body.appendChild(parent);
        registrationHandle.remove();

        body.removeAllChildren();

        assertEquals(triggered.get(), 5);
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
            assertFalse(parentAttached.get());
        });
        parent.addAttachListener(event -> {
            parentAttached.set(true);
            assertTrue(childAttached.get());
        });

        body.appendChild(parent);

        assertTrue(parentAttached.get());
        assertTrue(childAttached.get());
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
            assertFalse(parentDetached.get());
        });
        parent.addDetachListener(event -> {
            parentDetached.set(true);
            assertTrue(childDetached.get());
        });

        body.removeAllChildren();

        assertTrue(parentDetached.get());
        assertTrue(childDetached.get());
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
            assertTrue(detached.get());
        });
        child.addDetachListener(event -> {
            detached.set(true);
            assertFalse(attached.get());
        });

        bodyShadow.appendChild(child);

        assertTrue(attached.get());
        assertTrue(detached.get());
    }

    @Test
    public void attachEvent_stateTreeCanFound() {
        ShadowRoot bodyShadow = new UI().getElement().attachShadow();
        Element child = ElementFactory.createDiv();

        AtomicInteger attached = new AtomicInteger();

        child.addAttachListener(event -> {
            assertNotNull(event.getSource().getNode().getOwner());
            assertNotEquals(NullOwner.get(),
                    event.getSource().getNode().getOwner());
        });
        child.addAttachListener(event -> attached.incrementAndGet());

        bodyShadow.appendChild(child);
        assertEquals(1, attached.get());
    }

    @Test
    public void detachEvent_stateTreeCanFound() {
        ShadowRoot bodyShadow = new UI().getElement().attachShadow();
        Element child = ElementFactory.createDiv();
        bodyShadow.appendChild(child);

        AtomicInteger detached = new AtomicInteger();

        child.addDetachListener(event -> {
            assertNotNull(event.getSource().getNode().getOwner());
            assertNotEquals(NullOwner.get(),
                    event.getSource().getNode().getOwner());
        });
        child.addDetachListener(event -> detached.incrementAndGet());

        bodyShadow.removeAllChildren();

        assertEquals(1, detached.get());
    }

    @Test
    public void getParentNode_parentNodeIsNull() {
        ShadowRoot root = createParentNode();
        assertNull(root.getParentNode());
    }

    @Override
    protected ShadowRoot createParentNode() {
        return ElementFactory.createDiv().attachShadow();
    }
}
