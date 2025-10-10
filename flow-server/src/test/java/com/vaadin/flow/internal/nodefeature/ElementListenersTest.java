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
package com.vaadin.flow.internal.nodefeature;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.DomEventListener;
import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.StateTree;
import com.vaadin.flow.shared.JsonConstants;
import com.vaadin.flow.shared.Registration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;

public class ElementListenersTest
        extends AbstractNodeFeatureTest<ElementListenerMap> {
    private static final DomEventListener noOp = e -> {
        // no op
    };

    private ElementListenerMap ns;

    @Before
    public void init() {
        ns = createFeature();
    }

    @Test
    public void addedListenerGetsEvent() {

        AtomicInteger eventCount = new AtomicInteger();

        Registration handle = ns.add("foo", e -> eventCount.incrementAndGet());

        Assert.assertEquals(0, eventCount.get());

        ns.fireEvent(createEvent("foo"));

        Assert.assertEquals(1, eventCount.get());

        handle.remove();

        ns.fireEvent(createEvent("foo"));

        Assert.assertEquals(1, eventCount.get());
    }

    @Test
    public void eventNameInClientData() {
        Assert.assertFalse(ns.contains("foo"));

        Registration handle = ns.add("foo", noOp);

        Assert.assertEquals(0, getExpressions("foo").size());

        handle.remove();

        Assert.assertFalse(ns.contains("foo"));
    }

    @Test
    public void addAndRemoveEventData() {
        ns.add("eventType", noOp).addEventData("data1").addEventData("data2");

        Set<String> expressions = getExpressions("eventType");
        Assert.assertTrue(expressions.contains("data1"));
        Assert.assertTrue(expressions.contains("data2"));
        Assert.assertFalse(expressions.contains("data3"));

        Registration handle = ns.add("eventType", new DomEventListener() {
            /*
             * Can't use the existing noOp instance since there would then not
             * be any listeners left after calling remove()
             */

            @Override
            public void handleEvent(DomEvent event) {
                // no op
            }
        }).addEventData("data3");

        expressions = getExpressions("eventType");
        Assert.assertTrue(expressions.contains("data1"));
        Assert.assertTrue(expressions.contains("data2"));
        Assert.assertTrue(expressions.contains("data3"));

        handle.remove();

        expressions = getExpressions("eventType");
        Assert.assertTrue(expressions.contains("data1"));
        Assert.assertTrue(expressions.contains("data2"));
        // due to fix to #5090, data3 won't be present after removal
        Assert.assertFalse(expressions.contains("data3"));
    }

    @Test
    public void settingsAreOnlyUpdated_should_ListenersSharingTheTypeOfRemovedListenerExist() {
        ns = spy(createFeature());
        DomEventListener del1 = event -> {
        };
        DomEventListener del2 = event -> {
        };
        DomEventListener del3 = event -> {
        };
        Registration handle1 = ns.add("eventType", del1).addEventData("data1");
        Registration handle2 = ns.add("eventType", del2).addEventData("data2");
        Registration handle3 = ns.add("eventTypeOther", del3)
                .addEventData("data3");
        Mockito.reset(ns);

        Set<String> expressions = getExpressions("eventType");
        expressions.addAll(getExpressions("eventTypeOther"));

        Assert.assertTrue(expressions.contains("data1"));
        Assert.assertTrue(expressions.contains("data2"));
        Assert.assertTrue(expressions.contains("data3"));

        handle1.remove();

        Mockito.verify(ns, times(1)).put(eq("eventType"),
                any(Serializable.class));

        expressions = getExpressions("eventType");
        expressions.addAll(getExpressions("eventTypeOther"));

        Assert.assertFalse(expressions.contains("data1"));
        Assert.assertTrue(expressions.contains("data2"));
        Assert.assertTrue(expressions.contains("data3"));

        handle2.remove();
        // updating settings does not take place a second time
        Mockito.verify(ns, times(1)).put(eq("eventType"),
                any(Serializable.class));

        expressions = getExpressions("eventType");
        expressions.addAll(getExpressions("eventTypeOther"));

        Assert.assertFalse(expressions.contains("data1"));
        Assert.assertFalse(expressions.contains("data2"));
        Assert.assertTrue(expressions.contains("data3"));
    }

    @Test
    public void addingRemovingAndAddingListenerOfTheSameType() {
        DomEventListener del1 = event -> {
        };
        DomEventListener del2 = event -> {
        };
        Registration handle = ns.add("eventType", del1).addEventData("data1");

        Set<String> expressions = getExpressions("eventType");
        Assert.assertTrue(expressions.contains("data1"));

        handle.remove();
        expressions = getExpressions("eventType");
        Assert.assertFalse(expressions.contains("data1"));

        // re-add a listener for "eventType", using different eventData
        handle = ns.add("eventType", del2).addEventData("data2");
        expressions = getExpressions("eventType");
        Assert.assertFalse(expressions.contains("data1"));
        Assert.assertTrue(expressions.contains("data2"));

        handle.remove();
        expressions = getExpressions("eventType");
        Assert.assertFalse(expressions.contains("data1"));
        Assert.assertFalse(expressions.contains("data2"));
    }

    @Test
    public void eventDataInEvent() {
        AtomicReference<JsonNode> eventDataReference = new AtomicReference<>();
        ns.add("foo", e -> {
            Assert.assertNull(eventDataReference.get());
            eventDataReference.set(e.getEventData());
        });

        Assert.assertNull(eventDataReference.get());

        ObjectNode eventData = JacksonUtils.createObjectNode();
        eventData.put("baz", true);
        ns.fireEvent(new DomEvent(new Element("element"), "foo", eventData));

        JsonNode capturedJson = eventDataReference.get();
        Assert.assertNotNull(capturedJson);

        Assert.assertEquals(1, JacksonUtils.getKeys(capturedJson).size());
        Assert.assertEquals("true", capturedJson.get("baz").toString());
    }

    @Test
    public void disabledElement_listenerDoesntReceiveEvent() {
        AtomicInteger eventCount = new AtomicInteger();

        ns.add("foo", e -> eventCount.incrementAndGet());

        Assert.assertEquals(0, eventCount.get());
        DomEvent event = createEvent("foo");
        event.getSource().setEnabled(false);
        ns.fireEvent(event);
        Assert.assertEquals(0, eventCount.get());
    }

    @Test
    public void implicitlyDisabledElement_listenerDoesntReceiveEvent() {
        AtomicInteger eventCount = new AtomicInteger();

        ns.add("foo", e -> eventCount.incrementAndGet());

        Assert.assertEquals(0, eventCount.get());
        DomEvent event = createEvent("foo");

        Element parent = new Element("parent");
        parent.appendChild(event.getSource());
        parent.setEnabled(false);

        ns.fireEvent(event);
        Assert.assertEquals(0, eventCount.get());
    }

    @Test
    public void disabledElement_listenerWithAlwaysUpdateModeReceivesEvent() {
        AtomicInteger eventCount = new AtomicInteger();

        ns.add("foo", e -> eventCount.incrementAndGet())
                .setDisabledUpdateMode(DisabledUpdateMode.ALWAYS);

        Assert.assertEquals(0, eventCount.get());
        DomEvent event = createEvent("foo");
        event.getSource().setEnabled(false);
        ns.fireEvent(event);
        Assert.assertEquals(1, eventCount.get());
    }

    @Test
    public void serializable() {
        ns.add("click", noOp).addEventData("eventdata");

        ElementListenerMap roundtrip = SerializationUtils.roundtrip(ns);

        Set<String> expressions = roundtrip.getExpressions("click");
        Assert.assertEquals(Collections.singleton("eventdata"), expressions);
    }

    @Test
    public void synchronizeProperty_hasSynchronizedProperty() {
        DomListenerRegistration registration = ns.add("foo", noOp);

        Assert.assertNull(ns.getPropertySynchronizationMode("name"));

        registration.synchronizeProperty("anotherName");

        Assert.assertNull(ns.getPropertySynchronizationMode("name"));

        registration.synchronizeProperty("name");

        Assert.assertSame(DisabledUpdateMode.ONLY_WHEN_ENABLED,
                ns.getPropertySynchronizationMode("name"));
    }

    @Test
    public void synchronizeProperty_alwaysMode() {
        DomListenerRegistration registration = ns.add("foo", noOp)
                .setDisabledUpdateMode(DisabledUpdateMode.ALWAYS);

        registration.synchronizeProperty("name");

        Assert.assertSame(DisabledUpdateMode.ALWAYS,
                ns.getPropertySynchronizationMode("name"));
    }

    @Test
    public void synchronizeProperty_bothModes() {
        DomListenerRegistration registration1 = ns.add("foo", noOp)
                .setDisabledUpdateMode(DisabledUpdateMode.ALWAYS);

        registration1.synchronizeProperty("name");

        DomListenerRegistration registration2 = ns.add("foo", noOp);
        registration2.synchronizeProperty("name");

        Assert.assertSame(DisabledUpdateMode.ALWAYS,
                ns.getPropertySynchronizationMode("name"));
    }

    @Test
    public void synchronizeProperty_hasExpressionToken() {
        DomListenerRegistration registration = ns.add("foo", noOp);

        Assert.assertEquals(Collections.emptySet(), getExpressions("foo"));

        registration.synchronizeProperty("name");

        Assert.assertEquals(
                Collections.singleton(
                        JsonConstants.SYNCHRONIZE_PROPERTY_TOKEN + "name"),
                getExpressions("foo"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void synchronizeProperty_nullArgument_illegalArgumentException() {
        DomListenerRegistration registration = ns.add("foo", noOp);

        registration.synchronizeProperty(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void synchronizeProperty_emptyArgument_illegalArgumentException() {
        DomListenerRegistration registration = ns.add("foo", noOp);

        registration.synchronizeProperty("");
    }

    @Test
    public void mapEventTargetToElement_targetNodeIdInJsonData_elementMapped() {
        Element parent = new Element("parent");
        Element child = new Element("child");
        Element grandChild = new Element("grandChild");
        parent.appendChild(child.appendChild(grandChild));
        new StateTree(new UI().getInternals(), ElementChildrenList.class)
                .getUI().getElement().appendChild(parent);
        final String eventType = "click";

        AtomicReference<Element> capturedTarget = new AtomicReference<>();
        final DomListenerRegistration registration = parent
                .addEventListener(eventType, e -> {
                    capturedTarget.set(e.getEventTarget().orElse(null));
                });
        final ElementListenerMap listenerMap = parent.getNode()
                .getFeature(ElementListenerMap.class);
        Set<String> expressions = getExpressions(listenerMap, eventType);
        // event.detail is now included by default
        Assert.assertEquals(1, expressions.size());
        Assert.assertTrue(expressions.contains("event.detail"));

        registration.mapEventTargetElement();
        expressions = getExpressions(listenerMap, eventType);

        // Should now have both event.detail and MAP_STATE_NODE_EVENT_DATA
        Assert.assertEquals(2, expressions.size());
        Assert.assertTrue(expressions.contains("event.detail"));
        Assert.assertTrue(
                expressions.contains(JsonConstants.MAP_STATE_NODE_EVENT_DATA));

        // child
        final ObjectNode eventData = JacksonUtils.createObjectNode();
        eventData.put(JsonConstants.MAP_STATE_NODE_EVENT_DATA,
                child.getNode().getId());
        listenerMap.fireEvent(new DomEvent(parent, eventType, eventData));
        Assert.assertEquals(child, capturedTarget.get());

        // nothing reported -> empty optional
        listenerMap.fireEvent(new DomEvent(parent, eventType,
                JacksonUtils.createObjectNode()));
        Assert.assertNull("no element should be reported",
                capturedTarget.get());

        // grandchild
        eventData.put(JsonConstants.MAP_STATE_NODE_EVENT_DATA,
                grandChild.getNode().getId());
        listenerMap.fireEvent(new DomEvent(parent, eventType, eventData));
        Assert.assertEquals(grandChild, capturedTarget.get());

        // -1 -> empty optional
        eventData.put(JsonConstants.MAP_STATE_NODE_EVENT_DATA, -1);
        listenerMap.fireEvent(new DomEvent(parent, eventType, eventData));
        Assert.assertNull("no element should be reported",
                capturedTarget.get());
    }

    @Test
    public void addEventDataElement_targetNodeInJsonData_elementMapped() {
        Element parent = new Element("parent");
        Element child = new Element("child");
        Element sibling = new Element("sibling");
        parent.appendChild(child);
        new StateTree(new UI().getInternals(), ElementChildrenList.class)
                .getUI().getElement().appendChild(parent, sibling);
        final String eventType = "click";
        final String expression = "expression";
        final String key = JsonConstants.MAP_STATE_NODE_EVENT_DATA + expression;

        AtomicReference<DomEvent> capturedTarget = new AtomicReference<>();
        final DomListenerRegistration registration = parent
                .addEventListener(eventType, capturedTarget::set);
        final ElementListenerMap listenerMap = parent.getNode()
                .getFeature(ElementListenerMap.class);

        Set<String> expressions = getExpressions(listenerMap, eventType);
        // event.detail is now included by default
        Assert.assertEquals(1, expressions.size());
        Assert.assertTrue(expressions.contains("event.detail"));

        registration.addEventDataElement(expression);
        expressions = getExpressions(listenerMap, eventType);

        // Should now have both event.detail and the added element expression
        Assert.assertEquals(2, expressions.size());
        Assert.assertTrue(expressions.contains("event.detail"));
        Assert.assertTrue(expressions.contains(key));

        final ObjectNode eventData = JacksonUtils.createObjectNode();
        eventData.put(key, child.getNode().getId());
        listenerMap.fireEvent(new DomEvent(parent, eventType, eventData));
        Assert.assertEquals(child,
                capturedTarget.get().getEventDataElement(expression).get());

        // nothing reported -> empty optional
        listenerMap.fireEvent(new DomEvent(parent, eventType,
                JacksonUtils.createObjectNode()));
        Assert.assertFalse("no element should be reported", capturedTarget.get()
                .getEventDataElement(expression).isPresent());

        // sibling
        eventData.put(key, sibling.getNode().getId());
        listenerMap.fireEvent(new DomEvent(parent, eventType, eventData));
        Assert.assertEquals(sibling,
                capturedTarget.get().getEventDataElement(expression).get());
    }

    @Test
    public void addEventDataElement_eventTarget_usesMapEventTargetInstead() {
        Element parent = new Element("parent");
        Element child = new Element("child");
        parent.appendChild(child);
        new StateTree(new UI().getInternals(), ElementChildrenList.class)
                .getUI().getElement().appendChild(parent);

        final String eventType = "click";
        AtomicReference<DomEvent> capturedTarget = new AtomicReference<>();
        final DomListenerRegistration registration = parent
                .addEventListener(eventType, capturedTarget::set);
        final ElementListenerMap listenerMap = parent.getNode()
                .getFeature(ElementListenerMap.class);

        registration.addEventDataElement("event.target");
        Set<String> expressions = getExpressions(listenerMap, eventType);

        // Should have both event.detail and MAP_STATE_NODE_EVENT_DATA
        Assert.assertEquals(2, expressions.size());
        Assert.assertTrue(expressions.contains("event.detail"));
        Assert.assertTrue(
                expressions.contains(JsonConstants.MAP_STATE_NODE_EVENT_DATA));

        final ObjectNode eventData = JacksonUtils.createObjectNode();
        eventData.put(JsonConstants.MAP_STATE_NODE_EVENT_DATA,
                child.getNode().getId());
        listenerMap.fireEvent(new DomEvent(parent, eventType, eventData));
        Assert.assertEquals(child, capturedTarget.get().getEventTarget().get());
        Assert.assertEquals(child,
                capturedTarget.get().getEventDataElement("event.target").get());
    }

    @Test
    public void eventDataKeyNotPresentNotFail() {
        AtomicInteger eventCount = new AtomicInteger();
        DomListenerRegistration registration = ns.add("foo",
                e -> eventCount.incrementAndGet());
        registration.setFilter("filterKey");

        ns.fireEvent(createEvent("foo"));
        Assert.assertEquals(0, eventCount.get());

        ObjectNode eventData = JacksonUtils.createObjectNode();
        eventData.put("filterKey", true);
        ns.fireEvent(new DomEvent(new Element("element"), "foo", eventData));
        Assert.assertEquals(1, eventCount.get());
    }

    @Test
    public void testPreventDefaultWithFilter() {
        // Test that preventDefault only applies to filtered events (see issue
        // #22294)

        // Create a listener with filter for space and enter keys
        DomListenerRegistration registration = ns.add("keydown", noOp);
        registration.setFilter("event.key === ' ' || event.key === 'Enter'");
        registration.preventDefault();

        // Check that the event data includes preventDefault
        Set<String> expressions = getExpressions("keydown");

        // The expressions should include:
        // 1. The filter expression for debouncing
        // 2. The conditional preventDefault expression
        Assert.assertTrue("Should have the filter expression", expressions
                .contains("event.key === ' ' || event.key === 'Enter'"));

        // After the fix, preventDefault should be conditional on the filter
        Assert.assertTrue("Should have conditional preventDefault expression",
                expressions.contains(
                        "(event.key === ' ' || event.key === 'Enter') && event.preventDefault()"));

        // The unconditional preventDefault should NOT be present
        Assert.assertFalse("Should NOT have unconditional preventDefault",
                expressions.contains("event.preventDefault()"));
    }

    @Test
    public void testPreventDefaultWithoutFilter() {
        // Test preventDefault without filter - should apply to all events
        DomListenerRegistration registration = ns.add("keydown", noOp);
        registration.preventDefault();

        Set<String> expressions = getExpressions("keydown");

        // Without a filter, preventDefault should apply to all events
        Assert.assertTrue("Should have preventDefault expression",
                expressions.contains("event.preventDefault()"));
        Assert.assertEquals("Should only have preventDefault expression", 1,
                expressions.size());
    }

    @Test
    public void testPreventDefaultThenSetFilter() {
        // Test that preventDefault becomes conditional even when filter is set
        // after
        DomListenerRegistration registration = ns.add("keydown", noOp);
        registration.preventDefault();
        registration.setFilter("event.key === 'Escape'");

        Set<String> expressions = getExpressions("keydown");

        // Should have conditional preventDefault based on the filter
        Assert.assertTrue("Should have conditional preventDefault expression",
                expressions.contains(
                        "(event.key === 'Escape') && event.preventDefault()"));

        // The unconditional preventDefault should NOT be present
        Assert.assertFalse("Should NOT have unconditional preventDefault",
                expressions.contains("event.preventDefault()"));
    }

    @Test
    public void testSetFilterThenPreventDefault() {
        // Test that preventDefault is conditional when filter is set before
        DomListenerRegistration registration = ns.add("keydown", noOp);
        registration.setFilter("event.key === 'Delete'");
        registration.preventDefault();

        Set<String> expressions = getExpressions("keydown");

        // Should have conditional preventDefault based on the filter
        Assert.assertTrue("Should have conditional preventDefault expression",
                expressions.contains(
                        "(event.key === 'Delete') && event.preventDefault()"));

        // The unconditional preventDefault should NOT be present
        Assert.assertFalse("Should NOT have unconditional preventDefault",
                expressions.contains("event.preventDefault()"));
    }

    @Test
    public void testStopPropagationWithFilter() {
        // Test that stopPropagation only applies to filtered events

        // Create a listener with filter for space and enter keys
        DomListenerRegistration registration = ns.add("keydown", noOp);
        registration.setFilter("event.key === ' ' || event.key === 'Enter'");
        registration.stopPropagation();

        // Check that the event data includes stopPropagation
        Set<String> expressions = getExpressions("keydown");

        // The expressions should include:
        // 1. The filter expression for debouncing
        // 2. The conditional stopPropagation expression
        Assert.assertTrue("Should have the filter expression", expressions
                .contains("event.key === ' ' || event.key === 'Enter'"));

        // After the fix, stopPropagation should be conditional on the filter
        Assert.assertTrue("Should have conditional stopPropagation expression",
                expressions.contains(
                        "(event.key === ' ' || event.key === 'Enter') && event.stopPropagation()"));

        // The unconditional stopPropagation should NOT be present
        Assert.assertFalse("Should NOT have unconditional stopPropagation",
                expressions.contains("event.stopPropagation()"));
    }

    @Test
    public void testStopPropagationWithoutFilter() {
        // Test stopPropagation without filter - should apply to all events
        DomListenerRegistration registration = ns.add("keydown", noOp);
        registration.stopPropagation();

        Set<String> expressions = getExpressions("keydown");

        // Without a filter, stopPropagation should apply to all events
        Assert.assertTrue("Should have stopPropagation expression",
                expressions.contains("event.stopPropagation()"));
        Assert.assertEquals("Should only have stopPropagation expression", 1,
                expressions.size());
    }

    @Test
    public void testStopPropagationThenSetFilter() {
        // Test that stopPropagation becomes conditional even when filter is
        // set after
        DomListenerRegistration registration = ns.add("keydown", noOp);
        registration.stopPropagation();
        registration.setFilter("event.key === 'Escape'");

        Set<String> expressions = getExpressions("keydown");

        // Should have conditional stopPropagation based on the filter
        Assert.assertTrue("Should have conditional stopPropagation expression",
                expressions.contains(
                        "(event.key === 'Escape') && event.stopPropagation()"));

        // The unconditional stopPropagation should NOT be present
        Assert.assertFalse("Should NOT have unconditional stopPropagation",
                expressions.contains("event.stopPropagation()"));
    }

    @Test
    public void testSetFilterThenStopPropagation() {
        // Test that stopPropagation is conditional when filter is set before
        DomListenerRegistration registration = ns.add("keydown", noOp);
        registration.setFilter("event.key === 'Delete'");
        registration.stopPropagation();

        Set<String> expressions = getExpressions("keydown");

        // Should have conditional stopPropagation based on the filter
        Assert.assertTrue("Should have conditional stopPropagation expression",
                expressions.contains(
                        "(event.key === 'Delete') && event.stopPropagation()"));

        // The unconditional stopPropagation should NOT be present
        Assert.assertFalse("Should NOT have unconditional stopPropagation",
                expressions.contains("event.stopPropagation()"));
    }

    @Test
    public void testAddEventDataWithRecord() {
        // Test that addEventData correctly extracts nested record structure
        record EventDetails(int button, int clientX, int clientY) {
        }
        record MouseEventData(EventDetails event, String type) {
        }

        DomListenerRegistration registration = ns.add("click", noOp);
        registration.addEventData(MouseEventData.class);

        Set<String> expressions = getExpressions("click");

        // Should have captured all nested fields
        Assert.assertTrue("Should capture event.button",
                expressions.contains("event.button"));
        Assert.assertTrue("Should capture event.clientX",
                expressions.contains("event.clientX"));
        Assert.assertTrue("Should capture event.clientY",
                expressions.contains("event.clientY"));
        Assert.assertTrue("Should capture type", expressions.contains("type"));

        // Should have exactly these 4 expressions
        Assert.assertEquals("Should have 4 expressions", 4, expressions.size());
    }

    @Test
    public void testAddEventDataWithSimpleBean() {
        // Test with a simple bean (non-record)
        class SimpleEventData {
            private String message;
            private int code;

            public String getMessage() {
                return message;
            }

            public int getCode() {
                return code;
            }
        }

        DomListenerRegistration registration = ns.add("custom", noOp);
        registration.addEventData(SimpleEventData.class);

        Set<String> expressions = getExpressions("custom");

        // Should have captured both fields
        Assert.assertTrue("Should capture message",
                expressions.contains("message"));
        Assert.assertTrue("Should capture code", expressions.contains("code"));
        Assert.assertEquals("Should have 2 expressions", 2, expressions.size());
    }

    @Test
    public void testRemoveEventData() {
        // Test that removeEventData removes a specific expression
        DomListenerRegistration registration = ns.add("click", noOp);
        registration.addEventData("event.clientX")
                .addEventData("event.clientY").addEventData("event.button");

        Set<String> expressions = getExpressions("click");
        Assert.assertEquals("Should have 3 expressions", 3, expressions.size());

        // Remove one expression
        registration.removeEventData("event.clientY");

        expressions = getExpressions("click");
        Assert.assertTrue("Should still have event.clientX",
                expressions.contains("event.clientX"));
        Assert.assertFalse("Should not have event.clientY",
                expressions.contains("event.clientY"));
        Assert.assertTrue("Should still have event.button",
                expressions.contains("event.button"));
        Assert.assertEquals("Should have 2 expressions", 2, expressions.size());
    }

    @Test
    public void testRemoveEventDataChaining() {
        // Test that removeEventData can be chained
        DomListenerRegistration registration = ns.add("custom-event", noOp);
        registration.addEventData("data1").addEventData("data2")
                .addEventData("data3").removeEventData("data1")
                .removeEventData("data3");

        Set<String> expressions = getExpressions("custom-event");

        // Should only have data2
        Assert.assertFalse("Should not have data1",
                expressions.contains("data1"));
        Assert.assertTrue("Should have data2", expressions.contains("data2"));
        Assert.assertFalse("Should not have data3",
                expressions.contains("data3"));
        Assert.assertEquals("Should have 1 expression", 1, expressions.size());
    }

    @Test
    public void testGetEventDetailWithRecord() {
        // Test getEventDetail with a Java record
        record RgbColor(int r, int g, int b) {
        }

        ObjectNode eventData = JacksonUtils.createObjectNode();
        ObjectNode detailData = JacksonUtils.createObjectNode();
        detailData.put("r", 255);
        detailData.put("g", 128);
        detailData.put("b", 64);
        eventData.set("event.detail", detailData);

        DomEvent event = new DomEvent(new Element("element"), "color-change",
                eventData);

        RgbColor color = event.getEventDetail(RgbColor.class);

        Assert.assertNotNull("Color should not be null", color);
        Assert.assertEquals("Red should be 255", 255, color.r());
        Assert.assertEquals("Green should be 128", 128, color.g());
        Assert.assertEquals("Blue should be 64", 64, color.b());
    }

    public static class EventPayload {
        private String message;
        private int code;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }
    }

    @Test
    public void testGetEventDetailWithBean() {
        // Test getEventDetail with a regular bean class
        ObjectNode eventData = JacksonUtils.createObjectNode();
        ObjectNode detailData = JacksonUtils.createObjectNode();
        detailData.put("message", "Hello World");
        detailData.put("code", 42);
        eventData.set("event.detail", detailData);

        DomEvent event = new DomEvent(new Element("element"), "custom-event",
                eventData);

        EventPayload payload = event.getEventDetail(EventPayload.class);

        Assert.assertNotNull("Payload should not be null", payload);
        Assert.assertEquals("Message should match", "Hello World",
                payload.getMessage());
        Assert.assertEquals("Code should match", 42, payload.getCode());
    }

    @Test
    public void testGetEventDetailWithTypeReference() {
        // Test getEventDetail with TypeReference for generic types
        ObjectNode eventData = JacksonUtils.createObjectNode();
        tools.jackson.databind.node.ArrayNode detailArray = JacksonUtils
                .createArrayNode();
        detailArray.add("first");
        detailArray.add("second");
        detailArray.add("third");
        eventData.set("event.detail", detailArray);

        DomEvent event = new DomEvent(new Element("element"), "list-change",
                eventData);

        List<String> items = event
                .getEventDetail(new TypeReference<List<String>>() {
                });

        Assert.assertNotNull("Items should not be null", items);
        Assert.assertEquals("Should have 3 items", 3, items.size());
        Assert.assertEquals("First item", "first", items.get(0));
        Assert.assertEquals("Second item", "second", items.get(1));
        Assert.assertEquals("Third item", "third", items.get(2));
    }

    @Test
    public void testGetEventDetailReturnsNullWhenNotPresent() {
        // Test that getEventDetail returns null when event.detail is not
        // present
        ObjectNode eventData = JacksonUtils.createObjectNode();

        DomEvent event = new DomEvent(new Element("element"), "event",
                eventData);

        record SomeData(String value) {
        }
        SomeData data = event.getEventDetail(SomeData.class);

        Assert.assertNull("Should return null when event.detail not present",
                data);
    }

    @Test
    public void testGetEventDetailReturnsNullWhenNull() {
        // Test that getEventDetail returns null when event.detail is null
        ObjectNode eventData = JacksonUtils.createObjectNode();
        eventData.set("event.detail", JacksonUtils.nullNode());

        DomEvent event = new DomEvent(new Element("element"), "event",
                eventData);

        record SomeData(String value) {
        }
        SomeData data = event.getEventDetail(SomeData.class);

        Assert.assertNull("Should return null when event.detail is null", data);
    }

    @Test
    public void testEventDetailIncludedByDefault() {
        // Test that event.detail is automatically included when adding an event
        // listener
        Element element = new Element("div");
        element.addEventListener("custom-event", e -> {
        });

        ElementListenerMap listenerMap = element.getNode()
                .getFeature(ElementListenerMap.class);
        Set<String> expressions = getExpressions(listenerMap, "custom-event");

        // Should automatically include event.detail
        Assert.assertTrue("event.detail should be included by default",
                expressions.contains("event.detail"));
    }

    @Test
    public void testEventDetailIncludedByDefaultCanBeUsed() {
        // Test that the automatically included event.detail can be accessed
        record CustomData(String message, int value) {
        }

        Element element = new Element("div");
        AtomicReference<CustomData> capturedData = new AtomicReference<>();
        element.addEventListener("custom-event",
                e -> capturedData.set(e.getEventDetail(CustomData.class)));

        ElementListenerMap listenerMap = element.getNode()
                .getFeature(ElementListenerMap.class);

        // Verify event.detail is in expressions
        Set<String> expressions = getExpressions(listenerMap, "custom-event");
        Assert.assertTrue("event.detail should be included by default",
                expressions.contains("event.detail"));

        // Fire event with detail data
        ObjectNode eventData = JacksonUtils.createObjectNode();
        ObjectNode detailData = JacksonUtils.createObjectNode();
        detailData.put("message", "test message");
        detailData.put("value", 123);
        eventData.set("event.detail", detailData);

        listenerMap.fireEvent(new DomEvent(element, "custom-event", eventData));

        // Verify the data was captured correctly
        CustomData result = capturedData.get();
        Assert.assertNotNull("Should have captured event detail", result);
        Assert.assertEquals("Message should match", "test message",
                result.message());
        Assert.assertEquals("Value should match", 123, result.value());
    }

    @Test
    public void testExcludeEventDetail() {
        // Test that excludeEventDetail() removes event.detail from expressions
        Element element = new Element("div");
        element.addEventListener("click", e -> {
        }).excludeEventDetail();

        ElementListenerMap listenerMap = element.getNode()
                .getFeature(ElementListenerMap.class);
        Set<String> expressions = getExpressions(listenerMap, "click");

        // Should NOT have event.detail after excluding it
        Assert.assertFalse("event.detail should be excluded",
                expressions.contains("event.detail"));
        Assert.assertEquals("Should have 0 expressions", 0, expressions.size());
    }

    @Test
    public void testExcludeEventDetailWithOtherEventData() {
        // Test that excludeEventDetail() only removes event.detail, not other
        // event data
        Element element = new Element("div");
        element.addEventListener("click", e -> {
        }).addEventData("event.clientX").addEventData("event.clientY")
                .excludeEventDetail();

        ElementListenerMap listenerMap = element.getNode()
                .getFeature(ElementListenerMap.class);
        Set<String> expressions = getExpressions(listenerMap, "click");

        // Should NOT have event.detail
        Assert.assertFalse("event.detail should be excluded",
                expressions.contains("event.detail"));
        // But should have the other event data
        Assert.assertTrue("Should have event.clientX",
                expressions.contains("event.clientX"));
        Assert.assertTrue("Should have event.clientY",
                expressions.contains("event.clientY"));
        Assert.assertEquals("Should have 2 expressions", 2, expressions.size());
    }

    // Helper for accessing package private API from other tests
    public static Set<String> getExpressions(
            ElementListenerMap elementListenerMap, String eventName) {
        return new HashSet<>(elementListenerMap.getExpressions(eventName));
    }

    private Set<String> getExpressions(String name) {
        return getExpressions(ns, name);
    }

    private static DomEvent createEvent(String type) {
        return new DomEvent(new Element("fake"), type,
                JacksonUtils.createObjectNode());
    }
}
