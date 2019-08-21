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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.data.binder.testcomponents.TestLabel;
import com.vaadin.flow.data.provider.ComponentDataGenerator;
import com.vaadin.flow.data.provider.KeyMapper;
import com.vaadin.flow.dom.Element;

import elemental.json.Json;
import elemental.json.JsonObject;

public class ComponentRendererTest {

    private static class TestUIInternals extends UIInternals {

        private List<PendingJavaScriptInvocation> invocations = new ArrayList<>();

        public TestUIInternals(UI ui) {
            super(ui);
        }

        @Override
        public void addJavaScriptInvocation(
                PendingJavaScriptInvocation invocation) {
            invocations.add(invocation);
        }

    }

    private static class TestUI extends UI {

        private UIInternals internals;

        @Override
        public UIInternals getInternals() {
            if (internals == null) {
                internals = new TestUIInternals(this);
            }
            return internals;
        }
    }

    @Test
    public void templateRenderered_parentAttachedBeforeChild() {
        UI ui = new TestUI();
        TestUIInternals internals = (TestUIInternals) ui.getInternals();

        ComponentRenderer<TestLabel, String> renderer = new ComponentRenderer<>(
                e -> (new TestLabel()));

        Element containerParent = new Element("div");
        Element container = new Element("div");

        KeyMapper<String> keyMapper = new KeyMapper<>();

        ComponentDataGenerator<String> rendering = (ComponentDataGenerator<String>) renderer
                .render(container, keyMapper);

        // simulate a call from the grid to refresh data - template is not setup
        containerParent.getNode().runWhenAttached(
                ui2 -> ui2.getInternals().getStateTree()
                        .beforeClientResponse(containerParent.getNode(),
                                context -> {
                                    Assert.assertNotNull(
                                            "NodeIdPropertyName should not be null",
                                            rendering.getNodeIdPropertyName());
                                    JsonObject value = Json.createObject();
                                    rendering.generateData("item", value);
                                    Assert.assertEquals(
                                            "generateData should add one element in the jsonobject",
                                            1, value.keys().length);
                                }));

        // attach the parent (ex: grid) before the child (ex: column)
        attachElement(ui, containerParent);
        attachElement(ui, container);

        internals.getStateTree().runExecutionsBeforeClientResponse();

    }

    @Test
    public void templateRenderered_childAttachedBeforeParent() {
        UI ui = new TestUI();
        TestUIInternals internals = (TestUIInternals) ui.getInternals();

        ComponentRenderer<TestLabel, String> renderer = new ComponentRenderer<>(
                e -> (new TestLabel()));

        Element containerParent = new Element("div");
        Element container = new Element("div");
        KeyMapper<String> keyMapper = new KeyMapper<>();

        ComponentDataGenerator<String> rendering = (ComponentDataGenerator<String>) renderer
                .render(container, keyMapper);

        containerParent.getNode().runWhenAttached(
                ui2 -> ui2.getInternals().getStateTree()
                        .beforeClientResponse(containerParent.getNode(),
                                context -> {
                                    // if nodeid is null then the component won't be rendered correctly
                                    Assert.assertNotNull(
                                            "NodeIdPropertyName should not be null",
                                            rendering.getNodeIdPropertyName());
                                    JsonObject value = Json.createObject();
                                    rendering.generateData("item", value);
                                    Assert.assertEquals(
                                            "generateData should add one element in the jsonobject",
                                            1, value.keys().length);
                                }));
        // attach the child (ex: container) before the parent (ex: grid)
        attachElement(ui, container);
        attachElement(ui, containerParent);

        internals.getStateTree().runExecutionsBeforeClientResponse();

    }

    private void attachElement(UI ui, Element contentTemplate) {
        ui.getElement().appendChild(contentTemplate);
    }

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
        Component updatedComponent = renderer
                .updateComponent(div, "Updated item");

        Assert.assertEquals(
                "The component creation function should have been invoked once",
                1, createInvocations.get());
        Assert.assertEquals(
                "The component update function should have been invoked once",
                1, updateInvocations.get());

        Assert.assertEquals("The two components should be the same", div,
                updatedComponent);
    }

}
