/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.ui.event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;

import org.junit.Test;

import com.vaadin.flow.util.ReflectionCache;
import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentTest.TestComponent;

public class ComponentEventBusUtilTest {

    @DomEvent("dom-event")
    public class InnerClass extends ComponentEvent<Component> {

        public InnerClass(Component source, boolean fromClient) {
            super(source, fromClient);
        }

    }

    @DomEvent("dom-event")
    public static class NestedClass extends ComponentEvent<Component> {

        public NestedClass(Component source, boolean fromClient) {
            super(source, fromClient);
        }

    }

    @Test
    public void domEvent_constructorCached() {
        ReflectionCache<ComponentEvent<?>, ?> cache = ComponentEventBusUtil.cache;
        TestComponent component = new TestComponent();
        cache.clear();
        assertFalse(cache.contains(MappedToDomEvent.class));
        component.addListener(MappedToDomEvent.class, e -> {
        });
        assertTrue(cache.contains(MappedToDomEvent.class));
    }

    @Test
    public void domEvent_dataExpressionCached() {
        TestComponent component = new TestComponent();
        ReflectionCache<ComponentEvent<?>, ?> cache = ComponentEventBusUtil.cache;
        cache.clear();
        assertFalse(cache.contains(MappedToDomEvent.class));
        component.addListener(MappedToDomEvent.class, e -> {
        });
        assertTrue(cache.contains(MappedToDomEvent.class));
    }

    @Test
    public void domEvent_innerEventClass() {
        try {
            ComponentEventBusUtil.getEventConstructor(InnerClass.class);
        } catch (IllegalArgumentException exception) {
            assertEquals(
                    "Cannot instantiate 'com.vaadin.ui.event.ComponentEventBusUtilTest$InnerClass'. "
                            + "Make sure the class is static if it is an inner class.",
                    exception.getMessage());
        }
    }

    @Test
    public void domEvent_nestedEventClass() {
        Constructor<NestedClass> ctor = ComponentEventBusUtil
                .getEventConstructor(NestedClass.class);
        assertNotNull(ctor);
    }

    @Test
    public void domEvent_localEventClass() {
        @DomEvent("dom-event")
        class LocalClass extends ComponentEvent<Component> {

            public LocalClass(Component source, boolean fromClient) {
                super(source, fromClient);
            }

        }
        try {
            ComponentEventBusUtil.getEventConstructor(LocalClass.class);
        } catch (IllegalArgumentException exception) {
            assertEquals(
                    "Cannot instantiate local class 'com.vaadin.ui.event.ComponentEventBusUtilTest$1LocalClass'. "
                            + "Move class declaration outside the method.",
                    exception.getMessage());
        }
    }
}
