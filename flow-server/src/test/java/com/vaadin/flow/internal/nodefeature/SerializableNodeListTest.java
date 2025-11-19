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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.internal.change.ListRemoveChange;
import com.vaadin.flow.internal.change.NodeChange;

public class SerializableNodeListTest
        extends AbstractNodeFeatureTest<ElementClassList> {

    private ElementClassList nodeList = createFeature();

    @Test
    public void testSerializable() {
        nodeList.add("bar");
        nodeList.add(null);

        List<Serializable> values = new ArrayList<>();
        int size = nodeList.size();
        for (int i = 0; i < size; i++) {
            values.add(nodeList.get(i));
        }

        NodeList<Serializable> copy = SerializationUtils
                .deserialize(SerializationUtils.serialize(nodeList));

        Assert.assertNotSame(nodeList, copy);

        Assert.assertEquals(values.size(), copy.size());
        for (int i = 0; i < size; i++) {
            Assert.assertEquals(values.get(i), copy.get(i));
        }
        // Also verify that original value wasn't changed by the serialization
        Assert.assertEquals(values.size(), nodeList.size());
        for (int i = 0; i < size; i++) {
            Assert.assertEquals(values.get(i), nodeList.get(i));
        }

    }

    @Test
    public void testRemoveUsingIterator() {
        nodeList.add("1");
        nodeList.add("2");
        nodeList.add("3");
        nodeList.add("4");
        nodeList.add("5");
        collectChanges(nodeList);

        Iterator<Serializable> i = nodeList.iterator();
        i.next();
        i.remove();
        List<NodeChange> changes = collectChanges(nodeList);
        Assert.assertEquals(1, changes.size());
        Assert.assertEquals(0,
                ((ListRemoveChange<?>) changes.get(0)).getIndex());

        i.next();
        i.next();
        i.remove();
        changes = collectChanges(nodeList);
        Assert.assertEquals(1, changes.size());
        Assert.assertEquals(1,
                ((ListRemoveChange<?>) changes.get(0)).getIndex());

        List<String> actual = new ArrayList<>();
        for (int j = 0; j < nodeList.size(); j++) {
            actual.add((String) nodeList.get(j));
        }
        Assert.assertArrayEquals(new String[] { "2", "4", "5" },
                actual.toArray());

    }

    @Test
    public void clearUsingIterator() {
        nodeList.add("1");
        nodeList.add("2");
        collectChanges(nodeList);

        Iterator<Serializable> i = nodeList.iterator();
        i.next();
        i.remove();
        i.next();
        i.remove();

        List<NodeChange> changes = collectChanges(nodeList);
        Assert.assertEquals(2, changes.size());
        Assert.assertEquals(0,
                ((ListRemoveChange<?>) changes.get(0)).getIndex());
        Assert.assertEquals(0,
                ((ListRemoveChange<?>) changes.get(1)).getIndex());
        Assert.assertEquals(0, nodeList.size());
    }
}
