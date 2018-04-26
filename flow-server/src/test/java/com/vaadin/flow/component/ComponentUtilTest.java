/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.component;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.ComponentTest.TestDiv;

public class ComponentUtilTest {
    private Component component = new TestDiv();

    @Test
    public void setAttribute_byString() {
        Assert.assertNull("There should initially not be any value",
                ComponentUtil.getAttribute(component, "name"));

        ComponentUtil.setAttribute(component, "name", "value");
        Assert.assertEquals("The stored value should be returned", "value",
                ComponentUtil.getAttribute(component, "name"));

        ComponentUtil.setAttribute(component, "name", "value2");
        Assert.assertEquals("The replaced value should be returned", "value2",
                ComponentUtil.getAttribute(component, "name"));

        ComponentUtil.setAttribute(component, "name", null);
        Assert.assertNull("The value should be removed",
                ComponentUtil.getAttribute(component, "name"));
        Assert.assertNull(
                "Storage should be cleared after removing the last attribute",
                component.attributes);
    }

    @Test
    public void setAttribute_byClass() {
        Integer instance1 = new Integer(1);
        Integer instance2 = new Integer(2);

        Assert.assertNull("There should initially not be any value",
                ComponentUtil.getAttribute(component, Integer.class));

        ComponentUtil.setAttribute(component, Integer.class, instance1);
        Assert.assertSame("The stored value should be returned", instance1,
                ComponentUtil.getAttribute(component, Integer.class));

        Assert.assertNull(
                "Attribute should not be available based on super type",
                ComponentUtil.getAttribute(component, Number.class));

        ComponentUtil.setAttribute(component, Integer.class, instance2);
        Assert.assertSame("The replaced value should be returned", instance2,
                ComponentUtil.getAttribute(component, Integer.class));

        ComponentUtil.setAttribute(component, Integer.class, null);
        Assert.assertNull("The value should be removed",
                ComponentUtil.getAttribute(component, Integer.class));
        Assert.assertNull(
                "Storage should be cleared after removing the last attribute",
                component.attributes);
    }
}
