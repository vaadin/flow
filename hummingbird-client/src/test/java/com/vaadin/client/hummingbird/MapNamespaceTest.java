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
package com.vaadin.client.hummingbird;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class MapNamespaceTest {

    private final MapNamespace namespace = new MapNamespace(0,
            new StateNode(0, new StateTree()));

    @Test
    public void testNewNamespaceEmpty() {
        namespace.forEachProperty((p, n) -> Assert.fail());
    }

    @Test
    public void testPropertyCreation() {
        MapProperty property = namespace.getProperty("foo");
        Assert.assertEquals("foo", property.getName());
        Assert.assertSame(namespace, property.getNamespace());

        List<MapProperty> properties = collectProperties();

        Assert.assertEquals(Arrays.asList(property), properties);

        MapProperty getAgain = namespace.getProperty("foo");
        Assert.assertSame(property, getAgain);

        Assert.assertEquals(properties, collectProperties());
    }

    private List<MapProperty> collectProperties() {
        List<MapProperty> properties = new ArrayList<>();
        namespace.forEachProperty((p, n) -> properties.add(p));
        return properties;
    }
}
