package com.vaadin.client;

import static org.junit.Assert.assertSame;

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

        assertSame(instance, registry.get(StateTree.class));
        assertSame(instance, registry.getStateTree());
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
        assertSame(myClass, registry.get(MyClass.class));

    }
}
