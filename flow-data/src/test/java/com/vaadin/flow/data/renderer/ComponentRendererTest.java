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
import com.vaadin.flow.component.html.Div;

public class ComponentRendererTest {

    @Test
    public void componentFunction_invokedOnCreate() {
        AtomicInteger createInvocations = new AtomicInteger();
        ComponentRenderer<Div, String> renderer = new ComponentRenderer<>(
                item -> {
                    createInvocations.incrementAndGet();
                    Assert.assertEquals("New item", item);
                    return new Div();
                });

        renderer.createComponent("New item");

        Assert.assertEquals(
                "The component creation function should have been invoked once",
                1, createInvocations.get());
    }

    @Test
    public void componentFunction_noUpdateFunction_invokedOnUpdate() {
        AtomicInteger createInvocations = new AtomicInteger();
        Div div = new Div();
        ComponentRenderer<Div, String> renderer = new ComponentRenderer<>(
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
        ComponentRenderer<Div, String> renderer = new ComponentRenderer<>(
                item -> {
                    createInvocations.incrementAndGet();
                    Assert.assertEquals("New item", item);
                    return new Div();
                }, (component, item) -> {
                    updateInvocations.incrementAndGet();
                    Assert.assertEquals("Updated item", item);
                    return component;
                });

        Div div = renderer.createComponent("New item");
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

}
