/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal.nodefeature;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;

import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.change.NodeChange;

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
