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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.change.ListSpliceChange;
import com.vaadin.hummingbird.change.JsonNodeChange;

public class SerializableListNamespaceTest
        extends AbstractNamespaceTest<ClassListNamespace> {

    private ClassListNamespace namespace = createNamespace();

    @Test
    public void testSerializable() {
        namespace.add("bar");
        namespace.add(null);

        List<Serializable> values = new ArrayList<>();
        int size = namespace.size();
        for (int i = 0; i < size; i++) {
            values.add(namespace.get(i));
        }

        ListNamespace<Serializable> copy = SerializationUtils
                .deserialize(SerializationUtils.serialize(namespace));

        Assert.assertNotSame(namespace, copy);

        Assert.assertEquals(values.size(), copy.size());
        for (int i = 0; i < size; i++) {
            Assert.assertEquals(values.get(i), copy.get(i));
        }
        // Also verify that original value wasn't changed by the serialization
        Assert.assertEquals(values.size(), namespace.size());
        for (int i = 0; i < size; i++) {
            Assert.assertEquals(values.get(i), namespace.get(i));
        }

    }

    @Test
    public void testRemoveUsingIterator() {
        namespace.add("1");
        namespace.add("2");
        namespace.add("3");
        namespace.add("4");
        namespace.add("5");
        collectChanges(namespace);

        Iterator<String> i = namespace.iterator();
        i.next();
        i.remove();
        List<JsonNodeChange> changes = collectChanges(namespace);
        Assert.assertEquals(1, changes.size());
        Assert.assertEquals(0, ((ListSpliceChange) changes.get(0)).getIndex());
        Assert.assertEquals(1,
                ((ListSpliceChange) changes.get(0)).getRemoveCount());

        i.next();
        i.next();
        i.remove();
        changes = collectChanges(namespace);
        Assert.assertEquals(1, changes.size());
        Assert.assertEquals(1, ((ListSpliceChange) changes.get(0)).getIndex());
        Assert.assertEquals(1,
                ((ListSpliceChange) changes.get(0)).getRemoveCount());

        List<String> actual = new ArrayList<>();
        for (int j = 0; j < namespace.size(); j++) {
            actual.add(namespace.get(j));
        }
        Assert.assertArrayEquals(new String[] { "2", "4", "5" },
                actual.toArray());

    }

    @Test
    public void clearUsingIterator() {
        namespace.add("1");
        namespace.add("2");
        collectChanges(namespace);

        Iterator<String> i = namespace.iterator();
        i.next();
        i.remove();
        i.next();
        i.remove();

        List<JsonNodeChange> changes = collectChanges(namespace);
        Assert.assertEquals(2, changes.size());
        Assert.assertEquals(0, ((ListSpliceChange) changes.get(0)).getIndex());
        Assert.assertEquals(0, ((ListSpliceChange) changes.get(1)).getIndex());
        Assert.assertEquals(1,
                ((ListSpliceChange) changes.get(0)).getRemoveCount());
        Assert.assertEquals(1,
                ((ListSpliceChange) changes.get(1)).getRemoveCount());
        Assert.assertEquals(0, namespace.size());
    }
}
