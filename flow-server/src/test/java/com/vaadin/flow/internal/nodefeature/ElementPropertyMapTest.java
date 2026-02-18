/*
 * Copyright 2000-2026 Vaadin Ltd.
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
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.PropertyChangeEvent;
import com.vaadin.flow.dom.PropertyChangeListener;
import com.vaadin.flow.dom.impl.BasicElementStateProvider;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.shared.Registration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ElementPropertyMapTest {

    @Test
    void addPropertyChangeListener_fireServerEvent_listenerIsNotified() {
        listenerIsNotified(false);
    }

    @Test
    void addPropertyChangeListener_fireClientEvent_listenerIsNotified() {
        listenerIsNotified(true);
    }

    @Test
    void removeProperty_fireEvent_listenerIsNotNotified() {
        ElementPropertyMap map = createSimplePropertyMap();

        map.setProperty("foo", "bar");

        AtomicReference<PropertyChangeEvent> event = new AtomicReference<>();
        PropertyChangeListener listener = ev -> {
            assertNull(event.get());
            event.set(ev);
        };
        map.addPropertyChangeListener("foo", listener);

        map.remove("foo");
        assertNull(event.get().getValue());
        assertEquals("bar", event.get().getOldValue());
        assertEquals("foo", event.get().getPropertyName());
        assertEquals(Element.get(map.getNode()), event.get().getSource());
        assertTrue(event.get().isUserOriginated());
    }

    @Test
    void removePropertyChangeListener_fireEvent_listenerIsNotNotified() {
        ElementPropertyMap map = createSimplePropertyMap();
        PropertyChangeListener listener = ev -> {
            fail();
        };
        Registration registration = map.addPropertyChangeListener("foo",
                listener);
        registration.remove();

        // listener is not called. Otherwise its assertion fails.
        map.setProperty("foo", "bar", true);
    }

    @Test
    void addSeveralPropertyChangeListeners_fireEvent_listenersAreNotified() {
        ElementPropertyMap map = createSimplePropertyMap();
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
    void resolveModelList_modelListStateNodeHasReportedFeature() {
        ElementPropertyMap map = createSimplePropertyMap();
        map.resolveModelList("foo");

        StateNode stateNode = (StateNode) map.get("foo");
        assertTrue(stateNode.isReportedFeature(ModelList.class));
    }

    @Test
    void resolveModelMap_modelMapStateNodeHasReportedFeature() {
        ElementPropertyMap map = createSimplePropertyMap();
        map.resolveModelMap("foo");

        StateNode stateNode = (StateNode) map.get("foo");
        assertTrue(stateNode.isReportedFeature(ElementPropertyMap.class));
    }

    @Test
    void put_ignoreSameValue() {
        ElementPropertyMap map = createSimplePropertyMap();

        AtomicReference<Serializable> value = new AtomicReference<>();
        map.addPropertyChangeListener("foo", event -> {
            assertNull(value.get());
            value.set(event.getValue());
        });
        map.setProperty("foo", "bar");

        assertEquals("bar", value.get());

        // Doesn't throw assertion error because listener is not called
        map.setProperty("foo", "bar");
    }

    @Test
    void basicUpdateFromClientFilter() {
        ElementPropertyMap map = createSimplePropertyMap();

        Set<String> clientFilterQueries = new HashSet<>();
        // Allow updating the same property only once
        map.setUpdateFromClientFilter(name -> clientFilterQueries.add(name));

        assertTrue(map.mayUpdateFromClient("foo", "bar"));
        assertFalse(map.mayUpdateFromClient("foo", "bar"));
    }

    @Test
    void updateSynchronizedPropertyDespiteFilter() {
        ElementPropertyMap map = createSimplePropertyMap();

        map.setUpdateFromClientFilter(name -> false);
        assertFalse(map.mayUpdateFromClient("foo", "bar"));

        DomListenerRegistration domListenerRegistration = Element
                .get(map.getNode())
                .addPropertyChangeListener("foo", "event", event -> {
                });
        assertTrue(map.mayUpdateFromClient("foo", "bar"));

        domListenerRegistration.remove();
        assertFalse(map.mayUpdateFromClient("foo", "bar"));

        DomListenerRegistration registration = Element.get(map.getNode())
                .addEventListener("dummy", event -> {
                }).synchronizeProperty("foo");
        assertTrue(map.mayUpdateFromClient("foo", "bar"));

        registration.remove();
        assertFalse(map.mayUpdateFromClient("foo", "bar"));
    }

    @Test
    void updateFromClientFilter_replaceFilter() {
        ElementPropertyMap map = createSimplePropertyMap();

        map.setUpdateFromClientFilter("foo"::equals);

        assertTrue(map.mayUpdateFromClient("foo", "a"));
        assertFalse(map.mayUpdateFromClient("bar", "a"));

        map.setUpdateFromClientFilter("bar"::equals);

        assertFalse(map.mayUpdateFromClient("foo", "a"));
        assertTrue(map.mayUpdateFromClient("bar", "a"));
    }

    @Test
    void childPropertyUpdateFilter_setFilterBeforeChild() {
        ElementPropertyMap map = createSimplePropertyMap();
        StateNode child = new StateNode(ElementPropertyMap.class);
        ElementPropertyMap childModel = ElementPropertyMap.getModel(child);

        map.setUpdateFromClientFilter("foo.bar"::equals);
        map.put("foo", child);

        assertTrue(childModel.mayUpdateFromClient("bar", "a"));
        assertFalse(childModel.mayUpdateFromClient("baz", "a"));
    }

    @Test
    void deferredUpdateFromClient_filterAllowsUpdate()
            throws PropertyChangeDeniedException {
        ElementPropertyMap map = createSimplePropertyMap();
        StateNode child = new StateNode(ElementPropertyMap.class);
        ElementPropertyMap childModel = ElementPropertyMap.getModel(child);

        map.setUpdateFromClientFilter("foo.bar"::equals);
        map.put("foo", child);

        assertDeferredUpdate_putResult(childModel, "bar");
    }

    @Test
    void deferredUpdateFromClient_noFilter_throws()
            throws PropertyChangeDeniedException {
        ElementPropertyMap map = createSimplePropertyMap();
        StateNode child = new StateNode(ElementPropertyMap.class);
        ElementPropertyMap childModel = ElementPropertyMap.getModel(child);

        map.put("foo", child);
        assertThrows(PropertyChangeDeniedException.class,
                () -> childModel.deferredUpdateFromClient("bar", "a"));
    }

    @Test
    void deferredUpdateFromClient_filterDisallowsUpdate()
            throws PropertyChangeDeniedException {
        ElementPropertyMap map = createSimplePropertyMap();
        StateNode child = new StateNode(ElementPropertyMap.class);
        ElementPropertyMap childModel = ElementPropertyMap.getModel(child);

        map.setUpdateFromClientFilter(key -> false);
        map.put("foo", child);

        assertDeferredUpdate_noOp(childModel, "bar");
    }

    @Test
    void listChildPropertyUpdateFilter_setFilterBeforeChild() {
        ElementPropertyMap map = createSimplePropertyMap();
        ModelList list = map.resolveModelList("foo");
        StateNode child = new StateNode(ElementPropertyMap.class);

        map.setUpdateFromClientFilter("foo.bar"::equals);
        list.add(child);

        ElementPropertyMap childModel = ElementPropertyMap.getModel(child);

        assertTrue(childModel.mayUpdateFromClient("bar", "a"));
        assertFalse(childModel.mayUpdateFromClient("baz", "a"));
    }

    @Test
    void deferredUpdateFromClient_listChild_filterAllowsUpdate()
            throws PropertyChangeDeniedException {
        ElementPropertyMap map = createSimplePropertyMap();
        ModelList list = map.resolveModelList("foo");
        StateNode child = new StateNode(ElementPropertyMap.class);

        map.setUpdateFromClientFilter("foo.bar"::equals);
        list.add(child);

        ElementPropertyMap childModel = ElementPropertyMap.getModel(child);

        assertDeferredUpdate_putResult(childModel, "bar");
    }

    @Test
    void deferredUpdateFromClient_listItem_noFilter_throws()
            throws PropertyChangeDeniedException {
        ElementPropertyMap map = createSimplePropertyMap();
        ModelList list = map.resolveModelList("foo");
        StateNode child = new StateNode(ElementPropertyMap.class);

        list.add(child);

        ElementPropertyMap childModel = ElementPropertyMap.getModel(child);

        assertThrows(PropertyChangeDeniedException.class,
                () -> childModel.deferredUpdateFromClient("bar", "a"));
    }

    @Test
    void deferredUpdateFromClient_listChild_filterDisallowsUpdate()
            throws PropertyChangeDeniedException {
        ElementPropertyMap map = createSimplePropertyMap();
        ModelList list = map.resolveModelList("foo");
        StateNode child = new StateNode(ElementPropertyMap.class);

        map.setUpdateFromClientFilter(key -> false);
        list.add(child);

        ElementPropertyMap childModel = ElementPropertyMap.getModel(child);

        assertDeferredUpdate_noOp(childModel, "bar");
    }

    @Test
    void childPropertyUpdateFilter_setFilterAfterChild() {
        ElementPropertyMap map = createSimplePropertyMap();
        StateNode child = new StateNode(ElementPropertyMap.class);
        ElementPropertyMap childModel = ElementPropertyMap.getModel(child);

        map.put("foo", child);
        map.setUpdateFromClientFilter("foo.bar"::equals);

        assertTrue(childModel.mayUpdateFromClient("bar", "a"));
        assertFalse(childModel.mayUpdateFromClient("baz", "a"));
    }

    @Test
    void childPropertyUpdateFilter_renameProperty() {
        ElementPropertyMap map = createSimplePropertyMap();
        StateNode child = new StateNode(ElementPropertyMap.class);
        ElementPropertyMap childModel = ElementPropertyMap.getModel(child);

        map.put("foo", child);
        map.setUpdateFromClientFilter("foo.bar"::equals);

        assertTrue(childModel.mayUpdateFromClient("bar", "a"));

        map.remove("foo");
        assertFalse(childModel.mayUpdateFromClient("bar", "a"));

        map.put("bar", child);
        assertFalse(childModel.mayUpdateFromClient("bar", "a"));
    }

    @Test
    void childPropertyUpdateFilter_deepNesting() {
        ElementPropertyMap map = createSimplePropertyMap();

        map.setUpdateFromClientFilter("a.b.c.d.e.f.g.h.i.j.property"::equals);

        for (int i = 0; i < 10; i++) {
            StateNode child = new StateNode(ElementPropertyMap.class);
            map.setProperty(Character.toString((char) ('a' + i)), child);

            map = ElementPropertyMap.getModel(child);
        }

        assertTrue(map.mayUpdateFromClient("property", "foo"));
    }

    @Test
    void deferredUpdateFromClient_updateNotAllowed_throw()
            throws PropertyChangeDeniedException {
        ElementPropertyMap map = createSimplePropertyMap();

        assertThrows(PropertyChangeDeniedException.class,
                () -> map.deferredUpdateFromClient("foo", "value"));
    }

    @Test
    void deferredUpdateFromClient_filterDisallowUpdate_eventIsSynchronized()
            throws PropertyChangeDeniedException {
        ElementPropertyMap map = createSimplePropertyMap();
        Element.get(map.getNode()).addEventListener("dummy", event -> {

        }).synchronizeProperty("foo");

        map.setUpdateFromClientFilter(key -> false);

        assertDeferredUpdate_putResult(map, "foo");
    }

    @Test
    void deferredUpdateFromClient_filterAllowsUpdate_propertyIsForbidden_throw()
            throws PropertyChangeDeniedException {
        ElementPropertyMap map = createSimplePropertyMap();
        map.put("classList", "a");

        map.setUpdateFromClientFilter(key -> true);

        assertThrows(PropertyChangeDeniedException.class,
                () -> map.deferredUpdateFromClient("classList", "value"));
    }

    @Test
    void deferredUpdateFromClient_clientFiltersOutUpdate_noOpRunnable()
            throws PropertyChangeDeniedException {
        ElementPropertyMap map = createSimplePropertyMap();
        map.setUpdateFromClientFilter(name -> !name.equals("foo"));

        AtomicReference<PropertyChangeEvent> eventCapture = new AtomicReference<>();
        map.addPropertyChangeListener("foo", eventCapture::set);

        Runnable runnable = map.deferredUpdateFromClient("foo", "value");
        MatcherAssert.assertThat(runnable.getClass().getName(),
                CoreMatchers.not(CoreMatchers.equalTo(
                        ElementPropertyMap.class.getName() + "$PutResult")));
        runnable.run();
        assertNull(eventCapture.get());
    }

    @Test
    void deferredUpdateFromClient_clientFilterAcceptUpdate_putResultRunnable()
            throws PropertyChangeDeniedException {
        ElementPropertyMap map = createSimplePropertyMap();
        map.setUpdateFromClientFilter(name -> name.equals("foo"));

        AtomicReference<PropertyChangeEvent> eventCapture = new AtomicReference<>();
        map.addPropertyChangeListener("foo", eventCapture::set);

        Runnable runnable = assertDeferredUpdate_putResult(map, "foo");
        runnable.run();
        assertNotNull(eventCapture.get());
    }

    @Test
    void producePutChange_innerHTMLProperty_valueIsTheSame_returnsTrue() {
        ElementPropertyMap map = createSimplePropertyMap();
        map.setProperty("innerHTML", "foo");

        assertTrue(map.producePutChange("innerHTML", true, "foo"));
        assertTrue(map.producePutChange("innerHTML", false, "foo"));
    }

    @Test
    void producePutChange_notInnerHTMLProperty_valueIsTheSame_returnsFalse() {
        ElementPropertyMap map = createSimplePropertyMap();
        map.setProperty("foo", "bar");

        assertFalse(map.producePutChange("foo", true, "bar"));
    }

    private void listenerIsNotified(boolean clientEvent) {
        ElementPropertyMap map = createSimplePropertyMap();
        StateNode node = map.getNode();

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

    private Runnable assertDeferredUpdate_putResult(ElementPropertyMap map,
            String property) throws PropertyChangeDeniedException {
        Runnable runnable = map.deferredUpdateFromClient(property, "a");
        MatcherAssert.assertThat(runnable.getClass().getName(), CoreMatchers
                .equalTo(ElementPropertyMap.class.getName() + "$PutResult"));
        return runnable;
    }

    private void assertDeferredUpdate_noOp(ElementPropertyMap map,
            String property) throws PropertyChangeDeniedException {
        Runnable runnable = map.deferredUpdateFromClient(property, "a");
        MatcherAssert.assertThat(runnable.getClass().getName(),
                CoreMatchers.not(CoreMatchers.equalTo(
                        ElementPropertyMap.class.getName() + "$PutResult")));
    }

    private static ElementPropertyMap createSimplePropertyMap() {
        return BasicElementStateProvider.createStateNode("div")
                .getFeature(ElementPropertyMap.class);
    }
}
