/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

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
}
