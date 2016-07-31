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
package com.vaadin.client.hummingbird.model;

import com.vaadin.client.hummingbird.StateNode;
import com.vaadin.client.hummingbird.collection.JsArray;
import com.vaadin.client.hummingbird.collection.JsCollections;
import com.vaadin.client.hummingbird.nodefeature.NodeList;
import com.vaadin.hummingbird.shared.NodeFeatures;

import elemental.json.JsonObject;

/**
 * Model type representing a list.
 *
 * @author Vaadin Ltd
 */
public class ListModelType extends ModelType {

    private ModelType itemType;

    /**
     * Creates a list model type with the given item type.
     *
     * @param itemType
     *            the type of items in this list
     */
    public ListModelType(ModelType itemType) {
        this.itemType = itemType;
    }

    @Override
    public Object getJsRepresentation(Object value) {
        if (value == null) {
            return null;
        } else {
            assert value instanceof StateNode;
            StateNode node = (StateNode) value;

            assert node.hasFeature(
                    NodeFeatures.TEMPLATE_MODELLIST) : "Node does not have a model list namespace. "
                            + debugInfo(node);
            return createProxy(node.getList(NodeFeatures.TEMPLATE_MODELLIST));
        }
    }

    /**
     * Creates a JS proxy instance for accessing the given model list items in
     * this type.
     *
     * @param nodeList
     *            the model list
     * @return a JS proxy for the type
     */
    public Object createProxy(NodeList nodeList) {
        JsArray<Object> proxy = JsCollections.array();

        for (int i = 0; i < nodeList.length(); i++) {
            Object value = nodeList.get(i);
            proxy.set(i, itemType.getJsRepresentation(value));
        }

        return proxy;
    }
}
