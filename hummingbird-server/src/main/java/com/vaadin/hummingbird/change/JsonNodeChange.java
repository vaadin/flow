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

package com.vaadin.hummingbird.change;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.shared.JsonConstants;

import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * Base class describing a change to a state node.
 *
 * @since
 * @author Vaadin Ltd
 */
public abstract class JsonNodeChange extends NodeChange {
    private final StateNode node;

    /**
     * Creates a new change for the given node.
     *
     * @param node
     *            the changed node
     */
    public JsonNodeChange(StateNode node) {
        this.node = node;
    }

    /**
     * Gets the changed node.
     *
     * @return the node
     */
    public StateNode getNode() {
        return node;
    }

    /**
     * Serializes this change to JSON.
     *
     * @return a json representation of this change
     */
    public JsonObject toJson() {
        JsonObject json = Json.createObject();

        json.put(JsonConstants.CHANGE_NODE, node.getId());

        populateJson(json);

        return json;
    }

    @Override
    public void visit(ChangeVisitor visitor) {
        visitor.visit(this);
    }

    /**
     * Overridden by subclasses to populate a JSON object when serializing.
     *
     * @param json
     *            the json object to populate
     */
    protected abstract void populateJson(JsonObject json);
}
