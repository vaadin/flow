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
package com.vaadin.hummingbird.dom.impl;

import java.io.Serializable;
import java.util.stream.Stream;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.nodefeature.ClientDelegateHandlers;
import com.vaadin.hummingbird.nodefeature.ComponentMapping;
import com.vaadin.hummingbird.nodefeature.ElementAttributeMap;
import com.vaadin.hummingbird.nodefeature.ElementChildrenList;
import com.vaadin.hummingbird.nodefeature.ElementClassList;
import com.vaadin.hummingbird.nodefeature.ElementData;
import com.vaadin.hummingbird.nodefeature.ElementListenerMap;
import com.vaadin.hummingbird.nodefeature.ElementStylePropertyMap;
import com.vaadin.hummingbird.nodefeature.ModelMap;
import com.vaadin.hummingbird.nodefeature.NodeFeature;
import com.vaadin.hummingbird.nodefeature.ParentGeneratorHolder;
import com.vaadin.hummingbird.nodefeature.PolymerEventListenerMap;
import com.vaadin.hummingbird.nodefeature.PolymerServerEventHandlers;
import com.vaadin.hummingbird.nodefeature.SynchronizedPropertiesList;
import com.vaadin.hummingbird.nodefeature.SynchronizedPropertyEventsList;

/**
 * Polymer element state provider which stores element data and model data via
 * shared model map.
 * 
 * @author Vaadin Ltd
 *
 */
public final class PolymerElementStateProvider
        extends AbstractElementStateProvider {

    private static final PolymerElementStateProvider INSTANCE = new PolymerElementStateProvider();

    @SuppressWarnings("unchecked")
    private static final Class<? extends NodeFeature>[] FEATURES = new Class[] {
            ElementData.class, ElementAttributeMap.class,
            ElementChildrenList.class, ElementListenerMap.class,
            ElementClassList.class, ElementStylePropertyMap.class,
            SynchronizedPropertiesList.class,
            SynchronizedPropertyEventsList.class, ComponentMapping.class,
            ParentGeneratorHolder.class, PolymerServerEventHandlers.class,
            ClientDelegateHandlers.class, PolymerEventListenerMap.class,
            ModelMap.class };

    private PolymerElementStateProvider() {
        // Singleton
    }

    /**
     * Gets the one and only instance.
     *
     * @return the instance to use for all basic elements
     */
    public static PolymerElementStateProvider get() {
        return INSTANCE;
    }

    @Override
    public Object getProperty(StateNode node, String name) {
        assert node != null;
        assert name != null;

        return getPropertyFeature(node).getValue(name);
    }

    @Override
    public void setProperty(StateNode node, String name, Serializable value,
            boolean emitChange) {
        assert node != null;
        assert name != null;

        getPropertyFeature(node).setValue(name, value, emitChange);

    }

    @Override
    public void removeProperty(StateNode node, String name) {
        assert node != null;
        assert name != null;

        getPropertyFeature(node).remove(name);
    }

    @Override
    public boolean hasProperty(StateNode node, String name) {
        assert node != null;
        assert name != null;

        return getPropertyFeature(node).hasValue(name);
    }

    @Override
    public Stream<String> getPropertyNames(StateNode node) {
        assert node != null;

        return getPropertyFeature(node).getKeys();
    }

    @Override
    protected Class<? extends NodeFeature>[] getProviderFeatures() {
        return FEATURES;
    }

    private ModelMap getPropertyFeature(StateNode node) {
        return node.getFeature(ModelMap.class);
    }

}
