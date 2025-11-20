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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.dom.ClassList;

public class ListFeatureSetViewTest extends AbstractNodeFeatureTest<ElementClassList> {

    @Test
    public void testSetViewAdd() {
        ElementClassList feature = createFeature();
        ClassList set = feature.getClassList();

        Assert.assertEquals(0, set.size());
        Assert.assertTrue(set.add("a"));
        Assert.assertTrue(set.add("b"));
        Assert.assertEquals(2, set.size());

        Assert.assertTrue(set.contains("a"));
        Assert.assertTrue(set.contains("b"));

        // duplicate add returns false and doesn't change size
        Assert.assertFalse(set.add("a"));
        Assert.assertEquals(2, set.size());
    }

    @Test
    public void testSetViewClear() {
        ElementClassList feature = createFeature();
        ClassList set = feature.getClassList();

        set.add("x");
        set.add("y");
        Assert.assertEquals(2, set.size());

        Assert.assertTrue(set.remove("x"));
        Assert.assertEquals(1, set.size());
        Assert.assertFalse(set.contains("x"));
        Assert.assertTrue(set.contains("y"));

        // removing non-existing returns false
        Assert.assertFalse(set.remove("nope"));

        set.clear();
        //noinspection ConstantConditions
        Assert.assertEquals(0, set.size());
        Assert.assertFalse(set.contains("y"));
    }

    @Test
    public void testSetViewIteratorIterate() {
        ElementClassList feature = createFeature();
        ClassList set = feature.getClassList();
        set.add("first");
        set.add("second");
        set.add("third");

        List<String> seen = new ArrayList<>();
        Iterator<String> it = set.iterator();
        while (it.hasNext()) {
            seen.add(it.next());
        }
        Assert.assertArrayEquals(new String[] { "first", "second", "third" }, seen.toArray());
    }

    @Test
    public void testSetViewContains() {
        ElementClassList feature = createFeature();
        ClassList set = feature.getClassList();
        set.add("a");
        Assert.assertTrue(set.contains("a"));
        Assert.assertTrue(set.remove("a"));
        Assert.assertFalse(set.contains("a"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void add_null_throws() {
        ElementClassList feature = createFeature();
        feature.getClassList().add(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void add_empty_throws() {
        ElementClassList feature = createFeature();
        feature.getClassList().add("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetViewValidateAdd() {
        ElementClassList feature = createFeature();
        feature.getClassList().add("a b");
    }
}
