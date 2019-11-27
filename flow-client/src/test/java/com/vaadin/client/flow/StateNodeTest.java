/*
 * Copyright 2000-2019 Vaadin Ltd.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.client.flow.nodefeature.NodeFeature;
import com.vaadin.client.flow.nodefeature.NodeList;
import com.vaadin.client.flow.nodefeature.NodeMap;

public class StateNodeTest {
    private StateNode node = new StateNode(1, new StateTree(null));

    @Test
    public void testDefaultNoFeatures() {
        node.forEachFeature((ns, id) -> Assert.fail());
    }

    private static class TestData {

    }

    @Test
    public void testGetListFeature() {
        NodeList list = node.getList(1);

        Assert.assertEquals(1, list.getId());

        List<NodeFeature> features = collectFeatures();

        Assert.assertEquals(Arrays.asList(list), features);

        NodeList anotherList = node.getList(1);

        Assert.assertSame(anotherList, list);
        Assert.assertEquals(features, collectFeatures());
    }

    private List<NodeFeature> collectFeatures() {
        List<NodeFeature> features = new ArrayList<>();
        node.forEachFeature((ns, id) -> features.add(ns));
        return features;
    }

    @Test
    public void testGetMapFeature() {
        NodeMap map = node.getMap(1);

        Assert.assertEquals(1, map.getId());

        List<NodeFeature> features = collectFeatures();

        Assert.assertEquals(Arrays.asList(map), features);

        NodeMap anotherMap = node.getMap(1);

        Assert.assertSame(anotherMap, map);
    }

    @Test
    public void setNodeData_getNodeData_retrievedInstanceIsTheSame() {
        TestData data = new TestData();
        node.setNodeData(data);
        Assert.assertEquals(data, node.getNodeData(TestData.class));
    }
}
