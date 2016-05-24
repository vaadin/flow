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
package com.vaadin.hummingbird.nodefeature;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.nodefeature.PushConfigurationMap.PushConfigurationParametersMap;

/**
 * A registry of node features that are available based on type.
 *
 * @author Vaadin Ltd
 */
public class NodeFeatureRegistry {
    private static int nextNodeFeatureId = 0;

    // Non-private for testing purposes
    static final Map<Class<? extends NodeFeature>, NodeFeatureData> nodeFeatures = new HashMap<>();

    private static class NodeFeatureData {
        private Function<StateNode, ? extends NodeFeature> factory;
        private int id = nextNodeFeatureId++;

        private <T extends NodeFeature> NodeFeatureData(
                Function<StateNode, T> factory) {
            this.factory = factory;
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
        registerFeature(TemplateMap.class, TemplateMap::new);
        registerFeature(ModelMap.class, ModelMap::new);
        registerFeature(TemplateOverridesMap.class, TemplateOverridesMap::new);
        registerFeature(OverrideElementData.class, OverrideElementData::new);
        registerFeature(ParentGeneratorHolder.class,
                ParentGeneratorHolder::new);
        registerFeature(ModelList.class, ModelList::new);
        registerFeature(TemplateEventHandlerNames.class,
                TemplateEventHandlerNames::new);
        registerFeature(TemplateModelData.class, TemplateModelData::new);
    }

    private NodeFeatureRegistry() {
        // Static only
    }

    private static <T extends NodeFeature> void registerFeature(Class<T> type,
            Function<StateNode, T> factory) {
        nodeFeatures.put(type, new NodeFeatureData(factory));
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
}
