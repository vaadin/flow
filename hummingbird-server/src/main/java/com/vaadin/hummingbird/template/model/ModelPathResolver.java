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
package com.vaadin.hummingbird.template.model;

import java.io.Serializable;
import java.util.regex.Pattern;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.dom.impl.TemplateElementStateProvider;
import com.vaadin.hummingbird.nodefeature.ModelMap;
import com.vaadin.hummingbird.nodefeature.NodeFeature;

/**
 * Resolver for finding the state node or model map for a given model path.
 */
public class ModelPathResolver {
    private static final Pattern DOT_PATTERN = Pattern.compile("\\.");
    private String[] modelPathParts;
    private boolean pathContainsPropertyName;

    /**
     * Constructs a representation of the given model path.
     *
     * @param modelPath
     *            the path, in the format {@literal path1.path2} or
     *            {@literal path1.path2.propertyName}, depending on
     *            <code>containsPropertyName</code>
     * @param pathContainsPropertyName
     *            <code>true</code> if <code>modelPath</code> ends with a
     *            property, <code>false</code> if modelPath contains no property
     *            information
     */
    private ModelPathResolver(String modelPath,
            boolean pathContainsPropertyName) {
        this.pathContainsPropertyName = pathContainsPropertyName;
        if (modelPath.endsWith(".")) {
            throw new IllegalArgumentException(
                    "The model path must not end with a dot");
        }
        if ("".equals(modelPath)) {
            if (pathContainsPropertyName) {
                throw new IllegalArgumentException("The given model path \""
                        + modelPath
                        + "\" denotes a property and must therefore contain a dot");
            }
            modelPathParts = new String[0];
        } else {
            modelPathParts = DOT_PATTERN.split(modelPath);
        }
    }

    /**
     * Creates a new resolver for the given path of type
     * {@literal path1.path2.path3}.
     *
     * @param path
     *            the model path, without property information
     * @return a resolver for the given path
     */
    public static ModelPathResolver forPath(String path) {
        return new ModelPathResolver(path, false);
    }

    /**
     * Creates a new resolver for the given path of type
     * {@literal parent1.parent2.property}.
     *
     * @param pathWithProperty
     *            the model path, ending with a property name
     * @return a resolver for the given path
     */
    public static ModelPathResolver forProperty(String pathWithProperty) {
        return new ModelPathResolver(pathWithProperty, true);
    }

    /**
     * Resolves the model path starting from the given stateNode. Returns the
     * {@link ModelMap} which contains the property defined by the path, i.e.
     * uses all parts up until the last "." in the path.
     *
     * @param stateNode
     *            the state node to start resolving from
     * @return the model map of the resolved node
     */
    public ModelMap resolveModelMap(StateNode stateNode) {
        Class<ModelMap> childFeature = ModelMap.class;
        StateNode node = stateNode;
        int lastIndex = modelPathParts.length - 1;
        if (pathContainsPropertyName) {
            lastIndex--;
        }

        for (int i = 0; i <= lastIndex; i++) {
            node = resolveStateNode(node, modelPathParts[i], childFeature);
        }
        return node.getFeature(childFeature);
    }

    /**
     * Gets the the property name, i.e. the last part of the path.
     *
     * @return the property name
     */
    public String getPropertyName() {
        if (!pathContainsPropertyName) {
            throw new IllegalStateException(
                    "The resolver was created with only a path");
        }

        return modelPathParts[modelPathParts.length - 1];
    }

    /**
     * Finds a child node with the given name (one part of a model path) inside
     * the given parent node.
     * <p>
     * Creates a new node with the given feature if no node is found or if a
     * node is found but it does not have the correct feature.
     *
     * @param parentNode
     *            The parent state node
     * @param childNodeName
     *            The name of the child node
     * @param childFeature
     *            The feature to require from the child node
     * @return a state node, old or new, with the required feature
     */
    public static StateNode resolveStateNode(StateNode parentNode,
            String childNodeName, Class<? extends NodeFeature> childFeature) {
        assert !childNodeName.contains(".");

        ModelMap parentLevel = parentNode.getFeature(ModelMap.class);
        if (parentLevel.hasValue(childNodeName)) {
            Serializable value = parentLevel.getValue(childNodeName);
            if (value instanceof StateNode
                    && ((StateNode) value).hasFeature(childFeature)) {
                // reuse old one
                return (StateNode) value;
            } else {
                // just override
                return createSubModel(parentLevel, childNodeName, childFeature);
            }
        } else {
            return createSubModel(parentLevel, childNodeName, childFeature);
        }
    }

    /**
     * Creates a sub model node with the given model feature and attaches it to
     * the parent with the given name.
     *
     * @param parent
     *            the parent model map
     * @param propertyName
     *            the name to use when attaching
     * @param childFeature
     *            the feature (ModelMap or ModelList) to use
     * @return a new state node with the given feature
     */
    private static StateNode createSubModel(ModelMap parent,
            String propertyName, Class<? extends NodeFeature> childFeature) {
        StateNode node = TemplateElementStateProvider
                .createSubModelNode(childFeature);
        parent.setValue(propertyName, node);
        return node;
    }

}
