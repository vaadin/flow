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
package com.vaadin.flow.data.renderer;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.data.binder.testcomponents.TestLabel;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.VirtualChildrenList;

public class ComponentRendererTest {

    @Test
    public void componentFunction_invokedOnCreate() {
        AtomicInteger createInvocations = new AtomicInteger();
        ComponentRenderer<TestLabel, String> renderer = new ComponentRenderer<>(
                item -> {
                    createInvocations.incrementAndGet();
                    Assert.assertEquals("New item", item);
                    return new TestLabel();
                });

        renderer.createComponent("New item");

        Assert.assertEquals(
                "The component creation function should have been invoked once",
                1, createInvocations.get());
    }

    @Test
    public void componentFunction_noUpdateFunction_invokedOnUpdate() {
        AtomicInteger createInvocations = new AtomicInteger();
        TestLabel div = new TestLabel();
        ComponentRenderer<TestLabel, String> renderer = new ComponentRenderer<>(
                item -> {
                    createInvocations.incrementAndGet();
                    Assert.assertEquals("New item", item);
                    return div;
                });

        Component updatedComponent = renderer.updateComponent(div, "New item");

        Assert.assertEquals(
                "The component creation function should have been invoked once",
                1, createInvocations.get());

        Assert.assertEquals("The two components should be the same", div,
                updatedComponent);
    }

    @Test
    public void updateFunction_invokedOnUpdate() {
        AtomicInteger createInvocations = new AtomicInteger();
        AtomicInteger updateInvocations = new AtomicInteger();
        ComponentRenderer<TestLabel, String> renderer = new ComponentRenderer<>(
                item -> {
                    createInvocations.incrementAndGet();
                    Assert.assertEquals("New item", item);
                    return new TestLabel();
                }, (component, item) -> {
                    updateInvocations.incrementAndGet();
                    Assert.assertEquals("Updated item", item);
                    return component;
                });

        TestLabel div = renderer.createComponent("New item");
        Component updatedComponent = renderer.updateComponent(div,
                "Updated item");

        Assert.assertEquals(
                "The component creation function should have been invoked once",
                1, createInvocations.get());
        Assert.assertEquals(
                "The component update function should have been invoked once",
                1, updateInvocations.get());

        Assert.assertEquals("The two components should be the same", div,
                updatedComponent);
    }

    @Test
    public void enabledStateChangeOnAttachCalledForRenderedComponent() {
        UI ui = new UI();

        ComponentRenderer<MyCheckbox, ?> componentRenderer = new ComponentRenderer<>(
                item -> new MyCheckbox());

        MyDiv parent = new MyDiv();
        parent.setEnabled(false);
        ui.add(parent);

        Assert.assertFalse("Parent should be disabled", parent.isEnabled());

        componentRenderer.render(parent.getElement(), null, new Element("div"));
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        // fetch the virtual child of the parent that gets created when the
        // component is rendered
        StateNode stateNode = parent.getElement().getNode()
                .getFeatureIfInitialized(VirtualChildrenList.class).get()
                .get(0);
        Element child = Element.get(stateNode);
        Assert.assertFalse("After attach child should be disabled",
                child.isEnabled());
        Assert.assertTrue("The child should have a component", child.getComponent().isPresent());
        // Fetch the actual MyCheckbox component
        Component checkboxComponent = child.getChildren().findFirst().get()
                .getComponent().get();
        Assert.assertTrue("Component is of wrong type", checkboxComponent instanceof MyCheckbox);
        Assert.assertFalse("The component should be disabled", checkboxComponent.getElement().isEnabled());
    }

    @Tag("div")
    private static class MyDiv extends Component implements HasComponents {
    }

    @Tag("my-checkbox")
    private static class MyCheckbox extends Component {
    }

}
