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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.PushConfigurationMap.PushConfigurationParametersMap;

/**
 * A registry of node features that are available based on type.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class NodeFeatureRegistry {
    private static int nextNodePriority = 0;

    // Non-private for testing purposes
    static final Map<Class<? extends NodeFeature>, NodeFeatureData> nodeFeatures = new HashMap<>();
    private static final Map<Integer, Class<? extends NodeFeature>> idToFeature = new HashMap<>();

    /**
     * Comparator for finding the priority order between node feature types.
     */
    public static final Comparator<Class<? extends NodeFeature>> PRIORITY_COMPARATOR = Comparator
            .comparingInt(feature -> getData(feature).priority);

    private static class NodeFeatureData implements Serializable {
        private final SerializableFunction<StateNode, ? extends NodeFeature> factory;
        private final int id;
        private final int priority;

        private <T extends NodeFeature> NodeFeatureData(
                SerializableFunction<StateNode, T> factory, int id) {
            this.factory = factory;
            this.id = id;
            priority = nextNodePriority++;
        }
    }

    static {
        /* Primary features */
        registerFeature(ElementData.class, ElementData::new,
                NodeFeatures.ELEMENT_DATA);
        registerFeature(TextNodeMap.class, TextNodeMap::new,
                NodeFeatures.TEXT_NODE);
        registerFeature(ModelList.class, ModelList::new,
                NodeFeatures.TEMPLATE_MODELLIST);
        registerFeature(BasicTypeValue.class, BasicTypeValue::new,
                NodeFeatures.BASIC_TYPE_VALUE);

        /* Common element features */
        registerFeature(ElementChildrenList.class, ElementChildrenList::new,
                NodeFeatures.ELEMENT_CHILDREN);
        registerFeature(ElementPropertyMap.class, ElementPropertyMap::new,
                NodeFeatures.ELEMENT_PROPERTIES);

        /* Component mapped features */
        registerFeature(ComponentMapping.class, ComponentMapping::new,
                NodeFeatures.COMPONENT_MAPPING);
        registerFeature(ClientCallableHandlers.class,
                ClientCallableHandlers::new,
                NodeFeatures.CLIENT_DELEGATE_HANDLERS);

        /* Supplementary element stuff */
        registerFeature(ElementClassList.class, ElementClassList::new,
                NodeFeatures.CLASS_LIST);
        registerFeature(ElementAttributeMap.class, ElementAttributeMap::new,
                NodeFeatures.ELEMENT_ATTRIBUTES);
        registerFeature(ElementListenerMap.class, ElementListenerMap::new,
                NodeFeatures.ELEMENT_LISTENERS);
        registerFeature(SynchronizedPropertiesList.class,
                SynchronizedPropertiesList::new,
                NodeFeatures.SYNCHRONIZED_PROPERTIES);
        registerFeature(SynchronizedPropertyEventsList.class,
                SynchronizedPropertyEventsList::new,
                NodeFeatures.SYNCHRONIZED_PROPERTY_EVENTS);
        registerFeature(VirtualChildrenList.class, VirtualChildrenList::new,
                NodeFeatures.VIRTUAL_CHILDREN);

        /* Supplementary generic stuff */
        registerFeature(ReturnChannelMap.class, ReturnChannelMap::new,
                NodeFeatures.RETURN_CHANNEL_MAP);

        /* PolymerTemplate stuff */
        registerFeature(PolymerEventListenerMap.class,
                PolymerEventListenerMap::new,
                NodeFeatures.POLYMER_EVENT_LISTENERS);
        registerFeature(PolymerServerEventHandlers.class,
                PolymerServerEventHandlers::new,
                NodeFeatures.POLYMER_SERVER_EVENT_HANDLERS);

        /* Rarely used element stuff */
        registerFeature(ElementStylePropertyMap.class,
                ElementStylePropertyMap::new,
                NodeFeatures.ELEMENT_STYLE_PROPERTIES);
        registerFeature(ShadowRootData.class, ShadowRootData::new,
                NodeFeatures.SHADOW_ROOT_DATA);
        registerFeature(ShadowRootHost.class, ShadowRootHost::new,
                NodeFeatures.SHADOW_ROOT_HOST);
        registerFeature(AttachExistingElementFeature.class,
                AttachExistingElementFeature::new,
                NodeFeatures.ATTACH_EXISTING_ELEMENT);

        /* Only used for the root node */
        registerFeature(PushConfigurationMap.class, PushConfigurationMap::new,
                NodeFeatures.UI_PUSHCONFIGURATION);
        registerFeature(PushConfigurationParametersMap.class,
                PushConfigurationParametersMap::new,
                NodeFeatures.UI_PUSHCONFIGURATION_PARAMETERS);
        registerFeature(LoadingIndicatorConfigurationMap.class,
                LoadingIndicatorConfigurationMap::new,
                NodeFeatures.LOADING_INDICATOR_CONFIGURATION);
        registerFeature(PollConfigurationMap.class, PollConfigurationMap::new,
                NodeFeatures.POLL_CONFIGURATION);
        registerFeature(ReconnectDialogConfigurationMap.class,
                ReconnectDialogConfigurationMap::new,
                NodeFeatures.RECONNECT_DIALOG_CONFIGURATION);
    }

    private NodeFeatureRegistry() {
        // Static only
    }

    private static <T extends NodeFeature> void registerFeature(Class<T> type,
            SerializableFunction<StateNode, T> factory, int id) {
        NodeFeatureData featureData = new NodeFeatureData(factory, id);
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
