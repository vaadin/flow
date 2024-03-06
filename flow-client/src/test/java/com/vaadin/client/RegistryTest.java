/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.client;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.client.flow.StateTree;

public class RegistryTest {

    Registry registry;

    @Before
    public void setup() {
        registry = new Registry();
    }

    @Test
    public void setAndGet() {
        StateTree instance = new StateTree(registry);
        registry.set(StateTree.class, instance);

        Assert.assertSame(instance, registry.get(StateTree.class));
        Assert.assertSame(instance, registry.getStateTree());
    }

    @Test(expected = AssertionError.class)
    public void getUndefined() {
        Registry registry = new Registry();
        registry.get(StateTree.class);
    }

    public static class MyClass {

    }

    @Test
    public void setAndGetCustom() {
        MyClass myClass = new MyClass();
        registry.set(MyClass.class, myClass);
        Assert.assertSame(myClass, registry.get(MyClass.class));

    }
}
