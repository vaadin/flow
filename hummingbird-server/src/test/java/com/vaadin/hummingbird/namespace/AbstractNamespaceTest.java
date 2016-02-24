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

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.change.NodeChange;

public abstract class AbstractNamespaceTest<T extends Namespace> {

    public T createNamespace() {
        Class<T> namespaceType = findNamespaceType();

        return createNamespace(namespaceType);
    }

    public static <T extends Namespace> T createNamespace(
            Class<T> namespaceType) {
        StateNode node = new StateNode(namespaceType);

        return node.getNamespace(namespaceType);
    }

    @SuppressWarnings("unchecked")
    private Class<T> findNamespaceType() {
        ParameterizedType genericSuperclass = (ParameterizedType) getClass()
                .getGenericSuperclass();

        Class<?> paramType = (Class<?>) genericSuperclass
                .getActualTypeArguments()[0];

        Class<? extends Namespace> namespaceType = paramType
                .asSubclass(Namespace.class);

        return (Class<T>) namespaceType;
    }

    public List<NodeChange> collectChanges(Namespace namespace) {
        List<NodeChange> changes = new ArrayList<>();

        namespace.collectChanges(changes::add);

        return changes;
    }

    protected void assertNodeEquals(StateNode node1, StateNode node2) {
        Assert.assertEquals(node1.getId(), node2.getId());
        NamespaceRegistry.namespaces.keySet().forEach(k -> {
            Assert.assertEquals(node1.hasNamespace(k), node2.hasNamespace(k));
            if (node1.hasNamespace(k)) {
                assertNamespaceEquals(node1.getNamespace(k),
                        node2.getNamespace(k));
            }
        });

    }

    @SuppressWarnings("rawtypes")
    protected void assertNamespaceEquals(Namespace namespace1,
            Namespace namespace2) {
        Assert.assertEquals(namespace1.getClass(), namespace2.getClass());
        if (namespace1 instanceof MapNamespace) {
            assertMapNamespaceEquals((MapNamespace) namespace1,
                    (MapNamespace) namespace2);
        } else if (namespace1 instanceof ListNamespace) {
            assertListNamespaceEquals((ListNamespace) namespace1,
                    (ListNamespace) namespace2);
        } else {
            Assert.fail("Unknonwn namespace type "
                    + namespace1.getClass().getName());
        }
    }

    @SuppressWarnings("rawtypes")
    protected void assertListNamespaceEquals(ListNamespace namespace1,
            ListNamespace namespace2) {
        Assert.assertEquals(namespace1.size(), namespace2.size());
        for (int i = 0; i < namespace1.size(); i++) {
            Assert.assertEquals(namespace1.get(i), namespace2.get(i));
        }
    }

    protected void assertMapNamespaceEquals(MapNamespace namespace1,
            MapNamespace namespace2) {
        Assert.assertEquals(namespace1.keySet().size(),
                namespace2.keySet().size());
        namespace1.keySet().forEach(k -> {
            Assert.assertEquals(namespace1.get(k), namespace2.get(k));
        });
    }

}
