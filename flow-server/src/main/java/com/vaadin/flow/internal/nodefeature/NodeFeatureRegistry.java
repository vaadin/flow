/*
 * Copyright 2000-2018 Vaadin Ltd.
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.PushConfigurationMap.PushConfigurationParametersMap;

/**
 * A registry of node features that are available based on type.
 *
 * @author Vaadin Ltd
 */
public class NodeFeatureRegistry {
    private static int nextNodeFeatureId = 0;

    // Non-private for testing purposes
    static final Map<Class<? extends NodeFeature>, NodeFeatureData> nodeFeatures = new HashMap<>();
    private static final Map<Integer, Class<? extends NodeFeature>> idToFeature = new HashMap<>();

    private static class NodeFeatureData implements Serializable {
        private final SerializableFunction<StateNode, ? extends NodeFeature> factory;
        private final int id;

        private <T extends NodeFeature> NodeFeatureData(
                SerializableFunction<StateNode, T> factory) {
            this.factory = factory;
            id = nextNodeFeatureId++;
        }
    }

    static {
        registerFeature(ElementData.class, ElementData::new);
        registerFeature(ElementPropertyMap.class, ElementPropertyMap::new);
        registerFeature(ElementChildrenList.class, ElementChildrenList::new);
        registerFeature(ElementAttributeMap.class, ElementAttributeMap::new);
        registerFeature(ElementListenerMap.class, ElementListenerMap::new);
        registerFeature(PushConfigurationMap.class, PushConfigurationMap::new);
        registerFeature(PushConfigurationParametersMap.class,
                PushConfigurationParametersMap::new);
        registerFeature(TextNodeMap.class, TextNodeMap::new);
        registerFeature(PollConfigurationMap.class, PollConfigurationMap::new);
        registerFeature(ReconnectDialogConfigurationMap.class,
                ReconnectDialogConfigurationMap::new);
        registerFeature(LoadingIndicatorConfigurationMap.class,
                LoadingIndicatorConfigurationMap::new);
        registerFeature(ElementClassList.class, ElementClassList::new);
        registerFeature(ElementStylePropertyMap.class,
                ElementStylePropertyMap::new);
        registerFeature(SynchronizedPropertiesList.class,
                SynchronizedPropertiesList::new);
        registerFeature(SynchronizedPropertyEventsList.class,
                SynchronizedPropertyEventsList::new);
        registerFeature(ComponentMapping.class, ComponentMapping::new);
        registerFeature(ModelList.class, ModelList::new);
        registerFeature(PolymerServerEventHandlers.class,
                PolymerServerEventHandlers::new);
        registerFeature(PolymerEventListenerMap.class,
                PolymerEventListenerMap::new);
        registerFeature(ClientCallableHandlers.class,
                ClientCallableHandlers::new);
        registerFeature(ShadowRootData.class, ShadowRootData::new);
        registerFeature(ShadowRootHost.class, ShadowRootHost::new);
        registerFeature(AttachExistingElementFeature.class,
                AttachExistingElementFeature::new);
        registerFeature(BasicTypeValue.class, BasicTypeValue::new);
        registerFeature(VirtualChildrenList.class, VirtualChildrenList::new);
    }

    private NodeFeatureRegistry() {
        // Static only
    }

    private static <T extends NodeFeature> void registerFeature(Class<T> type,
            SerializableFunction<StateNode, T> factory) {
        NodeFeatureData featureData = new NodeFeatureData(factory);
        nodeFeatures.put(type, featureData);
        idToFeature.put(featureData.id, type);
    }

    /**
     * Creates a feature of the given type for a node.
     *
     * @param nodeFeatureType
     *            the type of the feature to create
     * @param node
     *            the node for which the feature should be created
     * @return a newly created feature
     */
    public static NodeFeature create(
            Class<? extends NodeFeature> nodeFeatureType, StateNode node) {
        assert node != null;

        return getData(nodeFeatureType).factory.apply(node);
    }

    /**
     * Gets the id of a node feature.
     *
     * @param nodeFeature
     *            the node feature
     * @return the id of the node feature
     */
    public static int getId(Class<? extends NodeFeature> nodeFeature) {
        return getData(nodeFeature).id;
    }

    /**
     * Gets all registered feature types.
     *
     * @return an unmodifiable collection of feature types, not
     *         <code>null</code>
     */
    public static Collection<Class<? extends NodeFeature>> getFeatures() {
        return Collections.unmodifiableCollection(nodeFeatures.keySet());
    }

    private static NodeFeatureData getData(
            Class<? extends NodeFeature> nodeFeature) {
        assert nodeFeature != null;

        NodeFeatureData data = nodeFeatures.get(nodeFeature);

        assert data != null : "Feature " + nodeFeature.getName()
                + " has not been registered in NodeFeatureRegistry";

        return data;
    }

    /**
     * Finds the node feature type corresponding to the give node feature id.
     *
     * @param featureId
     *            the feature id for which to get a node feature type
     *
     * @return the node feature type
     */
    public static Class<? extends NodeFeature> getFeature(int featureId) {
        return idToFeature.get(featureId);
    }
}
