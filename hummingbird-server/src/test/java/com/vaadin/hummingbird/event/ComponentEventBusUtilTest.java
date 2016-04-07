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

import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.ui.ComponentTest.TestComponent;

public class ComponentEventBusUtilTest {
    @Test
    public void domEvent_constructorCached() {
        EventDataCache cache = ComponentEventBusUtil.cache;
        TestComponent component = new TestComponent();
        cache.clear();
        assertEmpty(cache.getEventConstructor(MappedToDomEvent.class));
        component.addListener(MappedToDomEvent.class, e -> {
        });
        assertPresent(cache.getEventConstructor(MappedToDomEvent.class));
    }

    @Test
    public void domEvent_dataExpressionCached() {
        TestComponent component = new TestComponent();
        EventDataCache cache = ComponentEventBusUtil.cache;
        cache.clear();
        assertEmpty(cache.getDataExpressions(MappedToDomEvent.class));
        component.addListener(MappedToDomEvent.class, e -> {
        });
        assertPresent(cache.getDataExpressions(MappedToDomEvent.class));
    }

    private void assertEmpty(Optional<?> optional) {
        Assert.assertEquals("Optional should be empty but is " + optional,
                Optional.empty(), optional);
    }

    private void assertPresent(Optional<?> optional) {
        Assert.assertTrue("Value should be present but optional is " + optional,
                optional.isPresent());
    }
}
