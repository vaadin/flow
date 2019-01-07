/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.dom.DomListenerRegistration;
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
    public void removeProperty_fireEvent_listenerIsNotNotified() {
        ElementPropertyMap map = createSimplePropertyMap();

        map.setProperty("foo", "bar");

        AtomicReference<PropertyChangeEvent> event = new AtomicReference<>();
        PropertyChangeListener listener = ev -> {
            Assert.assertNull(event.get());
            event.set(ev);
        };
        map.addPropertyChangeListener("foo", listener);

        map.remove("foo");
        Assert.assertNull(event.get().getValue());
        Assert.assertEquals("bar", event.get().getOldValue());
        Assert.assertEquals("foo", event.get().getPropertyName());
        Assert.assertEquals(Element.get(map.getNode()),
                event.get().getSource());
        Assert.assertTrue(event.get().isUserOriginated());
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

        DomListenerRegistration registration = Element.get(map.getNode())
                .addEventListener("dummy", event -> {
                }).synchronizeProperty("foo");
        Assert.assertTrue(map.mayUpdateFromClient("foo", "bar"));

        registration.remove();
        Assert.assertFalse(map.mayUpdateFromClient("foo", "bar"));
    }

    @Test
    public void updateFromClientFilter_replaceFilter() {
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
    public void deferredUpdateFromClient_filterAllowsUpdate() {
        ElementPropertyMap map = createSimplePropertyMap();
        StateNode child = new StateNode(ElementPropertyMap.class);
        ElementPropertyMap childModel = ElementPropertyMap.getModel(child);

        map.setUpdateFromClientFilter("foo.bar"::equals);
        map.put("foo", child);

        assertDeferredUpdate_putResult(childModel, "bar");
    }

    @Test(expected = IllegalArgumentException.class)
    public void deferredUpdateFromClient_noFilter_throws() {
        ElementPropertyMap map = createSimplePropertyMap();
        StateNode child = new StateNode(ElementPropertyMap.class);
        ElementPropertyMap childModel = ElementPropertyMap.getModel(child);

        map.put("foo", child);
        childModel.deferredUpdateFromClient("bar", "a");
    }

    @Test
    public void deferredUpdateFromClient_filterDisallowsUpdate() {
        ElementPropertyMap map = createSimplePropertyMap();
        StateNode child = new StateNode(ElementPropertyMap.class);
        ElementPropertyMap childModel = ElementPropertyMap.getModel(child);

        map.setUpdateFromClientFilter(key -> false);
        map.put("foo", child);

        assertDeferredUpdate_noOp(childModel, "bar");
    }

    @Test
    public void listChildPropertyUpdateFilter_setFilterBeforeChild() {
        ElementPropertyMap map = createSimplePropertyMap();
        ModelList list = map.resolveModelList("foo");
        StateNode child = new StateNode(ElementPropertyMap.class);

        map.setUpdateFromClientFilter("foo.bar"::equals);
        list.add(child);

        ElementPropertyMap childModel = ElementPropertyMap.getModel(child);

        Assert.assertTrue(childModel.mayUpdateFromClient("bar", "a"));
        Assert.assertFalse(childModel.mayUpdateFromClient("baz", "a"));
    }

    @Test
    public void deferredUpdateFromClient_listChild_filterAllowsUpdate() {
        ElementPropertyMap map = createSimplePropertyMap();
        ModelList list = map.resolveModelList("foo");
        StateNode child = new StateNode(ElementPropertyMap.class);

        map.setUpdateFromClientFilter("foo.bar"::equals);
        list.add(child);

        ElementPropertyMap childModel = ElementPropertyMap.getModel(child);

        assertDeferredUpdate_putResult(childModel, "bar");
    }

    @Test(expected = IllegalArgumentException.class)
    public void deferredUpdateFromClient_listItem_noFilter_throws() {
        ElementPropertyMap map = createSimplePropertyMap();
        ModelList list = map.resolveModelList("foo");
        StateNode child = new StateNode(ElementPropertyMap.class);

        list.add(child);

        ElementPropertyMap childModel = ElementPropertyMap.getModel(child);

        childModel.deferredUpdateFromClient("bar", "a");
    }

    @Test
    public void deferredUpdateFromClient_listChild_filterDisallowsUpdate() {
        ElementPropertyMap map = createSimplePropertyMap();
        ModelList list = map.resolveModelList("foo");
        StateNode child = new StateNode(ElementPropertyMap.class);

        map.setUpdateFromClientFilter(key -> false);
        list.add(child);

        ElementPropertyMap childModel = ElementPropertyMap.getModel(child);

        assertDeferredUpdate_noOp(childModel, "bar");
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

    @Test(expected = IllegalArgumentException.class)
    public void deferredUpdateFromClient_updateNotAllowed_throw() {
        ElementPropertyMap map = createSimplePropertyMap();

        map.deferredUpdateFromClient("foo", "value");
    }

    @Test
    public void deferredUpdateFromClient_filterDisallowUpdate_propertyIsSynchronized() {
        ElementPropertyMap map = createSimplePropertyMap();
        map.getNode().getFeature(SynchronizedPropertiesList.class).add("foo");

        map.setUpdateFromClientFilter(key -> false);

        map.deferredUpdateFromClient("foo", "value");
        assertDeferredUpdate_putResult(map, "foo");
    }

    @Test
    public void deferredUpdateFromClient_filterDisallowUpdate_eventIsSynchronized() {
        ElementPropertyMap map = createSimplePropertyMap();
        Element.get(map.getNode()).addEventListener("dummy", event -> {

        }).synchronizeProperty("foo");

        map.setUpdateFromClientFilter(key -> false);

        assertDeferredUpdate_putResult(map, "foo");
    }

    @Test(expected = IllegalArgumentException.class)
    public void deferredUpdateFromClient_filterAllowsUpdate_propertyIsForbidden_throw() {
        ElementPropertyMap map = createSimplePropertyMap();
        map.put("classList", "a");

        map.setUpdateFromClientFilter(key -> true);

        map.deferredUpdateFromClient("classList", "value");
    }

    @Test
    public void deferredUpdateFromClient_clientFiltersOutUpdate_noOpRunnable() {
        ElementPropertyMap map = createSimplePropertyMap();
        map.setUpdateFromClientFilter(name -> !name.equals("foo"));

        AtomicReference<PropertyChangeEvent> eventCapture = new AtomicReference<>();
        map.addPropertyChangeListener("foo", eventCapture::set);

        Runnable runnable = map.deferredUpdateFromClient("foo", "value");
        Assert.assertThat(runnable.getClass().getName(),
                CoreMatchers.not(CoreMatchers.equalTo(
                        ElementPropertyMap.class.getName() + "$PutResult")));
        runnable.run();
        Assert.assertNull(eventCapture.get());
    }

    @Test
    public void deferredUpdateFromClient_clientFilterAcceptUpdate_putResultRunnable() {
        ElementPropertyMap map = createSimplePropertyMap();
        map.setUpdateFromClientFilter(name -> name.equals("foo"));

        AtomicReference<PropertyChangeEvent> eventCapture = new AtomicReference<>();
        map.addPropertyChangeListener("foo", eventCapture::set);

        Runnable runnable = assertDeferredUpdate_putResult(map, "foo");
        runnable.run();
        Assert.assertNotNull(eventCapture.get());
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

    private Runnable assertDeferredUpdate_putResult(ElementPropertyMap map,
            String property) {
        Runnable runnable = map.deferredUpdateFromClient(property, "a");
        Assert.assertThat(runnable.getClass().getName(), CoreMatchers
                .equalTo(ElementPropertyMap.class.getName() + "$PutResult"));
        return runnable;
    }

    private void assertDeferredUpdate_noOp(ElementPropertyMap map,
            String property) {
        Runnable runnable = map.deferredUpdateFromClient(property, "a");
        Assert.assertThat(runnable.getClass().getName(),
                CoreMatchers.not(CoreMatchers.equalTo(
                        ElementPropertyMap.class.getName() + "$PutResult")));
    }

    private static ElementPropertyMap createSimplePropertyMap() {
        return BasicElementStateProvider.createStateNode("div")
                .getFeature(ElementPropertyMap.class);
    }
}
