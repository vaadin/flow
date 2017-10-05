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
import java.util.Collections;
import java.util.stream.Stream;

import com.vaadin.flow.StateNode;

/**
 * Map for model values used in data binding in templates.
 *
 * @author Vaadin Ltd
 */
public class ModelMap extends NodeMap {

    /**
     * Creates an instance of this node feature.
     *
     * @param node
     *            the node that the feature belongs to
     */
    public ModelMap(StateNode node) {
        super(node);
    }

    /**
     * Sets the {@code value} for the specified {@code key}.
     *
     * @param key
     *            key with which the specified value is to be associated, not
     *            {@code null}
     * @param value
     *            value to be associated with the specified key
     */
    public void setValue(String key, Serializable value) {
        assert key != null;
        if (key.contains(".")) {
            throw new IllegalArgumentException(
                    "Model map key may not contain dots");
        }

        if (value instanceof Double) {
            Double doubleValue = (Double) value;
            if (doubleValue.isInfinite() || doubleValue.isNaN()) {
                throw new IllegalArgumentException(
                        "NaN and infinity are not supported");
            }
        }

        put(key, value);
    }

    /**
     * Gets the value corresponding to the given key.
     *
     * @param key
     *            the key to get a value for
     * @return the value corresponding to the key; <code>null</code> if there is
     *         no value stored, or if <code>null</code> is stored as a value
     */
    public Serializable getValue(String key) {
        return (Serializable) get(key);
    }

    /**
     * Checks whether a value is stored for the given key.
     * <p>
     * If method {@link #setValue(String, Serializable)} has never been called
     * for the {@code key} then {@code false} is returned. Otherwise (even if it
     * has been called with {@code null} as a value) it returns {@code true}. It
     * means that {@link #getValue(String)} may return {@code null} at the same
     * time when {@link #hasValue(String)} returns {@code true}.
     *
     * @see #setValue(String, Serializable)
     *
     * @param key
     *            the key to check a value for
     * @return <code>true</code> if there is a value stored; <code>false</code>
     *         if no value is stored
     */
    public boolean hasValue(String key) {
        return super.contains(key);
    }

    /**
     * Gets the keys for which values have been defined.
     *
     * @see #hasValue(String)
     *
     * @return a stream of keys
     */
    public Stream<String> getKeys() {
        return super.keySet().stream();
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
    public static ModelMap get(StateNode node) {
        assert node != null;
        return node.getFeature(ModelMap.class);
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
    private ModelMap getOrCreateModelMap(String key) {
        Serializable value = getValue(key);
        if (value == null) {
            value = new StateNode(Collections.singletonList(ModelMap.class));
            setValue(key, value);
        }

        assert value instanceof StateNode;
        assert ((StateNode) value).hasFeature(ModelMap.class);
        return ((StateNode) value).getFeature(ModelMap.class);
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
        Serializable value = getValue(key);
        if (value == null) {
            value = new StateNode(Collections.singletonList(ModelList.class));
            setValue(key, value);
        }

        assert value instanceof StateNode;
        assert ((StateNode) value).hasFeature(ModelList.class);
        return ((StateNode) value).getFeature(ModelList.class);
    }

    /**
     * Resolves the {@link ModelMap} that the model path refers to.
     * <p>
     * If the model path contains separate dot separated parts, any non-existing
     * part will be created during resolving.
     *
     * @param modelPath
     *            the path to resolve, either a single property name or a dot
     *            separated path
     * @return the resolved model map
     */
    public ModelMap resolveModelMap(String modelPath) {
        if ("".equals(modelPath)) {
            return this;
        }
        return resolve(modelPath, ModelMap.class);
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
     * Resolves the {@link ModelList} or {@link ModelMap} that the model path
     * refers to.
     * <p>
     * If the model path contains separate dot separated parts, any non-existing
     * part will be created during resolving.
     *
     * @param modelPath
     *            the path to resolve, either a single property name or a dot
     *            separated path
     * @param leafType
     *            the type of feature to resolve, {@link ModelList} or
     *            {@link ModelMap}
     * @return the resolved model list or map
     */
    @SuppressWarnings("unchecked")
    private <T extends NodeFeature> T resolve(String modelPath,
            Class<T> leafType) {
        assert modelPath != null;
        assert !"".equals(modelPath);
        assert !modelPath.startsWith(".");
        assert !modelPath.endsWith(".");
        assert leafType == ModelMap.class || leafType == ModelList.class;

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
            ModelMap subMap = getOrCreateModelMap(firstKey);
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

    @Override
    protected boolean mayUpdateFromClient(String key, Serializable value) {
        // Allow everything for now. Should figure out a sensible way of
        // defining what's allowed separately.
        return true;
    }

}
