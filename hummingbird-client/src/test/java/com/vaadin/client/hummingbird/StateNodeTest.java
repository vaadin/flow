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
package com.vaadin.client.hummingbird;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.client.hummingbird.namespace.AbstractNamespace;
import com.vaadin.client.hummingbird.namespace.ListNamespace;
import com.vaadin.client.hummingbird.namespace.MapNamespace;

public class StateNodeTest {
    private StateNode node = new StateNode(1, new StateTree());

    @Test
    public void testDefaultNoNamespaces() {
        node.forEachNamespace((ns, id) -> Assert.fail());
    }

    @Test
    public void testGetListNamespace() {
        ListNamespace namespace = node.getListNamespace(1);

        Assert.assertEquals(1, namespace.getId());

        List<AbstractNamespace> namespaces = collectNamespaces();

        Assert.assertEquals(Arrays.asList(namespace), namespaces);

        ListNamespace anotherNamespace = node.getListNamespace(1);

        Assert.assertSame(anotherNamespace, namespace);
        Assert.assertEquals(namespaces, collectNamespaces());
    }

    private List<AbstractNamespace> collectNamespaces() {
        List<AbstractNamespace> namespaces = new ArrayList<>();
        node.forEachNamespace((ns, id) -> namespaces.add(ns));
        return namespaces;
    }

    @Test
    public void testGetMapNamespace() {
        MapNamespace namespace = node.getMapNamespace(1);

        Assert.assertEquals(1, namespace.getId());

        List<AbstractNamespace> namespaces = collectNamespaces();

        Assert.assertEquals(Arrays.asList(namespace), namespaces);

        MapNamespace anotherNamespace = node.getMapNamespace(1);

        Assert.assertSame(anotherNamespace, namespace);
    }
}
