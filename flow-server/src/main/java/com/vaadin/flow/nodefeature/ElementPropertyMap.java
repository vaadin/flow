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

package com.vaadin.flow.nodefeature;

import java.io.Serializable;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.StateNode;

/**
 * Map for element property values.
 *
 * @author Vaadin Ltd
 */
public class ElementPropertyMap extends AbstractPropertyMap {

    private static final Set<String> forbiddenProperties = Stream
            .of("textContent", "classList", "className")
            .collect(Collectors.toSet());

    /**
     * Creates a new element property map for the given node.
     *
     * @param node
     *            the node that the map belongs to
     */
    public ElementPropertyMap(StateNode node) {
        super(node);
    }

    @Override
    public void setProperty(String name, Serializable value,
            boolean emitChange) {
        assert !forbiddenProperties.contains(name) : "Forbidden property name: "
                + name;

        super.setProperty(name, value, emitChange);
    }

    public void setProperty(String name, Serializable value) {
        setProperty(name, value, true);
    }

    @Override
    protected boolean mayUpdateFromClient(String key, Serializable value) {
        return !forbiddenProperties.contains(key);
    }

    /**
     * Gets a model map using the given key.
     * <p>
     * If the key is not mapped to a value, creates a model map for the key.
     *
     * @param key
     *            the key to use for the lookup
     * @return a model map attached to the given key, possibly created in this
     *         method
     */
    private ElementPropertyMap getOrCreateModelMap(String key) {
        Serializable value = getProperty(key);
        if (value == null) {
            value = new StateNode(ElementPropertyMap.class);
            setProperty(key, value);
        }

        assert value instanceof StateNode;
        assert ((StateNode) value).hasFeature(ElementPropertyMap.class);
        return ((StateNode) value).getFeature(ElementPropertyMap.class);
    }

    /**
     * Gets a model list using the given key.
     * <p>
     * If the key is not mapped to a value, creates a model list for the key.
     *
     * @param key
     *            the key to use for the lookup
     * @return a model list attached to the given key, possibly created in this
     *         method
     */
    private ModelList getOrCreateModelList(String key) {
        Serializable value = getProperty(key);
        if (value == null) {
            value = new StateNode(ModelList.class);
            setProperty(key, value);
        }

        assert value instanceof StateNode;
        assert ((StateNode) value).hasFeature(ModelList.class);
        return ((StateNode) value).getFeature(ModelList.class);
    }

    /**
     * Resolves the {@link ElementPropertyMap} that the model path refers to.
     * <p>
     * If the model path contains separate dot separated parts, any non-existing
     * part will be created during resolving.
     *
     * @param modelPath
     *            the path to resolve, either a single property name or a dot
     *            separated path
     * @return the resolved model map
     */
    public ElementPropertyMap resolveModelMap(String modelPath) {
        if ("".equals(modelPath)) {
            return this;
        }
        return resolve(modelPath, ElementPropertyMap.class);
    }

    /**
     * Resolves the {@link ModelList} that the model path refers to.
     * <p>
     * If the model path contains separate dot separated parts, any non-existing
     * part will be created during resolving.
     *
     * @param modelPath
     *            the path to resolve, either a single property name or a dot
     *            separated path
     * @return the resolved model list
     */
    public ModelList resolveModelList(String modelPath) {
        return resolve(modelPath, ModelList.class);
    }

    /**
     * Resolves the {@link ModelList} or {@link ElementPropertyMap} that the
     * model path refers to.
     * <p>
     * If the model path contains separate dot separated parts, any non-existing
     * part will be created during resolving.
     *
     * @param modelPath
     *            the path to resolve, either a single property name or a dot
     *            separated path
     * @param leafType
     *            the type of feature to resolve, {@link ModelList} or
     *            {@link ElementPropertyMap}
     * @return the resolved model list or map
     */
    @SuppressWarnings("unchecked")
    private <T extends NodeFeature> T resolve(String modelPath,
            Class<T> leafType) {
        assert modelPath != null;
        assert !"".equals(modelPath);
        assert !modelPath.startsWith(".");
        assert !modelPath.endsWith(".");
        assert leafType == ElementPropertyMap.class
                || leafType == ModelList.class;

        int dotLocation = modelPath.indexOf('.');
        if (dotLocation == -1) {
            if (leafType == ModelMap.class) {
                return (T) getOrCreateModelMap(modelPath);
            } else {
                return (T) getOrCreateModelList(modelPath);
            }
        } else {
            String firstKey = modelPath.substring(0, dotLocation);
            String remainingPath = modelPath.substring(dotLocation + 1);
            ElementPropertyMap subMap = getOrCreateModelMap(firstKey);
            return subMap.resolve(remainingPath, leafType);
        }
    }

    /**
     * Gets the last part of a dot separated model path.
     *
     * @param modelPath
     *            the model path
     * @return the last part of the model path
     */
    public static String getLastPart(String modelPath) {
        int dotLocation = modelPath.lastIndexOf('.');
        if (dotLocation == -1) {
            return modelPath;
        } else {
            return modelPath.substring(dotLocation + 1);
        }
    }

    /**
     * Gets the model map for the given node.
     * <p>
     * Throws an exception if the node does not have a model map.
     *
     * @param node
     *            the node which has a model map
     * @return the model map for the node
     */
    public static ElementPropertyMap getModel(StateNode node) {
        assert node != null;
        return node.getFeature(ElementPropertyMap.class);
    }
}
