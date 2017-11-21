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
package com.vaadin.flow.nodefeature;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import com.vaadin.flow.StateNode;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.impl.BasicElementStateProvider;
import com.vaadin.shared.Registration;
import com.vaadin.ui.event.PropertyChangeEvent;
import com.vaadin.ui.event.PropertyChangeListener;

public class ElementPropertyMapTest {

    @Test
    public void addPropertyChangeListener_fireServerEvent_listenerIsNotified() {
        listenerIsNotified(false);
    }

    @Test
    public void addPropertyChangeListener_fireClientEvent_listenerIsNotified() {
        listenerIsNotified(true);
    }

    @Test
    public void removePropertyChangeListener_fireEvent_listenerIsNotNotified() {
        StateNode node = BasicElementStateProvider.createStateNode("div");
        ElementPropertyMap map = node.getFeature(ElementPropertyMap.class);
        PropertyChangeListener listener = ev -> {
            fail();
        };
        Registration registration = map
                .addPropertyChangeListener("foo", listener);
        registration.remove();

        // listener is not called. Otherwise its assertion fails.
        map.setProperty("foo", "bar", true);
    }

    @Test
    public void addSeveralPropertyChangeListeners_fireEvent_listenersAreNotified() {
        StateNode node = BasicElementStateProvider.createStateNode("div");
        ElementPropertyMap map = node.getFeature(ElementPropertyMap.class);
        AtomicBoolean first = new AtomicBoolean();
        AtomicBoolean second = new AtomicBoolean();
        PropertyChangeListener listener1 = ev -> first.set(!first.get());
        PropertyChangeListener listener2 = ev -> second.set(!second.get());
        map.addPropertyChangeListener("foo", listener1);
        map.addPropertyChangeListener("foo", listener2);

        map.setProperty("foo", "bar", true);

        assertTrue(first.get());
        assertTrue(second.get());
    }

    @Test
    public void resolveModelList_modelListStateNodeHasReportedFeature() {
        StateNode node = BasicElementStateProvider.createStateNode("div");
        ElementPropertyMap map = node.getFeature(ElementPropertyMap.class);
        map.resolveModelList("foo");

        StateNode stateNode = (StateNode) map.get("foo");
        assertTrue(stateNode.isReportedFeature(ModelList.class));
    }

    @Test
    public void resolveModelMap_modelMapStateNodeHasReportedFeature() {
        StateNode node = BasicElementStateProvider.createStateNode("div");
        ElementPropertyMap map = node.getFeature(ElementPropertyMap.class);
        map.resolveModelMap("foo");

        StateNode stateNode = (StateNode) map.get("foo");
        assertTrue(
                stateNode.isReportedFeature(ElementPropertyMap.class));
    }

    private void listenerIsNotified(boolean clientEvent) {
        StateNode node = BasicElementStateProvider.createStateNode("div");
        ElementPropertyMap map = node.getFeature(ElementPropertyMap.class);
        AtomicReference<PropertyChangeEvent> event = new AtomicReference<>();
        PropertyChangeListener listener = ev -> {
            assertNull(event.get());
            event.set(ev);
        };
        map.addPropertyChangeListener("foo", listener);
        map.setProperty("foo", "bar", !clientEvent);

        assertNull(event.get().getOldValue());
        assertEquals("bar", event.get().getValue());
        assertEquals("foo", event.get().getPropertyName());
        assertEquals(Element.get(node), event.get().getSource());
        assertEquals(clientEvent, event.get().isUserOriginated());

        // listener is not called. Otherwise its assertion fails.
        map.setProperty("bar", "foo");
    }

}
