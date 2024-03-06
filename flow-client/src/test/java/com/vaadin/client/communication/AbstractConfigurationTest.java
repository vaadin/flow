/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.client.communication;

import java.util.function.Supplier;

import org.junit.Assert;

import com.vaadin.client.flow.nodefeature.MapProperty;

public abstract class AbstractConfigurationTest {

    protected abstract MapProperty getProperty(String key);

    protected void testString(String key, Supplier<String> getter) {
        String value1 = "foo";
        String value2 = "bar";

        getProperty(key).setValue(value1);
        Assert.assertEquals(value1, getter.get());

        getProperty(key).setValue(value2);
        Assert.assertEquals(value2, getter.get());
    }

    protected void testInt(String key, Supplier<Integer> getter) {
        Integer value1 = 1234;
        Integer value2 = 1;

        // Numbers are always passed as doubles from the server
        getProperty(key).setValue((double) value1);
        Assert.assertEquals(value1, getter.get());

        // Numbers are always passed as doubles from the server
        getProperty(key).setValue((double) value2);
        Assert.assertEquals(value2, getter.get());
    }

    protected void testBoolean(String key, Supplier<Boolean> getter) {
        Boolean value1 = Boolean.TRUE;
        Boolean value2 = Boolean.FALSE;

        getProperty(key).setValue(value1);
        Assert.assertEquals(value1, getter.get());

        getProperty(key).setValue(value2);
        Assert.assertEquals(value2, getter.get());
    }

}
