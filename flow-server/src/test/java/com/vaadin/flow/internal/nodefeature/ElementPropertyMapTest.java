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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.PropertyChangeEvent;
import com.vaadin.flow.dom.PropertyChangeListener;
import com.vaadin.flow.dom.impl.BasicElementStateProvider;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.shared.Registration;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ElementPropertyMapTest {

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
            Assertions.assertNull(event.get());
            event.set(ev);
        };
        map.addPropertyChangeListener("foo", listener);

        map.remove("foo");
        Assertions.assertNull(event.get().getValue());
        Assertions.assertEquals("bar", event.get().getOldValue());
        Assertions.assertEquals("foo", event.get().getPropertyName());
        Assertions.assertEquals(Element.get(map.getNode()),
                event.get().getSource());
        Assertions.assertTrue(event.get().isUserOriginated());
    }

    @Test
    public void removePropertyChangeListener_fireEvent_listenerIsNotNotified() {
        ElementPropertyMap map = createSimplePropertyMap();
        PropertyChangeListener listener = ev -> {
            Assertions.fail();
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

        Assertions.assertTrue(first.get());
        Assertions.assertTrue(second.get());
    }

    @Test
    public void resolveModelList_modelListStateNodeHasReportedFeature() {
        ElementPropertyMap map = createSimplePropertyMap();
        map.resolveModelList("foo");

        StateNode stateNode = (StateNode) map.get("foo");
        Assertions.assertTrue(stateNode.isReportedFeature(ModelList.class));
    }

    @Test
    public void resolveModelMap_modelMapStateNodeHasReportedFeature() {
        ElementPropertyMap map = createSimplePropertyMap();
        map.resolveModelMap("foo");

        StateNode stateNode = (StateNode) map.get("foo");
        Assertions.assertTrue(
                stateNode.isReportedFeature(ElementPropertyMap.class));
    }

    @Test
    public void put_ignoreSameValue() {
        ElementPropertyMap map = createSimplePropertyMap();

        AtomicReference<Serializable> value = new AtomicReference<>();
        map.addPropertyChangeListener("foo", event -> {
            Assertions.assertNull(value.get());
            value.set(event.getValue());
        });
        map.setProperty("foo", "bar");

        Assertions.assertEquals("bar", value.get());

        // Doesn't throw assertion error because listener is not called
        map.setProperty("foo", "bar");
    }

    @Test
    public void basicUpdateFromClientFilter() {
        ElementPropertyMap map = createSimplePropertyMap();

        Set<String> clientFilterQueries = new HashSet<>();
        // Allow updating the same property only once
        map.setUpdateFromClientFilter(name -> clientFilterQueries.add(name));

        Assertions.assertTrue(map.mayUpdateFromClient("foo", "bar"));
        Assertions.assertFalse(map.mayUpdateFromClient("foo", "bar"));
    }

    @Test
    public void updateSynchronizedPropertyDespiteFilter() {
        ElementPropertyMap map = createSimplePropertyMap();

        map.setUpdateFromClientFilter(name -> false);
        Assertions.assertFalse(map.mayUpdateFromClient("foo", "bar"));

        DomListenerRegistration domListenerRegistration = Element
                .get(map.getNode())
                .addPropertyChangeListener("foo", "event", event -> {
                });
        Assertions.assertTrue(map.mayUpdateFromClient("foo", "bar"));

        domListenerRegistration.remove();
        Assertions.assertFalse(map.mayUpdateFromClient("foo", "bar"));

        DomListenerRegistration registration = Element.get(map.getNode())
                .addEventListener("dummy", event -> {
                }).synchronizeProperty("foo");
        Assertions.assertTrue(map.mayUpdateFromClient("foo", "bar"));

        registration.remove();
        Assertions.assertFalse(map.mayUpdateFromClient("foo", "bar"));
    }

    @Test
    public void updateFromClientFilter_replaceFilter() {
        ElementPropertyMap map = createSimplePropertyMap();

        map.setUpdateFromClientFilter("foo"::equals);

        Assertions.assertTrue(map.mayUpdateFromClient("foo", "a"));
        Assertions.assertFalse(map.mayUpdateFromClient("bar", "a"));

        map.setUpdateFromClientFilter("bar"::equals);

        Assertions.assertFalse(map.mayUpdateFromClient("foo", "a"));
        Assertions.assertTrue(map.mayUpdateFromClient("bar", "a"));
    }

    @Test
    public void childPropertyUpdateFilter_setFilterBeforeChild() {
        ElementPropertyMap map = createSimplePropertyMap();
        StateNode child = new StateNode(ElementPropertyMap.class);
        ElementPropertyMap childModel = ElementPropertyMap.getModel(child);

        map.setUpdateFromClientFilter("foo.bar"::equals);
        map.put("foo", child);

        Assertions.assertTrue(childModel.mayUpdateFromClient("bar", "a"));
        Assertions.assertFalse(childModel.mayUpdateFromClient("baz", "a"));
    }

    @Test
    public void deferredUpdateFromClient_filterAllowsUpdate()
            throws PropertyChangeDeniedException {
        ElementPropertyMap map = createSimplePropertyMap();
        StateNode child = new StateNode(ElementPropertyMap.class);
        ElementPropertyMap childModel = ElementPropertyMap.getModel(child);

        map.setUpdateFromClientFilter("foo.bar"::equals);
        map.put("foo", child);

        assertDeferredUpdate_putResult(childModel, "bar");
    }

    @Test
    public void deferredUpdateFromClient_noFilter_throws()
            throws PropertyChangeDeniedException {
        assertThrows(PropertyChangeDeniedException.class, () -> {
            ElementPropertyMap map = createSimplePropertyMap();
            StateNode child = new StateNode(ElementPropertyMap.class);
            ElementPropertyMap childModel = ElementPropertyMap.getModel(child);

            map.put("foo", child);
            childModel.deferredUpdateFromClient("bar", "a");
        });
    }

    @Test
    public void deferredUpdateFromClient_filterDisallowsUpdate()
            throws PropertyChangeDeniedException {
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

        Assertions.assertTrue(childModel.mayUpdateFromClient("bar", "a"));
        Assertions.assertFalse(childModel.mayUpdateFromClient("baz", "a"));
    }

    @Test
    public void deferredUpdateFromClient_listChild_filterAllowsUpdate()
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
    public void deferredUpdateFromClient_listItem_noFilter_throws()
            throws PropertyChangeDeniedException {
        assertThrows(PropertyChangeDeniedException.class, () -> {
            ElementPropertyMap map = createSimplePropertyMap();
            ModelList list = map.resolveModelList("foo");
            StateNode child = new StateNode(ElementPropertyMap.class);

            list.add(child);

            ElementPropertyMap childModel = ElementPropertyMap.getModel(child);

            childModel.deferredUpdateFromClient("bar", "a");
        });
    }

    @Test
    public void deferredUpdateFromClient_listChild_filterDisallowsUpdate()
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
    public void childPropertyUpdateFilter_setFilterAfterChild() {
        ElementPropertyMap map = createSimplePropertyMap();
        StateNode child = new StateNode(ElementPropertyMap.class);
        ElementPropertyMap childModel = ElementPropertyMap.getModel(child);

        map.put("foo", child);
        map.setUpdateFromClientFilter("foo.bar"::equals);

        Assertions.assertTrue(childModel.mayUpdateFromClient("bar", "a"));
        Assertions.assertFalse(childModel.mayUpdateFromClient("baz", "a"));
    }

    @Test
    public void childPropertyUpdateFilter_renameProperty() {
        ElementPropertyMap map = createSimplePropertyMap();
        StateNode child = new StateNode(ElementPropertyMap.class);
        ElementPropertyMap childModel = ElementPropertyMap.getModel(child);

        map.put("foo", child);
        map.setUpdateFromClientFilter("foo.bar"::equals);

        Assertions.assertTrue(childModel.mayUpdateFromClient("bar", "a"));

        map.remove("foo");
        Assertions.assertFalse(childModel.mayUpdateFromClient("bar", "a"));

        map.put("bar", child);
        Assertions.assertFalse(childModel.mayUpdateFromClient("bar", "a"));
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

        Assertions.assertTrue(map.mayUpdateFromClient("property", "foo"));
    }

    @Test
    public void deferredUpdateFromClient_updateNotAllowed_throw()
            throws PropertyChangeDeniedException {
        assertThrows(PropertyChangeDeniedException.class, () -> {
            ElementPropertyMap map = createSimplePropertyMap();

            map.deferredUpdateFromClient("foo", "value");
        });
    }

    @Test
    public void deferredUpdateFromClient_filterDisallowUpdate_eventIsSynchronized()
            throws PropertyChangeDeniedException {
        ElementPropertyMap map = createSimplePropertyMap();
        Element.get(map.getNode()).addEventListener("dummy", event -> {

        }).synchronizeProperty("foo");

        map.setUpdateFromClientFilter(key -> false);

        assertDeferredUpdate_putResult(map, "foo");
    }

    @Test
    public void deferredUpdateFromClient_filterAllowsUpdate_propertyIsForbidden_throw()
            throws PropertyChangeDeniedException {
        assertThrows(PropertyChangeDeniedException.class, () -> {
            ElementPropertyMap map = createSimplePropertyMap();
            map.put("classList", "a");

            map.setUpdateFromClientFilter(key -> true);

            map.deferredUpdateFromClient("classList", "value");
        });
    }

    @Test
    public void deferredUpdateFromClient_clientFiltersOutUpdate_noOpRunnable()
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
        Assertions.assertNull(eventCapture.get());
    }

    @Test
    public void deferredUpdateFromClient_clientFilterAcceptUpdate_putResultRunnable()
            throws PropertyChangeDeniedException {
        ElementPropertyMap map = createSimplePropertyMap();
        map.setUpdateFromClientFilter(name -> name.equals("foo"));

        AtomicReference<PropertyChangeEvent> eventCapture = new AtomicReference<>();
        map.addPropertyChangeListener("foo", eventCapture::set);

        Runnable runnable = assertDeferredUpdate_putResult(map, "foo");
        runnable.run();
        Assertions.assertNotNull(eventCapture.get());
    }

    @Test
    public void producePutChange_innerHTMLProperty_valueIsTheSame_returnsTrue() {
        ElementPropertyMap map = createSimplePropertyMap();
        map.setProperty("innerHTML", "foo");

        Assertions.assertTrue(map.producePutChange("innerHTML", true, "foo"));
        Assertions.assertTrue(map.producePutChange("innerHTML", false, "foo"));
    }

    @Test
    public void producePutChange_notInnerHTMLProperty_valueIsTheSame_returnsFalse() {
        ElementPropertyMap map = createSimplePropertyMap();
        map.setProperty("foo", "bar");

        Assertions.assertFalse(map.producePutChange("foo", true, "bar"));
    }

    private void listenerIsNotified(boolean clientEvent) {
        ElementPropertyMap map = createSimplePropertyMap();
        StateNode node = map.getNode();

        AtomicReference<PropertyChangeEvent> event = new AtomicReference<>();
        PropertyChangeListener listener = ev -> {
            Assertions.assertNull(event.get());
            event.set(ev);
        };
        map.addPropertyChangeListener("foo", listener);
        map.setProperty("foo", "bar", !clientEvent);

        Assertions.assertNull(event.get().getOldValue());
        Assertions.assertEquals("bar", event.get().getValue());
        Assertions.assertEquals("foo", event.get().getPropertyName());
        Assertions.assertEquals(Element.get(node), event.get().getSource());
        Assertions.assertEquals(clientEvent, event.get().isUserOriginated());

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
