package com.vaadin.hummingbird.kernel;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.EventObject;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;

import com.vaadin.annotations.EventParameter;
import com.vaadin.annotations.EventType;
import com.vaadin.event.EventListener;

import org.junit.Assert;
import org.junit.Test;

import elemental.json.Json;

public class ElementTest {
    @Test
    public void elementOuterHTML() {
        Element element = new Element("span");
        element.setAttribute("class", "foobar");
        element.setAttribute("nullValued", null);
        element.insertChild(0, new Element("strong"));

        String html = element.getOuterHTML();
        Assert.assertEquals("<span class=\"foobar\"><strong></strong></span>",
                html);
    }

    @Test
    public void publicSettersReturnElement() {
        Set<String> ignore = new HashSet<>();
        ignore.add("toString");
        ignore.add("hashCode");
        ignore.add("equals");
        ignore.add("debug");

        for (Method m : Element.class.getDeclaredMethods()) {
            if (!Modifier.isPublic(m.getModifiers())) {
                continue;
            }
            if (m.getName().startsWith("get") || m.getName().startsWith("has")
                    || ignore.contains(m.getName())) {
                // Ignore
                /*
                 * } else if (m.getName().startsWith("set") ||
                 * m.getName().startsWith("append") ||
                 * m.getName().startsWith("add") ||
                 * m.getName().startsWith("insert") ||
                 * m.getName().startsWith("remove")) {
                 */
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
    public void textFromNormalElement() {
        Element e = new Element("div");
        Assert.assertEquals("", e.getTextContent());
    }

    @Test
    public void textFromSingleElement() {
        Element e = new Element("div");
        e.setTextContent("Foobar");
        Assert.assertEquals("Foobar", e.getTextContent());
    }

    @Test
    public void textFromSiblingElements() {
        Element e = new Element("div");
        Element e1 = new Element("span");
        Element e2 = new Element("button");
        e.appendChild(e1);
        e.appendChild(e2);
        e1.setTextContent("Foo");
        e2.setTextContent("Bar");
        Assert.assertEquals("FooBar", e.getTextContent());
    }

    @Test
    public void textFromNestedElements() {
        Element e = new Element("div");
        Element e1 = new Element("span");
        e.appendChild(e1);
        e1.setTextContent("Hello");
        Element e2 = new Element("span");
        e.appendChild(e2);
        Element e21 = new Element("span");
        e2.appendChild(e21);
        e21.setTextContent(" World!");
        Assert.assertEquals("Hello World!", e.getTextContent());
    }

    @Test
    public void textNodeOuterHTML() {
        Element strong = new Element("strong");
        strong.setTextContent("world!");

        Element root = new Element("span");
        root.setTextContent("Hello ");
        root.insertChild(1, strong);

        Assert.assertEquals("<span>Hello <strong>world!</strong></span>",
                root.getOuterHTML());

    }

    @Test
    public void setSingleStyle() {
        Element e = new Element("div");
        e.setStyle("foo", "bar");
        assertElementEquals(parse("<div style='foo:bar'>"), e);
    }

    @Test
    public void setMultipleStyles() {
        Element e = new Element("div");
        e.setStyle("foo", "bar");
        e.setStyle("baz", "12px");
        assertElementEquals(parse("<div style='foo:bar;baz:12px'>"), e);
    }

    @Test
    public void removeAllStyles() {
        Element e = new Element("div");
        e.setAttribute("style", "foo:bar;baz:12px;width:100px");
        e.removeStyle("foo");
        e.removeStyle("baz");
        e.removeStyle("width");
        assertElementEquals(parse("<div>"), e);
    }

    @Test
    public void removeFirstStyle() {
        Element e = new Element("div");
        e.setAttribute("style", "foo:bar;baz:12px;width:100px");
        e.removeStyle("foo");
        assertElementEquals(parse("<div style='baz:12px;width:100px'>"), e);
    }

    @Test
    public void removeMiddleStyle() {
        Element e = new Element("div");
        e.setAttribute("style", "foo:bar;baz:12px;width:100px");
        e.removeStyle("baz");
        assertElementEquals(parse("<div style='foo:bar;width:100px'>"), e);
    }

    @Test
    public void removeLastStyle() {
        Element e = new Element("div");
        e.setAttribute("style", "foo:bar;baz:12px;width:100px");
        e.removeStyle("width");
        assertElementEquals(parse("<div style='foo:bar;baz:12px'>"), e);
    }

    @Test
    public void addSingleClass() {
        Element e = new Element("div");
        e.addClass("foo");
        assertElementEquals(parse("<div class='foo'>"), e);
    }

    @Test
    public void addMultipleClasses() {
        Element e = new Element("div");
        e.addClass("foo");
        e.addClass("bar");
        assertElementEquals(parse("<div class='foo bar'>"), e);
    }

    @Test
    public void removeFirstClass() {
        Element e = new Element("div");
        e.setAttribute("class", "foo bar baz");
        e.removeClass("foo");
        assertElementEquals(parse("<div class='bar baz'>"), e);
    }

    @Test
    public void removeMiddleClass() {
        Element e = new Element("div");
        e.setAttribute("class", "foo bar baz");
        e.removeClass("bar");
        assertElementEquals(parse("<div class='foo baz'>"), e);
    }

    @Test
    public void removeLastClass() {
        Element e = new Element("div");
        e.setAttribute("class", "foo bar baz");
        e.removeClass("baz");
        assertElementEquals(parse("<div class='foo bar'>"), e);
    }

    @Test
    public void removeAllClasses() {
        Element e = new Element("div");
        e.setAttribute("class", "foo bar baz");
        e.removeClass("baz");
        e.removeClass("bar");
        e.removeClass("foo");
        assertElementEquals(parse("<div>"), e);
    }

    public static void assertElementEquals(Element expected, Element actual) {
        Assert.assertEquals(expected.getTag(), actual.getTag());

        for (String name : expected.getAttributeNames()) {
            Assert.assertTrue("Attribute " + name + " does not exist",
                    actual.hasAttribute(name));
            Assert.assertEquals(expected.getAttribute(name),
                    actual.getAttribute(name));
        }
        if (expected.getAttributeNames().size() != actual.getAttributeNames()
                .size()) {

            String err = "Unexpected attributes found in element: ";
            for (String name : actual.getAttributeNames()) {
                if (!expected.hasAttribute(name)) {
                    err += name + "=" + actual.getAttribute(name);
                }
            }
            Assert.fail(err);

        }

    }

    public static Element parse(String html) {
        Document d = Jsoup.parse(html);
        org.jsoup.nodes.Element e = d.body().child(0);
        return jsoupToElement(e);
    }

    private static Element jsoupToElement(org.jsoup.nodes.Element model) {
        Element e = new Element(model.tagName());
        Attributes attrs = model.attributes();
        attrs.forEach(c -> {
            e.setAttribute(c.getKey(), c.getValue());
        });
        for (org.jsoup.nodes.Element childModel : model.children()) {
            Element childElement = jsoupToElement(childModel);
            e.appendChild(childElement);
        }
        return e;
    }

    @Test
    public void integerAttributeSetUsingString() {
        Element e = new Element("div");
        e.setAttribute("foo", "10");
        Assert.assertEquals(10, e.getAttribute("foo", 1));
    }

    @Test
    public void integerAttribute() {
        Element e = new Element("div");
        e.setAttribute("foo", 10);
        Assert.assertEquals(10, e.getAttribute("foo", 1));
    }

    @Test
    public void integerAttributeUnparsable() {
        Element e = new Element("div");
        e.setAttribute("foo", "bar");
        Assert.assertEquals(1, e.getAttribute("foo", 1));
    }

    @Test
    public void integerAttributeDefaultValue() {
        Element e = new Element("div");
        Assert.assertEquals(12, e.getAttribute("foo", 12));
    }

    @Test
    public void stringAttribute() {
        Element e = new Element("div");
        e.setAttribute("foo", "Bar");
        Assert.assertEquals("Bar", e.getAttribute("foo", "baz"));
    }

    @Test
    public void stringAttributeDefaultValue() {
        Element e = new Element("div");
        Assert.assertEquals(e.getAttribute("foo", "bar"), "bar");
    }

    @Test
    public void appendChild() {
        Element root = new Element("div");
        Element child = new Element("div");
        root.appendChild(child);
        Assert.assertEquals(root, child.getParent());
        assertChildren(root, child);
    }

    private void assertChildren(Element root, Element... children) {
        Assert.assertEquals(root.getChildCount(), children.length);
        for (int i = 0; i < root.getChildCount(); i++) {
            assertElementEquals(root.getChild(i), children[i]);
        }
    }

    @EventType("nonStaticEventClassEvent")
    public class NonStaticEventClass extends EventObject {
        public NonStaticEventClass(Object source) {
            super(source);
        }
    }

    @EventType("testEvent")
    public static class TestEvent extends EventObject {
        public TestEvent(Object source) {
            super(source);
        }
    }

    @EventType("testEventWithParameters")
    public static class TestEventWithParameters extends EventObject {
        @EventParameter("string")
        private String stringParam;
        @EventParameter("int")
        private int intParam;
        @EventParameter("double")
        private double doubleParam;
        @EventParameter("boolean")
        private boolean booleanParam;
        @EventParameter("sub.parameter")
        private String subParameter;

        public TestEventWithParameters(Object source) {
            super(source);
        }

        public String getStringParam() {
            return stringParam;
        }

        public int getIntParam() {
            return intParam;
        }

        public double getDoubleParam() {
            return doubleParam;
        }

        public String getSubParameter() {
            return subParameter;
        }
    }

    @Test
    public void sensibleErrorFromInnerClass() {
        try {
            Element e = new Element("div");
            Object source = new Object();
            e.addEventListener(NonStaticEventClass.class, event -> {
            } , source);
            e.dispatchEvent("nonStaticEventClassEvent", Json.createObject());
        } catch (Exception ee) {
            Assert.assertTrue(
                    "Message should talk about inner classes: "
                            + ee.getMessage(),
                    ee.getMessage().contains(
                            "NonStaticEventClass is a non-static inner class"));
        }
    }

    @Test
    public void sendEventToListener() {
        Element e = new Element("div");
        Object source = new Object();
        AtomicInteger events = new AtomicInteger(0);
        EventListener<TestEvent> l = new EventListener<ElementTest.TestEvent>() {
            @Override
            public void onEvent(TestEvent event) {
                events.incrementAndGet();
            }
        };
        e.addEventListener(TestEvent.class, l, source);
        e.dispatchEvent("testEvent", Json.createObject());

        Assert.assertEquals("Event should have been fired once", 1,
                events.get());
    }

    @Test
    public void sameListenerTwice() {
        Element e = new Element("div");
        Object source = new Object();
        AtomicInteger events = new AtomicInteger(0);
        EventListener<TestEvent> l = new EventListener<ElementTest.TestEvent>() {
            @Override
            public void onEvent(TestEvent event) {
                events.incrementAndGet();
            }
        };
        e.addEventListener(TestEvent.class, l, source);
        e.addEventListener(TestEvent.class, l, source);
        e.dispatchEvent("testEvent", Json.createObject());

        Assert.assertEquals("Event should have been fired twice", 2,
                events.get());
    }

    @Test
    public void twoListeners() {
        Element e = new Element("div");
        Object source = new Object();
        AtomicInteger events = new AtomicInteger(0);
        EventListener<TestEvent> l = new EventListener<ElementTest.TestEvent>() {
            @Override
            public void onEvent(TestEvent event) {
                events.incrementAndGet();
            }
        };
        e.addEventListener(TestEvent.class, l, source);
        e.addEventListener(TestEvent.class, event -> {
            // Dummy
        } , source);
        e.dispatchEvent("testEvent", Json.createObject());

        Assert.assertEquals("Event should have been fired once", 1,
                events.get());

    }

    @Test
    public void addListener() {
        Element e = new Element("div");
        Object source = new Object();
        AtomicInteger events = new AtomicInteger(0);
        EventListener<TestEvent> l = new EventListener<ElementTest.TestEvent>() {
            @Override
            public void onEvent(TestEvent event) {
                events.incrementAndGet();
            }
        };
        Assert.assertEquals(0, e.getEventListeners("testEvent").size());
        e.addEventListener(TestEvent.class, l, source);
        Assert.assertEquals(1, e.getEventListeners("testEvent").size());

    }

    @Test
    public void removeListener() {
        Element e = new Element("div");
        Object source = new Object();
        AtomicInteger events = new AtomicInteger(0);
        EventListener<TestEvent> l = new EventListener<ElementTest.TestEvent>() {
            @Override
            public void onEvent(TestEvent event) {
                events.incrementAndGet();
            }
        };
        e.addEventListener(TestEvent.class, l, source);
        e.removeEventListener(TestEvent.class, l, source);

        Assert.assertEquals(0, e.getEventListeners("testEvent").size());

    }

    @Test
    public void sameListenerTwiceRemoveOne() {
        Element e = new Element("div");
        Object source = new Object();
        AtomicInteger events = new AtomicInteger(0);
        EventListener<TestEvent> l = new EventListener<ElementTest.TestEvent>() {
            @Override
            public void onEvent(TestEvent event) {
                events.incrementAndGet();
            }
        };
        e.addEventListener(TestEvent.class, l, source);
        e.addEventListener(TestEvent.class, l, source);
        e.removeEventListener(TestEvent.class, l, source);

        Assert.assertEquals(1, e.getEventListeners("testEvent").size());

    }

    @Test
    public void addEventData() {
        Element e = new Element("div");
        Assert.assertArrayEquals(new String[0],
                e.getEventData("foo").toArray());
        e.addEventData("foo", "bar");
        Assert.assertArrayEquals(new String[] { "bar" },
                e.getEventData("foo").toArray());
    }

    @Test
    public void addEventDataMultipleTimes() {
        Element e = new Element("div");
        e.addEventData("foo", "bar");
        e.addEventData("foo", "bar");
        Assert.assertArrayEquals(new String[] { "bar" },
                e.getEventData("foo").toArray());
    }

    @Test
    public void eventDataFromEventClass() {
        Element e = new Element("div");
        Object source = new Object();
        e.addEventListener(TestEventWithParameters.class, event -> {
        } , source);
        assertArrayEqualsIgnoreOrder(
                new String[] { "string", "int", "double", "boolean",
                        "sub.parameter" },
                e.getEventData("testEventWithParameters").toArray());

    }

    private void assertArrayEqualsIgnoreOrder(Object[] array1,
            Object[] array2) {
        Arrays.sort(array1);
        Arrays.sort(array2);
        Assert.assertArrayEquals(array1, array2);
    }
}
