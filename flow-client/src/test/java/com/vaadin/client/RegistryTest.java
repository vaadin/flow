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
