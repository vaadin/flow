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

package com.vaadin.flow.internal.nodefeature;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;

import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.change.NodeChange;
import com.vaadin.flow.internal.nodefeature.NodeFeature;
import com.vaadin.flow.internal.nodefeature.NodeFeatureRegistry;
import com.vaadin.flow.internal.nodefeature.NodeList;
import com.vaadin.flow.internal.nodefeature.NodeMap;

public abstract class AbstractNodeFeatureTest<T extends NodeFeature> {

    public T createFeature() {
        Class<T> featureType = findFeatureType();

        return createFeature(featureType);
    }

    public static <T extends NodeFeature> T createFeature(
            Class<T> featureType) {
        StateNode node = new StateNode(featureType);

        return node.getFeature(featureType);
    }

    @SuppressWarnings("unchecked")
    private Class<T> findFeatureType() {
        ParameterizedType genericSuperclass = (ParameterizedType) getClass()
                .getGenericSuperclass();

        Class<?> paramType = (Class<?>) genericSuperclass
                .getActualTypeArguments()[0];

        Class<? extends NodeFeature> featureType = paramType
                .asSubclass(NodeFeature.class);

        return (Class<T>) featureType;
    }

    public List<NodeChange> collectChanges(NodeFeature feature) {
        List<NodeChange> changes = new ArrayList<>();

        feature.collectChanges(changes::add);
        // Explicitly clear since collecting from one feature doesn't
        feature.getNode().clearChanges();

        return changes;
    }

    protected void assertNodeEquals(StateNode node1, StateNode node2) {
        Assert.assertEquals(node1.getId(), node2.getId());
        NodeFeatureRegistry.nodeFeatures.keySet().forEach(k -> {
            Assert.assertEquals(node1.hasFeature(k), node2.hasFeature(k));
            if (node1.hasFeature(k)) {
                assertFeaturesEquals(node1.getFeature(k), node2.getFeature(k));
            }
        });

    }

    @SuppressWarnings("rawtypes")
    protected void assertFeaturesEquals(NodeFeature feature1,
            NodeFeature feature2) {
        Assert.assertEquals(feature1.getClass(), feature2.getClass());
        if (feature1 instanceof NodeMap) {
            assertMapFeatureEquals((NodeMap) feature1, (NodeMap) feature2);
        } else if (feature1 instanceof NodeList) {
            assertListFeatureEquals((NodeList) feature1, (NodeList) feature2);
        } else {
            Assert.fail(
                    "Unknown feature type " + feature1.getClass().getName());
        }
    }

    @SuppressWarnings("rawtypes")
    protected void assertListFeatureEquals(NodeList feature1,
            NodeList feature2) {
        Assert.assertEquals(feature1.size(), feature2.size());
        for (int i = 0; i < feature1.size(); i++) {
            Assert.assertEquals(feature1.get(i), feature2.get(i));
        }
    }

    protected void assertMapFeatureEquals(NodeMap feature1, NodeMap feature2) {
        Assert.assertEquals(feature1.keySet().size(), feature2.keySet().size());
        feature1.keySet().forEach(k -> {
            Assert.assertEquals(feature1.get(k), feature2.get(k));
        });
    }

}
