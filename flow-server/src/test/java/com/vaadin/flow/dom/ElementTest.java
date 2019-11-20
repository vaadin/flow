package com.vaadin.flow.dom;

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
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.component.internal.UIInternals.JavaScriptInvocation;
import com.vaadin.flow.dom.impl.BasicElementStateProvider;
import com.vaadin.flow.internal.NullOwner;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.change.ListAddChange;
import com.vaadin.flow.internal.nodefeature.ComponentMapping;
import com.vaadin.flow.internal.nodefeature.ElementAttributeMap;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;
import com.vaadin.flow.internal.nodefeature.ElementListenersTest;
import com.vaadin.flow.internal.nodefeature.ElementPropertyMap;
import com.vaadin.flow.internal.nodefeature.ElementStylePropertyMap;
import com.vaadin.flow.internal.nodefeature.SynchronizedPropertiesList;
import com.vaadin.flow.internal.nodefeature.SynchronizedPropertyEventsList;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.JsonConstants;
import com.vaadin.flow.shared.Registration;
import com.vaadin.tests.util.AlwaysLockedVaadinSession;
import com.vaadin.tests.util.MockUI;
import com.vaadin.tests.util.TestUtil;
import net.jcip.annotations.NotThreadSafe;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import elemental.json.Json;
import elemental.json.JsonValue;
import elemental.json.impl.JreJsonObject;

@NotThreadSafe
public class ElementTest extends AbstractNodeTest {

    @Test
    public void createElementWithTag() {
        Element e = ElementFactory.createDiv();
        Assert.assertEquals(Tag.DIV, e.getTag());
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
        StateNode node = new StateNode(ElementPropertyMap.class);
        Element.get(node);
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

    @Test
    public void setBooleanAttribute() {
        Element e = ElementFactory.createDiv();

        e.setAttribute("foo", true);
        Assert.assertEquals("", e.getAttribute("foo"));
        Assert.assertTrue(e.hasAttribute("foo"));

        e.setAttribute("foo", false);
        Assert.assertEquals(null, e.getAttribute("foo"));
        Assert.assertFalse(e.hasAttribute("foo"));
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
        Element e = new Element(Tag.SPAN);
        e.setAttribute("foo", "bAr");
        Assert.assertEquals("bAr", e.getAttribute("foo"));
        e.setAttribute("foo", "BAR");
        Assert.assertEquals("BAR", e.getAttribute("foo"));
    }

    @Test
    public void setGetAttributeNameCaseInsensitive() {
        Element e = new Element(Tag.SPAN);
        e.setAttribute("foo", "bar");
        e.setAttribute("FOO", "baz");

        Assert.assertEquals("baz", e.getAttribute("foo"));
        Assert.assertEquals("baz", e.getAttribute("FOO"));
    }

    @Test
    public void hasAttributeNamesCaseInsensitive() {
        Element e = new Element(Tag.SPAN);
        e.setAttribute("fooo", "bar");
        Assert.assertTrue(e.hasAttribute("fOoO"));
    }

    @Test
    public void getAttributeNamesLowerCase() {
        Element e = new Element(Tag.SPAN);
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
    public void removeDetachedFromParent() {
        Element otherElement = new Element("other");
        Assert.assertNull(otherElement.getParent());
        otherElement.removeFromParent(); // No op
        Assert.assertNull(otherElement.getParent());
    }

    @Test
    public void getDetachedParent() {
        Element otherElement = new Element("other");
        Assert.assertNull(otherElement.getParent());
        Assert.assertNull(otherElement.getParentNode());
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
        Assert.assertFalse(e.equals(Tag.DIV));
    }

    @Test
    public void listenerReceivesEvents() {
        Element e = ElementFactory.createDiv();
        AtomicInteger listenerCalls = new AtomicInteger(0);
        DomEventListener myListener = event -> listenerCalls.incrementAndGet();

        e.addEventListener("click", myListener);
        Assert.assertEquals(0, listenerCalls.get());
        e.getNode().getFeature(ElementListenerMap.class)
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

        if (value instanceof JsonValue) {
            element.setPropertyJson("property", (JsonValue) value);
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

        Assert.assertEquals("foobar", element.getTextRecursively());
    }

    @Test
    public void testSetTextContent() {
        Element element = ElementFactory.createDiv();
        element.setText("foo");

        Assert.assertEquals("foo", element.getTextRecursively());
        Assert.assertEquals(1, element.getChildCount());
        Assert.assertTrue(element.getChild(0).isTextNode());
    }

    @Test
    public void testSetTextContentRemovesOldContent() {
        Element child = new Element("child");
        Element element = ElementFactory.createDiv();
        element.appendChild(child);

        element.setText("foo");

        Assert.assertNull(child.getParent());
        Assert.assertEquals("foo", element.getTextRecursively());
    }

    @Test
    public void testSetTextReplacesOldTextNode() {
        Element element = ElementFactory.createDiv();
        Element text = Element.createText("foo");
        element.appendChild(text);

        element.setText("bar");

        Assert.assertEquals(element, text.getParent());
        Assert.assertEquals("bar", text.getTextRecursively());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetTextContentPropertyThrows() {
        Element element = new Element("element");
        element.setProperty("textContent", "foo");
    }

    @Test(expected = IllegalArgumentException.class)
    public void setOuterHtmlProperty_throws() {
        Element element = new Element("element");
        element.setProperty("outerHTML", "<br>");
    }

    @Test
    public void setInnerHtmlProeprty_setValueAndRemoveAllChildren() {
        Element element = new Element("element");
        element.appendChild(ElementFactory.createAnchor(),
                ElementFactory.createDiv());
        element.setProperty("innerHTML", "<br>");

        Assert.assertEquals(0, element.getChildCount());
        Assert.assertEquals("<br>", element.getProperty("innerHTML"));
    }

    @Test
    public void testGetTextContentProperty() {
        Element element = ElementFactory.createDiv();
        element.setText("foo");

        Assert.assertFalse(element.hasProperty("textContent"));
        Assert.assertNull(element.getProperty("textContent"));
    }

    @Test
    // Because that's how it works in browsers
    public void clearTextContentRemovesChild() {
        Element element = ElementFactory.createDiv();
        element.setText("foo");

        Assert.assertEquals(1, element.getChildCount());

        element.setText("");

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
                element.getNode().getFeature(ElementAttributeMap.class)
                        .get("class"));
    }

    @Test
    public void testSetEmptyClassAttribute() {
        Element element = new Element(Tag.DIV);

        // Get instance right away to see that changes are live
        Set<String> classList = element.getClassList();

        element.setAttribute("class", "");

        Assert.assertEquals(0, classList.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddEmptyClassname() {
        Element element = new Element(Tag.DIV);

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
        Element e = new Element(Tag.DIV);
        Assert.assertTrue(e.getClassList().set("foo", true));
        Assert.assertEquals("foo", e.getAttribute("class"));
        Assert.assertFalse(e.getClassList().set("foo", true));
        Assert.assertEquals("foo", e.getAttribute("class"));
    }

    @Test
    public void classListSetRemove() {
        Element e = new Element(Tag.DIV);
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
    public void getSingleStyleAsAttribute() {
        Element e = ElementFactory.createDiv();
        Style s = e.getStyle();
        s.set("border", "1px solid black");
        Assert.assertTrue(e.hasAttribute("style"));
        Assert.assertEquals("border:1px solid black", e.getAttribute("style"));
    }

    @Test
    public void getMultipleStylesAsAttribute() {
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

    @Test
    public void setSingleStyleAsAttribute() {
        Element e = ElementFactory.createDiv();
        String style = "width:12em";
        e.setAttribute("style", style);
        Assert.assertEquals(style, e.getAttribute("style"));

    }

    @Test
    public void setStyleAttributeMultipleTimes() {
        Element e = ElementFactory.createDiv();
        e.setAttribute("style", "width:12em");
        e.setAttribute("style", "height:12em");
        Assert.assertEquals("height:12em", e.getAttribute("style"));

    }

    @Test
    public void setMultipleStylesAsAttribute() {
        Element e = ElementFactory.createDiv();
        String style = "width:12em;height:2em";
        e.setAttribute("style", style);
        Assert.assertEquals(style, e.getAttribute("style"));

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
        // Reduced calc does not work (http://cssnext.io/features/#reduced-calc)
        // testStyleAttribute("font-size:calc(var(--fontSize) * 2)");
    }

    private void testStyleAttribute(String style) {
        Element e = ElementFactory.createDiv();
        e.setAttribute("style", style);
        Assert.assertEquals(style, e.getAttribute("style"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void setInvalidStyleAsAttribute() {
        Element e = ElementFactory.createDiv();
        e.setAttribute("style", "width:");
    }

    @Test(expected = IllegalArgumentException.class)
    public void setInvalidStyleAsAttribute2() {
        Element e = ElementFactory.createDiv();
        e.setAttribute("style", "width");
    }

    @Test
    public void setVendorSpecificStylesProperty() {
        Element e = ElementFactory.createDiv();
        String style = "-moz-user-input:inherit";
        e.setAttribute("style", style);
        Assert.assertEquals("inherit", e.getStyle().get("mozUserInput"));
        Assert.assertEquals(style, e.getAttribute("style"));
    }

    @Test
    public void setVendorSpecificStylesValue() {
        Element e = ElementFactory.createDiv();
        String style = "display:-moz-box";
        e.setAttribute("style", style);
        Assert.assertEquals("-moz-box", e.getStyle().get("display"));
        Assert.assertEquals(style, e.getAttribute("style"));

    }

    @Test
    public void setStyleAttributeTrailingSemicolon() {
        Element e = ElementFactory.createDiv();
        String style = "width:12em";
        e.setAttribute("style", style + ";");
        Assert.assertEquals(style, e.getAttribute("style"));
    }

    private void assertEqualsOne(String[] expected, String actual) {
        for (String string : expected) {
            if (string.equals(actual)) {
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

    @Test(expected = IllegalArgumentException.class)
    public void setStyleNameColon() {
        Element e = ElementFactory.createDiv();
        e.getStyle().set("color:", "red");
    }

    @Test
    public void setStyleValueExtraWhitespace() {
        Element e = ElementFactory.createDiv();
        e.getStyle().set("color", "red   ");
        Assert.assertEquals("color:red", e.getAttribute("style"));
        Assert.assertEquals("red", e.getStyle().get("color"));
    }

    @Test
    public void removeStyles() {
        Element element = ElementFactory.createDiv();

        element.getStyle().set("zIndex", "12");
        element.getStyle().set("background", "blue");

        element.getStyle().remove("background");

        Assert.assertEquals("z-index:12", element.getAttribute("style"));

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

    @Test
    public void dashSeparatedSetStyle() {
        Element element = ElementFactory.createDiv();

        Style style = element.getStyle();
        style.set("border-color", "blue");
        Assert.assertEquals("blue", style.get("border-color"));
    }

    @Test
    public void dashSeparatedGetStyle() {
        Element element = ElementFactory.createDiv();

        Style style = element.getStyle();
        style.set("borderColor", "blue");
        style.set("border-foo", "bar");
        Assert.assertEquals("blue", style.get("border-color"));
        Assert.assertEquals("bar", style.get("border-foo"));
    }

    @Test
    public void dashSeparatedHasStyle() {
        Element element = ElementFactory.createDiv();

        Style style = element.getStyle();
        style.set("borderColor", "blue");
        style.set("border-foo", "bar");
        Assert.assertTrue(style.has("border-color"));
        Assert.assertTrue(style.has("border-foo"));
    }

    @Test
    public void dashSeparatedRemoveStyle() {
        Element element = ElementFactory.createDiv();

        Style style = element.getStyle();
        style.set("borderColor", "blue");
        style.set("border-foo", "bar");
        style.remove("border-color");
        style.remove("border-foo");

        Assert.assertFalse(style.has("border-color"));
        Assert.assertFalse(style.has("border-foo"));
    }

    @Test
    public void styleGetNamesDashAndCamelCase() {
        Element element = ElementFactory.createDiv();

        Style style = element.getStyle();
        style.set("borderColor", "blue");
        style.set("border-foo", "bar");

        List<String> styles = style.getNames().collect(Collectors.toList());
        Assert.assertEquals(2, styles.size());
        Assert.assertTrue(styles.contains("border-color"));
        Assert.assertTrue(styles.contains("border-foo"));
    }

    @Test
    public void nullStyleValue() {
        Element element = ElementFactory.createDiv();

        Style style = element.getStyle();
        style.set("borderColor", "blue");
        style.set("borderColor", null);
        List<String> styles = style.getNames().collect(Collectors.toList());
        Assert.assertFalse(styles.contains("borderColor"));
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
        Assert.assertEquals("foo", style.get(setUsingStyleApi));
        Assert.assertEquals(sentToClient, map.getPropertyNames().toArray()[0]);
        Assert.assertEquals("foo", map.getProperty(sentToClient));

    }

    @Test
    public void customPropertyStyle() {
        Element element = ElementFactory.createDiv();
        Style style = element.getStyle();
        style.set("--some-variable", "foo");
        Assert.assertEquals("foo", style.get("--some-variable"));
    }

    @Test
    public void useCustomPropertyStyle() {
        Element element = ElementFactory.createDiv();

        Style style = element.getStyle();
        style.set("color", "var(--some-var)");
        Assert.assertEquals("var(--some-var)", style.get("color"));
    }

    @Test
    public void listenersFiredInRegisteredOrder() {
        Element element = ElementFactory.createDiv();
        List<Integer> eventOrder = new ArrayList<>();

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
        element.getNode().getFeature(ElementListenerMap.class).fireEvent(
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
        e.getNode().getFeature(SynchronizedPropertiesList.class)
                .collectChanges(change -> i.addAndGet(
                        ((ListAddChange<?>) change).getNewItems().size()));
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
        e.getNode().getFeature(SynchronizedPropertyEventsList.class)
                .collectChanges(change -> i.addAndGet(
                        ((ListAddChange<?>) change).getNewItems().size()));
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

    @Override
    protected void checkIsNotChild(Node<?> parent, Element child) {
        Assert.assertNotEquals(child.getParent(), parent);

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

        Assert.assertEquals("fooAnother text node", element.getText());
        Assert.assertEquals("foospan contentsstrong contentsAnother text node",
                element.getTextRecursively());
    }

    @Test
    public void setResourceAttribute_elementIsNotAttached_elementHasAttribute() {
        UI.setCurrent(createUI());
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
        UI.setCurrent(createUI());
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
        UI.setCurrent(ui);
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
        UI.setCurrent(ui);
        StreamResource resource = createEmptyResource("resource1");
        ui.getElement().setAttribute("foo", resource);

        String uri = ui.getElement().getAttribute("foo");
        Optional<StreamResource> res = ui.getSession().getResourceRegistry()
                .getResource(StreamResource.class, new URI(uri));
        Assert.assertTrue(res.isPresent());

        String resName = "resource2";
        ui.getElement().setAttribute("foo", createEmptyResource(resName));
        res = ui.getSession().getResourceRegistry()
                .getResource(StreamResource.class, new URI(uri));
        Assert.assertFalse(res.isPresent());

        Assert.assertTrue(ui.getElement().hasAttribute("foo"));
        Assert.assertTrue(
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
        Assert.assertTrue(res.isPresent());
        res = null;

        WeakReference<StreamResource> ref = new WeakReference<>(resource);
        resource = null;

        ui.getElement().setAttribute("foo", "bar");

        TestUtil.isGarbageCollected(ref);
        res = ui.getSession().getResourceRegistry()
                .getResource(StreamResource.class, new URI(uri));

        Assert.assertFalse(res.isPresent());
        Assert.assertTrue(ui.getElement().hasAttribute("foo"));
        Assert.assertTrue(ui.getElement().getAttribute("foo").equals("bar"));
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
        Assert.assertTrue(res.isPresent());
        res = null;

        WeakReference<StreamResource> ref = new WeakReference<>(resource);
        resource = null;

        ui.getElement().removeAttribute("foo");
        TestUtil.isGarbageCollected(ref);

        res = ui.getSession().getResourceRegistry()
                .getResource(StreamResource.class, new URI(uri));
        Assert.assertFalse(res.isPresent());
        Assert.assertFalse(ui.getElement().hasAttribute("foo"));
        Assert.assertNull(ui.getElement().getAttribute("foo"));
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

        Assert.assertTrue(element.hasAttribute("foo"));

        String uri = element.getAttribute("foo");
        Optional<StreamResource> res = ui.getSession().getResourceRegistry()
                .getResource(StreamResource.class, new URI(uri));
        Assert.assertTrue(res.isPresent());
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

        Assert.assertTrue(element.hasAttribute("foo"));

        String uri = element.getAttribute("foo");
        Optional<StreamResource> res = ui.getSession().getResourceRegistry()
                .getResource(StreamResource.class, new URI(uri));
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
        UI.setCurrent(ui);

        StreamResource resource = createEmptyResource("resource");
        Element element = ElementFactory.createDiv();
        element.setAttribute("foo", resource);

        WeakReference<StreamResource> ref = new WeakReference<>(resource);
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
        UI.setCurrent(ui);

        StreamResource resource = createEmptyResource("resource");
        Element element = ElementFactory.createDiv();
        element.setAttribute("foo", resource);

        WeakReference<StreamResource> ref = new WeakReference<>(resource);
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
        UI.setCurrent(ui);

        StreamResource resource = createEmptyResource("resource1");
        Element element = ElementFactory.createDiv();
        element.setAttribute("foo", resource);

        WeakReference<StreamResource> ref = new WeakReference<>(resource);
        resource = null;

        ui.getElement().appendChild(element);

        String resName = "resource2";
        element.setAttribute("foo", createEmptyResource(resName));

        Assert.assertTrue(element.hasAttribute("foo"));

        TestUtil.isGarbageCollected(ref);

        Assert.assertNull(ref.get());

        String uri = element.getAttribute("foo");
        Optional<StreamResource> res = ui.getSession().getResourceRegistry()
                .getResource(StreamResource.class, new URI(uri));
        Assert.assertTrue(res.isPresent());
        Assert.assertTrue(uri.endsWith(resName));
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

        Assert.assertNull(ref.get());

        Assert.assertTrue(element.hasAttribute("foo"));

        Assert.assertEquals("bar", element.getAttribute("foo"));
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

        Assert.assertNull(ref.get());

        Assert.assertFalse(element.hasAttribute("foo"));

        Assert.assertNull(element.getAttribute("foo"));
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
        Assert.assertTrue(res.isPresent());

        ui.getElement().removeAllChildren();

        res = ui.getSession().getResourceRegistry()
                .getResource(StreamResource.class, uri);
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
        Assert.assertTrue(res.isPresent());

        ui.getElement().removeAllChildren();

        res = ui.getSession().getResourceRegistry()
                .getResource(StreamResource.class, uri);
        Assert.assertFalse(res.isPresent());

        ui.getElement().appendChild(element);

        res = ui.getSession().getResourceRegistry()
                .getResource(StreamResource.class, uri);
        Assert.assertTrue(res.isPresent());
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
        Assert.assertTrue(res.isPresent());

        ui.getElement().removeAllChildren();

        res = ui.getSession().getResourceRegistry()
                .getResource(StreamResource.class, uri);
        Assert.assertFalse(res.isPresent());

        ui.getElement().appendChild(element);

        res = ui.getSession().getResourceRegistry()
                .getResource(StreamResource.class, uri);
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

        Registration registrationHandle = child.addAttachListener(event -> {
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

        Registration registrationHandle = child.addDetachListener(event -> {
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

    @Test
    public void testRemoveFromTree_inDetachListener_removedFromParent() {
        Element body = new UI().getElement();
        Element child = ElementFactory.createDiv();
        body.appendChild(child);

        child.addDetachListener(event -> child.removeFromTree());

        body.removeAllChildren();

        Assert.assertEquals(null, child.getParent());
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
                e -> Assert.fail("Child should not be detached"));
        parent.insertChild(0, child);
    }

    @Test
    public void textNodeOuterHtml() {
        Element e = Element.createText("foobar");
        Assert.assertEquals("foobar", e.getOuterHTML());
    }

    @Test
    public void singleElementOuterHtml() {
        Element e = ElementFactory.createAnchor();
        Assert.assertEquals("<a></a>", e.getOuterHTML());
    }

    @Test
    public void elementTreeOuterHtml() {
        Element div = ElementFactory.createDiv();
        Element span = ElementFactory.createSpan();
        Element button = ElementFactory.createButton("hello");

        div.appendChild(span);
        span.appendChild(button);

        Assert.assertEquals(
                "<div>\n" + " <span><button>hello</button></span>\n" + "</div>",
                div.getOuterHTML());
    }

    @Test
    public void elementAttributesOuterHtml() {
        Element div = ElementFactory.createDiv();
        div.setAttribute("foo", "bar");
        div.getStyle().set("width", "20px");
        div.getClassList().add("cls");
        div.setAttribute("pin", "");

        Assert.assertEquals(
                "<div pin foo=\"bar\" style=\"width:20px\" class=\"cls\"></div>",
                div.getOuterHTML());
    }

    @Test
    public void elementAttributeSpecialCharactersOuterHtml() {
        Element div = ElementFactory.createDiv();
        div.setAttribute("foo", "bar\"'&quot;");

        Assert.assertEquals("<div foo=\"bar&quot;'&amp;quot;\"></div>",
                div.getOuterHTML());
    }

    @Test
    public void htmlComponentOuterHtml() {
        Html html = new Html(
                "<div style='background:green'><span><button>hello</button></span></div>");
        Assert.assertEquals(
                "<div style=\"background:green\">\n"
                        + " <span><button>hello</button></span>\n" + "</div>",
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
        Assert.assertTrue(invocations.isEmpty());
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
        Assert.assertNotNull(shadow);
        Assert.assertEquals(element, shadow.getHost());
        Assert.assertEquals(shadow, element.getShadowRoot().get());
        Assert.assertEquals(2, element.getChildCount());
        Assert.assertEquals(2, element.getChildren().count());
        Assert.assertEquals(button, element.getChild(0));
        Assert.assertEquals(emphasis, element.getChild(1));
    }

    @Test
    public void getShadowRoot_shadowRootIsEmpty() {
        Element element = ElementFactory.createDiv();
        Assert.assertTrue(!element.getShadowRoot().isPresent());
    }

    @Test
    public void getParentNode_parentNodeIsTheSameAsParent() {
        Element element = ElementFactory.createDiv();
        Element child = ElementFactory.createDiv();

        element.appendChild(child);

        Assert.assertEquals(child.getParent(), child.getParentNode());
    }

    @Test
    public void getParentNode_elementInShadowRoot_parentIsNull() {
        ShadowRoot element = ElementFactory.createDiv().attachShadow();
        Element child = ElementFactory.createDiv();

        element.appendChild(child);

        Assert.assertNull(child.getParent());
        Assert.assertEquals(element, child.getParentNode());
    }

    @Test
    public void parentIsDisabled_childIsDisabled() {
        Element parent = ElementFactory.createDiv();
        Element child = ElementFactory.createDiv();

        parent.appendChild(child);

        Assert.assertTrue("Parent should be enabled", parent.isEnabled());
        Assert.assertTrue("Child should be enabled", child.isEnabled());

        parent.setEnabled(false);

        Assert.assertFalse("Parent should be disabled", parent.isEnabled());
        Assert.assertFalse("Child should be disabled", child.isEnabled());

        child.removeFromParent();

        Assert.assertTrue("Child should be enabled", child.isEnabled());
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

        Assert.assertFalse(
                "getComponent() shouldn't initialize a component mapping feature",
                element.getNode()
                        .getFeatureIfInitialized(ComponentMapping.class)
                        .isPresent());
    }

    @Test
    public void readMissingProperty_noFeatureInitialized() {
        Element element = ElementFactory.createDiv();

        element.getProperty("foo");
        element.hasProperty("foo");
        element.removeProperty("foo");
        element.getPropertyNames().collect(Collectors.toList());

        Assert.assertFalse(
                "reading a property value shouldn't initialize a property map feature",
                element.getNode()
                        .getFeatureIfInitialized(ElementPropertyMap.class)
                        .isPresent());
    }

    @Test
    public void readMissingAttribute_noFeatureInitialized() {
        Element element = ElementFactory.createDiv();

        element.getAttribute("foo");
        element.hasAttribute("foo");
        element.removeAttribute("foo");
        element.getAttributeNames().collect(Collectors.toList());

        Assert.assertFalse(
                "reading an attribute value shouldn't initialize an attribute map feature",
                element.getNode()
                        .getFeatureIfInitialized(ElementAttributeMap.class)
                        .isPresent());
    }

    public void syncProperty_delegateTo3ArgsMethod() {
        Element element = Mockito.mock(Element.class);

        Mockito.doCallRealMethod().when(element)
                .synchronizeProperty(Mockito.anyString(), Mockito.anyString());
        element.synchronizeProperty("foo", "bar");

        Mockito.verify(element).synchronizeProperty("foo", "bar",
                DisabledUpdateMode.ONLY_WHEN_ENABLED);
    }

    @Test
    public void addSyncProperty_delegateTo2ArgsMethod() {
        Element element = Mockito.mock(Element.class);

        Mockito.doCallRealMethod().when(element)
                .addSynchronizedProperty(Mockito.anyString());
        element.addSynchronizedProperty("foo");

        Mockito.verify(element).addSynchronizedProperty("foo",
                DisabledUpdateMode.ONLY_WHEN_ENABLED);
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

        Assert.assertFalse(parent.isVirtualChild());
        Assert.assertFalse(child.isVirtualChild());
        Assert.assertTrue(virtualChild.isVirtualChild());
        Assert.assertFalse(grandVirtualChild.isVirtualChild());
    }

    @Test
    public void domPropertyListener_registersListenerAndDomTrigger() {
        Element element = ElementFactory.createDiv();

        AtomicReference<Serializable> listenerValue = new AtomicReference<>();

        element.addPropertyChangeListener("property", "event", event -> {
            if (listenerValue.getAndSet(event.getValue()) != null) {
                Assert.fail("Unexpected event");
            }
        });

        Assert.assertEquals("The property should be synchronized",
                DisabledUpdateMode.ONLY_WHEN_ENABLED,
                element.getNode().getFeature(ElementListenerMap.class)
                        .getPropertySynchronizationMode("property"));

        Assert.assertEquals(
                "There should not be any 'conventional' property sync events",
                0, element.getSynchronizedPropertyEvents().count());

        ElementListenerMap listenerMap = element.getNode()
                .getFeature(ElementListenerMap.class);

        Assert.assertEquals("A DOM event synchronization should be defined",
                Collections.singleton(
                        JsonConstants.SYNCHRONIZE_PROPERTY_TOKEN + "property"),
                ElementListenersTest.getExpressions(listenerMap, "event"));

        element.setProperty("property", "value");
        Assert.assertEquals("Listener shold be registered", listenerValue.get(),
                "value");
    }

    @Test
    public void domPropertyListener_unregisterCleansEverything() {
        Element element = ElementFactory.createDiv();

        DomListenerRegistration registration = element
                .addPropertyChangeListener("property", "event", event -> {
                    Assert.fail("Unexpected event");
                });
        registration.remove();

        Assert.assertNull("The property should not be synchronized",
                element.getNode().getFeature(ElementListenerMap.class)
                        .getPropertySynchronizationMode("property"));

        ElementListenerMap listenerMap = element.getNode()
                .getFeature(ElementListenerMap.class);

        Assert.assertEquals("There should be no DOM listener",
                Collections.emptySet(),
                ElementListenersTest.getExpressions(listenerMap, "event"));

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

        Assert.assertNull(child1.getParent());
        Assert.assertFalse(child1.isVirtualChild());

        Assert.assertNull(child2.getParent());
        Assert.assertFalse(child2.isVirtualChild());
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeVirtualChildren_notVirtualChild_fails() {
        Element parent = new Element("root");
        Element child1 = new Element("main");

        parent.appendChild(child1);

        parent.removeVirtualChild(child1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeFromParent_virtualChild_fails() {
        Element parent = new Element("root");
        Element child1 = new Element("main");

        parent.appendVirtualChild(child1);

        child1.removeFromParent();
    }

    @Override
    protected Element createParentNode() {
        return ElementFactory.createDiv();
    }

    @Override
    protected void assertChild(Node<?> parent, int index, Element child) {
        Assert.assertEquals(parent, child.getParent());
        Assert.assertEquals(child, parent.getChild(index));
    }

    private void assertPendingJs(UI ui, String js, Serializable... arguments) {
        List<PendingJavaScriptInvocation> pendingJs = ui.getInternals()
                .dumpPendingJavaScriptInvocations();
        JavaScriptInvocation expected = new JavaScriptInvocation(js, arguments);
        Assert.assertEquals(1, pendingJs.size());
        assertEquals(expected, pendingJs.get(0).getInvocation());

    }

    private void assertEquals(JavaScriptInvocation expected,
            JavaScriptInvocation actual) {
        Assert.assertEquals(expected.getExpression(), actual.getExpression());
        Assert.assertArrayEquals(expected.getParameters().toArray(),
                actual.getParameters().toArray());

    }
}
