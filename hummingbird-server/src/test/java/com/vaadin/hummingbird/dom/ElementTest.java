package com.vaadin.hummingbird.dom;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.NullOwner;
import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.change.ListSpliceChange;
import com.vaadin.hummingbird.dom.impl.BasicElementStateProvider;
import com.vaadin.hummingbird.namespace.ElementAttributeNamespace;
import com.vaadin.hummingbird.namespace.ElementChildrenNamespace;
import com.vaadin.hummingbird.namespace.ElementListenersNamespace;
import com.vaadin.hummingbird.namespace.ElementPropertyNamespace;
import com.vaadin.hummingbird.namespace.SynchronizedPropertiesNamespace;
import com.vaadin.hummingbird.namespace.SynchronizedPropertyEventsNamespace;
import com.vaadin.server.StreamResource;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinSession;
import com.vaadin.tests.util.TestUtil;
import com.vaadin.ui.UI;

import elemental.json.Json;
import elemental.json.JsonValue;
import elemental.json.impl.JreJsonObject;

public class ElementTest {

    @Test
    public void createElementWithTag() {
        Element e = ElementFactory.createDiv();
        Assert.assertEquals("div", e.getTag());
        Assert.assertFalse(e.hasAttribute("is"));
        Assert.assertFalse(e.isTextNode());
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
    public void publicElementMethodsShouldReturnElement() {
        HashSet<String> ignore = new HashSet<>();
        ignore.add("toString");
        ignore.add("hashCode");
        ignore.add("equals");

        // Returns EventRegistrationHandle
        ignore.add("addEventListener");
        ignore.add("addAttachListener");
        ignore.add("addDetachListener");

        // Returns index of child element
        ignore.add("indexOfChild");

        for (Method m : Element.class.getDeclaredMethods()) {
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
                Assert.assertEquals(
                        "Method " + m.getName() + " has invalid return type",
                        Element.class, returnType);
            }
        }

    }

    @Test
    public void publicElementStyleMethodsShouldReturnElement() {
        HashSet<String> ignore = new HashSet<>();
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
                Assert.assertEquals(
                        "Method " + m.getName() + " has invalid return type",
                        Style.class, returnType);
            }
        }

    }

    @Test
    public void stringAttribute() {
        Element e = ElementFactory.createDiv();
        e.setAttribute("foo", "bar");
        Assert.assertEquals("bar", e.getAttribute("foo"));
    }

    @Test
    public void setEmptyAttribute() {
        Element e = ElementFactory.createDiv();
        e.setAttribute("foo", "");
        Assert.assertEquals("", e.getAttribute("foo"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void setNullAttribute() {
        Element e = ElementFactory.createDiv();
        e.setAttribute("foo", (String) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getNullAttribute() {
        Element e = ElementFactory.createDiv();
        e.getAttribute(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void hasNullAttribute() {
        Element e = ElementFactory.createDiv();
        e.hasAttribute(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeNullAttribute() {
        Element e = ElementFactory.createDiv();
        e.removeAttribute(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setInvalidAttribute() {
        Element e = ElementFactory.createDiv();
        e.setAttribute("\"foo\"", "bar");
    }

    @Test
    public void hasDefinedAttribute() {
        Element e = ElementFactory.createDiv();
        e.setAttribute("foo", "bar");
        Assert.assertTrue(e.hasAttribute("foo"));
    }

    @Test
    public void doesNotHaveUndefinedAttribute() {
        Element e = ElementFactory.createDiv();
        Assert.assertFalse(e.hasAttribute("foo"));
    }

    @Test
    public void doesNotHaveRemovedAttribute() {
        Element e = ElementFactory.createDiv();
        e.setAttribute("foo", "bar");
        e.removeAttribute("foo");
        Assert.assertFalse(e.hasAttribute("foo"));
    }

    @Test
    public void removeNonExistingAttributeIsNoOp() {
        Element e = ElementFactory.createDiv();
        Assert.assertFalse(e.hasAttribute("foo"));
        e.removeAttribute("foo");
        Assert.assertFalse(e.hasAttribute("foo"));
    }

    @Test
    public void attributesWhenNoneDefined() {
        Element e = ElementFactory.createDiv();
        Assert.assertEquals(0, e.getAttributeNames().count());
    }

    @Test
    public void attributesNames() {
        Element e = ElementFactory.createDiv();
        e.setAttribute("foo", "bar");
        Assert.assertArrayEquals(new String[] { "foo" },
                e.getAttributeNames().toArray());
    }

    @Test
    public void attributesNamesAfterRemoved() {
        Element e = ElementFactory.createDiv();
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

        Set<String> attributeNames = e.getAttributeNames()
                .collect(Collectors.toSet());
        Assert.assertTrue(attributeNames.contains("foo"));
        Assert.assertFalse(attributeNames.contains("FOO"));
        Assert.assertTrue(attributeNames.contains("baz"));
        Assert.assertFalse(attributeNames.contains("Baz"));
    }

    @Test
    public void appendChild() {
        Element parent = ElementFactory.createDiv();
        Element child = new Element("child");
        parent.appendChild(child);

        assertChildren(parent, child);
    }

    @Test(expected = IllegalArgumentException.class)
    public void appendNullChild() {
        Element parent = ElementFactory.createDiv();
        parent.appendChild((Element[]) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void insertNullChild() {
        Element parent = ElementFactory.createDiv();
        parent.insertChild(0, (Element[]) null);
    }

    @Test
    public void appendChildren() {
        Element parent = ElementFactory.createDiv();
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
        Element parent = ElementFactory.createDiv();
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        parent.appendChild(child1);
        parent.insertChild(0, child2);

        assertChildren(parent, child2, child1);
    }

    @Test
    public void insertChildMiddle() {
        Element parent = ElementFactory.createDiv();
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        Element child3 = new Element("child3");
        parent.appendChild(child1, child2);
        parent.insertChild(1, child3);

        assertChildren(parent, child1, child3, child2);
    }

    @Test
    public void insertChildAsLast() {
        Element parent = ElementFactory.createDiv();
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        Element child3 = new Element("child3");
        parent.appendChild(child1, child2);
        parent.insertChild(2, child3);

        assertChildren(parent, child1, child2, child3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void insertChildAfterLast() {
        Element parent = ElementFactory.createDiv();
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        Element child3 = new Element("child3");
        parent.appendChild(child1, child2);
        parent.insertChild(3, child3);
    }

    @Test
    public void removeChildFirst() {
        Element parent = ElementFactory.createDiv();
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        Element child3 = new Element("child3");
        parent.appendChild(child1, child2, child3);
        parent.removeChild(child1);

        assertChildren(parent, child2, child3);
    }

    @Test
    public void removeChildFirstIndex() {
        Element parent = ElementFactory.createDiv();
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        Element child3 = new Element("child3");
        parent.appendChild(child1, child2, child3);
        parent.removeChild(0);

        assertChildren(parent, child2, child3);
    }

    @Test
    public void removeChildrenFirst() {
        Element parent = ElementFactory.createDiv();
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        Element child3 = new Element("child3");
        parent.appendChild(child1, child2, child3);
        parent.removeChild(child1, child2);

        assertChildren(parent, child3);
    }

    @Test
    public void removeChildMiddle() {
        Element parent = ElementFactory.createDiv();
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        Element child3 = new Element("child3");
        parent.appendChild(child1, child2, child3);
        parent.removeChild(child2);

        assertChildren(parent, child1, child3);
    }

    @Test
    public void removeChildMiddleIndex() {
        Element parent = ElementFactory.createDiv();
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        Element child3 = new Element("child3");
        parent.appendChild(child1, child2, child3);
        parent.removeChild(1);

        assertChildren(parent, child1, child3);
    }

    @Test
    public void removeChildrenMiddle() {
        Element parent = ElementFactory.createDiv();
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
        Element parent = ElementFactory.createDiv();
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        Element child3 = new Element("child3");
        parent.appendChild(child1, child2, child3);
        parent.removeChild(child3);

        assertChildren(parent, child1, child2);
    }

    @Test
    public void removeChildLastIndex() {
        Element parent = ElementFactory.createDiv();
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        Element child3 = new Element("child3");
        parent.appendChild(child1, child2, child3);
        parent.removeChild(2);

        assertChildren(parent, child1, child2);
    }

    @Test
    public void removeChildrenLast() {
        Element parent = ElementFactory.createDiv();
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
        Element parent = ElementFactory.createDiv();
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
        Element parent = ElementFactory.createDiv();
        parent.removeAllChildren();

        assertChildren(parent);
    }

    @Test
    public void testGetChildren() {
        Element element = ElementFactory.createDiv();

        Element child1 = ElementFactory.createDiv();
        Element child2 = ElementFactory.createDiv();
        Element child3 = ElementFactory.createDiv();

        element.appendChild(child1, child2, child3);

        List<Element> children = element.getChildren()
                .collect(Collectors.toList());
        Assert.assertEquals(Arrays.asList(child1, child2, child3), children);
    }

    @Test
    public void testGetChildren_empty() {
        Element element = ElementFactory.createDiv();

        Assert.assertEquals(0, element.getChildren().count());
    }

    @Test
    public void removeFromParent() {
        Element parent = ElementFactory.createDiv();
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
        Element parent = ElementFactory.createDiv();
        Element otherElement = new Element("other");
        parent.removeChild(otherElement);
    }

    @Test
    public void getChild() {
        Element parent = ElementFactory.createDiv();
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
        Element parent = ElementFactory.createDiv();
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        parent.appendChild(child1, child2);
        parent.getChild(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getAfterLastChild() {
        Element parent = ElementFactory.createDiv();
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
        Element e = ElementFactory.createDiv();
        e.addEventListener("foo", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void addEventListenerForNullType() {
        Element e = ElementFactory.createDiv();
        e.addEventListener(null, ignore -> {
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void replaceNullChild() {
        Element parent = ElementFactory.createDiv();
        Element child1 = new Element("child1");
        parent.appendChild(child1);
        parent.setChild(0, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeNullChild() {
        Element parent = ElementFactory.createDiv();
        parent.removeChild((Element[]) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void replaceBeforeFirstChild() {
        Element parent = ElementFactory.createDiv();
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        parent.appendChild(child1);
        parent.setChild(-1, child2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void replaceAfterLastChild() {
        Element parent = ElementFactory.createDiv();
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        parent.appendChild(child1);
        parent.setChild(1, child2);
    }

    @Test
    public void replaceFirstChild() {
        Element parent = ElementFactory.createDiv();
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        parent.appendChild(child1);
        parent.setChild(0, child2);
        Assert.assertNull(child1.getParent());
        Assert.assertEquals(parent, child2.getParent());
    }

    @Test
    public void replaceChildWithItself() {
        Element parent = ElementFactory.createDiv();
        Element child1 = new Element("child1");
        parent.appendChild(child1);

        parent.getNode().getNamespace(ElementChildrenNamespace.class)
                .collectChanges(e -> {
                    // Remove the "append" change
                });

        parent.setChild(0, child1);

        AtomicInteger changesCausedBySetChild = new AtomicInteger(0);
        parent.getNode().getNamespace(ElementChildrenNamespace.class)
                .collectChanges(change -> {
                    changesCausedBySetChild.incrementAndGet();
                });
        Assert.assertEquals(0, changesCausedBySetChild.get());
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeChildBeforeFirst() {
        Element parent = ElementFactory.createDiv();
        Element child1 = new Element("child1");
        parent.appendChild(child1);
        parent.removeChild(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeChildAfterLast() {
        Element parent = ElementFactory.createDiv();
        Element child1 = new Element("child1");
        parent.appendChild(child1);
        parent.removeChild(1);
    }

    @Test
    public void equalsSelf() {
        Element e = ElementFactory.createDiv();
        Assert.assertTrue(e.equals(e));
    }

    @Test
    public void notEqualsNull() {
        Element e = ElementFactory.createDiv();
        Assert.assertFalse(e.equals(null));
    }

    @Test
    public void notEqualsString() {
        Element e = ElementFactory.createDiv();
        Assert.assertFalse(e.equals("div"));
    }

    @Test
    public void listenerReceivesEvents() {
        Element e = ElementFactory.createDiv();
        AtomicInteger listenerCalls = new AtomicInteger(0);
        DomEventListener myListener = event -> listenerCalls.incrementAndGet();

        e.addEventListener("click", myListener);
        Assert.assertEquals(0, listenerCalls.get());
        e.getNode().getNamespace(ElementListenersNamespace.class)
                .fireEvent(new DomEvent(e, "click", Json.createObject()));
        Assert.assertEquals(1, listenerCalls.get());
    }

    @Test
    public void getPropertyDefaults() {
        Element element = ElementFactory.createDiv();

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
        Element element = ElementFactory.createDiv();

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
        Element element = ElementFactory.createDiv();

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
        Element element = ElementFactory.createDiv();

        Assert.assertEquals(0, element.getPropertyNames().count());

        element.setProperty("foo", "bar");
        Assert.assertEquals(Collections.singleton("foo"),
                element.getPropertyNames().collect(Collectors.toSet()));

        element.removeProperty("foo");
        Assert.assertEquals(0, element.getPropertyNames().count());
    }

    private static Element createPropertyAssertElement(Object value) {
        Element element = ElementFactory.createDiv();

        if (value instanceof Number && !(value instanceof Double)) {
            throw new IllegalArgumentException(
                    "Double is the only accepted numeric type");
        }

        if (value instanceof Serializable) {
            BasicElementStateProvider.get().setProperty(element.getNode(),
                    "property", (Serializable) value, true);
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

    @Test
    public void testGetTextContent() {
        Element child = new Element("child");
        child.appendChild(Element.createText("bar"));

        Element element = ElementFactory.createDiv();

        element.appendChild(Element.createText("foo"));
        element.appendChild(child);

        Assert.assertEquals("foobar", element.getTextContent());
    }

    @Test
    public void testSetTextContent() {
        Element element = ElementFactory.createDiv();
        element.setTextContent("foo");

        Assert.assertEquals("foo", element.getTextContent());
        Assert.assertEquals(1, element.getChildCount());
        Assert.assertTrue(element.getChild(0).isTextNode());
    }

    @Test
    public void testSetTextContentRemovesOldContent() {
        Element child = new Element("child");
        Element element = ElementFactory.createDiv();
        element.appendChild(child);

        element.setTextContent("foo");

        Assert.assertNull(child.getParent());
        Assert.assertEquals("foo", element.getTextContent());
    }

    @Test
    public void testSetTextReplacesOldTextNode() {
        Element element = ElementFactory.createDiv();
        Element text = Element.createText("foo");
        element.appendChild(text);

        element.setTextContent("bar");

        Assert.assertEquals(element, text.getParent());
        Assert.assertEquals("bar", text.getTextContent());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetTextContentPropertyThrows() {
        Element element = new Element("element");
        element.setProperty("textContent", "foo");
    }

    @Test
    public void testGetTextContentProperty() {
        Element element = ElementFactory.createDiv();
        element.setTextContent("foo");

        Assert.assertFalse(element.hasProperty("textContent"));
        Assert.assertNull(element.getProperty("textContent"));
    }

    @Test
    // Because that's how it works in browsers
    public void clearTextContentRemovesChild() {
        Element element = ElementFactory.createDiv();
        element.setTextContent("foo");

        Assert.assertEquals(1, element.getChildCount());

        element.setTextContent("");

        Assert.assertEquals(0, element.getChildCount());
    }

    @Test
    public void newElementClasses() {
        Element element = ElementFactory.createDiv();

        Assert.assertFalse(element.hasAttribute("class"));
        Assert.assertEquals(Collections.emptySet(), element.getClassList());
    }

    @Test
    public void addElementClasses() {
        Element element = ElementFactory.createDiv();

        element.getClassList().add("foo");

        Assert.assertEquals(Collections.singleton("foo"),
                element.getClassList());
        Assert.assertTrue(element.hasAttribute("class"));

        Assert.assertEquals(Collections.singleton("class"),
                element.getAttributeNames().collect(Collectors.toSet()));
        Assert.assertTrue(element.hasAttribute("class"));
        Assert.assertEquals("foo", element.getAttribute("class"));

        element.getClassList().add("bar");

        Assert.assertEquals("foo bar", element.getAttribute("class"));
    }

    @Test
    public void testSetClassAttribute() {
        Element element = ElementFactory.createDiv();

        // Get instance right away to see that changes are live
        Set<String> classList = element.getClassList();

        element.setAttribute("class", "foo bar");

        Assert.assertEquals(2, classList.size());
        Assert.assertTrue(classList.contains("foo"));
        Assert.assertTrue(classList.contains("bar"));

        Assert.assertNull("class should not be stored as a regular attribute",
                element.getNode().getNamespace(ElementAttributeNamespace.class)
                        .get("class"));
    }

    @Test
    public void testSetEmptyClassAttribute() {
        Element element = new Element("div");

        // Get instance right away to see that changes are live
        Set<String> classList = element.getClassList();

        element.setAttribute("class", "");

        Assert.assertEquals(0, classList.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddEmptyClassname() {
        Element element = new Element("div");

        // Get instance right away to see that changes are live
        Set<String> classList = element.getClassList();

        classList.add("");
    }

    @Test
    public void testRemoveClassName() {
        Element element = ElementFactory.createDiv();

        element.setAttribute("class", "foo bar");

        element.getClassList().remove("foo");

        Assert.assertEquals("bar", element.getAttribute("class"));

        element.getClassList().remove("bar");

        Assert.assertNull(element.getAttribute("class"));
        Assert.assertFalse(element.hasAttribute("class"));

        Assert.assertEquals(0, element.getAttributeNames().count());
    }

    @Test
    public void testRemoveClassAttribute() {
        Element element = ElementFactory.createDiv();

        Set<String> classList = element.getClassList();

        classList.add("foo");

        element.removeAttribute("class");

        Assert.assertEquals(Collections.emptySet(), classList);
    }

    @Test
    public void addExistingClass_noop() {
        Element element = ElementFactory.createDiv();

        element.setAttribute("class", "foo");

        element.getClassList().add("foo");

        Assert.assertEquals(Collections.singleton("foo"),
                element.getClassList());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddClassWithSpaces_throws() {
        ElementFactory.createDiv().getClassList().add("foo bar");
    }

    @Test
    public void testRemoveClassWithSpaces() {
        ClassList cl = ElementFactory.createDiv().getClassList();
        cl.add("foo");
        cl.add("bar");
        cl.remove("foo bar");
        Assert.assertEquals(2, cl.size());
    }

    @Test
    public void testContainsClassWithSpaces() {
        ClassList cl = ElementFactory.createDiv().getClassList();
        cl.add("foo");
        cl.add("bar");

        Assert.assertFalse(cl.contains("foo bar"));
    }

    @Test
    public void classListSetAdd() {
        Element e = new Element("div");
        Assert.assertTrue(e.getClassList().set("foo", true));
        Assert.assertEquals("foo", e.getAttribute("class"));
        Assert.assertFalse(e.getClassList().set("foo", true));
        Assert.assertEquals("foo", e.getAttribute("class"));
    }

    @Test
    public void classListSetRemove() {
        Element e = new Element("div");
        e.setAttribute("class", "foo bar");
        Assert.assertTrue(e.getClassList().set("foo", false));
        Assert.assertEquals("bar", e.getAttribute("class"));
        Assert.assertFalse(e.getClassList().set("foo", false));
        Assert.assertEquals("bar", e.getAttribute("class"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testClassListProperty_throws() {
        ElementFactory.createDiv().setProperty("classList", "foo");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testClassNameProperty_throws() {
        ElementFactory.createDiv().setProperty("className", "foo");
    }

    public void setStyle() {
        Element e = ElementFactory.createDiv();
        Style s = e.getStyle();
        s.set("foo", "bar");
        Assert.assertEquals("bar", s.get("foo"));
    }

    @Test
    public void getUnsetStyle() {
        Element e = ElementFactory.createDiv();
        Style s = e.getStyle();
        Assert.assertNull(s.get("foo"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getNullStyle() {
        Element e = ElementFactory.createDiv();
        Style s = e.getStyle();
        s.get(null);
    }

    @Test
    public void replaceStyle() {
        Element e = ElementFactory.createDiv();
        Style s = e.getStyle();
        s.set("foo", "bar");
        s.set("foo", "baz");
        Assert.assertEquals("baz", s.get("foo"));
    }

    @Test
    public void removeSingleStyle() {
        Element e = ElementFactory.createDiv();
        Style s = e.getStyle();
        s.set("foo", "bar");
        s.remove("foo");
        Assert.assertEquals(null, s.get("foo"));
    }

    @Test
    public void emptyStyleAsAttribute() {
        Element e = ElementFactory.createDiv();
        Assert.assertFalse(e.hasAttribute("style"));
        Assert.assertNull(e.getAttribute("style"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void semicolonInStyle() {
        Element e = ElementFactory.createDiv();
        Style s = e.getStyle();
        s.set("border", "1 px solid black;");
    }

    @Test
    public void singleStyleAsAttribute() {
        Element e = ElementFactory.createDiv();
        Style s = e.getStyle();
        s.set("border", "1px solid black");
        Assert.assertTrue(e.hasAttribute("style"));
        Assert.assertEquals("border:1px solid black", e.getAttribute("style"));
    }

    @Test
    public void multipleStylesAsAttribute() {
        Element e = ElementFactory.createDiv();
        Style s = e.getStyle();
        s.set("border", "1px solid black");
        s.set("margin", "1em");
        Assert.assertTrue(e.hasAttribute("style"));
        assertEqualsOne(
                new String[] { "border:1px solid black;margin:1em",
                        "margin:1em;border:1px solid black" },
                e.getAttribute("style"));
    }

    private void assertEqualsOne(String[] expected, String actual) {
        for (int i = 0; i < expected.length; i++) {
            if (expected[i].equals(actual)) {
                return;
            }
        }
        String expectedString = Arrays.stream(expected)
                .collect(Collectors.joining("> or <"));
        Assert.fail(
                "expected: <" + expectedString + "> but was <" + actual + ">");

    }

    @Test(expected = IllegalArgumentException.class)
    public void setEmptyStyleName() {
        Element e = ElementFactory.createDiv();
        e.getStyle().set("", "foo");
    }

    @Test(expected = IllegalArgumentException.class)
    public void setStyleNameExtraWhitespace() {
        Element e = ElementFactory.createDiv();
        e.getStyle().set("   color", "red");
    }

    @Test
    public void setStyleValueExtraWhitespace() {
        Element e = ElementFactory.createDiv();
        e.getStyle().set("color", "red   ");
        Assert.assertEquals("color:red", e.getAttribute("style"));
        Assert.assertEquals("red", e.getStyle().get("color"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void setStyleAttribute() {
        Element e = ElementFactory.createDiv();
        e.setAttribute("style", "foo: bar;");
    }

    @Test
    public void removeStyles() {
        Element element = ElementFactory.createDiv();

        element.getStyle().set("zIndex", "12");
        element.getStyle().set("background", "blue");

        element.getStyle().remove("background");

        Assert.assertEquals("zIndex:12", element.getAttribute("style"));

        element.getStyle().remove("zIndex");

        Assert.assertNull(element.getAttribute("style"));
        Assert.assertFalse(element.hasAttribute("style"));

        Assert.assertEquals(0, element.getStyle().getNames().count());
    }

    @Test
    public void removeStyleAttribute() {
        Element element = ElementFactory.createDiv();

        Style style = element.getStyle();

        style.set("border", "1px solid green");

        element.removeAttribute("style");

        Assert.assertEquals(0, style.getNames().count());
    }

    @Test
    public void validStyleWithSemicolon() {
        Element element = ElementFactory.createDiv();
        String validStyle = "background: url('foo;bar')";
        Style style = element.getStyle();
        style.set("background", validStyle);
        Assert.assertEquals(validStyle, style.get("background"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void warnAboutDashSeparated() {
        Element element = ElementFactory.createDiv();

        Style style = element.getStyle();
        style.set("border-color", "blue");
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullStyleValue() {
        Element element = ElementFactory.createDiv();

        Style style = element.getStyle();
        style.set("borderColor", null);
    }

    @Test
    public void listenersFiredInRegisteredOrder() {
        Element element = ElementFactory.createDiv();
        ArrayList<Integer> eventOrder = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            final int j = i;
            element.addEventListener("click", e -> {
                eventOrder.add(j);
            });
        }
        fireEvent(element, "click");
        Assert.assertArrayEquals(new Object[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 },
                eventOrder.toArray());
    }

    private void fireEvent(Element element, String eventType) {
        element.getNode().getNamespace(ElementListenersNamespace.class)
                .fireEvent(
                        new DomEvent(element, eventType, Json.createObject()));

    }

    @Test
    public void eventsWhenListenerIsRegisteredManyTimes() {
        AtomicInteger invocations = new AtomicInteger(0);

        DomEventListener listener = e -> {
            invocations.incrementAndGet();
        };
        Element element = ElementFactory.createDiv();
        element.addEventListener("click", listener);
        element.addEventListener("click", listener);

        fireEvent(element, "click");

        Assert.assertEquals(2, invocations.get());
    }

    @Test
    public void getSetSynchronizedProperty() {
        Element e = ElementFactory.createDiv();
        e.addSynchronizedProperty("foo").addSynchronizedProperty("bar");

        Set<String> expected = new HashSet<>(Arrays.asList("bar", "foo"));

        List<String> list = e.getSynchronizedProperties()
                .collect(Collectors.toList());
        Assert.assertEquals(expected.size(), list.size());
        expected.removeAll(list);
        Assert.assertEquals(0, expected.size());
    }

    @Test
    public void setSameSynchronizedPropertyManyTimes() {
        Element e = ElementFactory.createDiv();
        e.addSynchronizedProperty("foo").addSynchronizedProperty("foo");
        String[] expected = new String[] { "foo" };

        Assert.assertArrayEquals(expected,
                e.getSynchronizedProperties().toArray());

        AtomicInteger i = new AtomicInteger(0);
        e.getNode().getNamespace(SynchronizedPropertiesNamespace.class)
                .collectChanges(change -> i.addAndGet(
                        ((ListSpliceChange) change).getNewItems().size()));
        Assert.assertEquals(1, i.get());
    }

    @Test
    public void synchronizeProperty() {
        Element element = ElementFactory.createDiv();
        element.synchronizeProperty("foo", "event");

        Assert.assertTrue(element.getSynchronizedProperties()
                .allMatch(prop -> prop.equals("foo")));
        Assert.assertTrue(element.getSynchronizedPropertyEvents()
                .allMatch(event -> event.equals("event")));
    }

    @Test
    public void removeSynchronizedProperty() {
        Element element = ElementFactory.createDiv();
        element.addSynchronizedProperty("foo");
        element.addSynchronizedProperty("bar");

        element.removeSynchronizedProperty("foo");
        Assert.assertTrue(element.getSynchronizedProperties()
                .allMatch(prop -> prop.equals("bar")));
    }

    @Test
    public void removeSynchronizedPropertyEvent() {
        Element element = ElementFactory.createDiv();
        element.addSynchronizedPropertyEvent("foo");
        element.addSynchronizedPropertyEvent("bar");

        element.removeSynchronizedPropertyEvent("foo");
        Assert.assertTrue(element.getSynchronizedPropertyEvents()
                .allMatch(event -> event.equals("bar")));
    }

    @Test
    public void setSameSynchronizedEventManyTimes() {
        Element e = ElementFactory.createDiv();
        e.addSynchronizedPropertyEvent("foo")
                .addSynchronizedPropertyEvent("foo");
        String[] expected = new String[] { "foo" };

        Assert.assertArrayEquals(expected,
                e.getSynchronizedPropertyEvents().toArray());

        AtomicInteger i = new AtomicInteger(0);
        e.getNode().getNamespace(SynchronizedPropertyEventsNamespace.class)
                .collectChanges(change -> i.addAndGet(
                        ((ListSpliceChange) change).getNewItems().size()));
        Assert.assertEquals(1, i.get());
    }

    @Test
    public void getDefaultSynchronizedProperties() {
        Element e = ElementFactory.createDiv();
        Assert.assertEquals(0, e.getSynchronizedProperties().count());
    }

    @Test
    public void getDefaultSynchronizedPropertiesEvent() {
        Element e = ElementFactory.createDiv();
        Assert.assertEquals(0, e.getSynchronizedPropertyEvents().count());
    }

    @Test
    public void getSetSynchronizedEvent() {
        Element e = ElementFactory.createDiv();
        e.addSynchronizedPropertyEvent("foo")
                .addSynchronizedPropertyEvent("bar");
        Set<String> expected = new HashSet<>(Arrays.asList("bar", "foo"));

        List<String> list = e.getSynchronizedPropertyEvents()
                .collect(Collectors.toList());
        Assert.assertEquals(expected.size(), list.size());
        expected.removeAll(list);
        Assert.assertEquals(0, expected.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void setNullSynchronizedEvent() {
        Element e = ElementFactory.createDiv();
        e.addSynchronizedPropertyEvent(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setNullSynchronizedProperty() {
        Element e = ElementFactory.createDiv();
        e.addSynchronizedProperty(null);
    }

    @Test(expected = IllegalStateException.class)
    public void addAsOwnChild() {
        Element element = ElementFactory.createDiv();
        element.appendChild(element);
    }

    @Test(expected = IllegalStateException.class)
    public void addAsChildOfChild() {
        Element parent = ElementFactory.createDiv();
        Element child = ElementFactory.createDiv();
        parent.appendChild(child);

        child.appendChild(parent);
    }

    @Test
    public void appendAttachedChild() {
        Element parent = ElementFactory.createDiv();
        Element child = ElementFactory.createDiv();
        parent.appendChild(child);

        Element target = ElementFactory.createDiv();

        target.appendChild(child);

        Assert.assertEquals(child.getParent(), target);

        checkIsNotChild(parent, child);
    }

    @Test
    public void insertAttachedChild() {
        Element parent = ElementFactory.createDiv();
        Element child = ElementFactory.createDiv();
        parent.appendChild(child);

        Element target = ElementFactory.createDiv();
        target.appendChild(ElementFactory.createAnchor());

        target.insertChild(0, child);

        Assert.assertEquals(child.getParent(), target);

        checkIsNotChild(parent, child);
    }

    @Test
    public void setAttachedChild() {
        Element parent = ElementFactory.createDiv();
        Element child = ElementFactory.createDiv();
        parent.appendChild(child);

        Element target = ElementFactory.createDiv();
        target.appendChild(ElementFactory.createAnchor());

        target.setChild(0, child);

        Assert.assertEquals(child.getParent(), target);

        checkIsNotChild(parent, child);
    }

    private void checkIsNotChild(Element parent, Element child) {
        Assert.assertNotEquals(child.getParent(), parent);

        Assert.assertFalse(
                parent.getChildren().anyMatch(el -> el.equals(child)));
    }

    public void indexOfChild_firstChild() {
        Element parent = ElementFactory.createDiv();
        Element child = ElementFactory.createDiv();
        parent.appendChild(child);

        Assert.assertEquals(0, parent.indexOfChild(child));
    }

    @Test
    public void indexOfChild_childInTheMiddle() {
        Element parent = ElementFactory.createDiv();
        Element child1 = ElementFactory.createDiv();
        Element child2 = ElementFactory.createAnchor();
        Element child3 = ElementFactory.createButton();
        parent.appendChild(child1, child2, child3);

        Assert.assertEquals(1, parent.indexOfChild(child2));
    }

    @Test
    public void indexOfChild_notAChild() {
        Element parent = ElementFactory.createDiv();
        Element child = ElementFactory.createDiv();

        Assert.assertEquals(-1, parent.indexOfChild(child));
    }

    @Test
    public void testGetOwnTextContent() {
        Element element = ElementFactory.createDiv();
        element.setTextContent("foo");
        element.appendChild(ElementFactory.createDiv()
                .appendChild(ElementFactory.createSpan("span contents")));
        element.appendChild(ElementFactory.createStrong("strong contents"));
        element.appendChild(Element.createText("Another text node"));

        Assert.assertEquals("fooAnother text node",
                element.getOwnTextContent());
        Assert.assertEquals("foospan contentsstrong contentsAnother text node",
                element.getTextContent());
    }

    @Test
    public void setResourceAttribute_elementIsNotAttached_elementHasAttribute() {
        Element element = ElementFactory.createDiv();
        String resName = "resource";
        StreamResource resource = createEmptyResource(resName);
        element.setAttribute("foo", resource);

        Assert.assertTrue(element.hasAttribute("foo"));

        Assert.assertTrue(element.getAttribute("foo").endsWith(resName));
    }

    @Test
    public void setResourceAttribute_elementIsNotAttachedAndHasAttribute_elementHasAttribute() {
        Element element = ElementFactory.createDiv();
        element.setAttribute("foo", "bar");

        String resName = "resource";
        StreamResource resource = createEmptyResource(resName);
        element.setAttribute("foo", resource);

        Assert.assertTrue(element.hasAttribute("foo"));

        Assert.assertTrue(element.getAttribute("foo").endsWith(resName));
    }

    @Test
    public void setResourceAttributeSeveralTimes_elementIsNotAttached_elementHasAttribute() {
        Element element = ElementFactory.createDiv();
        String resName = "resource";
        StreamResource resource = createEmptyResource(resName);
        element.setAttribute("foo", resource);

        Assert.assertTrue(element.hasAttribute("foo"));

        resName = "resource1";
        resource = createEmptyResource(resName);
        element.setAttribute("foo", resource);

        Assert.assertTrue(element.hasAttribute("foo"));

        Assert.assertTrue(element.getAttribute("foo").endsWith(resName));
    }

    @Test(expected = IllegalArgumentException.class)
    public void setResourceAttribute_nullValue() {
        Element element = ElementFactory.createDiv();
        element.setAttribute("foo", (StreamResource) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setResourceAttribute_classAttribute() {
        Element element = ElementFactory.createDiv();
        element.setAttribute("class",
                EasyMock.createMock(StreamResource.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void setResourceAttribute_nullAttribute() {
        Element element = ElementFactory.createDiv();
        element.setAttribute(null, EasyMock.createMock(StreamResource.class));
    }

    @Test
    public void setResourceAttribute_elementIsAttached_elementHasAttribute() {
        UI ui = createUI();
        String resName = "resource";
        StreamResource resource = createEmptyResource(resName);
        ui.getElement().setAttribute("foo", resource);

        Assert.assertTrue(ui.getElement().hasAttribute("foo"));
        Assert.assertTrue(
                ui.getElement().getAttribute("foo").endsWith(resName));
    }

    @Test
    public void setResourceAttribute_elementIsAttached_setAnotherResource()
            throws URISyntaxException {
        UI ui = createUI();
        StreamResource resource = createEmptyResource("resource1");
        ui.getElement().setAttribute("foo", resource);

        String uri = ui.getElement().getAttribute("foo");
        Optional<StreamResource> res = ui.getSession().getResourceRegistry()
                .getResource(new URI(uri));
        Assert.assertTrue(res.isPresent());

        String resName = "resource2";
        ui.getElement().setAttribute("foo", createEmptyResource(resName));
        res = ui.getSession().getResourceRegistry().getResource(new URI(uri));
        Assert.assertFalse(res.isPresent());

        Assert.assertTrue(ui.getElement().hasAttribute("foo"));
        Assert.assertTrue(
                ui.getElement().getAttribute("foo").endsWith(resName));
    }

    @Test
    public void setResourceAttribute_elementIsAttached_setRawAttribute()
            throws URISyntaxException, InterruptedException {
        UI ui = createUI();
        StreamResource resource = createEmptyResource("resource");
        ui.getElement().setAttribute("foo", resource);

        String uri = ui.getElement().getAttribute("foo");
        Optional<StreamResource> res = ui.getSession().getResourceRegistry()
                .getResource(new URI(uri));
        Assert.assertTrue(res.isPresent());
        res = null;

        WeakReference<StreamResource> ref = new WeakReference<StreamResource>(
                resource);
        resource = null;

        ui.getElement().setAttribute("foo", "bar");

        TestUtil.isGarbageCollected(ref);
        res = ui.getSession().getResourceRegistry().getResource(new URI(uri));

        Assert.assertFalse(res.isPresent());
        Assert.assertTrue(ui.getElement().hasAttribute("foo"));
        Assert.assertTrue(ui.getElement().getAttribute("foo").equals("bar"));
    }

    @Test
    public void setResourceAttribute_elementIsAttached_removeAttribute()
            throws URISyntaxException, InterruptedException {
        UI ui = createUI();
        StreamResource resource = createEmptyResource("resource");
        ui.getElement().setAttribute("foo", resource);

        String uri = ui.getElement().getAttribute("foo");
        Optional<StreamResource> res = ui.getSession().getResourceRegistry()
                .getResource(new URI(uri));
        Assert.assertTrue(res.isPresent());
        res = null;

        WeakReference<StreamResource> ref = new WeakReference<StreamResource>(
                resource);
        resource = null;

        ui.getElement().removeAttribute("foo");
        TestUtil.isGarbageCollected(ref);

        res = ui.getSession().getResourceRegistry().getResource(new URI(uri));
        Assert.assertFalse(res.isPresent());
        Assert.assertFalse(ui.getElement().hasAttribute("foo"));
        Assert.assertNull(ui.getElement().getAttribute("foo"));
    }

    @Test
    public void setResourceAttribute_attachElement_resourceIsRegistered()
            throws URISyntaxException {
        UI ui = createUI();

        StreamResource resource = createEmptyResource("resource");
        Element element = ElementFactory.createDiv();
        element.setAttribute("foo", resource);

        ui.getElement().appendChild(element);

        Assert.assertTrue(element.hasAttribute("foo"));

        String uri = element.getAttribute("foo");
        Optional<StreamResource> res = ui.getSession().getResourceRegistry()
                .getResource(new URI(uri));
        Assert.assertTrue(res.isPresent());
    }

    @Test
    public void setResourceAttribute_attachElement_setAnotherResource()
            throws URISyntaxException, InterruptedException {
        UI ui = createUI();

        StreamResource resource = createEmptyResource("resource1");
        Element element = ElementFactory.createDiv();
        element.setAttribute("foo", resource);

        WeakReference<StreamResource> ref = new WeakReference<StreamResource>(
                resource);
        resource = null;

        String resName = "resource2";
        element.setAttribute("foo", createEmptyResource(resName));

        ui.getElement().appendChild(element);

        Assert.assertTrue(element.hasAttribute("foo"));

        String uri = element.getAttribute("foo");
        Optional<StreamResource> res = ui.getSession().getResourceRegistry()
                .getResource(new URI(uri));
        Assert.assertTrue(res.isPresent());
        Assert.assertTrue(uri.endsWith(resName));

        // allow GC to collect element and all its (detach) listeners
        element = null;

        TestUtil.isGarbageCollected(ref);
    }

    @Test
    public void setResourceAttribute_attachElement_setRawAttribute()
            throws URISyntaxException, InterruptedException {
        UI ui = createUI();

        StreamResource resource = createEmptyResource("resource");
        Element element = ElementFactory.createDiv();
        element.setAttribute("foo", resource);

        WeakReference<StreamResource> ref = new WeakReference<StreamResource>(
                resource);
        resource = null;

        element.setAttribute("foo", "bar");

        TestUtil.isGarbageCollected(ref);

        ui.getElement().appendChild(element);

        Assert.assertTrue(element.hasAttribute("foo"));
        Assert.assertEquals("bar", element.getAttribute("foo"));
    }

    @Test
    public void setResourceAttribute_attachElement_removeAttribute()
            throws URISyntaxException, InterruptedException {
        UI ui = createUI();

        StreamResource resource = createEmptyResource("resource");
        Element element = ElementFactory.createDiv();
        element.setAttribute("foo", resource);

        WeakReference<StreamResource> ref = new WeakReference<StreamResource>(
                resource);
        resource = null;

        element.removeAttribute("foo");

        ui.getElement().appendChild(element);

        TestUtil.isGarbageCollected(ref);

        Assert.assertFalse(element.hasAttribute("foo"));

        Assert.assertNull(element.getAttribute("foo"));
    }

    @Test
    public void setResourceAttribute_attachElement_setAnotherResourceAfterAttaching()
            throws URISyntaxException, InterruptedException {
        UI ui = createUI();

        StreamResource resource = createEmptyResource("resource1");
        Element element = ElementFactory.createDiv();
        element.setAttribute("foo", resource);

        WeakReference<StreamResource> ref = new WeakReference<StreamResource>(
                resource);
        resource = null;

        ui.getElement().appendChild(element);

        String resName = "resource2";
        element.setAttribute("foo", createEmptyResource(resName));

        Assert.assertTrue(element.hasAttribute("foo"));

        TestUtil.isGarbageCollected(ref);

        Assert.assertNull(ref.get());

        String uri = element.getAttribute("foo");
        Optional<StreamResource> res = ui.getSession().getResourceRegistry()
                .getResource(new URI(uri));
        Assert.assertTrue(res.isPresent());
        Assert.assertTrue(uri.endsWith(resName));
    }

    @Test
    public void setResourceAttribute_attachElement_setRawAttributeAfterAttaching()
            throws URISyntaxException, InterruptedException {
        UI ui = createUI();

        StreamResource resource = createEmptyResource("resource");
        Element element = ElementFactory.createDiv();
        element.setAttribute("foo", resource);

        WeakReference<StreamResource> ref = new WeakReference<StreamResource>(
                resource);
        resource = null;

        ui.getElement().appendChild(element);

        element.setAttribute("foo", "bar");

        TestUtil.isGarbageCollected(ref);

        Assert.assertNull(ref.get());

        Assert.assertTrue(element.hasAttribute("foo"));

        Assert.assertEquals("bar", element.getAttribute("foo"));
    }

    @Test
    public void setResourceAttribute_attachElement_removeAttributeAfterAttaching()
            throws URISyntaxException, InterruptedException {
        UI ui = createUI();

        StreamResource resource = createEmptyResource("resource");
        Element element = ElementFactory.createDiv();
        element.setAttribute("foo", resource);

        WeakReference<StreamResource> ref = new WeakReference<StreamResource>(
                resource);
        resource = null;

        ui.getElement().appendChild(element);

        element.removeAttribute("foo");

        TestUtil.isGarbageCollected(ref);

        Assert.assertNull(ref.get());

        Assert.assertFalse(element.hasAttribute("foo"));

        Assert.assertNull(element.getAttribute("foo"));
    }

    @Test
    public void setResourceAttribute_detachElement_resourceIsUnregistered()
            throws URISyntaxException, InterruptedException {
        UI ui = createUI();
        Element element = ElementFactory.createDiv();
        ui.getElement().appendChild(element);

        String resName = "resource";
        StreamResource resource = createEmptyResource(resName);
        element.setAttribute("foo", resource);
        String attribute = element.getAttribute("foo");

        WeakReference<StreamResource> ref = new WeakReference<StreamResource>(
                resource);
        resource = null;

        URI uri = new URI(attribute);
        Optional<StreamResource> res = ui.getSession().getResourceRegistry()
                .getResource(uri);
        Assert.assertTrue(res.isPresent());

        ui.getElement().removeAllChildren();

        res = ui.getSession().getResourceRegistry().getResource(uri);
        Assert.assertFalse(res.isPresent());

        Assert.assertTrue(element.hasAttribute("foo"));
        Assert.assertNotNull(element.getAttribute("foo"));
        Assert.assertTrue(element.getAttribute("foo").endsWith(resName));

        element.setAttribute("foo", "bar");
        Assert.assertTrue(element.hasAttribute("foo"));
        Assert.assertEquals("bar", element.getAttribute("foo"));

        TestUtil.isGarbageCollected(ref);
    }

    @Test
    public void setResourceAttribute_detachAndReattachElement_resourceReregistered()
            throws URISyntaxException {
        UI ui = createUI();
        Element element = ElementFactory.createDiv();
        ui.getElement().appendChild(element);

        String resName = "resource";
        StreamResource resource = createEmptyResource(resName);
        element.setAttribute("foo", resource);
        String attribute = element.getAttribute("foo");

        URI uri = new URI(attribute);
        Optional<StreamResource> res = ui.getSession().getResourceRegistry()
                .getResource(uri);
        Assert.assertTrue(res.isPresent());

        ui.getElement().removeAllChildren();

        res = ui.getSession().getResourceRegistry().getResource(uri);
        Assert.assertFalse(res.isPresent());

        ui.getElement().appendChild(element);

        res = ui.getSession().getResourceRegistry().getResource(uri);
        Assert.assertTrue(res.isPresent());
    }

    @Test
    public void setResourceAttribute_attachAndDetachAndReattachElement_resourceReregistered()
            throws URISyntaxException {
        UI ui = createUI();
        Element element = ElementFactory.createDiv();

        String resName = "resource";
        StreamResource resource = createEmptyResource(resName);
        element.setAttribute("foo", resource);
        String attribute = element.getAttribute("foo");

        ui.getElement().appendChild(element);

        URI uri = new URI(attribute);
        Optional<StreamResource> res = ui.getSession().getResourceRegistry()
                .getResource(uri);
        Assert.assertTrue(res.isPresent());

        ui.getElement().removeAllChildren();

        res = ui.getSession().getResourceRegistry().getResource(uri);
        Assert.assertFalse(res.isPresent());

        ui.getElement().appendChild(element);

        res = ui.getSession().getResourceRegistry().getResource(uri);
        Assert.assertTrue(res.isPresent());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void setResourceAttribute_elementIsText_operationIsNotSupported() {
        Element.createText("").setAttribute("foo",
                EasyMock.createMock(StreamResource.class));
    }

    @Test
    public void testAttachListener_parentAttach_childListenersTriggered() {
        Element body = new UI().getElement();
        Element parent = ElementFactory.createDiv();
        Element child = ElementFactory.createDiv();
        Element grandChild = ElementFactory.createDiv();

        AtomicInteger childTriggered = new AtomicInteger();
        AtomicInteger grandChildTriggered = new AtomicInteger();

        EventRegistrationHandle registrationHandle = child
                .addAttachListener(event -> {
                    childTriggered.addAndGet(1);
                });
        child.addAttachListener(event -> {
            Assert.assertEquals(child, event.getSource());
        });
        grandChild.addAttachListener(event -> {
            grandChildTriggered.addAndGet(1);
        });
        grandChild.addAttachListener(event -> {
            Assert.assertEquals(grandChild, event.getSource());
        });

        parent.appendChild(child);
        child.appendChild(grandChild);

        Assert.assertEquals(childTriggered.get(), 0);
        Assert.assertEquals(grandChildTriggered.get(), 0);

        body.appendChild(parent);

        Assert.assertEquals(childTriggered.get(), 1);
        Assert.assertEquals(grandChildTriggered.get(), 1);

        body.removeAllChildren();
        parent.removeAllChildren();

        body.appendChild(parent);
        parent.appendChild(child);

        Assert.assertEquals(childTriggered.get(), 2);
        Assert.assertEquals(grandChildTriggered.get(), 2);

        registrationHandle.remove();

        body.removeAllChildren();
        body.appendChild(child);

        Assert.assertEquals(childTriggered.get(), 2);
        Assert.assertEquals(grandChildTriggered.get(), 3);
    }

    @Test
    public void testDetachListener_parentDetach_childListenersTriggered() {
        Element body = new UI().getElement();
        Element parent = ElementFactory.createDiv();
        Element child = ElementFactory.createDiv();
        Element grandChild = ElementFactory.createDiv();

        AtomicInteger triggered = new AtomicInteger();

        EventRegistrationHandle registrationHandle = child
                .addDetachListener(event -> {
                    triggered.addAndGet(1);
                    Assert.assertEquals(child, event.getSource());
                });

        grandChild.addDetachListener(event -> {
            triggered.addAndGet(1);
            Assert.assertEquals(grandChild, event.getSource());
        });

        child.appendChild(grandChild);
        parent.appendChild(child);
        body.appendChild(parent);

        Assert.assertEquals(triggered.get(), 0);

        body.removeAllChildren();
        Assert.assertEquals(triggered.get(), 2);

        body.appendChild(parent);
        body.removeAllChildren();

        Assert.assertEquals(triggered.get(), 4);

        body.appendChild(parent);
        registrationHandle.remove();

        body.removeAllChildren();

        Assert.assertEquals(triggered.get(), 5);
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
            Assert.assertFalse(parentAttached.get());
        });
        parent.addAttachListener(event -> {
            parentAttached.set(true);
            Assert.assertTrue(childAttached.get());
        });

        body.appendChild(parent);

        Assert.assertTrue(parentAttached.get());
        Assert.assertTrue(childAttached.get());
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
            Assert.assertFalse(parentDetached.get());
        });
        parent.addDetachListener(event -> {
            parentDetached.set(true);
            Assert.assertTrue(childDetached.get());
        });

        body.removeAllChildren();

        Assert.assertTrue(parentDetached.get());
        Assert.assertTrue(childDetached.get());
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
            Assert.assertTrue(detached.get());
        });
        child.addDetachListener(event -> {
            detached.set(true);
            Assert.assertFalse(attached.get());
        });

        body.appendChild(child);

        Assert.assertTrue(attached.get());
        Assert.assertTrue(detached.get());
    }

    @Test
    public void testAttachEvent_stateTreeCanFound() {
        Element body = new UI().getElement();
        Element child = ElementFactory.createDiv();

        AtomicInteger attached = new AtomicInteger();

        child.addAttachListener(event -> {
            Assert.assertNotNull(event.getSource().getNode().getOwner());
            Assert.assertNotEquals(NullOwner.get(),
                    event.getSource().getNode().getOwner());
        });
        child.addAttachListener(event -> attached.incrementAndGet());

        body.appendChild(child);
        Assert.assertEquals(1, attached.get());
    }

    @Test
    public void testDetachEvent_stateTreeCanFound() {
        Element body = new UI().getElement();
        Element child = ElementFactory.createDiv();
        body.appendChild(child);

        AtomicInteger detached = new AtomicInteger();

        child.addDetachListener(event -> {
            Assert.assertNotNull(event.getSource().getNode().getOwner());
            Assert.assertNotEquals(NullOwner.get(),
                    event.getSource().getNode().getOwner());
        });
        child.addDetachListener(event -> detached.incrementAndGet());

        body.removeAllChildren();

        Assert.assertEquals(1, detached.get());
    }

    private StreamResource createEmptyResource(String resName) {
        return new StreamResource(resName,
                () -> new ByteArrayInputStream(new byte[0]));
    }

    private UI createUI() {
        VaadinSession session = new VaadinSession(
                EasyMock.createMock(VaadinService.class)) {
            @Override
            public boolean hasLock() {
                return true;
            }
        };
        UI ui = new UI() {
            @Override
            public VaadinSession getSession() {
                return session;
            }
        };
        return ui;
    }

    @Test
    public void appendFirstChildToOwnParent() {
        Element parent = ElementFactory.createDiv();
        Element child1 = ElementFactory.createDiv();
        Element child2 = ElementFactory.createDiv();
        parent.appendChild(child1, child2);

        parent.appendChild(child1);
        assertChildren(parent, child2, child1);
    }

    @Test
    public void appendLastChildToOwnParent() {
        Element parent = ElementFactory.createDiv();
        Element child1 = ElementFactory.createDiv();
        Element child2 = ElementFactory.createDiv();
        parent.appendChild(child1, child2);

        parent.appendChild(child2);
        assertChildren(parent, child1, child2);
    }

    @Test
    public void appendManyChildrenToOwnParent() {
        Element parent = ElementFactory.createDiv();
        Element child1 = ElementFactory.createDiv();
        Element child2 = ElementFactory.createDiv();
        parent.appendChild(child1, child2);

        parent.appendChild(child2, child1);
        // Order should be changed
        assertChildren(parent, child2, child1);
    }

    @Test
    public void appendExistingAndNewChildren() {
        Element parent = ElementFactory.createDiv();
        Element child1 = ElementFactory.createDiv();
        Element child2 = ElementFactory.createDiv();
        parent.appendChild(child1);

        parent.appendChild(child2, child1);

        assertChildren(parent, child2, child1);
    }

    @Test
    public void insertAtCurrentPositionNoOp() {
        // Must have an UI to get attach events
        UI ui = new UI();
        Element parent = ui.getElement();
        Element child = ElementFactory.createDiv();

        parent.appendChild(child);

        child.addDetachListener(
                e -> Assert.fail("Child should not be detached"));
        parent.insertChild(0, child);
    }

}
