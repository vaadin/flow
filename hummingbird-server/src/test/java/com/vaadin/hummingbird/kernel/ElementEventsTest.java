package com.vaadin.hummingbird.kernel;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventListener;
import java.util.EventObject;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.annotations.EventParameter;
import com.vaadin.annotations.EventType;
import com.vaadin.event.ElementEvents;
import com.vaadin.tests.server.TestField;

import elemental.json.Json;
import elemental.json.JsonObject;

public class ElementEventsTest {

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

    @EventType("jsonEventDataEvent")
    public static class JsonEventDataEvent extends EventObject {

        private JsonObject eventData;

        public JsonEventDataEvent(Object source) {
            super(source);
        }

        public JsonObject getEventData() {
            return eventData;
        }
    }

    public interface TestEventListener extends EventListener {
        public void testEventOccurred(TestEvent e);
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
            TestField component = new TestField();
            Element e = component.getElement();

            ElementEvents.addElementListener(component,
                    NonStaticEventClass.class,
                    (com.vaadin.event.EventListener<NonStaticEventClass>) event -> {
                    });
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
        TestField c = new TestField();
        Element e = c.getElement();
        AtomicInteger events = new AtomicInteger(0);
        TestEventListener l = new TestEventListener() {

            @Override
            public void testEventOccurred(TestEvent e) {
                events.incrementAndGet();
            }
        };
        ElementEvents.addElementListener(c, TestEvent.class, l);
        e.dispatchEvent("testEvent", Json.createObject());

        Assert.assertEquals("Event should have been fired once", 1,
                events.get());
    }

    @Test
    public void sameListenerTwice() {
        TestField c = new TestField();
        Element e = c.getElement();
        AtomicInteger events = new AtomicInteger(0);
        TestEventListener l = new TestEventListener() {

            @Override
            public void testEventOccurred(TestEvent e) {
                events.incrementAndGet();
            }
        };

        ElementEvents.addElementListener(c, TestEvent.class, l);
        ElementEvents.addElementListener(c, TestEvent.class, l);
        e.dispatchEvent("testEvent", Json.createObject());

        Assert.assertEquals("Event should have been fired once", 1,
                events.get());
    }

    @Test
    public void twoListeners() {
        TestField c = new TestField();
        Element e = c.getElement();

        AtomicInteger events = new AtomicInteger(0);
        TestEventListener l = new TestEventListener() {

            @Override
            public void testEventOccurred(TestEvent e) {
                events.incrementAndGet();
            }
        };
        ElementEvents.addElementListener(c, TestEvent.class, l);
        ElementEvents.addElementListener(c, TestEvent.class,
                (TestEventListener) event -> {
                    // Dummy
                });
        e.dispatchEvent("testEvent", Json.createObject());

        Assert.assertEquals("Event should have been fired once", 1,
                events.get());

    }

    @Test
    public void addListener() {
        TestField c = new TestField();
        Element e = c.getElement();

        AtomicInteger events = new AtomicInteger(0);
        TestEventListener l = new TestEventListener() {
            @Override
            public void testEventOccurred(TestEvent event) {
                events.incrementAndGet();
            }
        };
        Assert.assertEquals(0, e.getEventListeners("testEvent").size());
        ElementEvents.addElementListener(c, TestEvent.class, l);
        Assert.assertEquals(1, e.getEventListeners("testEvent").size());

    }

    @Test
    public void removeListener() {
        TestField c = new TestField();
        Element e = c.getElement();

        AtomicInteger events = new AtomicInteger(0);
        TestEventListener l = new TestEventListener() {
            @Override
            public void testEventOccurred(TestEvent event) {
                events.incrementAndGet();
            }
        };
        ElementEvents.addElementListener(c, TestEvent.class, l);
        ElementEvents.removeElementListener(c, TestEvent.class, l);

        Assert.assertEquals(0, e.getEventListeners("testEvent").size());

    }

    @Test
    public void sameListenerTwiceRemoveOne() {
        TestField c = new TestField();
        Element e = c.getElement();

        AtomicInteger events = new AtomicInteger(0);
        TestEventListener l = new TestEventListener() {
            @Override
            public void testEventOccurred(TestEvent event) {
                events.incrementAndGet();
            }
        };
        ElementEvents.addElementListener(c, TestEvent.class, l);
        ElementEvents.addElementListener(c, TestEvent.class, l);
        ElementEvents.removeElementListener(c, TestEvent.class, l);

        // Only added once
        Assert.assertEquals(0, e.getEventListeners("testEvent").size());

    }

    @Test
    public void addEventData() {
        TestField c = new TestField();
        Element e = c.getElement();

        Assert.assertArrayEquals(new String[0],
                e.getEventData("foo").toArray());
        e.addEventData("foo", "bar");
        Assert.assertArrayEquals(new String[] { "bar" },
                e.getEventData("foo").toArray());
    }

    @Test
    public void eventDataFromEventClass() {
        TestField c = new TestField();
        Element e = c.getElement();

        ElementEvents.addElementListener(c, TestEventWithParameters.class,
                (com.vaadin.event.EventListener<TestEventWithParameters>) event -> {
                });
        assertArrayEqualsIgnoreOrder(
                new String[] { "string", "int", "double", "boolean",
                        "sub.parameter" },
                e.getEventData("testEventWithParameters").toArray());

    }

    @Test
    public void jsonEventData() {
        TestField c = new TestField();
        Element e = c.getElement();

        List<JsonEventDataEvent> events = new ArrayList<>();

        ElementEvents.addElementListener(c, JsonEventDataEvent.class,
                (com.vaadin.event.EventListener<JsonEventDataEvent>) event -> {
                    events.add(event);
                });

        JsonObject eventData = Json.createObject();
        eventData.put("value", "foobar");

        e.dispatchEvent("jsonEventDataEvent", eventData);

        assertEquals("only one event should be fired", 1, events.size());
        assertEquals("event data should equal", eventData,
                events.get(0).getEventData());
    }

    private void assertArrayEqualsIgnoreOrder(Object[] array1,
            Object[] array2) {
        Arrays.sort(array1);
        Arrays.sort(array2);
        Assert.assertArrayEquals(array1, array2);
    }
}
