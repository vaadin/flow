/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
