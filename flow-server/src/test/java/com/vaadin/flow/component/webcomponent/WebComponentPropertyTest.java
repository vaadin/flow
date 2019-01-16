package com.vaadin.flow.component.webcomponent;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.shared.Registration;

public class WebComponentPropertyTest {

    @Test
    public void webComponentProperty_returnsCorrectType() {
        // String
        WebComponentProperty<String> string = new WebComponentProperty<>(
                String.class);
        Assert.assertEquals("Wrong class value type for given class type",
                String.class, string.getValueType());

        string = new WebComponentProperty<>("hi");
        Assert.assertEquals(
                "Wrong class value type received from initalValue.'",
                String.class, string.getValueType());

        // Integer
        WebComponentProperty<Integer> integer = new WebComponentProperty<>(
                Integer.class);
        Assert.assertEquals("Wrong class value type for given class type",
                Integer.class, integer.getValueType());

        integer = new WebComponentProperty<>(15);
        Assert.assertEquals(
                "Wrong class value type received from initalValue.'",
                Integer.class, integer.getValueType());

        // Boolean
        WebComponentProperty<Boolean> bool = new WebComponentProperty<>(
                Boolean.class);
        Assert.assertEquals("Wrong class value type for given class type",
                Boolean.class, bool.getValueType());

        bool = new WebComponentProperty<>(true);
        Assert.assertEquals(
                "Wrong class value type received from initalValue.'",
                Boolean.class, bool.getValueType());

        // Long
        WebComponentProperty<Long> longProperty = new WebComponentProperty<>(
                Long.class);
        Assert.assertEquals("Wrong class value type for given class type",
                Long.class, longProperty.getValueType());

        longProperty = new WebComponentProperty<>(15l);
        Assert.assertEquals(
                "Wrong class value type received from initalValue.'",
                Long.class, longProperty.getValueType());
    }

    @Test
    public void webComponentProperty_changeEventFiredWithCorrectData() {

        WebComponentProperty<String> string = new WebComponentProperty<>(
                String.class);
        WebComponentProperty<Long> longProperty = new WebComponentProperty<>(
                Long.class);

        List<PropertyValueChangeEvent<?>> events = new ArrayList<>();

        string.addValueChangeListener(events::add);
        longProperty.addValueChangeListener(events::add);

        longProperty.set(20l);

        string.set("new");

        string.set("second");

        longProperty.set(42l);

        Assert.assertEquals(4, events.size());

        Assert.assertEquals(longProperty, events.get(0).getSource());
        Assert.assertEquals(string, events.get(1).getSource());
        Assert.assertEquals(string, events.get(2).getSource());
        Assert.assertEquals(longProperty, events.get(3).getSource());

        Assert.assertNull("Unexpected non null value for longValue",
                events.get(0).getOldValue());
        Assert.assertEquals("Unexpected new value", 20l,
                events.get(0).getNewValue());
        Assert.assertEquals("Second update old value was wrong", 20l,
                events.get(3).getOldValue());
        Assert.assertEquals("Unexpected new value", 42l,
                events.get(3).getNewValue());

        Assert.assertNull("Unexpected non null value for string",
                events.get(1).getOldValue());
        Assert.assertEquals("Unexpected new value", "new",
                events.get(1).getNewValue());
        Assert.assertEquals("Second update old value was wrong", "new",
                events.get(2).getOldValue());
        Assert.assertEquals("Unexpected new value", "second",
                events.get(2).getNewValue());
    }

    @Test
    public void webComponentProperty_removedListenerGetsNoEvent() {
        WebComponentProperty<String> string = new WebComponentProperty<>(
                String.class);

        List<PropertyValueChangeEvent<?>> events = new ArrayList<>();

        Registration registration = string.addValueChangeListener(events::add);

        string.set("new");

        Assert.assertEquals("Listener should have fired", 1, events.size());

        registration.remove();

        string.set("old");

        Assert.assertEquals("Removed listener should not get an event", 1,
                events.size());
    }
}
