/*
 * Copyright 2000-2022 Vaadin Ltd.
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

import org.junit.Assert;
import org.junit.Test;
import org.junit.internal.runners.statements.ExpectException;

import com.vaadin.flow.component.ComponentTest.TestComponent;
import com.vaadin.flow.component.ComponentTest.TestDiv;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;

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

    @Test
    public void findComponent_existingComponentFound() {
        TestComponent testComponent = createTestComponentInSession();
        int nodeId = testComponent.getElement().getNode().getId();
        String appId = testComponent.getUI().get().getInternals().getAppId();
        VaadinSession session = testComponent.getUI().get().getSession();
        ComponentReference ref = new ComponentReference(session, nodeId, appId);
        Assert.assertSame(testComponent, ComponentUtil.findComponent(ref));
    }

    @Test(expected = IllegalArgumentException.class)
    public void findComponent_nonExistingNodeIdThrows() {
        TestComponent testComponent = createTestComponentInSession();
        int nodeId = testComponent.getElement().getNode().getId();
        String appId = testComponent.getUI().get().getInternals().getAppId();
        VaadinSession session = testComponent.getUI().get().getSession();
        ComponentReference ref = new ComponentReference(session, nodeId * 10,
                appId);
        ComponentUtil.findComponent(ref);
    }

    @Test(expected = IllegalArgumentException.class)
    public void findComponent_nonExistingAppIdThrows() {
        TestComponent testComponent = createTestComponentInSession();
        int nodeId = testComponent.getElement().getNode().getId();
        VaadinSession session = testComponent.getUI().get().getSession();
        ComponentReference ref = new ComponentReference(session, nodeId, "foo");
        ComponentUtil.findComponent(ref);
    }

    private TestComponent createTestComponentInSession() {
        TestComponent testComponent = new TestComponent();
        MockVaadinSession session = new MockVaadinSession();
        session.lock();

        UI ui = new UI();
        ui.getInternals().setSession(session);
        ui.add(testComponent);
        ui.doInit(null, 0, "unit-test-app");
        session.addUI(ui);
        return testComponent;
    }
}
