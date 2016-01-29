/*
 * Copyright 2000-2014 Vaadin Ltd.
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
package com.vaadin.client.hummingbird;

import com.vaadin.client.hummingbird.collection.JsCollections;
import com.vaadin.client.hummingbird.collection.JsMap;

public class StateTree {
    // Double instead of Integer since GWT 2.8 doesn't box doubles
    private final JsMap<Double, StateNode> idToNode = JsCollections.map();

    private final StateNode rootNode = new StateNode(0, this);

    public StateTree() {
        registerNode(rootNode);
    }

    public void registerNode(StateNode node) {
        assert node != null;
        assert node.getTree() == this;

        Double key = Double.valueOf(node.getId());
        assert !idToNode.has(key) : "Node " + key + " is already registered";

        idToNode.set(key, node);
    }

    public StateNode getNode(int id) {
        Double key = Double.valueOf(id);

        return idToNode.get(key);
    }

    public StateNode getRootNode() {
        return rootNode;
    }
}
