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
package com.vaadin.hummingbird.namespace;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.StateNode;

/**
 * @author Vaadin Ltd
 *
 */
public class SynchronizedPropertiesNamespaceTest {

    private SynchronizedPropertiesNamespace namespace = new SynchronizedPropertiesNamespace(
            new StateNode(SynchronizedPropertiesNamespace.class));

    @Test
    public void getSynchronizedProperties_addProperty() {
        Set<String> set = namespace.getSynchronizedProperties();
        String item = "item";
        set.add(item);
        Assert.assertEquals(set.size(), 1);
        Assert.assertTrue(set.contains(item));
    }

    @Test
    public void getSynchronizedProperties_addSameProperties() {
        Set<String> set = namespace.getSynchronizedProperties();
        String item = "item";
        set.add(item);
        set.add(item);
        Assert.assertEquals(set.size(), 1);
        Assert.assertTrue(set.contains(item));
    }

    @Test
    public void getSynchronizedProperties_removeProperty() {
        Set<String> set = namespace.getSynchronizedProperties();
        String item = "item";
        set.add(item);
        set.remove(item);
        Assert.assertEquals(set.size(), 0);
        Assert.assertFalse(set.contains(item));
    }

    @Test
    public void getSynchronizedProperties_removeProperties() {
        Set<String> set = namespace.getSynchronizedProperties();
        String item = "item";
        String item1 = "item1";
        set.add(item);
        set.add(item1);
        set.removeAll(Arrays.asList(item, item1));
        Assert.assertEquals(set.size(), 0);
        Assert.assertFalse(set.contains(item));
        Assert.assertFalse(set.contains(item1));
    }

    @Test
    public void getSynchronizedProperties_addProperties() {
        Set<String> set = namespace.getSynchronizedProperties();
        String item = "item";
        String item1 = "item1";
        set.addAll(Arrays.asList(item, item1));
        Assert.assertEquals(set.size(), 2);
        Assert.assertTrue(set.contains(item));
        Assert.assertTrue(set.contains(item1));
    }

    @Test
    public void getSynchronizedProperties_clear() {
        Set<String> set = namespace.getSynchronizedProperties();
        String item = "item";
        set.add(item);
        set.clear();
        Assert.assertEquals(set.size(), 0);
        Assert.assertFalse(set.contains(item));
    }

    @Test
    public void getSynchronizedProperties_retain() {
        Set<String> set = namespace.getSynchronizedProperties();
        String item = "item";
        String item1 = "item1";
        set.addAll(Arrays.asList(item, item1));
        set.retainAll(Arrays.asList(item));
        Assert.assertEquals(set.size(), 1);
        Assert.assertFalse(set.contains(item1));
    }

    @Test
    public void getSynchronizedProperties_iterator() {
        Set<String> set = namespace.getSynchronizedProperties();
        String item = "item";
        String item1 = "item1";
        set.addAll(Arrays.asList(item, item1));
        Iterator<String> iterator = set.iterator();
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(item, iterator.next());
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(item1, iterator.next());
        Assert.assertFalse(iterator.hasNext());
    }

    @Test(expected = ConcurrentModificationException.class)
    public void getSynchronizedProperties_modifyWhileIterating() {
        Set<String> set = namespace.getSynchronizedProperties();
        String item = "item";
        String item1 = "item1";
        set.add(item);
        Iterator<String> iterator = set.iterator();
        set.add(item1);
        iterator.next();
    }

    @Test
    public void getSynchronizedPropertyEvents_setClassIsTheSame() {
        // Don't test everything for getSynchronizedPropertyEvents(). Just check
        // that it's the same class.
        Class<? extends Set> class1 = namespace.getSynchronizedProperties()
                .getClass();
        Class<? extends Set> class2 = namespace.getSynchronizedPropertyEvents()
                .getClass();
        Assert.assertEquals(class1, class2);
    }
}
