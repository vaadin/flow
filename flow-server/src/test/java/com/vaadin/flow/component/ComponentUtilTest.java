/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

import java.util.Collection;

import com.vaadin.flow.shared.Registration;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.ComponentTest.TestDiv;

public class ComponentUtilTest {
    private Component component = new TestDiv();

    @Test
    public void setData_byString() {
        Assert.assertNull("There should initially not be any value",
                ComponentUtil.getData(component, "name"));

        ComponentUtil.setData(component, "name", "value");
        Assert.assertEquals("The stored value should be returned", "value",
                ComponentUtil.getData(component, "name"));

        ComponentUtil.setData(component, "name", "value2");
        Assert.assertEquals("The replaced value should be returned", "value2",
                ComponentUtil.getData(component, "name"));

        ComponentUtil.setData(component, "name", null);
        Assert.assertNull("The value should be removed",
                ComponentUtil.getData(component, "name"));
        Assert.assertNull(
                "Storage should be cleared after removing the last attribute",
                component.attributes);
    }

    @Test
    public void setData_byClass() {
        Integer instance1 = new Integer(1);
        Integer instance2 = new Integer(2);

        Assert.assertNull("There should initially not be any value",
                ComponentUtil.getData(component, Integer.class));

        ComponentUtil.setData(component, Integer.class, instance1);
        Assert.assertSame("The stored value should be returned", instance1,
                ComponentUtil.getData(component, Integer.class));

        Assert.assertNull(
                "Attribute should not be available based on super type",
                ComponentUtil.getData(component, Number.class));

        ComponentUtil.setData(component, Integer.class, instance2);
        Assert.assertSame("The replaced value should be returned", instance2,
                ComponentUtil.getData(component, Integer.class));

        ComponentUtil.setData(component, Integer.class, null);
        Assert.assertNull("The value should be removed",
                ComponentUtil.getData(component, Integer.class));
        Assert.assertNull(
                "Storage should be cleared after removing the last attribute",
                component.attributes);
    }

    @Test
    public void addListenerToComponent_hasListener_returnsTrue() {
        Assert.assertFalse(
                ComponentUtil.hasEventListener(component, PollEvent.class));

        Registration listener = ComponentUtil.addListener(component,
                PollEvent.class, event -> {
                });
        Assert.assertTrue(
                ComponentUtil.hasEventListener(component, PollEvent.class));

        listener.remove();
        Assert.assertFalse(
                ComponentUtil.hasEventListener(component, PollEvent.class));
    }

    @Test
    public void addListenerToComponent_getListeners_returnsCollection() {
        Assert.assertFalse(
                ComponentUtil.hasEventListener(component, PollEvent.class));

        Registration listener = ComponentUtil.addListener(component,
                PollEvent.class, event -> {
                });
        Collection<?> listeners = ComponentUtil.getListeners(component,
                PollEvent.class);
        Assert.assertEquals(1, listeners.size());

        listener.remove();
        Assert.assertTrue(ComponentUtil.getListeners(component, PollEvent.class)
                .isEmpty());
    }
}
