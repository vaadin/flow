/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.nodefeature;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.StateNode;
import com.vaadin.flow.change.AbstractListChange;
import com.vaadin.flow.nodefeature.NodeList.SetView;

public class ListFeatureSetViewTest {

    private TestFeature feature;
    private SetView<String> set;

    private static class TestFeature extends SerializableNodeList<String> {
        private final ArrayList<AbstractListChange<String>> changes = new ArrayList<>();

        public TestFeature() {
            super(Mockito.mock(StateNode.class));
        }

        @Override
        protected List<AbstractListChange<String>> getChangeTracker() {
            // Default implementation calls unmocked method in StateNode
            return changes;
        }
    }

    private static class TestSetView extends NodeList.SetView<String> {
        public TestSetView(NodeList<String> list) {
            super(list);
        }

        @Override
        protected void validate(String string) {
            if (string.length() > 5) {
                throw new IllegalArgumentException(
                        "All strings must be less than 5 characters");
            }
        }
    }

    @Before
    public void setup() {
        feature = new TestFeature();
        set = new TestSetView(feature);
    }

    @Test
    public void testSetViewAdd() {
        set.add("0");
        assertContents("0");
        set.add("1");
        assertContents("0", "1");
        set.add("2");
        assertContents("0", "1", "2");
    }

    @Test
    public void testSetViewSize() {
        set.add("0");
        assertEquals(1, set.size());
        set.add("1");
        assertEquals(2, set.size());
        set.remove("1");
        assertEquals(1, set.size());
        set.remove("1"); // Not in the list
        assertEquals(1, set.size());
        set.remove("0");
        assertEquals(0, set.size());
    }

    @Test
    public void testSetViewRemove() {
        set.add("0");
        set.add("1");
        set.remove("0");
        assertContents("1");
        set.remove("1");
        assertContents();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetViewValidateAdd() {
        set.add("as89w4573");
    }

    @Test
    public void testSetViewClear() {
        set.add("a");
        set.add("b");
        set.add("c");
        set.clear();
        assertContents();
        set.clear();
    }

    @Test
    public void testSetViewIteratorIterate() {
        set.add("a");
        set.add("b");
        set.add("c");

        Iterator<String> i = set.iterator();
        assertEquals("a", i.next());
        assertEquals("b", i.next());
        assertEquals("c", i.next());
        assertFalse(i.hasNext());
    }

    @Test
    public void testSetViewIteratorRemove() {
        set.add("a");
        set.add("b");
        set.add("c");

        Iterator<String> i = set.iterator();
        i.next();
        i.next();
        i.remove();
        assertContents("a", "c");

    }

    @Test
    public void testSetViewContains() {
        set.add("a");
        set.add("b");
        set.add("c");

        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        assertTrue(set.contains("c"));
        assertFalse(set.contains("d"));
    }

    @Test
    public void testSetViewSameFeatureEquals() {
        set.add("a");

        TestSetView otherSet = new TestSetView(feature);

        assertEquals(set, otherSet);
        assertEquals(set.hashCode(), otherSet.hashCode());
    }

    @Test
    public void testSetViewDifferentFeatureEquals() {
        set.add("a");
        TestFeature otherFeature = new TestFeature();
        TestSetView otherSet = new TestSetView(otherFeature);
        otherSet.add("a");

        assertEquals(set, otherSet);
        assertEquals(set.hashCode(), otherSet.hashCode());

    }

    private void assertContents(String... expected) {
        String[] actual = new String[feature.size()];
        for (int i = 0; i < feature.size(); i++) {
            actual[i] = feature.get(i);
        }
        assertArrayEquals(expected, actual);

    }
}
