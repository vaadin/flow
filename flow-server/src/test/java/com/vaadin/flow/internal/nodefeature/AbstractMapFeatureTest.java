/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.internal.nodefeature;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.Assert;

public abstract class AbstractMapFeatureTest<T extends NodeFeature>
        extends AbstractNodeFeatureTest<T> {

    protected void testString(NodeMap mapFeature, String key,
            Consumer<String> setter, Supplier<String> getter) {
        String value1 = "foo";
        String value2 = "bar";

        setter.accept(value1);
        Assert.assertEquals(value1, getter.get());
        Assert.assertEquals(value1, mapFeature.get(key));

        mapFeature.put(key, value2);
        Assert.assertEquals(value2, getter.get());
    }

    protected void testInt(NodeMap mapFeature, String key,
            Consumer<Integer> setter, Supplier<Integer> getter) {
        Integer value1 = 37;
        Integer value2 = 5844;

        setter.accept(value1);
        Assert.assertEquals(value1, getter.get());
        Assert.assertEquals(value1, mapFeature.get(key));

        mapFeature.put(key, value2);
        Assert.assertEquals(value2, getter.get());
    }

    protected void testBoolean(NodeMap mapFeature, String key,
            Consumer<Boolean> setter, Supplier<Boolean> getter) {
        Boolean value1 = true;
        Boolean value2 = false;

        setter.accept(value1);
        Assert.assertEquals(value1, getter.get());
        Assert.assertEquals(value1, mapFeature.get(key));

        mapFeature.put(key, value2);
        Assert.assertEquals(value2, getter.get());
    }

}
