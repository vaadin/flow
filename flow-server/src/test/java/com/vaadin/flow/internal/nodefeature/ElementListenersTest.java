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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @BeforeEach
    public void init() {
        ns = createFeature();
    }

    @Test
    public void addedListenerGetsEvent() {

        AtomicInteger eventCount = new AtomicInteger();

        Registration handle = ns.add("foo", e -> eventCount.incrementAndGet());

        assertEquals(0, eventCount.get());

        ns.fireEvent(createEvent("foo"));

        assertEquals(1, eventCount.get());

        handle.remove();

        ns.fireEvent(createEvent("foo"));

        assertEquals(1, eventCount.get());
    }

    @Test
    public void eventNameInClientData() {
        assertFalse(ns.contains("foo"));

        Registration handle = ns.add("foo", noOp);

        assertEquals(0, getExpressions("foo").size());

        handle.remove();

        assertFalse(ns.contains("foo"));
    }

    @Test
    public void addAndRemoveEventData() {
        ns.add("eventType", noOp).addEventData("data1").addEventData("data2");

        Set<String> expressions = getExpressions("eventType");
        assertTrue(expressions.contains("data1"));
        assertTrue(expressions.contains("data2"));
        assertFalse(expressions.contains("data3"));

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
        assertTrue(expressions.contains("data1"));
        assertTrue(expressions.contains("data2"));
        assertTrue(expressions.contains("data3"));

        handle.remove();

        expressions = getExpressions("eventType");
        assertTrue(expressions.contains("data1"));
        assertTrue(expressions.contains("data2"));
        // due to fix to #5090, data3 won't be present after removal
        assertFalse(expressions.contains("data3"));
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

        assertTrue(expressions.contains("data1"));
        assertTrue(expressions.contains("data2"));
        assertTrue(expressions.contains("data3"));

        handle1.remove();

        Mockito.verify(ns, times(1)).put(eq("eventType"),
                any(Serializable.class));

        expressions = getExpressions("eventType");
        expressions.addAll(getExpressions("eventTypeOther"));

        assertFalse(expressions.contains("data1"));
        assertTrue(expressions.contains("data2"));
        assertTrue(expressions.contains("data3"));

        handle2.remove();
        // updating settings does not take place a second time
        Mockito.verify(ns, times(1)).put(eq("eventType"),
                any(Serializable.class));

        expressions = getExpressions("eventType");
        expressions.addAll(getExpressions("eventTypeOther"));

        assertFalse(expressions.contains("data1"));
        assertFalse(expressions.contains("data2"));
        assertTrue(expressions.contains("data3"));
    }

    @Test
    public void addingRemovingAndAddingListenerOfTheSameType() {
        DomEventListener del1 = event -> {
        };
        DomEventListener del2 = event -> {
        };
        Registration handle = ns.add("eventType", del1).addEventData("data1");

        Set<String> expressions = getExpressions("eventType");
        assertTrue(expressions.contains("data1"));

        handle.remove();
        expressions = getExpressions("eventType");
        assertFalse(expressions.contains("data1"));

        // re-add a listener for "eventType", using different eventData
        handle = ns.add("eventType", del2).addEventData("data2");
        expressions = getExpressions("eventType");
        assertFalse(expressions.contains("data1"));
        assertTrue(expressions.contains("data2"));

        handle.remove();
        expressions = getExpressions("eventType");
        assertFalse(expressions.contains("data1"));
        assertFalse(expressions.contains("data2"));
    }

    @Test
    public void eventDataInEvent() {
        AtomicReference<JsonNode> eventDataReference = new AtomicReference<>();
        ns.add("foo", e -> {
            assertNull(eventDataReference.get());
            eventDataReference.set(e.getEventData());
        });

        assertNull(eventDataReference.get());

        ObjectNode eventData = JacksonUtils.createObjectNode();
        eventData.put("baz", true);
        ns.fireEvent(new DomEvent(new Element("element"), "foo", eventData));

        JsonNode capturedJson = eventDataReference.get();
        assertNotNull(capturedJson);

        assertEquals(1, JacksonUtils.getKeys(capturedJson).size());
        assertEquals("true", capturedJson.get("baz").toString());
    }

    @Test
    public void disabledElement_listenerDoesntReceiveEvent() {
        AtomicInteger eventCount = new AtomicInteger();

        ns.add("foo", e -> eventCount.incrementAndGet());

        assertEquals(0, eventCount.get());
        DomEvent event = createEvent("foo");
        event.getSource().setEnabled(false);
        ns.fireEvent(event);
        assertEquals(0, eventCount.get());
    }

    @Test
    public void implicitlyDisabledElement_listenerDoesntReceiveEvent() {
        AtomicInteger eventCount = new AtomicInteger();

        ns.add("foo", e -> eventCount.incrementAndGet());

        assertEquals(0, eventCount.get());
        DomEvent event = createEvent("foo");

        Element parent = new Element("parent");
        parent.appendChild(event.getSource());
        parent.setEnabled(false);

        ns.fireEvent(event);
        assertEquals(0, eventCount.get());
    }

    @Test
    public void disabledElement_listenerWithAlwaysUpdateModeReceivesEvent() {
        AtomicInteger eventCount = new AtomicInteger();

        ns.add("foo", e -> eventCount.incrementAndGet())
                .setDisabledUpdateMode(DisabledUpdateMode.ALWAYS);

        assertEquals(0, eventCount.get());
        DomEvent event = createEvent("foo");
        event.getSource().setEnabled(false);
        ns.fireEvent(event);
        assertEquals(1, eventCount.get());
    }

    @Test
    public void serializable() {
        ns.add("click", noOp).addEventData("eventdata");

        ElementListenerMap roundtrip = SerializationUtils.roundtrip(ns);

        Set<String> expressions = roundtrip.getExpressions("click");
        assertEquals(Collections.singleton("eventdata"), expressions);
    }

    @Test
    public void synchronizeProperty_hasSynchronizedProperty() {
        DomListenerRegistration registration = ns.add("foo", noOp);

        assertNull(ns.getPropertySynchronizationMode("name"));

        registration.synchronizeProperty("anotherName");

        assertNull(ns.getPropertySynchronizationMode("name"));

        registration.synchronizeProperty("name");

        assertSame(DisabledUpdateMode.ONLY_WHEN_ENABLED,
                ns.getPropertySynchronizationMode("name"));
    }

    @Test
    public void synchronizeProperty_alwaysMode() {
        DomListenerRegistration registration = ns.add("foo", noOp)
                .setDisabledUpdateMode(DisabledUpdateMode.ALWAYS);

        registration.synchronizeProperty("name");

        assertSame(DisabledUpdateMode.ALWAYS,
                ns.getPropertySynchronizationMode("name"));
    }

    @Test
    public void synchronizeProperty_bothModes() {
        DomListenerRegistration registration1 = ns.add("foo", noOp)
                .setDisabledUpdateMode(DisabledUpdateMode.ALWAYS);

        registration1.synchronizeProperty("name");

        DomListenerRegistration registration2 = ns.add("foo", noOp);
        registration2.synchronizeProperty("name");

        assertSame(DisabledUpdateMode.ALWAYS,
                ns.getPropertySynchronizationMode("name"));
    }

    @Test
    public void synchronizeProperty_hasExpressionToken() {
        DomListenerRegistration registration = ns.add("foo", noOp);

        assertEquals(Collections.emptySet(), getExpressions("foo"));

        registration.synchronizeProperty("name");

        assertEquals(
                Collections.singleton(
                        JsonConstants.SYNCHRONIZE_PROPERTY_TOKEN + "name"),
                getExpressions("foo"));
    }

    @Test
    public void synchronizeProperty_nullArgument_illegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> {
            DomListenerRegistration registration = ns.add("foo", noOp);

            registration.synchronizeProperty(null);
        });
    }

    @Test
    public void synchronizeProperty_emptyArgument_illegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> {
            DomListenerRegistration registration = ns.add("foo", noOp);

            registration.synchronizeProperty("");
        });
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
        assertEquals(0, expressions.size());

        registration.mapEventTargetElement();
        expressions = getExpressions(listenerMap, eventType);

        assertEquals(1, expressions.size());
        assertEquals(JsonConstants.MAP_STATE_NODE_EVENT_DATA,
                expressions.iterator().next());

        // child
        final ObjectNode eventData = JacksonUtils.createObjectNode();
        eventData.put(JsonConstants.MAP_STATE_NODE_EVENT_DATA,
                child.getNode().getId());
        listenerMap.fireEvent(new DomEvent(parent, eventType, eventData));
        assertEquals(child, capturedTarget.get());

        // nothing reported -> empty optional
        listenerMap.fireEvent(new DomEvent(parent, eventType,
                JacksonUtils.createObjectNode()));
        assertNull(capturedTarget.get(), "no element should be reported");

        // grandchild
        eventData.put(JsonConstants.MAP_STATE_NODE_EVENT_DATA,
                grandChild.getNode().getId());
        listenerMap.fireEvent(new DomEvent(parent, eventType, eventData));
        assertEquals(grandChild, capturedTarget.get());

        // -1 -> empty optional
        eventData.put(JsonConstants.MAP_STATE_NODE_EVENT_DATA, -1);
        listenerMap.fireEvent(new DomEvent(parent, eventType, eventData));
        assertNull(capturedTarget.get(), "no element should be reported");
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
        assertEquals(0, expressions.size());

        registration.addEventDataElement(expression);
        expressions = getExpressions(listenerMap, eventType);

        assertEquals(1, expressions.size());
        assertEquals(key, expressions.iterator().next());

        final ObjectNode eventData = JacksonUtils.createObjectNode();
        eventData.put(key, child.getNode().getId());
        listenerMap.fireEvent(new DomEvent(parent, eventType, eventData));
        assertEquals(child,
                capturedTarget.get().getEventDataElement(expression).get());

        // nothing reported -> empty optional
        listenerMap.fireEvent(new DomEvent(parent, eventType,
                JacksonUtils.createObjectNode()));
        assertFalse(capturedTarget.get().getEventDataElement(expression)
                .isPresent(), "no element should be reported");

        // sibling
        eventData.put(key, sibling.getNode().getId());
        listenerMap.fireEvent(new DomEvent(parent, eventType, eventData));
        assertEquals(sibling,
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

        assertEquals(1, expressions.size());
        assertEquals(JsonConstants.MAP_STATE_NODE_EVENT_DATA,
                expressions.iterator().next());

        final ObjectNode eventData = JacksonUtils.createObjectNode();
        eventData.put(JsonConstants.MAP_STATE_NODE_EVENT_DATA,
                child.getNode().getId());
        listenerMap.fireEvent(new DomEvent(parent, eventType, eventData));
        assertEquals(child, capturedTarget.get().getEventTarget().get());
        assertEquals(child,
                capturedTarget.get().getEventDataElement("event.target").get());
    }

    @Test
    public void eventDataKeyNotPresentNotFail() {
        AtomicInteger eventCount = new AtomicInteger();
        DomListenerRegistration registration = ns.add("foo",
                e -> eventCount.incrementAndGet());
        registration.setFilter("filterKey");

        ns.fireEvent(createEvent("foo"));
        assertEquals(0, eventCount.get());

        ObjectNode eventData = JacksonUtils.createObjectNode();
        eventData.put("filterKey", true);
        ns.fireEvent(new DomEvent(new Element("element"), "foo", eventData));
        assertEquals(1, eventCount.get());
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
        assertTrue(
                expressions
                        .contains("event.key === ' ' || event.key === 'Enter'"),
                "Should have the filter expression");

        // After the fix, preventDefault should be conditional on the filter
        assertTrue(expressions.contains(
                "(event.key === ' ' || event.key === 'Enter') && event.preventDefault()"),
                "Should have conditional preventDefault expression");

        // The unconditional preventDefault should NOT be present
        assertFalse(expressions.contains("event.preventDefault()"),
                "Should NOT have unconditional preventDefault");
    }

    @Test
    public void testPreventDefaultWithoutFilter() {
        // Test preventDefault without filter - should apply to all events
        DomListenerRegistration registration = ns.add("keydown", noOp);
        registration.preventDefault();

        Set<String> expressions = getExpressions("keydown");

        // Without a filter, preventDefault should apply to all events
        assertTrue(expressions.contains("event.preventDefault()"),
                "Should have preventDefault expression");
        assertEquals(1, expressions.size(),
                "Should only have preventDefault expression");
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
        assertTrue(
                expressions.contains(
                        "(event.key === 'Escape') && event.preventDefault()"),
                "Should have conditional preventDefault expression");

        // The unconditional preventDefault should NOT be present
        assertFalse(expressions.contains("event.preventDefault()"),
                "Should NOT have unconditional preventDefault");
    }

    @Test
    public void testSetFilterThenPreventDefault() {
        // Test that preventDefault is conditional when filter is set before
        DomListenerRegistration registration = ns.add("keydown", noOp);
        registration.setFilter("event.key === 'Delete'");
        registration.preventDefault();

        Set<String> expressions = getExpressions("keydown");

        // Should have conditional preventDefault based on the filter
        assertTrue(
                expressions.contains(
                        "(event.key === 'Delete') && event.preventDefault()"),
                "Should have conditional preventDefault expression");

        // The unconditional preventDefault should NOT be present
        assertFalse(expressions.contains("event.preventDefault()"),
                "Should NOT have unconditional preventDefault");
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
        assertTrue(
                expressions
                        .contains("event.key === ' ' || event.key === 'Enter'"),
                "Should have the filter expression");

        // After the fix, stopPropagation should be conditional on the filter
        assertTrue(expressions.contains(
                "(event.key === ' ' || event.key === 'Enter') && event.stopPropagation()"),
                "Should have conditional stopPropagation expression");

        // The unconditional stopPropagation should NOT be present
        assertFalse(expressions.contains("event.stopPropagation()"),
                "Should NOT have unconditional stopPropagation");
    }

    @Test
    public void testStopPropagationWithoutFilter() {
        // Test stopPropagation without filter - should apply to all events
        DomListenerRegistration registration = ns.add("keydown", noOp);
        registration.stopPropagation();

        Set<String> expressions = getExpressions("keydown");

        // Without a filter, stopPropagation should apply to all events
        assertTrue(expressions.contains("event.stopPropagation()"),
                "Should have stopPropagation expression");
        assertEquals(1, expressions.size(),
                "Should only have stopPropagation expression");
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
        assertTrue(
                expressions.contains(
                        "(event.key === 'Escape') && event.stopPropagation()"),
                "Should have conditional stopPropagation expression");

        // The unconditional stopPropagation should NOT be present
        assertFalse(expressions.contains("event.stopPropagation()"),
                "Should NOT have unconditional stopPropagation");
    }

    @Test
    public void testSetFilterThenStopPropagation() {
        // Test that stopPropagation is conditional when filter is set before
        DomListenerRegistration registration = ns.add("keydown", noOp);
        registration.setFilter("event.key === 'Delete'");
        registration.stopPropagation();

        Set<String> expressions = getExpressions("keydown");

        // Should have conditional stopPropagation based on the filter
        assertTrue(
                expressions.contains(
                        "(event.key === 'Delete') && event.stopPropagation()"),
                "Should have conditional stopPropagation expression");

        // The unconditional stopPropagation should NOT be present
        assertFalse(expressions.contains("event.stopPropagation()"),
                "Should NOT have unconditional stopPropagation");
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
        assertTrue(expressions.contains("event.button"),
                "Should capture event.button");
        assertTrue(expressions.contains("event.clientX"),
                "Should capture event.clientX");
        assertTrue(expressions.contains("event.clientY"),
                "Should capture event.clientY");
        assertTrue(expressions.contains("type"), "Should capture type");

        // Should have exactly these 4 expressions
        assertEquals(4, expressions.size(), "Should have 4 expressions");
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
        assertTrue(expressions.contains("message"), "Should capture message");
        assertTrue(expressions.contains("code"), "Should capture code");
        assertEquals(2, expressions.size(), "Should have 2 expressions");
    }

    @Test
    public void testAddEventDetail() {
        // Test that addEventDetail adds "event.detail" to expressions
        DomListenerRegistration registration = ns.add("color-change", noOp);
        registration.addEventDetail();

        Set<String> expressions = getExpressions("color-change");

        // Should have captured event.detail
        assertTrue(expressions.contains("event.detail"),
                "Should capture event.detail");
        assertEquals(1, expressions.size(), "Should have 1 expression");
    }

    @Test
    public void testAddEventDetailChaining() {
        // Test that addEventDetail can be chained with other methods
        DomListenerRegistration registration = ns.add("custom-event", noOp);
        registration.addEventDetail().addEventData("event.timestamp");

        Set<String> expressions = getExpressions("custom-event");

        // Should have both event.detail and event.timestamp
        assertTrue(expressions.contains("event.detail"),
                "Should capture event.detail");
        assertTrue(expressions.contains("event.timestamp"),
                "Should capture event.timestamp");
        assertEquals(2, expressions.size(), "Should have 2 expressions");
    }

    @Test
    public void testAddEventDetailWithClass() {
        // Test that addEventDetail(Class) adds specific properties from
        // event.detail
        record RgbColor(int r, int g, int b) {
        }

        DomListenerRegistration registration = ns.add("color-change", noOp);
        registration.addEventDetail(RgbColor.class);

        Set<String> expressions = getExpressions("color-change");

        // Should have captured all properties with event.detail prefix
        assertTrue(expressions.contains("event.detail.r"),
                "Should capture event.detail.r");
        assertTrue(expressions.contains("event.detail.g"),
                "Should capture event.detail.g");
        assertTrue(expressions.contains("event.detail.b"),
                "Should capture event.detail.b");

        // Should NOT have the entire event.detail
        assertFalse(expressions.contains("event.detail"),
                "Should NOT capture entire event.detail");

        // Should have exactly these 3 expressions
        assertEquals(3, expressions.size(), "Should have 3 expressions");
    }

    @Test
    public void testAddEventDetailWithClassAndGetEventDetail() {
        // Test full flow: addEventDetail(Class) and getEventDetail(Class)
        record RgbColor(int r, int g, int b) {
        }

        Element element = new Element("div");
        AtomicReference<RgbColor> capturedColor = new AtomicReference<>();
        element.addEventListener("color-change",
                e -> capturedColor.set(e.getEventDetail(RgbColor.class)))
                .addEventDetail(RgbColor.class);

        ElementListenerMap listenerMap = element.getNode()
                .getFeature(ElementListenerMap.class);

        // Verify the expressions are correct
        Set<String> expressions = getExpressions(listenerMap, "color-change");
        assertTrue(expressions.contains("event.detail.r"),
                "Should capture event.detail.r");
        assertTrue(expressions.contains("event.detail.g"),
                "Should capture event.detail.g");
        assertTrue(expressions.contains("event.detail.b"),
                "Should capture event.detail.b");

        // Fire event with detail data
        ObjectNode eventData = JacksonUtils.createObjectNode();
        eventData.put("event.detail.r", 255);
        eventData.put("event.detail.g", 128);
        eventData.put("event.detail.b", 64);

        listenerMap.fireEvent(new DomEvent(element, "color-change", eventData));

        // Verify the data was captured correctly
        RgbColor result = capturedColor.get();
        assertNotNull(result, "Should have captured color");
        assertEquals(255, result.r(), "Red should be 255");
        assertEquals(128, result.g(), "Green should be 128");
        assertEquals(64, result.b(), "Blue should be 64");
    }

    @Test
    public void testAddEventDetailWithNestedClass() {
        // Test that nested properties work correctly
        record Position(int x, int y) {
        }
        record DragDetail(Position start, Position end) {
        }

        DomListenerRegistration registration = ns.add("drag", noOp);
        registration.addEventDetail(DragDetail.class);

        Set<String> expressions = getExpressions("drag");

        // Should have captured all nested properties with event.detail prefix
        assertTrue(expressions.contains("event.detail.start.x"),
                "Should capture event.detail.start.x");
        assertTrue(expressions.contains("event.detail.start.y"),
                "Should capture event.detail.start.y");
        assertTrue(expressions.contains("event.detail.end.x"),
                "Should capture event.detail.end.x");
        assertTrue(expressions.contains("event.detail.end.y"),
                "Should capture event.detail.end.y");

        // Should have exactly these 4 expressions
        assertEquals(4, expressions.size(), "Should have 4 expressions");
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

        assertNotNull(color, "Color should not be null");
        assertEquals(255, color.r(), "Red should be 255");
        assertEquals(128, color.g(), "Green should be 128");
        assertEquals(64, color.b(), "Blue should be 64");
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

        assertNotNull(payload, "Payload should not be null");
        assertEquals("Hello World", payload.getMessage(),
                "Message should match");
        assertEquals(42, payload.getCode(), "Code should match");
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

        assertNotNull(items, "Items should not be null");
        assertEquals(3, items.size(), "Should have 3 items");
        assertEquals("first", items.get(0), "First item");
        assertEquals("second", items.get(1), "Second item");
        assertEquals("third", items.get(2), "Third item");
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

        assertNull(data, "Should return null when event.detail not present");
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

        assertNull(data, "Should return null when event.detail is null");
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
