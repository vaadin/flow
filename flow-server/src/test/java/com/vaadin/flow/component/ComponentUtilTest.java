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
package com.vaadin.flow.component;

import java.util.Collection;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.ComponentTest.TestDiv;
import com.vaadin.flow.shared.Registration;

class ComponentUtilTest {
    private Component component = new TestDiv();

    @Test
    public void setData_byString() {
        Assertions.assertNull(ComponentUtil.getData(component, "name"),
                "There should initially not be any value");

        ComponentUtil.setData(component, "name", "value");
        Assertions.assertEquals("value",
                ComponentUtil.getData(component, "name"),
                "The stored value should be returned");

        ComponentUtil.setData(component, "name", "value2");
        Assertions.assertEquals("value2",
                ComponentUtil.getData(component, "name"),
                "The replaced value should be returned");

        ComponentUtil.setData(component, "name", null);
        Assertions.assertNull(ComponentUtil.getData(component, "name"),
                "The value should be removed");
        Assertions.assertNull(component.attributes,
                "Storage should be cleared after removing the last attribute");
    }

    @Test
    public void setData_byClass() {
        Integer instance1 = new Integer(1);
        Integer instance2 = new Integer(2);

        Assertions.assertNull(ComponentUtil.getData(component, Integer.class),
                "There should initially not be any value");

        ComponentUtil.setData(component, Integer.class, instance1);
        Assertions.assertSame(instance1,
                ComponentUtil.getData(component, Integer.class),
                "The stored value should be returned");

        Assertions.assertNull(ComponentUtil.getData(component, Number.class),
                "Attribute should not be available based on super type");

        ComponentUtil.setData(component, Integer.class, instance2);
        Assertions.assertSame(instance2,
                ComponentUtil.getData(component, Integer.class),
                "The replaced value should be returned");

        ComponentUtil.setData(component, Integer.class, null);
        Assertions.assertNull(ComponentUtil.getData(component, Integer.class),
                "The value should be removed");
        Assertions.assertNull(component.attributes,
                "Storage should be cleared after removing the last attribute");
    }

    @Test
    public void addListenerToComponent_hasListener_returnsTrue() {
        Assertions.assertFalse(
                ComponentUtil.hasEventListener(component, PollEvent.class));

        Registration listener = ComponentUtil.addListener(component,
                PollEvent.class, event -> {
                });
        Assertions.assertTrue(
                ComponentUtil.hasEventListener(component, PollEvent.class));

        listener.remove();
        Assertions.assertFalse(
                ComponentUtil.hasEventListener(component, PollEvent.class));
    }

    @Test
    public void addListenerToComponent_getListeners_returnsCollection() {
        Assertions.assertFalse(
                ComponentUtil.hasEventListener(component, PollEvent.class));

        Registration listener = ComponentUtil.addListener(component,
                PollEvent.class, event -> {
                });
        Collection<?> listeners = ComponentUtil.getListeners(component,
                PollEvent.class);
        Assertions.assertEquals(1, listeners.size());

        listener.remove();
        Assertions.assertTrue(ComponentUtil
                .getListeners(component, PollEvent.class).isEmpty());
    }

    @Test
    public void registerComponentClass_and_getComponentsByTag_shouldReturnCorrectComponent() {
        Class<? extends Component> testComponentClass = TestDiv.class;
        String testTag = "test-div";

        ComponentUtil.registerComponentClass(testTag, testComponentClass);

        Set<Class<? extends Component>> retrievedClasses = ComponentUtil
                .getComponentsByTag(testTag);

        Assertions.assertTrue(retrievedClasses.contains(testComponentClass),
                "The retrieved classes should contain the registered component class");

        ComponentUtil.getComponentsByTag(testTag).clear();
    }

    @Test
    public void getComponentsByTag_withUnregisteredTag_shouldReturnEmptySet() {
        String unregisteredTag = "unregistered-tag";

        Set<Class<? extends Component>> retrievedClasses = ComponentUtil
                .getComponentsByTag(unregisteredTag);

        Assertions.assertTrue(retrievedClasses.isEmpty(),
                "The retrieved classes should be empty for an unregistered tag");
    }

}
