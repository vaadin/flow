package com.vaadin.client;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.client.hummingbird.StateTree;

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

        Assert.assertEquals(instance, registry.get(StateTree.class));
        Assert.assertEquals(instance, registry.getStateTree());
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
        Assert.assertEquals(myClass, registry.get(MyClass.class));

    }
}
