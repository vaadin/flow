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
package com.vaadin.flow.dom;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

import net.jcip.annotations.NotThreadSafe;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.BaseJsonNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.component.internal.UIInternals.JavaScriptInvocation;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.dom.impl.BasicElementStateProvider;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.NullOwner;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ComponentMapping;
import com.vaadin.flow.internal.nodefeature.ElementAttributeMap;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;
import com.vaadin.flow.internal.nodefeature.ElementListenersTest;
import com.vaadin.flow.internal.nodefeature.ElementPropertyMap;
import com.vaadin.flow.internal.nodefeature.ElementStylePropertyMap;
import com.vaadin.flow.internal.nodefeature.VirtualChildrenList;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.JsonConstants;
import com.vaadin.flow.shared.Registration;
import com.vaadin.tests.util.AlwaysLockedVaadinSession;
import com.vaadin.tests.util.MockUI;
import com.vaadin.tests.util.TestUtil;

import static org.junit.jupiter.api.Assertions.assertThrows;

@NotThreadSafe
class ElementJacksonTest extends AbstractNodeTest {

    @Test
    public void createElementWithTag() {
        Element e = ElementFactory.createDiv();
        Assertions.assertEquals(Tag.DIV, e.getTag());
        Assertions.assertFalse(e.hasAttribute("is"));
        Assertions.assertFalse(e.isTextNode());
    }

    @Test
    public void createElementWithInvalidTag() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Element("<div>");
        });
    }

    @Test
    public void createElementWithEmptyTag() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Element("");
        });
    }

    @Test
    public void createElementWithNullTag() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Element(null);
        });
    }

    @Test
    public void elementsUpdateSameData() {
        Element te = new Element("testelem");
        Element e = Element.get(te.getNode());

        // Elements must be equal but not necessarily the same
        Assertions.assertEquals(te, e);

        te.setAttribute("foo", "bar");
        Assertions.assertEquals("bar", e.getAttribute("foo"));

        e.setAttribute("baz", "123");
        Assertions.assertEquals("123", te.getAttribute("baz"));
    }

    @Test
    public void getElementFromInvalidNode() {
        assertThrows(IllegalArgumentException.class, () -> {
            StateNode node = new StateNode(ElementPropertyMap.class);
            Element.get(node);
        });
    }

    @Test
    public void publicElementMethodsShouldReturnElement() {
        Set<String> ignore = new HashSet<>();
        ignore.add("toString");
        ignore.add("hashCode");
        ignore.add("equals");

        // Returns EventRegistrationHandle
        ignore.add("addEventListener");
        ignore.add("addAttachListener");
        ignore.add("addDetachListener");
        ignore.add("addPropertyChangeListener");

        // Returns index of child element
        ignore.add("indexOfChild");
        // Component wrapper
        ignore.add("as");
        // Possibly returns a remover or a wrapped return value in the future
        ignore.add("callFunction");
        ignore.add("executeJavaScript");
        // Returns a future-ish thing with access to the return value
        ignore.add("callJsFunction");
        ignore.add("executeJs");

        // ignore shadow root methods
        ignore.add("attachShadow");
        ignore.add("getShadowRoot");

        // ignore signal binding methods
        ignore.add("bindEnabled");
        ignore.add("bindProperty");
        ignore.add("bindAttribute");
        ignore.add("bindText");
        ignore.add("bindVisible");

        assertMethodsReturnType(Element.class, ignore);
    }

    @Test
    public void publicElementStyleMethodsShouldReturnElement() {
        Set<String> ignore = new HashSet<>();
        ignore.add("toString");
        ignore.add("hashCode");
        ignore.add("equals");

        for (Method m : Style.class.getDeclaredMethods()) {
            if (!Modifier.isPublic(m.getModifiers())) {
                continue;
            }
            if (Modifier.isStatic(m.getModifiers())) {
                continue;
            }
            if (m.getName().startsWith("get") || m.getName().startsWith("has")
                    || m.getName().startsWith("is")
                    || ignore.contains(m.getName())) {
                // Ignore
            } else {
                // Setters and such
                Class<?> returnType = m.getReturnType();
                Assertions.assertEquals(Style.class, returnType,
                        "Method " + m.getName() + " has invalid return type");
            }
        }

    }

    @Test
    public void stringAttribute() {
        Element e = ElementFactory.createDiv();
        e.setAttribute("foo", "bar");
        Assertions.assertEquals("bar", e.getAttribute("foo"));
    }

    @Test
    public void setEmptyAttribute() {
        Element e = ElementFactory.createDiv();
        e.setAttribute("foo", "");
        Assertions.assertEquals("", e.getAttribute("foo"));
    }

    @Test
    public void setBooleanAttribute() {
        Element e = ElementFactory.createDiv();

        e.setAttribute("foo", true);
        Assertions.assertEquals("", e.getAttribute("foo"));
        Assertions.assertTrue(e.hasAttribute("foo"));

        e.setAttribute("foo", false);
        Assertions.assertEquals(null, e.getAttribute("foo"));
        Assertions.assertFalse(e.hasAttribute("foo"));
    }

    @Test
    public void setNullAttribute() {
        assertThrows(IllegalArgumentException.class, () -> {
            Element e = ElementFactory.createDiv();
            e.setAttribute("foo", (String) null);
        });
    }

    @Test
    public void getNullAttribute() {
        assertThrows(IllegalArgumentException.class, () -> {
            Element e = ElementFactory.createDiv();
            e.getAttribute(null);
        });
    }

    @Test
    public void hasNullAttribute() {
        assertThrows(IllegalArgumentException.class, () -> {
            Element e = ElementFactory.createDiv();
            e.hasAttribute(null);
        });
    }

    @Test
    public void removeNullAttribute() {
        assertThrows(IllegalArgumentException.class, () -> {
            Element e = ElementFactory.createDiv();
            e.removeAttribute(null);
        });
    }

    @Test
    public void setInvalidAttribute() {
        assertThrows(IllegalArgumentException.class, () -> {
            Element e = ElementFactory.createDiv();
            e.setAttribute("\"foo\"", "bar");
        });
    }

    @Test
    public void hasDefinedAttribute() {
        Element e = ElementFactory.createDiv();
        e.setAttribute("foo", "bar");
        Assertions.assertTrue(e.hasAttribute("foo"));
    }

    @Test
    public void doesNotHaveUndefinedAttribute() {
        Element e = ElementFactory.createDiv();
        Assertions.assertFalse(e.hasAttribute("foo"));
    }

    @Test
    public void doesNotHaveRemovedAttribute() {
        Element e = ElementFactory.createDiv();
        e.setAttribute("foo", "bar");
        e.removeAttribute("foo");
        Assertions.assertFalse(e.hasAttribute("foo"));
    }

    @Test
    public void removeNonExistingAttributeIsNoOp() {
        Element e = ElementFactory.createDiv();
        Assertions.assertFalse(e.hasAttribute("foo"));
        e.removeAttribute("foo");
        Assertions.assertFalse(e.hasAttribute("foo"));
    }

    @Test
    public void attributesWhenNoneDefined() {
        Element e = ElementFactory.createDiv();
        Assertions.assertEquals(0, e.getAttributeNames().count());
    }

    @Test
    public void attributesNames() {
        Element e = ElementFactory.createDiv();
        e.setAttribute("foo", "bar");
        Assertions.assertArrayEquals(new String[] { "foo" },
                e.getAttributeNames().toArray());
    }

    @Test
    public void attributesNamesAfterRemoved() {
        Element e = ElementFactory.createDiv();
        e.setAttribute("foo", "bar");
        e.setAttribute("bar", "baz");
        e.removeAttribute("foo");
        Assertions.assertArrayEquals(new String[] { "bar" },
                e.getAttributeNames().toArray());
    }

    @Test
    public void setGetAttributeValueCaseSensitive() {
        Element e = new Element(Tag.SPAN);
        e.setAttribute("foo", "bAr");
        Assertions.assertEquals("bAr", e.getAttribute("foo"));
        e.setAttribute("foo", "BAR");
        Assertions.assertEquals("BAR", e.getAttribute("foo"));
    }

    @Test
    public void setGetAttributeNameCaseInsensitive() {
        Element e = new Element(Tag.SPAN);
        e.setAttribute("foo", "bar");
        e.setAttribute("FOO", "baz");

        Assertions.assertEquals("baz", e.getAttribute("foo"));
        Assertions.assertEquals("baz", e.getAttribute("FOO"));
    }

    @Test
    public void hasAttributeNamesCaseInsensitive() {
        Element e = new Element(Tag.SPAN);
        e.setAttribute("fooo", "bar");
        Assertions.assertTrue(e.hasAttribute("fOoO"));
    }

    @Test
    public void getAttributeNamesLowerCase() {
        Element e = new Element(Tag.SPAN);
        e.setAttribute("FOO", "bar");
        e.setAttribute("Baz", "bar");

        Set<String> attributeNames = e.getAttributeNames()
                .collect(Collectors.toSet());
        Assertions.assertTrue(attributeNames.contains("foo"));
        Assertions.assertFalse(attributeNames.contains("FOO"));
        Assertions.assertTrue(attributeNames.contains("baz"));
        Assertions.assertFalse(attributeNames.contains("Baz"));
    }

    @Test
    public void removeDetachedFromParent() {
        Element otherElement = new Element("other");
        Assertions.assertNull(otherElement.getParent());
        otherElement.removeFromParent(); // No op
        Assertions.assertNull(otherElement.getParent());
    }

    @Test
    public void getDetachedParent() {
        Element otherElement = new Element("other");
        Assertions.assertNull(otherElement.getParent());
        Assertions.assertNull(otherElement.getParentNode());
    }

    @Test
    public void addNullEventListener() {
        assertThrows(IllegalArgumentException.class, () -> {
            Element e = ElementFactory.createDiv();
            e.addEventListener("foo", null);
        });
    }

    @Test
    public void addEventListenerForNullType() {
        assertThrows(IllegalArgumentException.class, () -> {
            Element e = ElementFactory.createDiv();
            e.addEventListener(null, ignore -> {
            });
        });
    }

    @Test
    public void equalsSelf() {
        Element e = ElementFactory.createDiv();
        Assertions.assertTrue(e.equals(e));
    }

    @Test
    public void notEqualsNull() {
        Element e = ElementFactory.createDiv();
        Assertions.assertFalse(e.equals(null));
    }

    @Test
    public void notEqualsString() {
        Element e = ElementFactory.createDiv();
        Assertions.assertFalse(e.equals(Tag.DIV));
    }

    // @Test
    // public void listenerReceivesEvents() {
    // Element e = ElementFactory.createDiv();
    // AtomicInteger listenerCalls = new AtomicInteger(0);
    // DomEventListener myListener = event -> listenerCalls.incrementAndGet();
    //
    // e.addEventListener("click", myListener);
    // Assertions.assertEquals(0, listenerCalls.get());
    // e.getNode().getFeature(ElementListenerMap.class)
    // .fireEvent(new DomEvent(e, "click", JacksonUtils.createObjectNode()));
    // Assertions.assertEquals(1, listenerCalls.get());
    // }
    //
    // @Test
    // public void listenerReceivesEventsWithAllowInert() {
    // Element e = ElementFactory.createDiv();
    // // Inert the node, verify events no more passed through
    // InertData inertData = e.getNode().getFeature(InertData.class);
    // inertData.setInertSelf(true);
    // inertData.generateChangesFromEmpty();
    //
    // AtomicInteger listenerCalls = new AtomicInteger(0);
    // DomEventListener myListener = event -> listenerCalls.incrementAndGet();
    //
    // DomListenerRegistration domListenerRegistration = e
    // .addEventListener("click", myListener);
    // Assertions.assertEquals(0, listenerCalls.get());
    // e.getNode().getFeature(ElementListenerMap.class)
    // .fireEvent(new DomEvent(e, "click", JacksonUtils.createObjectNode()));
    // // Event should not go through
    // Assertions.assertEquals(0, listenerCalls.get());
    //
    // // Now should pass inert check and get notified
    // domListenerRegistration.allowInert();
    // e.getNode().getFeature(ElementListenerMap.class)
    // .fireEvent(new DomEvent(e, "click", JacksonUtils.createObjectNode()));
    // Assertions.assertEquals(1, listenerCalls.get());
    //
    // }

    @Test
    public void getPropertyDefaults() {
        Element element = ElementFactory.createDiv();

        element.setProperty("null", null);
        element.setProperty("empty", "");

        Assertions.assertEquals("d", element.getProperty("null", "d"));
        Assertions.assertEquals("d", element.getProperty("notThere", "d"));
        Assertions.assertNotEquals("d", element.getProperty("empty", "d"));

        Assertions.assertTrue(element.getProperty("null", true));
        Assertions.assertFalse(element.getProperty("null", false));
        Assertions.assertTrue(element.getProperty("notThere", true));
        Assertions.assertFalse(element.getProperty("notThere", false));
        Assertions.assertFalse(element.getProperty("empty", true));
        Assertions.assertFalse(element.getProperty("empty", false));

        Assertions.assertEquals(0.1, element.getProperty("null", 0.1), 0);
        Assertions.assertEquals(0.1, element.getProperty("notThere", 0.1), 0);
        Assertions.assertNotEquals(0.1, element.getProperty("empty", 0.1), 0);

        Assertions.assertEquals(42, element.getProperty("null", 42));
        Assertions.assertEquals(42, element.getProperty("notThere", 42));
        Assertions.assertNotEquals(42, element.getProperty("empty", 42));
    }

    @Test
    public void getPropertyStringConversions() {
        assertPropertyString(null, null);
        assertPropertyString("foo", "foo");
        assertPropertyString("", "");
        assertPropertyString("45.6e1", "45.6e1");
        assertPropertyString("true", Boolean.TRUE);
        assertPropertyString("false", Boolean.FALSE);
        assertPropertyString(String.valueOf(143534123423.243e23),
                Double.valueOf(143534123423.243e23));
        assertPropertyString("42", Double.valueOf(42));

        assertPropertyString(null, JacksonUtils.nullNode());
        assertPropertyString("{}", JacksonUtils.createObjectNode());
    }

    private static void assertPropertyString(String expected, Object value) {
        Element element = createPropertyAssertElement(value);

        Assertions.assertEquals(expected, element.getProperty("property"));
    }

    @Test
    public void testPropertyBooleanConversions() {
        assertPropertyBoolean(true, Boolean.TRUE);
        assertPropertyBoolean(false, Boolean.FALSE);

        assertPropertyBoolean(true, "true");
        assertPropertyBoolean(true, "false");
        assertPropertyBoolean(false, "");

        assertPropertyBoolean(true, Double.valueOf(1));
        assertPropertyBoolean(true, Double.valueOf(3.14));
        assertPropertyBoolean(false, Double.valueOf(0));
        assertPropertyBoolean(false, Double.valueOf(Double.NaN));

        assertPropertyBoolean(false, JacksonUtils.nullNode());
        assertPropertyBoolean(false, JacksonUtils.createNode(false));
        assertPropertyBoolean(true, JacksonUtils.createNode(true));
        assertPropertyBoolean(true, JacksonUtils.createObjectNode());
    }

    private static void assertPropertyBoolean(boolean expected, Object value) {
        Element element = createPropertyAssertElement(value);

        // !expected -> default value will always fail
        boolean actual = element.getProperty("property", !expected);

        if (expected) {
            Assertions.assertTrue(actual);
        } else {
            Assertions.assertFalse(actual);
        }
    }

    @Test
    public void testPropertyDoubleConversions() {
        assertPropertyDouble(1, Double.valueOf(1));
        assertPropertyDouble(.1, Double.valueOf(.1));
        assertPropertyDouble(Double.NaN, Double.valueOf(Double.NaN));

        assertPropertyDouble(1, "1");
        assertPropertyDouble(.1, ".1");
        assertPropertyDouble(12.34e56, "12.34e56");
        assertPropertyDouble(Double.NaN, "foo");

        assertPropertyDouble(1, Boolean.TRUE);
        assertPropertyDouble(0, Boolean.FALSE);

        assertPropertyDouble(.1, JacksonUtils.createNode(.1));
        assertPropertyDouble(1, JacksonUtils.createNode(true));
        assertPropertyDouble(0, JacksonUtils.createNode(false));
        assertPropertyDouble(.1, JacksonUtils.createNode(".1"));
        assertPropertyDouble(Double.NaN, JacksonUtils.createNode("foo"));
        assertPropertyDouble(Double.NaN, JacksonUtils.createObjectNode());
    }

    private static void assertPropertyDouble(double expected, Object value) {
        Element element = createPropertyAssertElement(value);

        int delta = 0;
        double defaultValue = 1234d;

        if (defaultValue == expected) {
            throw new IllegalArgumentException(
                    "Expecting the default value might cause unintended results");
        }

        Assertions.assertEquals(expected,
                element.getProperty("property", defaultValue), delta);
    }

    @Test
    public void testPropertyIntConversions() {
        assertPropertyInt(1, Double.valueOf(1));
        assertPropertyInt(1, Double.valueOf(1.9));
        assertPropertyInt(0, Double.valueOf(Double.NaN));
        assertPropertyInt(Integer.MAX_VALUE, Double.valueOf(12.34e56));

        assertPropertyInt(1, "1");
        assertPropertyInt(1, "1.9");
        assertPropertyInt(Integer.MAX_VALUE, "12.34e56");
        assertPropertyInt(0, "foo");

        assertPropertyInt(1, Boolean.TRUE);
        assertPropertyInt(0, Boolean.FALSE);

        assertPropertyInt(1, JacksonUtils.createNode(1));
        assertPropertyInt(1, JacksonUtils.createNode(1.9));
        assertPropertyInt(1, JacksonUtils.createNode(true));
        assertPropertyInt(0, JacksonUtils.createNode(false));
        assertPropertyInt(1, JacksonUtils.createNode("1"));
        assertPropertyInt(0, JacksonUtils.createNode("foo"));
        assertPropertyInt(0, JacksonUtils.createObjectNode());
    }

    private static void assertPropertyInt(int expected, Object value) {
        Element element = createPropertyAssertElement(value);

        int defaultValue = 1234;

        if (defaultValue == expected) {
            throw new IllegalArgumentException(
                    "Expecting the default value might cause unintended results");
        }

        Assertions.assertEquals(expected,
                element.getProperty("property", defaultValue));
    }

    public static class SimpleBean {
        private String string = "value";
        private int number = 1;
        private float flt = 2.3f;
        private double dbl = 4.56;

        public String getString() {
            return string;
        }

        public int getNumber() {
            return number;
        }

        public float getFlt() {
            return flt;
        }

        public double getDbl() {
            return dbl;
        }
    }

    public static class BeanWithTemporalFields {

        public LocalTime localTime = LocalTime.of(10, 23, 55);

        public LocalDate localDate = LocalDate.of(2024, 6, 26);

        public LocalDateTime localDateTime = localDate.atTime(localTime);

        public java.sql.Date sqlDate = java.sql.Date.valueOf(localDate);

        public Date date = new Date(sqlDate.getTime());

        public ZonedDateTime zonedDateTime = ZonedDateTime.of(localDateTime,
                ZoneId.of("Europe/Rome"));

        public Duration duration = Duration.ofSeconds(10);
    }

    @Test
    public void propertyRawValues() {
        Element element = ElementFactory.createDiv();

        element.setProperty("p", "v");
        Assertions.assertEquals("v", element.getPropertyRaw("p"));

        element.setProperty("p", true);
        Assertions.assertEquals(Boolean.TRUE, element.getPropertyRaw("p"));

        element.setProperty("p", 3.14);
        Assertions.assertEquals(Double.valueOf(3.14),
                element.getPropertyRaw("p"));

        element.setPropertyJson("p", JacksonUtils.createObjectNode());
        Assertions.assertEquals(ObjectNode.class,
                element.getPropertyRaw("p").getClass());

        // TODO: Use setPropertyBean when updated to jackson
        element.setPropertyJson("p", JacksonUtils.beanToJson(new SimpleBean()));
        JsonNode json = (JsonNode) element.getPropertyRaw("p");
        Assertions.assertEquals("value", json.get("string").textValue());
        Assertions.assertEquals(1.0, json.get("number").doubleValue(), 0.0);
        Assertions.assertEquals(2.3f, json.get("flt").floatValue(), 0.0);
        Assertions.assertEquals(4.56, json.get("dbl").doubleValue(), 0.0);

        List<SimpleBean> list = new ArrayList<>();
        SimpleBean bean1 = new SimpleBean();
        bean1.string = "bean1";
        SimpleBean bean2 = new SimpleBean();
        bean2.string = "bean2";
        list.add(bean1);
        list.add(bean2);

        // TODO: Use setPropertyList when updated to jackson
        element.setPropertyJson("p", JacksonUtils.listToJson(list));
        ArrayNode jsonArray = (ArrayNode) element.getPropertyRaw("p");
        Assertions.assertEquals("bean1",
                jsonArray.get(0).get("string").textValue());
        Assertions.assertEquals("bean2",
                jsonArray.get(1).get("string").textValue());

        Map<String, SimpleBean> map = new HashMap<>();
        map.put("one", bean1);
        map.put("two", bean2);
        // TODO: Use setPropertyMap when updated to jackson
        element.setPropertyJson("p", JacksonUtils.mapToJson(map));
        JsonNode jsonObject = (JsonNode) element.getPropertyRaw("p");
        Assertions.assertEquals("bean1",
                jsonObject.get("one").get("string").textValue());
        Assertions.assertEquals("bean2",
                jsonObject.get("two").get("string").textValue());
    }

    @Test
    public void addAndRemoveProperty() {
        Element element = ElementFactory.createDiv();

        Assertions.assertFalse(element.hasProperty("foo"));
        element.removeProperty("foo");
        Assertions.assertFalse(element.hasProperty("foo"));

        element.setProperty("foo", "bar");
        Assertions.assertTrue(element.hasProperty("foo"));
        element.setProperty("foo", null);
        Assertions.assertTrue(element.hasProperty("foo"));

        element.removeProperty("foo");
        Assertions.assertFalse(element.hasProperty("foo"));
    }

    @Test
    public void propertyNames() {
        Element element = ElementFactory.createDiv();

        Assertions.assertEquals(0, element.getPropertyNames().count());

        element.setProperty("foo", "bar");
        Assertions.assertEquals(Collections.singleton("foo"),
                element.getPropertyNames().collect(Collectors.toSet()));

        element.removeProperty("foo");
        Assertions.assertEquals(0, element.getPropertyNames().count());
    }

    @Test
    public void setProperty_javaTimeObject() {
        BeanWithTemporalFields bean = new BeanWithTemporalFields();
        Element element = ElementFactory.createDiv();

        // TODO: Use setPropertyBean when updated to jackson
        element.setPropertyBean("bean", bean);
        ObjectNode json = (ObjectNode) element.getPropertyRaw("bean");

        Assertions.assertTrue(
                JacksonUtils.jsonEquals(JacksonUtils.createNode("10:23:55"),
                        json.get("localTime")),
                "LocalTime not serialized as expected");
        Assertions.assertTrue(
                JacksonUtils.jsonEquals(JacksonUtils.createNode("2024-06-26"),
                        json.get("localDate")),
                "LocalDate not serialized as expected");
        Assertions.assertTrue(
                JacksonUtils.jsonEquals(
                        JacksonUtils.createNode("2024-06-26T10:23:55"),
                        json.get("localDateTime")),
                "LocalDateTime not serialized as expected");
        Assertions.assertEquals(bean.zonedDateTime.toEpochSecond(),
                ZonedDateTime.parse(json.get("zonedDateTime").asString())
                        .toEpochSecond(),
                0, "ZonedDateTime not serialized as expected");
        Assertions.assertEquals(bean.sqlDate.getTime(),
                ZonedDateTime.parse(json.get("sqlDate").asString()).toInstant()
                        .toEpochMilli(),
                0, "ZonedDateTime not serialized as expected");
        Assertions.assertEquals(bean.date.getTime(),
                ZonedDateTime.parse(json.get("date").asString()).toInstant()
                        .toEpochMilli(),
                0, "ZonedDateTime not serialized as expected");
        Assertions.assertEquals(10.0,
                Duration.parse(json.get("duration").asString()).toSeconds(), 0);
    }

    private static Element createPropertyAssertElement(Object value) {
        Element element = ElementFactory.createDiv();

        if (value instanceof Number && !(value instanceof Double)) {
            throw new IllegalArgumentException(
                    "Double is the only accepted numeric type");
        }

        if (value instanceof BaseJsonNode) {
            element.setPropertyJson("property", (BaseJsonNode) value);
        } else if (value instanceof Serializable) {
            BasicElementStateProvider.get().setProperty(element.getNode(),
                    "property", (Serializable) value, true);
        } else if (value == null) {
            element.setProperty("property", null);
        } else {
            throw new IllegalArgumentException(
                    "Invalid value type: " + value.getClass());
        }

        return element;
    }

    @Test
    public void testGetTextContent() {
        Element child = new Element("child");
        child.appendChild(Element.createText("bar"));

        Element element = ElementFactory.createDiv();

        element.appendChild(Element.createText("foo"));
        element.appendChild(child);

        Assertions.assertEquals("foobar", element.getTextRecursively());
    }

    @Test
    public void testSetTextContent() {
        Element element = ElementFactory.createDiv();
        element.setText("foo");

        Assertions.assertEquals("foo", element.getTextRecursively());
        Assertions.assertEquals(1, element.getChildCount());
        Assertions.assertTrue(element.getChild(0).isTextNode());
    }

    @Test
    public void testSetTextContentRemovesOldContent() {
        Element child = new Element("child");
        Element element = ElementFactory.createDiv();
        element.appendChild(child);

        element.setText("foo");

        Assertions.assertNull(child.getParent());
        Assertions.assertEquals("foo", element.getTextRecursively());
    }

    @Test
    public void testSetTextReplacesOldTextNode() {
        Element element = ElementFactory.createDiv();
        Element text = Element.createText("foo");
        element.appendChild(text);

        element.setText("bar");

        Assertions.assertEquals(element, text.getParent());
        Assertions.assertEquals("bar", text.getTextRecursively());
    }

    @Test
    public void testSetTextContentPropertyThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            Element element = new Element("element");
            element.setProperty("textContent", "foo");
        });
    }

    @Test
    public void setOuterHtmlProperty_throws() {
        assertThrows(IllegalArgumentException.class, () -> {
            Element element = new Element("element");
            element.setProperty("outerHTML", "<br>");
        });
    }

    @Test
    public void setInnerHtmlProeprty_setValueAndRemoveAllChildren() {
        Element element = new Element("element");
        element.appendChild(ElementFactory.createAnchor(),
                ElementFactory.createDiv());
        element.setProperty("innerHTML", "<br>");

        Assertions.assertEquals(0, element.getChildCount());
        Assertions.assertEquals("<br>", element.getProperty("innerHTML"));
    }

    @Test
    public void testGetTextContentProperty() {
        Element element = ElementFactory.createDiv();
        element.setText("foo");

        Assertions.assertFalse(element.hasProperty("textContent"));
        Assertions.assertNull(element.getProperty("textContent"));
    }

    @Test
    // Because that's how it works in browsers
    public void clearTextContentRemovesChild() {
        Element element = ElementFactory.createDiv();
        element.setText("foo");

        Assertions.assertEquals(1, element.getChildCount());

        element.setText("");

        Assertions.assertEquals(0, element.getChildCount());
    }

    @Test
    public void newElementClasses() {
        Element element = ElementFactory.createDiv();

        Assertions.assertFalse(element.hasAttribute("class"));
        Assertions.assertEquals(Collections.emptySet(), element.getClassList());
    }

    @Test
    public void addElementClasses() {
        Element element = ElementFactory.createDiv();

        element.getClassList().add("foo");

        Assertions.assertEquals(Collections.singleton("foo"),
                element.getClassList());
        Assertions.assertTrue(element.hasAttribute("class"));

        Assertions.assertEquals(Collections.singleton("class"),
                element.getAttributeNames().collect(Collectors.toSet()));
        Assertions.assertTrue(element.hasAttribute("class"));
        Assertions.assertEquals("foo", element.getAttribute("class"));

        element.getClassList().add("bar");

        Assertions.assertEquals("foo bar", element.getAttribute("class"));
    }

    @Test
    public void testSetClassAttribute() {
        Element element = ElementFactory.createDiv();

        // Get instance right away to see that changes are live
        Set<String> classList = element.getClassList();

        element.setAttribute("class", "       foo bar ");

        Assertions.assertEquals(2, classList.size());
        Assertions.assertTrue(classList.contains("foo"));
        Assertions.assertTrue(classList.contains("bar"));

        Assertions.assertNull(
                element.getNode().getFeature(ElementAttributeMap.class)
                        .get("class"),
                "class should not be stored as a regular attribute");
    }

    @Test
    public void testSetEmptyClassAttribute() {
        Element element = new Element(Tag.DIV);

        // Get instance right away to see that changes are live
        Set<String> classList = element.getClassList();

        element.setAttribute("class", "");

        Assertions.assertEquals(0, classList.size());
    }

    @Test
    public void testAddEmptyClassname() {
        assertThrows(IllegalArgumentException.class, () -> {
            Element element = new Element(Tag.DIV);

            // Get instance right away to see that changes are live
            Set<String> classList = element.getClassList();

            classList.add("");
        });
    }

    @Test
    public void testRemoveClassName() {
        Element element = ElementFactory.createDiv();

        element.setAttribute("class", "foo bar");

        element.getClassList().remove("foo");

        Assertions.assertEquals("bar", element.getAttribute("class"));

        element.getClassList().remove("bar");

        Assertions.assertNull(element.getAttribute("class"));
        Assertions.assertFalse(element.hasAttribute("class"));

        Assertions.assertEquals(0, element.getAttributeNames().count());
    }

    @Test
    public void testRemoveClassAttribute() {
        Element element = ElementFactory.createDiv();

        Set<String> classList = element.getClassList();

        classList.add("foo");

        element.removeAttribute("class");

        Assertions.assertEquals(Collections.emptySet(), classList);
    }

    @Test
    public void addExistingClass_noop() {
        Element element = ElementFactory.createDiv();

        element.setAttribute("class", "foo");

        element.getClassList().add("foo");

        Assertions.assertEquals(Collections.singleton("foo"),
                element.getClassList());
    }

    @Test
    public void testAddClassWithSpaces_throws() {
        assertThrows(IllegalArgumentException.class, () -> {
            ElementFactory.createDiv().getClassList().add("foo bar");
        });
    }

    @Test
    public void testRemoveClassWithSpaces() {
        ClassList cl = ElementFactory.createDiv().getClassList();
        cl.add("foo");
        cl.add("bar");
        cl.remove("foo bar");
        Assertions.assertEquals(2, cl.size());
    }

    @Test
    public void testContainsClassWithSpaces() {
        ClassList cl = ElementFactory.createDiv().getClassList();
        cl.add("foo");
        cl.add("bar");

        Assertions.assertFalse(cl.contains("foo bar"));
    }

    @Test
    public void classListSetAdd() {
        Element e = new Element(Tag.DIV);
        Assertions.assertTrue(e.getClassList().set("foo", true));
        Assertions.assertEquals("foo", e.getAttribute("class"));
        Assertions.assertFalse(e.getClassList().set("foo", true));
        Assertions.assertEquals("foo", e.getAttribute("class"));
    }

    @Test
    public void classListSetRemove() {
        Element e = new Element(Tag.DIV);
        e.setAttribute("class", "foo bar");
        Assertions.assertTrue(e.getClassList().set("foo", false));
        Assertions.assertEquals("bar", e.getAttribute("class"));
        Assertions.assertFalse(e.getClassList().set("foo", false));
        Assertions.assertEquals("bar", e.getAttribute("class"));
    }

    @Test
    public void testClassListProperty_throws() {
        assertThrows(IllegalArgumentException.class, () -> {
            ElementFactory.createDiv().setProperty("classList", "foo");
        });
    }

    @Test
    public void testClassNameProperty_throws() {
        assertThrows(IllegalArgumentException.class, () -> {
            ElementFactory.createDiv().setProperty("className", "foo");
        });
    }

    @Test
    public void setStyle() {
        Element e = ElementFactory.createDiv();
        Style s = e.getStyle();
        s.set("foo", "bar");
        Assertions.assertEquals("bar", s.get("foo"));
        s.set("--lumo-primary-text-color", "hsl(12, 12%, 12%)");
        Assertions.assertEquals("hsl(12, 12%, 12%)",
                s.get("--lumo-primary-text-color"));
    }

    @Test
    public void getUnsetStyle() {
        Element e = ElementFactory.createDiv();
        Style s = e.getStyle();
        Assertions.assertNull(s.get("foo"));
    }

    @Test
    public void getNullStyle() {
        assertThrows(IllegalArgumentException.class, () -> {
            Element e = ElementFactory.createDiv();
            Style s = e.getStyle();
            s.get(null);
        });
    }

    @Test
    public void replaceStyle() {
        Element e = ElementFactory.createDiv();
        Style s = e.getStyle();
        s.set("foo", "bar");
        s.set("foo", "baz");
        Assertions.assertEquals("baz", s.get("foo"));
    }

    @Test
    public void removeSingleStyle() {
        Element e = ElementFactory.createDiv();
        Style s = e.getStyle();
        s.set("foo", "bar");
        s.remove("foo");
        Assertions.assertEquals(null, s.get("foo"));
    }

    @Test
    public void emptyStyleAsAttribute() {
        Element e = ElementFactory.createDiv();
        Assertions.assertFalse(e.hasAttribute("style"));
        Assertions.assertNull(e.getAttribute("style"));
    }

    @Test
    public void semicolonInStyle() {
        assertThrows(IllegalArgumentException.class, () -> {
            Element e = ElementFactory.createDiv();
            Style s = e.getStyle();
            s.set("border", "1 px solid black;");
        });
    }

    @Test
    public void getSingleStyleAsAttribute() {
        Element e = ElementFactory.createDiv();
        Style s = e.getStyle();
        s.setBorder("1px solid black");
        Assertions.assertTrue(e.hasAttribute("style"));
        Assertions.assertEquals("border:1px solid black",
                e.getAttribute("style"));
    }

    @Test
    public void getMultipleStylesAsAttribute() {
        Element e = ElementFactory.createDiv();
        Style s = e.getStyle();
        s.set("border", "1px solid black");
        s.setMargin("1em");
        Assertions.assertTrue(e.hasAttribute("style"));
        assertEqualsOne(
                new String[] { "border:1px solid black;margin:1em",
                        "margin:1em;border:1px solid black" },
                e.getAttribute("style"));
    }

    @Test
    public void setSingleStyleAsAttribute() {
        Element e = ElementFactory.createDiv();
        String style = "width:12em";
        e.setAttribute("style", style);
        Assertions.assertEquals(style, e.getAttribute("style"));

    }

    @Test
    public void setStyleAttributeMultipleTimes() {
        Element e = ElementFactory.createDiv();
        e.setAttribute("style", "width:12em");
        e.setAttribute("style", "height:12em");
        Assertions.assertEquals("height:12em", e.getAttribute("style"));

    }

    @Test
    public void setMultipleStylesAsAttribute() {
        Element e = ElementFactory.createDiv();
        String style = "width:12em;height:2em";
        e.setAttribute("style", style);
        Assertions.assertEquals(style, e.getAttribute("style"));
        Assertions.assertEquals("2em", e.getStyle().get("height"));
    }

    @Test
    public void setComplexStylesAsAttribute() {
        testStyleAttribute(
                "background:rgb(0,255,0) url(http://foo.bar/smiley.gif) no-repeat fixed center");
        testStyleAttribute("content:\"content: bar\"");
        testStyleAttribute("width:12px;content:\"content: bar\";height:12px");
        testStyleAttribute("width:calc(100% - 80px)");
        testStyleAttribute("width:var(--widthB)");
        testStyleAttribute("color:var(--mainColor)");
        testStyleAttribute("font-size:calc(var(--fontSize) * 2)");
        testStyleAttribute("--lumo-primary-text-color:hsl(12, 12%, 12%)");
        testStyleAttribute(
                "background:url(\"https://example.com/images/myImg.jpg?q;param\")");
        var style = testStyleAttribute(
                "background-image:cross-fade(20% url(first.png?foo;bar&d=3), url(second.png))");
        Assertions.assertEquals(
                "cross-fade(20% url(first.png?foo;bar&d=3), url(second.png))",
                style.get("background-image"));
        testStyleAttribute(
                "mask-image:image(url(mask.png), skyblue, linear-gradient(rgb(0 0 0 / 100%), transparent))");
        style = testStyleAttribute(
                "width:var(--widthB);color:var(--mainColor);background-image:cross-fade(20% url(first.png?foo;bar&d=3), url(second.png))");
        Assertions.assertEquals("var(--widthB)", style.get("width"));
        Assertions.assertEquals("var(--mainColor)", style.get("color"));
        Assertions.assertEquals(
                "cross-fade(20% url(first.png?foo;bar&d=3), url(second.png))",
                style.get("background-image"));
    }

    private Style testStyleAttribute(String style) {
        Element e = ElementFactory.createDiv();
        e.setAttribute("style", style);
        Assertions.assertEquals(style, e.getAttribute("style"));
        return e.getStyle();
    }

    @Test
    public void setInvalidStyleAsAttribute() {
        assertThrows(IllegalArgumentException.class, () -> {
            Element e = ElementFactory.createDiv();
            e.setAttribute("style", "width:");
        });
    }

    @Test
    public void setInvalidStyleAsAttribute2() {
        assertThrows(IllegalArgumentException.class, () -> {
            Element e = ElementFactory.createDiv();
            e.setAttribute("style", "width");
        });
    }

    @Test
    public void setVendorSpecificStylesProperty() {
        Element e = ElementFactory.createDiv();
        String style = "-moz-user-input:inherit";
        e.setAttribute("style", style);
        Assertions.assertEquals("inherit", e.getStyle().get("mozUserInput"));
        Assertions.assertEquals(style, e.getAttribute("style"));
    }

    @Test
    public void setVendorSpecificStylesValue() {
        Element e = ElementFactory.createDiv();
        String style = "display:-moz-box";
        e.setAttribute("style", style);
        Assertions.assertEquals("-moz-box", e.getStyle().get("display"));
        Assertions.assertEquals(style, e.getAttribute("style"));

    }

    @Test
    public void setStyleAttributeTrailingSemicolon() {
        Element e = ElementFactory.createDiv();
        String style = "width:12em";
        e.setAttribute("style", style + ";");
        Assertions.assertEquals(style, e.getAttribute("style"));
    }

    private void assertEqualsOne(String[] expected, String actual) {
        for (String string : expected) {
            if (string.equals(actual)) {
                return;
            }
        }
        String expectedString = Arrays.stream(expected)
                .collect(Collectors.joining("> or <"));
        Assertions.fail(
                "expected: <" + expectedString + "> but was <" + actual + ">");

    }

    @Test
    public void setEmptyStyleName() {
        assertThrows(IllegalArgumentException.class, () -> {
            Element e = ElementFactory.createDiv();
            e.getStyle().set("", "foo");
        });
    }

    @Test
    public void setStyleNameExtraWhitespace() {
        assertThrows(IllegalArgumentException.class, () -> {
            Element e = ElementFactory.createDiv();
            e.getStyle().set("   color", "red");
        });
    }

    @Test
    public void setStyleNameColon() {
        assertThrows(IllegalArgumentException.class, () -> {
            Element e = ElementFactory.createDiv();
            e.getStyle().set("color:", "red");
        });
    }

    @Test
    public void setStyleValueExtraWhitespace() {
        Element e = ElementFactory.createDiv();
        e.getStyle().setColor("red   ");
        Assertions.assertEquals("color:red", e.getAttribute("style"));
        Assertions.assertEquals("red", e.getStyle().get("color"));
    }

    @Test
    public void removeStyles() {
        Element element = ElementFactory.createDiv();

        element.getStyle().setZIndex(12);
        element.getStyle().set("background", "blue");

        element.getStyle().remove("background");

        Assertions.assertEquals("z-index:12", element.getAttribute("style"));

        element.getStyle().setZIndex(null);

        Assertions.assertNull(element.getAttribute("style"));
        Assertions.assertFalse(element.hasAttribute("style"));

        Assertions.assertEquals(0, element.getStyle().getNames().count());
    }

    @Test
    public void removeStyleAttribute() {
        Element element = ElementFactory.createDiv();

        Style style = element.getStyle();

        style.setBorder("1px solid green");

        element.removeAttribute("style");

        Assertions.assertEquals(0, style.getNames().count());
    }

    @Test
    public void validStyleWithSemicolon() {
        Element element = ElementFactory.createDiv();
        String validStyle = "background: url('foo;bar')";
        Style style = element.getStyle();
        style.setBackground(validStyle);
        Assertions.assertEquals(validStyle, style.get("background"));
    }

    @Test
    public void dashSeparatedSetStyle() {
        Element element = ElementFactory.createDiv();

        Style style = element.getStyle();
        style.set("border-color", "blue");
        Assertions.assertEquals("blue", style.get("border-color"));
    }

    @Test
    public void dashSeparatedGetStyle() {
        Element element = ElementFactory.createDiv();

        Style style = element.getStyle();
        style.set("borderColor", "blue");
        style.set("border-foo", "bar");
        Assertions.assertEquals("blue", style.get("border-color"));
        Assertions.assertEquals("bar", style.get("border-foo"));
    }

    @Test
    public void dashSeparatedHasStyle() {
        Element element = ElementFactory.createDiv();

        Style style = element.getStyle();
        style.set("borderColor", "blue");
        style.set("border-foo", "bar");
        Assertions.assertTrue(style.has("border-color"));
        Assertions.assertTrue(style.has("border-foo"));
    }

    @Test
    public void dashSeparatedRemoveStyle() {
        Element element = ElementFactory.createDiv();

        Style style = element.getStyle();
        style.set("borderColor", "blue");
        style.set("border-foo", "bar");
        style.remove("border-color");
        style.remove("border-foo");

        Assertions.assertFalse(style.has("border-color"));
        Assertions.assertFalse(style.has("border-foo"));
    }

    @Test
    public void styleGetNamesDashAndCamelCase() {
        Element element = ElementFactory.createDiv();

        Style style = element.getStyle();
        style.set("borderColor", "blue");
        style.set("border-foo", "bar");

        List<String> styles = style.getNames().collect(Collectors.toList());
        Assertions.assertEquals(2, styles.size());
        Assertions.assertTrue(styles.contains("border-color"));
        Assertions.assertTrue(styles.contains("border-foo"));
    }

    @Test
    public void nullStyleValue() {
        Element element = ElementFactory.createDiv();

        Style style = element.getStyle();
        style.set("borderColor", "blue");
        style.set("borderColor", null);
        List<String> styles = style.getNames().collect(Collectors.toList());
        Assertions.assertFalse(styles.contains("borderColor"));
    }

    @Test
    public void sendPropertyInCorrectFormatToClient() {
        assertClientStyleKey("--some-variable", "--some-variable");
        assertClientStyleKey("-webkit-border", "-webkit-border");
        assertClientStyleKey("background-color", "background-color");
        assertClientStyleKey("color", "color");

        assertClientStyleKey("-webkit-border", "webkitBorder");
        assertClientStyleKey("background-color", "backgroundColor");
    }

    private void assertClientStyleKey(String sentToClient,
            String setUsingStyleApi) {
        Element element = ElementFactory.createDiv();
        StateNode stateNode = element.getNode();
        ElementStylePropertyMap map = stateNode
                .getFeature(ElementStylePropertyMap.class);

        Style style = element.getStyle();
        style.set(setUsingStyleApi, "foo");
        Assertions.assertEquals("foo", style.get(setUsingStyleApi));
        Assertions.assertEquals(sentToClient,
                map.getPropertyNames().toArray()[0]);
        Assertions.assertEquals("foo", map.getProperty(sentToClient));

    }

    @Test
    public void customPropertyStyle() {
        Element element = ElementFactory.createDiv();
        Style style = element.getStyle();
        style.set("--some-variable", "foo");
        Assertions.assertEquals("foo", style.get("--some-variable"));
    }

    @Test
    public void useCustomPropertyStyle() {
        Element element = ElementFactory.createDiv();

        Style style = element.getStyle();
        style.setColor("var(--some-var)");
        Assertions.assertEquals("var(--some-var)", style.get("color"));
    }

    // TODO: enable when DomEvent uses jackson
    // @Test
    // public void listenersFiredInRegisteredOrder() {
    // Element element = ElementFactory.createDiv();
    // List<Integer> eventOrder = new ArrayList<>();
    //
    // for (int i = 0; i < 10; i++) {
    // final int j = i;
    // element.addEventListener("click", e -> {
    // eventOrder.add(j);
    // });
    // }
    // fireEvent(element, "click");
    // Assertions.assertArrayEquals(new Object[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9
    // },
    // eventOrder.toArray());
    // }
    //
    // private void fireEvent(Element element, String eventType) {
    // element.getNode().getFeature(ElementListenerMap.class).fireEvent(
    // new DomEvent(element, eventType, JacksonUtils.createObjectNode()));
    //
    // }
    //
    // @Test
    // public void eventsWhenListenerIsRegisteredManyTimes() {
    // AtomicInteger invocations = new AtomicInteger(0);
    //
    // DomEventListener listener = e -> {
    // invocations.incrementAndGet();
    // };
    // Element element = ElementFactory.createDiv();
    // element.addEventListener("click", listener);
    // element.addEventListener("click", listener);
    //
    // fireEvent(element, "click");
    //
    // Assertions.assertEquals(2, invocations.get());
    // }

    @Test
    public void addAsOwnChild() {
        assertThrows(IllegalStateException.class, () -> {
            Element element = ElementFactory.createDiv();
            element.appendChild(element);
        });
    }

    @Test
    public void addAsChildOfChild() {
        assertThrows(IllegalStateException.class, () -> {
            Element parent = ElementFactory.createDiv();
            Element child = ElementFactory.createDiv();
            parent.appendChild(child);

            child.appendChild(parent);
        });
    }

    @Override
    protected void checkIsNotChild(Node<?> parent, Element child) {
        Assertions.assertNotEquals(child.getParent(), parent);

        super.checkIsNotChild(parent, child);
    }

    @Test
    public void testGetOwnTextContent() {
        Element element = ElementFactory.createDiv();
        element.setText("foo");
        element.appendChild(ElementFactory.createDiv()
                .appendChild(ElementFactory.createSpan("span contents")));
        element.appendChild(ElementFactory.createStrong("strong contents"));
        element.appendChild(Element.createText("Another text node"));

        Assertions.assertEquals("fooAnother text node", element.getText());
        Assertions.assertEquals(
                "foospan contentsstrong contentsAnother text node",
                element.getTextRecursively());
    }

    @Test
    public void setResourceAttribute_elementIsNotAttached_elementHasAttribute() {
        UI.setCurrent(createUI());
        Element element = ElementFactory.createDiv();
        String resName = "resource";
        StreamResource resource = createEmptyResource(resName);
        element.setAttribute("foo", resource);

        Assertions.assertTrue(element.hasAttribute("foo"));

        Assertions.assertTrue(element.getAttribute("foo").endsWith(resName));
    }

    @Test
    public void setResourceAttribute_elementIsNotAttachedAndHasAttribute_elementHasAttribute() {
        UI.setCurrent(createUI());
        Element element = ElementFactory.createDiv();
        element.setAttribute("foo", "bar");

        String resName = "resource";
        StreamResource resource = createEmptyResource(resName);
        element.setAttribute("foo", resource);

        Assertions.assertTrue(element.hasAttribute("foo"));

        Assertions.assertTrue(element.getAttribute("foo").endsWith(resName));
    }

    @Test
    public void setResourceAttributeSeveralTimes_elementIsNotAttached_elementHasAttribute() {
        UI.setCurrent(createUI());
        Element element = ElementFactory.createDiv();
        String resName = "resource";
        StreamResource resource = createEmptyResource(resName);
        element.setAttribute("foo", resource);

        Assertions.assertTrue(element.hasAttribute("foo"));

        resName = "resource1";
        resource = createEmptyResource(resName);
        element.setAttribute("foo", resource);

        Assertions.assertTrue(element.hasAttribute("foo"));

        Assertions.assertTrue(element.getAttribute("foo").endsWith(resName));
    }

    @Test
    public void setResourceAttribute_nullValue() {
        assertThrows(IllegalArgumentException.class, () -> {
            Element element = ElementFactory.createDiv();
            element.setAttribute("foo", (StreamResource) null);
        });
    }

    @Test
    public void setResourceAttribute_classAttribute() {
        assertThrows(IllegalArgumentException.class, () -> {
            Element element = ElementFactory.createDiv();
            element.setAttribute("class", Mockito.mock(StreamResource.class));
        });
    }

    @Test
    public void setResourceAttribute_nullAttribute() {
        assertThrows(IllegalArgumentException.class, () -> {
            Element element = ElementFactory.createDiv();
            element.setAttribute(null, Mockito.mock(StreamResource.class));
        });
    }

    @Test
    public void setResourceAttribute_elementIsAttached_elementHasAttribute() {
        UI ui = createUI();
        UI.setCurrent(ui);
        String resName = "resource";
        StreamResource resource = createEmptyResource(resName);
        ui.getElement().setAttribute("foo", resource);

        Assertions.assertTrue(ui.getElement().hasAttribute("foo"));
        Assertions.assertTrue(
                ui.getElement().getAttribute("foo").endsWith(resName));
    }

    @Test
    public void setResourceAttribute_elementIsAttached_setAnotherResource()
            throws URISyntaxException {
        UI ui = createUI();
        UI.setCurrent(ui);
        StreamResource resource = createEmptyResource("resource1");
        ui.getElement().setAttribute("foo", resource);

        String uri = ui.getElement().getAttribute("foo");
        Optional<StreamResource> res = ui.getSession().getResourceRegistry()
                .getResource(StreamResource.class, new URI(uri));
        Assertions.assertTrue(res.isPresent());

        String resName = "resource2";
        ui.getElement().setAttribute("foo", createEmptyResource(resName));
        res = ui.getSession().getResourceRegistry()
                .getResource(StreamResource.class, new URI(uri));
        Assertions.assertFalse(res.isPresent());

        Assertions.assertTrue(ui.getElement().hasAttribute("foo"));
        Assertions.assertTrue(
                ui.getElement().getAttribute("foo").endsWith(resName));
    }

    @Test
    public void setResourceAttribute_elementIsAttached_setRawAttribute()
            throws URISyntaxException, InterruptedException {
        UI ui = createUI();
        UI.setCurrent(ui);
        StreamResource resource = createEmptyResource("resource");
        ui.getElement().setAttribute("foo", resource);

        String uri = ui.getElement().getAttribute("foo");
        Optional<StreamResource> res = ui.getSession().getResourceRegistry()
                .getResource(StreamResource.class, new URI(uri));
        Assertions.assertTrue(res.isPresent());
        res = null;

        WeakReference<StreamResource> ref = new WeakReference<>(resource);
        resource = null;

        ui.getElement().setAttribute("foo", "bar");

        TestUtil.isGarbageCollected(ref);
        res = ui.getSession().getResourceRegistry()
                .getResource(StreamResource.class, new URI(uri));

        Assertions.assertFalse(res.isPresent());
        Assertions.assertTrue(ui.getElement().hasAttribute("foo"));
        Assertions
                .assertTrue(ui.getElement().getAttribute("foo").equals("bar"));
    }

    @Test
    public void setResourceAttribute_elementIsAttached_removeAttribute()
            throws URISyntaxException, InterruptedException {
        UI ui = createUI();
        UI.setCurrent(ui);
        StreamResource resource = createEmptyResource("resource");
        ui.getElement().setAttribute("foo", resource);

        String uri = ui.getElement().getAttribute("foo");
        Optional<StreamResource> res = ui.getSession().getResourceRegistry()
                .getResource(StreamResource.class, new URI(uri));
        Assertions.assertTrue(res.isPresent());
        res = null;

        WeakReference<StreamResource> ref = new WeakReference<>(resource);
        resource = null;

        ui.getElement().removeAttribute("foo");
        TestUtil.isGarbageCollected(ref);

        res = ui.getSession().getResourceRegistry()
                .getResource(StreamResource.class, new URI(uri));
        Assertions.assertFalse(res.isPresent());
        Assertions.assertFalse(ui.getElement().hasAttribute("foo"));
        Assertions.assertNull(ui.getElement().getAttribute("foo"));
    }

    @Test
    public void setResourceAttribute_attachElement_resourceIsRegistered()
            throws URISyntaxException {
        UI ui = createUI();
        UI.setCurrent(ui);

        StreamResource resource = createEmptyResource("resource");
        Element element = ElementFactory.createDiv();
        element.setAttribute("foo", resource);

        ui.getElement().appendChild(element);

        Assertions.assertTrue(element.hasAttribute("foo"));

        String uri = element.getAttribute("foo");
        Optional<StreamResource> res = ui.getSession().getResourceRegistry()
                .getResource(StreamResource.class, new URI(uri));
        Assertions.assertTrue(res.isPresent());
    }

    @Test
    public void setResourceAttribute_attachElement_setAnotherResource()
            throws URISyntaxException, InterruptedException {
        UI ui = createUI();
        UI.setCurrent(ui);

        StreamResource resource = createEmptyResource("resource1");
        Element element = ElementFactory.createDiv();
        element.setAttribute("foo", resource);

        WeakReference<StreamResource> ref = new WeakReference<>(resource);
        resource = null;

        String resName = "resource2";
        element.setAttribute("foo", createEmptyResource(resName));

        ui.getElement().appendChild(element);

        Assertions.assertTrue(element.hasAttribute("foo"));

        String uri = element.getAttribute("foo");
        Optional<StreamResource> res = ui.getSession().getResourceRegistry()
                .getResource(StreamResource.class, new URI(uri));
        Assertions.assertTrue(res.isPresent());
        Assertions.assertTrue(uri.endsWith(resName));

        // allow GC to collect element and all its (detach) listeners
        element = null;

        TestUtil.isGarbageCollected(ref);
    }

    @Test
    public void setResourceAttribute_attachElement_setRawAttribute()
            throws URISyntaxException, InterruptedException {
        UI ui = createUI();
        UI.setCurrent(ui);

        StreamResource resource = createEmptyResource("resource");
        Element element = ElementFactory.createDiv();
        element.setAttribute("foo", resource);

        WeakReference<StreamResource> ref = new WeakReference<>(resource);
        resource = null;

        element.setAttribute("foo", "bar");

        TestUtil.isGarbageCollected(ref);

        ui.getElement().appendChild(element);

        Assertions.assertTrue(element.hasAttribute("foo"));
        Assertions.assertEquals("bar", element.getAttribute("foo"));
    }

    @Test
    public void setResourceAttribute_attachElement_removeAttribute()
            throws URISyntaxException, InterruptedException {
        UI ui = createUI();
        UI.setCurrent(ui);

        StreamResource resource = createEmptyResource("resource");
        Element element = ElementFactory.createDiv();
        element.setAttribute("foo", resource);

        WeakReference<StreamResource> ref = new WeakReference<>(resource);
        resource = null;

        element.removeAttribute("foo");

        ui.getElement().appendChild(element);

        TestUtil.isGarbageCollected(ref);

        Assertions.assertFalse(element.hasAttribute("foo"));

        Assertions.assertNull(element.getAttribute("foo"));
    }

    @Test
    public void setResourceAttribute_attachElement_setAnotherResourceAfterAttaching()
            throws URISyntaxException, InterruptedException {
        UI ui = createUI();
        UI.setCurrent(ui);

        StreamResource resource = createEmptyResource("resource1");
        Element element = ElementFactory.createDiv();
        element.setAttribute("foo", resource);

        WeakReference<StreamResource> ref = new WeakReference<>(resource);
        resource = null;

        ui.getElement().appendChild(element);

        String resName = "resource2";
        element.setAttribute("foo", createEmptyResource(resName));

        Assertions.assertTrue(element.hasAttribute("foo"));

        TestUtil.isGarbageCollected(ref);

        Assertions.assertNull(ref.get());

        String uri = element.getAttribute("foo");
        Optional<StreamResource> res = ui.getSession().getResourceRegistry()
                .getResource(StreamResource.class, new URI(uri));
        Assertions.assertTrue(res.isPresent());
        Assertions.assertTrue(uri.endsWith(resName));
    }

    @Test
    public void setResourceAttribute_attachElement_setRawAttributeAfterAttaching()
            throws URISyntaxException, InterruptedException {
        UI ui = createUI();
        UI.setCurrent(ui);

        StreamResource resource = createEmptyResource("resource");
        Element element = ElementFactory.createDiv();
        element.setAttribute("foo", resource);

        WeakReference<StreamResource> ref = new WeakReference<>(resource);
        resource = null;

        ui.getElement().appendChild(element);

        element.setAttribute("foo", "bar");

        TestUtil.isGarbageCollected(ref);

        Assertions.assertNull(ref.get());

        Assertions.assertTrue(element.hasAttribute("foo"));

        Assertions.assertEquals("bar", element.getAttribute("foo"));
    }

    @Test
    public void setResourceAttribute_attachElement_removeAttributeAfterAttaching()
            throws URISyntaxException, InterruptedException {
        UI ui = createUI();
        UI.setCurrent(ui);

        StreamResource resource = createEmptyResource("resource");
        Element element = ElementFactory.createDiv();
        element.setAttribute("foo", resource);

        WeakReference<StreamResource> ref = new WeakReference<>(resource);
        resource = null;

        ui.getElement().appendChild(element);

        element.removeAttribute("foo");

        TestUtil.isGarbageCollected(ref);

        Assertions.assertNull(ref.get());

        Assertions.assertFalse(element.hasAttribute("foo"));

        Assertions.assertNull(element.getAttribute("foo"));
    }

    @Test
    public void setResourceAttribute_detachElement_resourceIsUnregistered()
            throws URISyntaxException, InterruptedException {
        UI ui = createUI();
        UI.setCurrent(ui);
        Element element = ElementFactory.createDiv();
        ui.getElement().appendChild(element);

        String resName = "resource";
        StreamResource resource = createEmptyResource(resName);
        element.setAttribute("foo", resource);
        String attribute = element.getAttribute("foo");

        WeakReference<StreamResource> ref = new WeakReference<>(resource);
        resource = null;

        URI uri = new URI(attribute);
        Optional<StreamResource> res = ui.getSession().getResourceRegistry()
                .getResource(StreamResource.class, uri);
        Assertions.assertTrue(res.isPresent());

        ui.getElement().removeAllChildren();

        res = ui.getSession().getResourceRegistry()
                .getResource(StreamResource.class, uri);
        Assertions.assertFalse(res.isPresent());

        Assertions.assertTrue(element.hasAttribute("foo"));
        Assertions.assertNotNull(element.getAttribute("foo"));
        Assertions.assertTrue(element.getAttribute("foo").endsWith(resName));

        element.setAttribute("foo", "bar");
        Assertions.assertTrue(element.hasAttribute("foo"));
        Assertions.assertEquals("bar", element.getAttribute("foo"));

        TestUtil.isGarbageCollected(ref);
    }

    @Test
    public void setResourceAttribute_detachAndReattachElement_resourceReregistered()
            throws URISyntaxException {
        UI ui = createUI();
        UI.setCurrent(ui);
        Element element = ElementFactory.createDiv();
        ui.getElement().appendChild(element);

        String resName = "resource";
        StreamResource resource = createEmptyResource(resName);
        element.setAttribute("foo", resource);
        String attribute = element.getAttribute("foo");

        URI uri = new URI(attribute);
        Optional<StreamResource> res = ui.getSession().getResourceRegistry()
                .getResource(StreamResource.class, uri);
        Assertions.assertTrue(res.isPresent());

        ui.getElement().removeAllChildren();

        res = ui.getSession().getResourceRegistry()
                .getResource(StreamResource.class, uri);
        Assertions.assertFalse(res.isPresent());

        ui.getElement().appendChild(element);

        res = ui.getSession().getResourceRegistry()
                .getResource(StreamResource.class, uri);
        Assertions.assertTrue(res.isPresent());
    }

    @Test
    public void setResourceAttribute_attachAndDetachAndReattachElement_resourceReregistered()
            throws URISyntaxException {
        UI ui = createUI();
        UI.setCurrent(ui);
        Element element = ElementFactory.createDiv();

        String resName = "resource";
        StreamResource resource = createEmptyResource(resName);
        element.setAttribute("foo", resource);
        String attribute = element.getAttribute("foo");

        ui.getElement().appendChild(element);

        URI uri = new URI(attribute);
        Optional<StreamResource> res = ui.getSession().getResourceRegistry()
                .getResource(StreamResource.class, uri);
        Assertions.assertTrue(res.isPresent());

        ui.getElement().removeAllChildren();

        res = ui.getSession().getResourceRegistry()
                .getResource(StreamResource.class, uri);
        Assertions.assertFalse(res.isPresent());

        ui.getElement().appendChild(element);

        res = ui.getSession().getResourceRegistry()
                .getResource(StreamResource.class, uri);
        Assertions.assertTrue(res.isPresent());
    }

    @Test
    public void setResourceAttribute_elementIsText_operationIsNotSupported() {
        assertThrows(UnsupportedOperationException.class, () -> {
            Element.createText("").setAttribute("foo",
                    Mockito.mock(StreamResource.class));
        });
    }

    @Test
    public void testAttachListener_parentAttach_childListenersTriggered() {
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
            Assertions.assertEquals(child, event.getSource());
        });
        grandChild.addAttachListener(event -> {
            grandChildTriggered.addAndGet(1);
        });
        grandChild.addAttachListener(event -> {
            Assertions.assertEquals(grandChild, event.getSource());
        });

        parent.appendChild(child);
        child.appendChild(grandChild);

        Assertions.assertEquals(childTriggered.get(), 0);
        Assertions.assertEquals(grandChildTriggered.get(), 0);

        body.appendChild(parent);

        Assertions.assertEquals(childTriggered.get(), 1);
        Assertions.assertEquals(grandChildTriggered.get(), 1);

        body.removeAllChildren();
        parent.removeAllChildren();

        body.appendChild(parent);
        parent.appendChild(child);

        Assertions.assertEquals(childTriggered.get(), 2);
        Assertions.assertEquals(grandChildTriggered.get(), 2);

        registrationHandle.remove();

        body.removeAllChildren();
        body.appendChild(child);

        Assertions.assertEquals(childTriggered.get(), 2);
        Assertions.assertEquals(grandChildTriggered.get(), 3);
    }

    @Test
    public void testDetachListener_parentDetach_childListenersTriggered() {
        Element body = new UI().getElement();
        Element parent = ElementFactory.createDiv();
        Element child = ElementFactory.createDiv();
        Element grandChild = ElementFactory.createDiv();

        AtomicInteger triggered = new AtomicInteger();

        Registration registrationHandle = child.addDetachListener(event -> {
            triggered.addAndGet(1);
            Assertions.assertEquals(child, event.getSource());
        });

        grandChild.addDetachListener(event -> {
            triggered.addAndGet(1);
            Assertions.assertEquals(grandChild, event.getSource());
        });

        child.appendChild(grandChild);
        parent.appendChild(child);
        body.appendChild(parent);

        Assertions.assertEquals(triggered.get(), 0);

        body.removeAllChildren();
        Assertions.assertEquals(triggered.get(), 2);

        body.appendChild(parent);
        body.removeAllChildren();

        Assertions.assertEquals(triggered.get(), 4);

        body.appendChild(parent);
        registrationHandle.remove();

        body.removeAllChildren();

        Assertions.assertEquals(triggered.get(), 5);
    }

    @Test
    public void testAttachListener_eventOrder_childFirst() {
        Element body = new UI().getElement();
        Element parent = ElementFactory.createDiv();
        Element child = ElementFactory.createDiv();
        parent.appendChild(child);

        AtomicBoolean parentAttached = new AtomicBoolean();
        AtomicBoolean childAttached = new AtomicBoolean();

        child.addAttachListener(event -> {
            childAttached.set(true);
            Assertions.assertFalse(parentAttached.get());
        });
        parent.addAttachListener(event -> {
            parentAttached.set(true);
            Assertions.assertTrue(childAttached.get());
        });

        body.appendChild(parent);

        Assertions.assertTrue(parentAttached.get());
        Assertions.assertTrue(childAttached.get());
    }

    @Test
    public void testDetachListener_eventOrder_childFirst() {
        Element body = new UI().getElement();
        Element parent = ElementFactory.createDiv();
        Element child = ElementFactory.createDiv();
        parent.appendChild(child);
        body.appendChild(parent);

        AtomicBoolean parentDetached = new AtomicBoolean();
        AtomicBoolean childDetached = new AtomicBoolean();

        child.addDetachListener(event -> {
            childDetached.set(true);
            Assertions.assertFalse(parentDetached.get());
        });
        parent.addDetachListener(event -> {
            parentDetached.set(true);
            Assertions.assertTrue(childDetached.get());
        });

        body.removeAllChildren();

        Assertions.assertTrue(parentDetached.get());
        Assertions.assertTrue(childDetached.get());
    }

    @Test
    public void testAttachDetach_elementMoved_bothEventsTriggered() {
        Element body = new UI().getElement();
        Element parent = ElementFactory.createDiv();
        Element child = ElementFactory.createDiv();

        parent.appendChild(child);
        body.appendChild(parent);

        AtomicBoolean attached = new AtomicBoolean();
        AtomicBoolean detached = new AtomicBoolean();

        child.addAttachListener(event -> {
            attached.set(true);
            Assertions.assertTrue(detached.get());
        });
        child.addDetachListener(event -> {
            detached.set(true);
            Assertions.assertFalse(attached.get());
        });

        body.appendChild(child);

        Assertions.assertTrue(attached.get());
        Assertions.assertTrue(detached.get());
    }

    @Test
    public void testAttachEvent_stateTreeCanFound() {
        Element body = new UI().getElement();
        Element child = ElementFactory.createDiv();

        AtomicInteger attached = new AtomicInteger();

        child.addAttachListener(event -> {
            Assertions.assertNotNull(event.getSource().getNode().getOwner());
            Assertions.assertNotEquals(NullOwner.get(),
                    event.getSource().getNode().getOwner());
        });
        child.addAttachListener(event -> attached.incrementAndGet());

        body.appendChild(child);
        Assertions.assertEquals(1, attached.get());
    }

    @Test
    public void testDetachEvent_stateTreeCanFound() {
        Element body = new UI().getElement();
        Element child = ElementFactory.createDiv();
        body.appendChild(child);

        AtomicInteger detached = new AtomicInteger();

        child.addDetachListener(event -> {
            Assertions.assertNotNull(event.getSource().getNode().getOwner());
            Assertions.assertNotEquals(NullOwner.get(),
                    event.getSource().getNode().getOwner());
        });
        child.addDetachListener(event -> detached.incrementAndGet());

        body.removeAllChildren();

        Assertions.assertEquals(1, detached.get());
    }

    @Test
    public void testMoveFromUiToUi_doesNotThrow() {
        Element body = new UI().getElement();
        Element child = ElementFactory.createDiv();
        body.appendChild(child);

        child.removeFromTree();

        body = new UI().getElement();
        body.appendChild(child);
        Assertions.assertEquals(body, child.getParent());
    }

    @Test
    public void testRemoveFromTree_inDetachListener_removedFromParent() {
        Element body = new UI().getElement();
        Element child = ElementFactory.createDiv();
        body.appendChild(child);

        child.addDetachListener(event -> child.removeFromTree());

        body.removeAllChildren();

        Assertions.assertEquals(null, child.getParent());
    }

    @Test
    public void testRemoveFromTree_isVirtualChild_removedFromParent() {
        Element body = new UI().getElement();
        Element child = ElementFactory.createDiv();

        body.getNode().getFeature(VirtualChildrenList.class)
                .append(child.getNode(), "");

        Assertions.assertTrue(child.isVirtualChild());

        child.removeFromTree();

        Assertions.assertFalse(child.isVirtualChild());
        Assertions.assertEquals(0,
                body.getNode().getFeature(VirtualChildrenList.class).size());
    }

    private StreamResource createEmptyResource(String resName) {
        return new StreamResource(resName,
                () -> new ByteArrayInputStream(new byte[0]));
    }

    @SuppressWarnings("serial")
    private UI createUI() {
        VaadinSession session = new AlwaysLockedVaadinSession(
                new MockVaadinServletService());
        UI ui = new UI() {
            @Override
            public VaadinSession getSession() {
                return session;
            }
        };
        return ui;
    }

    @Test
    public void insertAtCurrentPositionNoOp() {
        // Must have an UI to get attach events
        UI ui = new UI();
        Element parent = ui.getElement();
        Element child = ElementFactory.createDiv();

        parent.appendChild(child);

        child.addDetachListener(
                e -> Assertions.fail("Child should not be detached"));
        parent.insertChild(0, child);
    }

    @Test
    public void textNodeTransformsNullToEmptyAndDoesNotThrowException() {
        Element e = Element.createText(null);
        Assertions.assertEquals("", e.getText());
    }

    @Test
    public void textNodeOuterHtml() {
        Element e = Element.createText("foobar");
        Assertions.assertEquals("foobar", e.getOuterHTML());
    }

    @Test
    public void singleElementOuterHtml() {
        Element e = ElementFactory.createAnchor();
        Assertions.assertEquals("<a></a>", e.getOuterHTML());
    }

    @Test
    public void elementTreeOuterHtml() {
        Element div = ElementFactory.createDiv();
        Element span = ElementFactory.createSpan();
        Element button = ElementFactory.createButton("hello");

        div.appendChild(span);
        span.appendChild(button);

        Assertions.assertEquals("<div>\n"
                + " <span>\n  <button>hello</button>\n </span>\n" + "</div>",
                div.getOuterHTML());
    }

    @Test
    public void elementAttributesOuterHtml() {
        Element div = ElementFactory.createDiv();
        div.setAttribute("foo", "bar");
        div.getStyle().setWidth("20px");
        div.getClassList().add("cls");
        div.setAttribute("pin", "");

        Assertions.assertEquals(
                "<div pin foo=\"bar\" style=\"width:20px\" class=\"cls\"></div>",
                div.getOuterHTML());
    }

    @Test
    public void elementAttributeSpecialCharactersOuterHtml() {
        Element div = ElementFactory.createDiv();
        div.setAttribute("foo", "bar\"'&quot;");

        Assertions.assertEquals("<div foo=\"bar&quot;'&amp;quot;\"></div>",
                div.getOuterHTML());
    }

    @Test
    public void htmlComponentOuterHtml() {
        Html html = new Html(
                "<div style='background:green'><span><button>hello</button></span></div>");
        Assertions.assertEquals("<div style=\"background:green\">\n"
                + " <span>\n  <button>hello</button>\n </span>\n" + "</div>",
                html.getElement().getOuterHTML());
    }

    @Test
    public void callFunctionBeforeAttach() {
        UI ui = new MockUI();
        Element element = ElementFactory.createDiv();
        element.callJsFunction("noArgsMethod");
        ui.getElement().appendChild(element);
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        assertPendingJs(ui, "return $0.noArgsMethod()", element);
    }

    @Test
    public void callFunctionAfterAttach() {
        UI ui = new MockUI();
        Element element = ElementFactory.createDiv();
        ui.getElement().appendChild(element);
        element.callJsFunction("noArgsMethod");
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        assertPendingJs(ui, "return $0.noArgsMethod()", element);
    }

    @Test
    public void callFunctionBeforeDetach() {
        UI ui = new MockUI();
        Element element = ElementFactory.createDiv();
        ui.getElement().appendChild(element);
        element.callJsFunction("noArgsMethod");
        ui.getElement().removeAllChildren();
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        List<PendingJavaScriptInvocation> invocations = ui.getInternals()
                .dumpPendingJavaScriptInvocations();
        Assertions.assertTrue(invocations.isEmpty());
    }

    @Test
    public void callFunctionBeforeReAttach() {
        UI ui = new MockUI();
        Element element = ElementFactory.createDiv();
        ui.getElement().appendChild(element);
        element.callJsFunction("noArgsMethod");

        Element div = ElementFactory.createDiv();
        ui.getElement().appendChild(div);
        div.appendChild(element);

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        assertPendingJs(ui, "return $0.noArgsMethod()", element);
    }

    @Test
    public void callFunctionOneParam() {
        UI ui = new MockUI();
        Element element = ElementFactory.createDiv();
        element.callJsFunction("method", "foo");
        ui.getElement().appendChild(element);

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        assertPendingJs(ui, "return $0.method($1)", element, "foo");

    }

    @Test
    public void callFunctionTwoParams() {
        UI ui = new MockUI();
        Element element = ElementFactory.createDiv();
        element.callJsFunction("method", "foo", 123);
        ui.getElement().appendChild(element);
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        assertPendingJs(ui, "return $0.method($1,$2)", element, "foo", 123);
    }

    @Test
    public void callFunctionWithBean() {
        UI ui = new MockUI();
        Element element = ElementFactory.createDiv();
        SimpleBean bean = new SimpleBean();
        element.callJsFunction("method", bean);
        ui.getElement().appendChild(element);
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        assertPendingJs(ui, "return $0.method($1)", element, bean);
    }

    @Test
    public void callFunctionOnProperty() {
        UI ui = new MockUI();
        Element element = ElementFactory.createDiv();
        element.callJsFunction("property.method");
        ui.getElement().appendChild(element);
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        assertPendingJs(ui, "return $0.property.method()", element);
    }

    @Test
    public void callFunctionOnSubProperty() {
        UI ui = new MockUI();
        Element element = ElementFactory.createDiv();
        element.callJsFunction("property.other.method");
        ui.getElement().appendChild(element);
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        assertPendingJs(ui, "return $0.property.other.method()", element);
    }

    @Test
    public void attachShadowRoot_shadowRootCreatedAndChildrenArePreserved() {
        Element element = ElementFactory.createDiv();
        Element button = ElementFactory.createButton();
        Element emphasis = ElementFactory.createEmphasis();
        element.appendChild(button, emphasis);

        ShadowRoot shadow = element.attachShadow();
        Assertions.assertNotNull(shadow);
        Assertions.assertEquals(element, shadow.getHost());
        Assertions.assertEquals(shadow, element.getShadowRoot().get());
        Assertions.assertEquals(2, element.getChildCount());
        Assertions.assertEquals(2, element.getChildren().count());
        Assertions.assertEquals(button, element.getChild(0));
        Assertions.assertEquals(emphasis, element.getChild(1));
    }

    @Test
    public void getShadowRoot_shadowRootIsEmpty() {
        Element element = ElementFactory.createDiv();
        Assertions.assertFalse(element.getShadowRoot().isPresent());
    }

    @Test
    public void getParentNode_parentNodeIsTheSameAsParent() {
        Element element = ElementFactory.createDiv();
        Element child = ElementFactory.createDiv();

        element.appendChild(child);

        Assertions.assertEquals(child.getParent(), child.getParentNode());
    }

    @Test
    public void getParentNode_elementInShadowRoot_parentIsNull() {
        ShadowRoot element = ElementFactory.createDiv().attachShadow();
        Element child = ElementFactory.createDiv();

        element.appendChild(child);

        Assertions.assertNull(child.getParent());
        Assertions.assertEquals(element, child.getParentNode());
    }

    @Test
    public void parentIsDisabled_childIsDisabled() {
        Element parent = ElementFactory.createDiv();
        Element child = ElementFactory.createDiv();

        parent.appendChild(child);

        Assertions.assertTrue(parent.isEnabled(), "Parent should be enabled");
        Assertions.assertTrue(child.isEnabled(), "Child should be enabled");

        parent.setEnabled(false);

        Assertions.assertFalse(parent.isEnabled(), "Parent should be disabled");
        Assertions.assertFalse(child.isEnabled(), "Child should be disabled");

        child.removeFromParent();

        Assertions.assertTrue(child.isEnabled(), "Child should be enabled");
    }

    @Test
    public void emptyElement_setDisabled_noChildFeatures() {
        Element element = ElementFactory.createDiv();

        element.setEnabled(false);

        BasicElementStateProviderTest.assertNoChildFeatures(element);
    }

    @Test
    public void emptyElement_isVirtualChild_noChildFeatures() {
        Element element = ElementFactory.createDiv();

        element.isVirtualChild();

        BasicElementStateProviderTest.assertNoChildFeatures(element);
    }

    @Test
    public void elementWithoutComponent_getComponentFeature() {
        Element element = ElementFactory.createDiv();
        element.appendChild(ElementFactory.createDiv());

        element.getComponent();

        Assertions.assertFalse(element.getNode()
                .getFeatureIfInitialized(ComponentMapping.class).isPresent(),
                "getComponent() shouldn't initialize a component mapping feature");
    }

    @Test
    public void readMissingProperty_noFeatureInitialized() {
        Element element = ElementFactory.createDiv();

        element.getProperty("foo");
        element.hasProperty("foo");
        element.removeProperty("foo");
        element.getPropertyNames().collect(Collectors.toList());

        Assertions.assertFalse(element.getNode()
                .getFeatureIfInitialized(ElementPropertyMap.class).isPresent(),
                "reading a property value shouldn't initialize a property map feature");
    }

    @Test
    public void readMissingAttribute_noFeatureInitialized() {
        Element element = ElementFactory.createDiv();

        element.getAttribute("foo");
        element.hasAttribute("foo");
        element.removeAttribute("foo");
        element.getAttributeNames().collect(Collectors.toList());

        Assertions.assertFalse(element.getNode()
                .getFeatureIfInitialized(ElementAttributeMap.class).isPresent(),
                "reading an attribute value shouldn't initialize an attribute map feature");
    }

    @Test
    public void virtualChildren_areIdentifiedAsSuch() {
        Element parent = ElementFactory.createDiv();
        Element child = ElementFactory.createDiv();
        Element virtualChild = ElementFactory.createDiv();
        Element grandVirtualChild = ElementFactory.createDiv();

        parent.appendChild(child);
        parent.appendVirtualChild(virtualChild);
        virtualChild.appendChild(grandVirtualChild);

        Assertions.assertFalse(parent.isVirtualChild());
        Assertions.assertFalse(child.isVirtualChild());
        Assertions.assertTrue(virtualChild.isVirtualChild());
        Assertions.assertFalse(grandVirtualChild.isVirtualChild());
    }

    @Test
    public void domPropertyListener_registersListenerAndDomTrigger() {
        Element element = ElementFactory.createDiv();

        AtomicReference<Serializable> listenerValue = new AtomicReference<>();

        element.addPropertyChangeListener("property", "event", event -> {
            if (listenerValue.getAndSet(event.getValue()) != null) {
                Assertions.fail("Unexpected event");
            }
        });

        Assertions.assertEquals(DisabledUpdateMode.ONLY_WHEN_ENABLED,
                element.getNode().getFeature(ElementListenerMap.class)
                        .getPropertySynchronizationMode("property"),
                "The property should be synchronized");

        ElementListenerMap listenerMap = element.getNode()
                .getFeature(ElementListenerMap.class);

        Assertions.assertEquals(
                Collections.singleton(
                        JsonConstants.SYNCHRONIZE_PROPERTY_TOKEN + "property"),
                ElementListenersTest.getExpressions(listenerMap, "event"),
                "A DOM event synchronization should be defined");

        element.setProperty("property", "value");
        Assertions.assertEquals(listenerValue.get(), "value",
                "Listener shold be registered");
    }

    @Test
    public void domPropertyListener_unregisterCleansEverything() {
        Element element = ElementFactory.createDiv();

        DomListenerRegistration registration = element
                .addPropertyChangeListener("property", "event", event -> {
                    Assertions.fail("Unexpected event");
                });
        registration.remove();

        Assertions.assertNull(
                element.getNode().getFeature(ElementListenerMap.class)
                        .getPropertySynchronizationMode("property"),
                "The property should not be synchronized");

        ElementListenerMap listenerMap = element.getNode()
                .getFeature(ElementListenerMap.class);

        Assertions.assertEquals(Collections.emptySet(),
                ElementListenersTest.getExpressions(listenerMap, "event"),
                "There should be no DOM listener");

        // Should not trigger assert in the listener
        element.setProperty("property", "value");
    }

    @Test
    public void removingVirtualChildrenIsPossible() {
        Element parent = new Element("root");
        Element child1 = new Element("main");
        Element child2 = new Element("menu");

        parent.appendVirtualChild(child1, child2);

        parent.removeVirtualChild(child2, child1);

        Assertions.assertNull(child1.getParent());
        Assertions.assertFalse(child1.isVirtualChild());

        Assertions.assertNull(child2.getParent());
        Assertions.assertFalse(child2.isVirtualChild());
    }

    @Test
    public void removeVirtualChildren_notVirtualChild_fails() {
        assertThrows(IllegalArgumentException.class, () -> {
            Element parent = new Element("root");
            Element child1 = new Element("main");

            parent.appendChild(child1);

            parent.removeVirtualChild(child1);
        });
    }

    @Test
    public void removeFromParent_virtualChild_fails() {
        assertThrows(IllegalArgumentException.class, () -> {
            Element parent = new Element("root");
            Element child1 = new Element("main");

            parent.appendVirtualChild(child1);

            child1.removeFromParent();
        });
    }

    @Test
    public void executeJavaScript_delegatesToExecJs() {
        AtomicReference<String> invokedExpression = new AtomicReference<>();
        AtomicReference<Object[]> invokedParams = new AtomicReference<>();

        Element element = new Element("div") {
            @Override
            public PendingJavaScriptResult executeJs(String expression,
                    Object... parameters) {
                String oldExpression = invokedExpression.getAndSet(expression);
                Assertions.assertNull(oldExpression,
                        "There should be no old expression");

                Object[] oldParams = invokedParams.getAndSet(parameters);
                Assertions.assertNull(oldParams,
                        "There should be no old params");

                return null;
            }
        };

        element.executeJs("foo", 1, true);

        Assertions.assertEquals("foo", invokedExpression.get());
        Assertions.assertEquals(Integer.valueOf(1), invokedParams.get()[0]);
        Assertions.assertEquals(Boolean.TRUE, invokedParams.get()[1]);
    }

    @Test
    public void callFunction_delegatesToCallJsFunction() {
        AtomicReference<String> invokedFuction = new AtomicReference<>();
        AtomicReference<Object[]> invokedParams = new AtomicReference<>();

        Element element = new Element("div") {
            @Override
            public PendingJavaScriptResult callJsFunction(String functionName,
                    Object... arguments) {
                String oldExpression = invokedFuction.getAndSet(functionName);
                Assertions.assertNull(oldExpression,
                        "There should be no old function name");

                Object[] oldParams = invokedParams.getAndSet(arguments);
                Assertions.assertNull(oldParams,
                        "There should be no old params");

                return null;
            }
        };

        element.callJsFunction("foo", 1, true);

        Assertions.assertEquals("foo", invokedFuction.get());
        Assertions.assertEquals(Integer.valueOf(1), invokedParams.get()[0]);
        Assertions.assertEquals(Boolean.TRUE, invokedParams.get()[1]);
    }

    @Override
    protected Element createParentNode() {
        return ElementFactory.createDiv();
    }

    @Override
    protected void assertChild(Node<?> parent, int index, Element child) {
        Assertions.assertEquals(parent, child.getParent());
        Assertions.assertEquals(child, parent.getChild(index));
    }

    private void assertPendingJs(UI ui, String js, Object... arguments) {
        List<PendingJavaScriptInvocation> pendingJs = ui.getInternals()
                .dumpPendingJavaScriptInvocations();
        JavaScriptInvocation expected = new JavaScriptInvocation(js, arguments);
        Assertions.assertEquals(1, pendingJs.size());
        assertEquals(expected, pendingJs.get(0).getInvocation());

    }

    private void assertEquals(JavaScriptInvocation expected,
            JavaScriptInvocation actual) {
        Assertions.assertEquals(expected.getExpression(),
                actual.getExpression());
        Assertions.assertArrayEquals(expected.getParameters().toArray(),
                actual.getParameters().toArray());

    }

    private static ArrayNode createNumberArray(double... items) {
        return DoubleStream.of(items).mapToObj(JacksonUtils::createNode)
                .collect(JacksonUtils.asArray());
    }

}
