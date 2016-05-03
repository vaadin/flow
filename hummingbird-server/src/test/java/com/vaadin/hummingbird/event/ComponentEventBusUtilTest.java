/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.event;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.util.ReflectionCache;
import com.vaadin.ui.ComponentEvent;
import com.vaadin.ui.ComponentTest.TestComponent;

public class ComponentEventBusUtilTest {
    @Test
    public void domEvent_constructorCached() {
        ReflectionCache<ComponentEvent<?>, ?> cache = ComponentEventBusUtil.cache;
        TestComponent component = new TestComponent();
        cache.clear();
        Assert.assertFalse(cache.contains(MappedToDomEvent.class));
        component.addListener(MappedToDomEvent.class, e -> {
        });
        Assert.assertTrue(cache.contains(MappedToDomEvent.class));
    }

    @Test
    public void domEvent_dataExpressionCached() {
        TestComponent component = new TestComponent();
        ReflectionCache<ComponentEvent<?>, ?> cache = ComponentEventBusUtil.cache;
        cache.clear();
        Assert.assertFalse(cache.contains(MappedToDomEvent.class));
        component.addListener(MappedToDomEvent.class, e -> {
        });
        Assert.assertTrue(cache.contains(MappedToDomEvent.class));
    }
}
