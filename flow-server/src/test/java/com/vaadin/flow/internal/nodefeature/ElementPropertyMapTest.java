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
package com.vaadin.flow.internal.nodefeature;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.PropertyChangeEvent;
import com.vaadin.flow.dom.PropertyChangeListener;
import com.vaadin.flow.dom.impl.BasicElementStateProvider;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.shared.Registration;

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
        ElementPropertyMap map = createSimplePropertyMap();
        PropertyChangeListener listener = ev -> {
            Assert.fail();
        };
        Registration registration = map.addPropertyChangeListener("foo",
                listener);
        registration.remove();

        // listener is not called. Otherwise its assertion fails.
        map.setProperty("foo", "bar", true);
    }

    @Test
    public void addSeveralPropertyChangeListeners_fireEvent_listenersAreNotified() {
        ElementPropertyMap map = createSimplePropertyMap();
        AtomicBoolean first = new AtomicBoolean();
        AtomicBoolean second = new AtomicBoolean();
        PropertyChangeListener listener1 = ev -> first.set(!first.get());
        PropertyChangeListener listener2 = ev -> second.set(!second.get());
        map.addPropertyChangeListener("foo", listener1);
        map.addPropertyChangeListener("foo", listener2);

        map.setProperty("foo", "bar", true);

        Assert.assertTrue(first.get());
        Assert.assertTrue(second.get());
    }

    @Test
    public void resolveModelList_modelListStateNodeHasReportedFeature() {
        ElementPropertyMap map = createSimplePropertyMap();
        map.resolveModelList("foo");

        StateNode stateNode = (StateNode) map.get("foo");
        Assert.assertTrue(stateNode.isReportedFeature(ModelList.class));
    }

    @Test
    public void resolveModelMap_modelMapStateNodeHasReportedFeature() {
        ElementPropertyMap map = createSimplePropertyMap();
        map.resolveModelMap("foo");

        StateNode stateNode = (StateNode) map.get("foo");
        Assert.assertTrue(
                stateNode.isReportedFeature(ElementPropertyMap.class));
    }

    @Test
    public void put_ignoreSameValue() {
        ElementPropertyMap map = createSimplePropertyMap();

        AtomicReference<Serializable> value = new AtomicReference<>();
        map.addPropertyChangeListener("foo", event -> {
            Assert.assertNull(value.get());
            value.set(event.getValue());
        });
        map.setProperty("foo", "bar");

        Assert.assertEquals("bar", value.get());

        // Doesn't throw assertion error because listener is not called
        map.setProperty("foo", "bar");
    }

    @Test
    public void basicUpdateFromClientFilter() {
        ElementPropertyMap map = createSimplePropertyMap();

        Set<String> clientFilterQueries = new HashSet<>();
        // Allow updating the same property only once
        map.setUpdateFromClientFilter(name -> clientFilterQueries.add(name));

        Assert.assertTrue(map.mayUpdateFromClient("foo", "bar"));
        Assert.assertFalse(map.mayUpdateFromClient("foo", "bar"));
    }

    @Test
    public void updateSynchronizedPropertyDespiteFilter() {
        ElementPropertyMap map = createSimplePropertyMap();

        map.setUpdateFromClientFilter(name -> false);
        Assert.assertFalse(map.mayUpdateFromClient("foo", "bar"));

        Element.get(map.getNode()).synchronizeProperty("foo", "event");
        Assert.assertTrue(map.mayUpdateFromClient("foo", "bar"));

        Element.get(map.getNode()).removeSynchronizedProperty("foo");
        Assert.assertFalse(map.mayUpdateFromClient("foo", "bar"));
    }

    @Test
    public void childPropertyUpdateFilter_replaceFilter() {
        ElementPropertyMap map = createSimplePropertyMap();

        map.setUpdateFromClientFilter("foo"::equals);

        Assert.assertTrue(map.mayUpdateFromClient("foo", "a"));
        Assert.assertFalse(map.mayUpdateFromClient("bar", "a"));

        map.setUpdateFromClientFilter("bar"::equals);

        Assert.assertFalse(map.mayUpdateFromClient("foo", "a"));
        Assert.assertTrue(map.mayUpdateFromClient("bar", "a"));
    }

    @Test
    public void childPropertyUpdateFilter_setFilterBeforeChild() {
        ElementPropertyMap map = createSimplePropertyMap();
        StateNode child = new StateNode(ElementPropertyMap.class);
        ElementPropertyMap childModel = ElementPropertyMap.getModel(child);

        map.setUpdateFromClientFilter("foo.bar"::equals);
        map.put("foo", child);

        Assert.assertTrue(childModel.mayUpdateFromClient("bar", "a"));
        Assert.assertFalse(childModel.mayUpdateFromClient("baz", "a"));
    }

    @Test
    public void childPropertyUpdateFilter_setFilterAfterChild() {
        ElementPropertyMap map = createSimplePropertyMap();
        StateNode child = new StateNode(ElementPropertyMap.class);
        ElementPropertyMap childModel = ElementPropertyMap.getModel(child);

        map.put("foo", child);
        map.setUpdateFromClientFilter("foo.bar"::equals);

        Assert.assertTrue(childModel.mayUpdateFromClient("bar", "a"));
        Assert.assertFalse(childModel.mayUpdateFromClient("baz", "a"));
    }

    @Test
    public void childPropertyUpdateFilter_renameProperty() {
        ElementPropertyMap map = createSimplePropertyMap();
        StateNode child = new StateNode(ElementPropertyMap.class);
        ElementPropertyMap childModel = ElementPropertyMap.getModel(child);

        map.put("foo", child);
        map.setUpdateFromClientFilter("foo.bar"::equals);

        Assert.assertTrue(childModel.mayUpdateFromClient("bar", "a"));

        map.remove("foo");
        Assert.assertFalse(childModel.mayUpdateFromClient("bar", "a"));

        map.put("bar", child);
        Assert.assertFalse(childModel.mayUpdateFromClient("bar", "a"));
    }

    @Test
    public void childPropertyUpdateFilter_deepNesting() {
        ElementPropertyMap map = createSimplePropertyMap();

        map.setUpdateFromClientFilter("a.b.c.d.e.f.g.h.i.j.property"::equals);

        for (int i = 0; i < 10; i++) {
            StateNode child = new StateNode(ElementPropertyMap.class);
            map.setProperty(Character.toString((char) ('a' + i)), child);

            map = ElementPropertyMap.getModel(child);
        }

        Assert.assertTrue(map.mayUpdateFromClient("property", "foo"));
    }

    private void listenerIsNotified(boolean clientEvent) {
        ElementPropertyMap map = createSimplePropertyMap();
        StateNode node = map.getNode();

        AtomicReference<PropertyChangeEvent> event = new AtomicReference<>();
        PropertyChangeListener listener = ev -> {
            Assert.assertNull(event.get());
            event.set(ev);
        };
        map.addPropertyChangeListener("foo", listener);
        map.setProperty("foo", "bar", !clientEvent);

        Assert.assertNull(event.get().getOldValue());
        Assert.assertEquals("bar", event.get().getValue());
        Assert.assertEquals("foo", event.get().getPropertyName());
        Assert.assertEquals(Element.get(node), event.get().getSource());
        Assert.assertEquals(clientEvent, event.get().isUserOriginated());

        // listener is not called. Otherwise its assertion fails.
        map.setProperty("bar", "foo");
    }

    private static ElementPropertyMap createSimplePropertyMap() {
        return BasicElementStateProvider.createStateNode("div")
                .getFeature(ElementPropertyMap.class);
    }
}
