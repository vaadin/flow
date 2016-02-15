package com.vaadin.hummingbird.dom;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.dom.impl.BasicElementStateProvider;
import com.vaadin.hummingbird.namespace.ElementListenersNamespace;
import com.vaadin.hummingbird.namespace.ElementPropertyNamespace;

import elemental.json.Json;
import elemental.json.JsonValue;
import elemental.json.impl.JreJsonObject;

public class ElementTest {

    @Test
    public void createElementWithTag() {
        Element e = new Element("div");
        Assert.assertEquals("div", e.getTag());
        Assert.assertFalse(e.hasAttribute("is"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void createElementWithInvalidTag() {
        new Element("<div>");
    }

    @Test(expected = IllegalArgumentException.class)
    public void createElementWithEmptyTag() {
        new Element("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void createElementWithNullTag() {
        new Element(null);
    }

    @Test
    public void elementsUpdateSameData() {
        Element te = new Element("testelem");
        Element e = Element.get(te.getNode());

        // Elements must be equal but not necessarily the same
        Assert.assertEquals(te, e);

        te.setAttribute("foo", "bar");
        Assert.assertEquals("bar", e.getAttribute("foo"));

        e.setAttribute("baz", "123");
        Assert.assertEquals("123", te.getAttribute("baz"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getElementFromInvalidNode() {
        StateNode node = new StateNode(ElementPropertyNamespace.class);
        Element.get(node);
    }

    @Test
    public void publicMethodsShouldReturnElement() {
        HashSet<String> ignore = new HashSet<>();
        ignore.add("toString");
        ignore.add("hashCode");
        ignore.add("equals");

        // Returns EventRegistrationHandle
        ignore.add("addEventListener");

        for (Method m : Element.class.getDeclaredMethods()) {
            if (!Modifier.isPublic(m.getModifiers())) {
                continue;
            }
            if (m.getName().startsWith("get") || m.getName().startsWith("has")
                    || m.getName().startsWith("is")
                    || ignore.contains(m.getName())) {
                // Ignore
            } else {
                // Setters and such
                Class<?> returnType = m.getReturnType();
                Assert.assertEquals(
                        "Method " + m.getName() + " has invalid return type",
                        Element.class, returnType);
            }
        }

    }

    @Test
    public void stringAttribute() {
        Element e = new Element("div");
        e.setAttribute("foo", "bar");
        Assert.assertEquals("bar", e.getAttribute("foo"));
    }

    @Test
    public void setEmptyAttribute() {
        Element e = new Element("div");
        e.setAttribute("foo", "");
        Assert.assertEquals("", e.getAttribute("foo"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void setNullAttribute() {
        Element e = new Element("div");
        e.setAttribute("foo", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getNullAttribute() {
        Element e = new Element("div");
        e.getAttribute(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void hasNullAttribute() {
        Element e = new Element("div");
        e.hasAttribute(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeNullAttribute() {
        Element e = new Element("div");
        e.removeAttribute(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setInvalidAttribute() {
        Element e = new Element("div");
        e.setAttribute("\"foo\"", "bar");
    }

    @Test
    public void isNullValidAttribute() {
        Assert.assertFalse(Element.isValidAttributeName(null));
    }

    @Test
    public void isEmptyValidAttribute() {
        Assert.assertFalse(Element.isValidAttributeName(""));
    }

    @Test(expected = AssertionError.class)
    public void isUpperCaseValidAttribute() {
        // isValidAttributeName is designed to only be called with lowercase
        // attribute names
        Element.isValidAttributeName("FOO");
    }

    @Test
    public void hasDefinedAttribute() {
        Element e = new Element("div");
        e.setAttribute("foo", "bar");
        Assert.assertTrue(e.hasAttribute("foo"));
    }

    @Test
    public void doesNotHaveUndefinedAttribute() {
        Element e = new Element("div");
        Assert.assertFalse(e.hasAttribute("foo"));
    }

    @Test
    public void doesNotHaveRemovedAttribute() {
        Element e = new Element("div");
        e.setAttribute("foo", "bar");
        e.removeAttribute("foo");
        Assert.assertFalse(e.hasAttribute("foo"));
    }

    @Test
    public void removeNonExistingAttributeIsNoOp() {
        Element e = new Element("div");
        Assert.assertFalse(e.hasAttribute("foo"));
        e.removeAttribute("foo");
        Assert.assertFalse(e.hasAttribute("foo"));
    }

    @Test
    public void attributesWhenNoneDefined() {
        Element e = new Element("div");
        Assert.assertTrue(e.getAttributeNames().isEmpty());
    }

    @Test
    public void attributesNames() {
        Element e = new Element("div");
        e.setAttribute("foo", "bar");
        Assert.assertArrayEquals(new String[] { "foo" },
                e.getAttributeNames().toArray());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void attributesNamesModificationsNotReflected() {
        Element e = new Element("div");
        e.setAttribute("foo", "bar");
        e.getAttributeNames().remove("foo");
        Assert.assertTrue(e.hasAttribute("foo"));
    }

    @Test
    public void attributesNamesShouldNotBeDynamic() {
        Element e = new Element("div");
        e.setAttribute("foo", "bar");
        Set<String> names = e.getAttributeNames();
        Assert.assertEquals(1, names.size());
        e.setAttribute("baz", "zoo");

        // NOTE: This only tests the current implementation. This is not
        // guaranteed behavior
        Assert.assertEquals(2, names.size());
        Assert.assertTrue(e.hasAttribute("baz"));
    }

    @Test
    public void attributesNamesAfterRemoved() {
        Element e = new Element("div");
        e.setAttribute("foo", "bar");
        e.setAttribute("bar", "baz");
        e.removeAttribute("foo");
        Assert.assertArrayEquals(new String[] { "bar" },
                e.getAttributeNames().toArray());
    }

    @Test
    public void setGetAttributeValueCaseSensitive() {
        Element e = new Element("span");
        e.setAttribute("foo", "bAr");
        Assert.assertEquals("bAr", e.getAttribute("foo"));
        e.setAttribute("foo", "BAR");
        Assert.assertEquals("BAR", e.getAttribute("foo"));
    }

    @Test
    public void setGetAttributeNameCaseInsensitive() {
        Element e = new Element("span");
        e.setAttribute("foo", "bar");
        e.setAttribute("FOO", "baz");

        Assert.assertEquals("baz", e.getAttribute("foo"));
        Assert.assertEquals("baz", e.getAttribute("FOO"));
    }

    @Test
    public void hasAttributeNamesCaseInsensitive() {
        Element e = new Element("span");
        e.setAttribute("fooo", "bar");
        Assert.assertTrue(e.hasAttribute("fOoO"));
    }

    @Test
    public void getAttributeNamesLowerCase() {
        Element e = new Element("span");
        e.setAttribute("FOO", "bar");
        e.setAttribute("Baz", "bar");

        Assert.assertTrue(e.getAttributeNames().contains("foo"));
        Assert.assertFalse(e.getAttributeNames().contains("FOO"));
        Assert.assertTrue(e.getAttributeNames().contains("baz"));
        Assert.assertFalse(e.getAttributeNames().contains("Baz"));
    }

    @Test
    public void appendChild() {
        Element parent = new Element("div");
        Element child = new Element("child");
        parent.appendChild(child);

        assertChildren(parent, child);
    }

    @Test(expected = IllegalArgumentException.class)
    public void appendNullChild() {
        Element parent = new Element("div");
        parent.appendChild((Element[]) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void insertNullChild() {
        Element parent = new Element("div");
        parent.insertChild(0, (Element[]) null);
    }

    @Test
    public void appendChildren() {
        Element parent = new Element("div");
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        parent.appendChild(child1, child2);

        assertChildren(parent, child1, child2);
    }

    private void assertChildren(Element parent, Element... children) {

        Assert.assertEquals(children.length, parent.getChildCount());
        for (int i = 0; i < children.length; i++) {
            Assert.assertEquals(parent, children[i].getParent());
            Assert.assertEquals(children[i], parent.getChild(i));
        }
    }

    @Test
    public void insertChildFirst() {
        Element parent = new Element("div");
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        parent.appendChild(child1);
        parent.insertChild(0, child2);

        assertChildren(parent, child2, child1);
    }

    @Test
    public void insertChildMiddle() {
        Element parent = new Element("div");
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        Element child3 = new Element("child3");
        parent.appendChild(child1, child2);
        parent.insertChild(1, child3);

        assertChildren(parent, child1, child3, child2);
    }

    @Test
    public void insertChildAsLast() {
        Element parent = new Element("div");
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        Element child3 = new Element("child3");
        parent.appendChild(child1, child2);
        parent.insertChild(2, child3);

        assertChildren(parent, child1, child2, child3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void insertChildAfterLast() {
        Element parent = new Element("div");
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        Element child3 = new Element("child3");
        parent.appendChild(child1, child2);
        parent.insertChild(3, child3);
    }

    @Test
    public void removeChildFirst() {
        Element parent = new Element("div");
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        Element child3 = new Element("child3");
        parent.appendChild(child1, child2, child3);
        parent.removeChild(child1);

        assertChildren(parent, child2, child3);
    }

    @Test
    public void removeChildFirstIndex() {
        Element parent = new Element("div");
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        Element child3 = new Element("child3");
        parent.appendChild(child1, child2, child3);
        parent.removeChild(0);

        assertChildren(parent, child2, child3);
    }

    @Test
    public void removeChildrenFirst() {
        Element parent = new Element("div");
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        Element child3 = new Element("child3");
        parent.appendChild(child1, child2, child3);
        parent.removeChild(child1, child2);

        assertChildren(parent, child3);
    }

    @Test
    public void removeChildMiddle() {
        Element parent = new Element("div");
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        Element child3 = new Element("child3");
        parent.appendChild(child1, child2, child3);
        parent.removeChild(child2);

        assertChildren(parent, child1, child3);
    }

    @Test
    public void removeChildMiddleIndex() {
        Element parent = new Element("div");
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        Element child3 = new Element("child3");
        parent.appendChild(child1, child2, child3);
        parent.removeChild(1);

        assertChildren(parent, child1, child3);
    }

    @Test
    public void removeChildrenMiddle() {
        Element parent = new Element("div");
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        Element child3 = new Element("child3");
        Element child4 = new Element("child4");
        parent.appendChild(child1, child2, child3, child4);
        parent.removeChild(child2, child3);

        assertChildren(parent, child1, child4);
    }

    @Test
    public void removeChildLast() {
        Element parent = new Element("div");
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        Element child3 = new Element("child3");
        parent.appendChild(child1, child2, child3);
        parent.removeChild(child3);

        assertChildren(parent, child1, child2);
    }

    @Test
    public void removeChildLastIndex() {
        Element parent = new Element("div");
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        Element child3 = new Element("child3");
        parent.appendChild(child1, child2, child3);
        parent.removeChild(2);

        assertChildren(parent, child1, child2);
    }

    @Test
    public void removeChildrenLast() {
        Element parent = new Element("div");
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        Element child3 = new Element("child3");
        Element child4 = new Element("child4");
        parent.appendChild(child1, child2, child3, child4);
        parent.removeChild(child3, child4);

        assertChildren(parent, child1, child2);
    }

    @Test
    public void removeAllChildren() {
        Element parent = new Element("div");
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        Element child3 = new Element("child3");
        Element child4 = new Element("child4");
        parent.appendChild(child1, child2, child3, child4);
        parent.removeAllChildren();

        assertChildren(parent);
    }

    @Test
    public void removeAllChildrenEmpty() {
        Element parent = new Element("div");
        parent.removeAllChildren();

        assertChildren(parent);
    }

    @Test
    public void removeFromParent() {
        Element parent = new Element("div");
        Element otherElement = new Element("other");
        parent.appendChild(otherElement);
        Assert.assertEquals(parent, otherElement.getParent());
        otherElement.removeFromParent();
        Assert.assertNull(otherElement.getParent());
    }

    @Test
    public void removeDetachedFromParent() {
        Element otherElement = new Element("other");
        Assert.assertNull(otherElement.getParent());
        otherElement.removeFromParent(); // No op
        Assert.assertNull(otherElement.getParent());
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeNonChild() {
        Element parent = new Element("div");
        Element otherElement = new Element("other");
        parent.removeChild(otherElement);
    }

    @Test
    public void getChild() {
        Element parent = new Element("div");
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        Element child3 = new Element("child3");
        Element child4 = new Element("child4");
        parent.appendChild(child1, child2, child3, child4);
        Assert.assertEquals(child1, parent.getChild(0));
        Assert.assertEquals(child2, parent.getChild(1));
        Assert.assertEquals(child3, parent.getChild(2));
        Assert.assertEquals(child4, parent.getChild(3));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getNegativeChild() {
        Element parent = new Element("div");
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        parent.appendChild(child1, child2);
        parent.getChild(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getAfterLastChild() {
        Element parent = new Element("div");
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        parent.appendChild(child1, child2);
        parent.getChild(2);
    }

    @Test
    public void getDetachedParent() {
        Element otherElement = new Element("other");
        Assert.assertNull(otherElement.getParent());
    }

    @Test(expected = IllegalArgumentException.class)
    public void addNullEventListener() {
        Element e = new Element("div");
        e.addEventListener("foo", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void addEventListenerForNullType() {
        Element e = new Element("div");
        e.addEventListener(null, () -> {
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void replaceNullChild() {
        Element parent = new Element("div");
        Element child1 = new Element("child1");
        parent.appendChild(child1);
        parent.setChild(0, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeNullChild() {
        Element parent = new Element("div");
        parent.removeChild((Element[]) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void replaceBeforeFirstChild() {
        Element parent = new Element("div");
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        parent.appendChild(child1);
        parent.setChild(-1, child2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void replaceAfterLastChild() {
        Element parent = new Element("div");
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        parent.appendChild(child1);
        parent.setChild(1, child2);
    }

    @Test
    public void replaceFirstChild() {
        Element parent = new Element("div");
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        parent.appendChild(child1);
        parent.setChild(0, child2);
        Assert.assertNull(child1.getParent());
        Assert.assertEquals(parent, child2.getParent());
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeChildBeforeFirst() {
        Element parent = new Element("div");
        Element child1 = new Element("child1");
        parent.appendChild(child1);
        parent.removeChild(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeChildAfterLast() {
        Element parent = new Element("div");
        Element child1 = new Element("child1");
        parent.appendChild(child1);
        parent.removeChild(1);
    }

    @Test
    public void equalsSelf() {
        Element e = new Element("div");
        Assert.assertTrue(e.equals(e));
    }

    @Test
    public void notEqualsNull() {
        Element e = new Element("div");
        Assert.assertFalse(e.equals(null));
    }

    @Test
    public void notEqualsString() {
        Element e = new Element("div");
        Assert.assertFalse(e.equals("div"));
    }

    @Test
    public void listenerReceivesEvents() {
        Element e = new Element("div");
        AtomicInteger listenerCalls = new AtomicInteger(0);
        DomEventListener myListener = new DomEventListener() {

            @Override
            public void handleEvent() {
                listenerCalls.incrementAndGet();
            }
        };
        e.addEventListener("click", myListener);
        Assert.assertEquals(0, listenerCalls.get());
        e.getNode().getNamespace(ElementListenersNamespace.class)
                .fireEvent("click");
        Assert.assertEquals(1, listenerCalls.get());
    }

    @Test
    public void getPropertyDefaults() {
        Element element = new Element("div");

        element.setProperty("null", null);
        element.setProperty("empty", "");

        Assert.assertEquals("d", element.getProperty("null", "d"));
        Assert.assertEquals("d", element.getProperty("notThere", "d"));
        Assert.assertNotEquals("d", element.getProperty("empty", "d"));

        Assert.assertTrue(element.getProperty("null", true));
        Assert.assertFalse(element.getProperty("null", false));
        Assert.assertTrue(element.getProperty("notThere", true));
        Assert.assertFalse(element.getProperty("notThere", false));
        Assert.assertFalse(element.getProperty("empty", true));
        Assert.assertFalse(element.getProperty("empty", false));

        Assert.assertEquals(0.1, element.getProperty("null", 0.1), 0);
        Assert.assertEquals(0.1, element.getProperty("notThere", 0.1), 0);
        Assert.assertNotEquals(0.1, element.getProperty("empty", 0.1), 0);

        Assert.assertEquals(42, element.getProperty("null", 42));
        Assert.assertEquals(42, element.getProperty("notThere", 42));
        Assert.assertNotEquals(42, element.getProperty("empty", 42));
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

        assertPropertyString(null, Json.createNull());
        assertPropertyString("{}", Json.createObject());
    }

    private static void assertPropertyString(String expected, Object value) {
        Element element = createPropertyAssertElement(value);

        Assert.assertEquals(expected, element.getProperty("property"));
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

        assertPropertyBoolean(false, Json.createNull());
        assertPropertyBoolean(false, Json.create(false));
        assertPropertyBoolean(true, Json.create(true));
        assertPropertyBoolean(true, Json.createObject());
    }

    private static void assertPropertyBoolean(boolean expected, Object value) {
        Element element = createPropertyAssertElement(value);

        // !expected -> default value will always fail
        boolean actual = element.getProperty("property", !expected);

        if (expected) {
            Assert.assertTrue(actual);
        } else {
            Assert.assertFalse(actual);
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

        assertPropertyDouble(.1, Json.create(.1));
        assertPropertyDouble(1, Json.create(true));
        assertPropertyDouble(0, Json.create(false));
        assertPropertyDouble(.1, Json.create(".1"));
        assertPropertyDouble(Double.NaN, Json.create("foo"));
        assertPropertyDouble(Double.NaN, Json.createObject());
    }

    private static void assertPropertyDouble(double expected, Object value) {
        Element element = createPropertyAssertElement(value);

        int delta = 0;
        double defaultValue = 1234d;

        if (defaultValue == expected) {
            throw new IllegalArgumentException(
                    "Expecting the default value might cause unintended results");
        }

        Assert.assertEquals(expected,
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

        assertPropertyInt(1, Json.create(1));
        assertPropertyInt(1, Json.create(1.9));
        assertPropertyInt(1, Json.create(true));
        assertPropertyInt(0, Json.create(false));
        assertPropertyInt(1, Json.create("1"));
        assertPropertyInt(0, Json.create("foo"));
        assertPropertyInt(0, Json.createObject());
    }

    private static void assertPropertyInt(int expected, Object value) {
        Element element = createPropertyAssertElement(value);

        int defaultValue = 1234;

        if (defaultValue == expected) {
            throw new IllegalArgumentException(
                    "Expecting the default value might cause unintended results");
        }

        Assert.assertEquals(expected,
                element.getProperty("property", defaultValue));
    }

    @Test
    public void propertyRawValues() {
        Element element = new Element("div");

        element.setProperty("p", "v");
        Assert.assertEquals("v", element.getPropertyRaw("p"));

        element.setProperty("p", true);
        Assert.assertEquals(Boolean.TRUE, element.getPropertyRaw("p"));

        element.setProperty("p", 3.14);
        Assert.assertEquals(Double.valueOf(3.14), element.getPropertyRaw("p"));

        element.setPropertyJson("p", Json.createObject());
        Assert.assertEquals(JreJsonObject.class,
                element.getPropertyRaw("p").getClass());
    }

    @Test
    public void addAndRemoveProperty() {
        Element element = new Element("div");

        Assert.assertFalse(element.hasProperty("foo"));
        element.removeProperty("foo");
        Assert.assertFalse(element.hasProperty("foo"));

        element.setProperty("foo", "bar");
        Assert.assertTrue(element.hasProperty("foo"));
        element.setProperty("foo", null);
        Assert.assertTrue(element.hasProperty("foo"));

        element.removeProperty("foo");
        Assert.assertFalse(element.hasProperty("foo"));
    }

    @Test
    public void propertyNames() {
        Element element = new Element("div");

        Assert.assertEquals(Collections.emptySet(), element.getPropertyNames());

        element.setProperty("foo", "bar");
        Assert.assertEquals(Collections.singleton("foo"),
                element.getPropertyNames());

        element.removeProperty("foo");
        Assert.assertEquals(Collections.emptySet(), element.getPropertyNames());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void propertyNamesUnmodifiable() {
        Element element = new Element("foo");

        element.getPropertyNames().remove("bar");
    }

    private static Element createPropertyAssertElement(Object value) {
        Element element = new Element("div");

        if (value instanceof Number && !(value instanceof Double)) {
            throw new IllegalArgumentException(
                    "Double is the only accepted numeric type");
        }

        if (value instanceof Serializable) {
            BasicElementStateProvider.get().setProperty(element.getNode(),
                    "property", (Serializable) value);
        } else if (value instanceof JsonValue) {
            element.setPropertyJson("property", (JsonValue) value);
        } else if (value == null) {
            element.setProperty("property", null);
        } else {
            throw new IllegalArgumentException(
                    "Invalid value type: " + value.getClass());
        }

        return element;
    }
}
