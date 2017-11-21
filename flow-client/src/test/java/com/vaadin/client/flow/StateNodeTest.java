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
package com.vaadin.client.flow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.vaadin.client.flow.nodefeature.NodeFeature;
import com.vaadin.client.flow.nodefeature.NodeList;
import com.vaadin.client.flow.nodefeature.NodeMap;

public class StateNodeTest {
    private StateNode node = new StateNode(1, new StateTree(null));

    @Test
    public void testDefaultNoFeatures() {
        node.forEachFeature((ns, id) -> fail());
    }

    @Test
    public void testGetListFeature() {
        NodeList list = node.getList(1);

        assertEquals(1, list.getId());

        List<NodeFeature> features = collectFeatures();

        assertEquals(Arrays.asList(list), features);

        NodeList anotherList = node.getList(1);

        assertSame(anotherList, list);
        assertEquals(features, collectFeatures());
    }

    private List<NodeFeature> collectFeatures() {
        List<NodeFeature> features = new ArrayList<>();
        node.forEachFeature((ns, id) -> features.add(ns));
        return features;
    }

    @Test
    public void testGetMapFeature() {
        NodeMap map = node.getMap(1);

        assertEquals(1, map.getId());

        List<NodeFeature> features = collectFeatures();

        assertEquals(Arrays.asList(map), features);

        NodeMap anotherMap = node.getMap(1);

        assertSame(anotherMap, map);
    }
}
