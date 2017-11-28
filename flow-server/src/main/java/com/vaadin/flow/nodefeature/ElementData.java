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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import com.vaadin.flow.StateNode;
import com.vaadin.flow.change.EmptyChange;
import com.vaadin.flow.change.MapPutChange;
import com.vaadin.flow.change.NodeChange;

import elemental.json.JsonValue;

/**
 * Map of basic element information.
 *
 * @author Vaadin Ltd
 */
public class ElementData extends NodeValue<Serializable[]> {

    /**
     * Creates a new element data map for the given node.
     *
     * @param node
     *            the node that the map belongs to
     *
     */
    public ElementData(StateNode node) {
        super(node);
    }

    @Override
    protected String getKey() {
        return NodeProperties.TAG;
    }

    /**
     * Sets the tag name of the element.
     *
     * @param tag
     *            the tag name
     */
    public void setTag(String tag) {
        Serializable[] value = new Serializable[2];
        value[0] = tag;
        value[1] = getPyload();
        setValue(value);
    }

    /**
     * Gets the tag name of the element.
     *
     * @return the tag name
     */
    public String getTag() {
        return getValue() == null ? null : (String) getValue()[0];
    }

    /**
     * Sets the payload data of the element.
     *
     * @param payload
     *            the payload data
     */
    public void setPyload(JsonValue payload) {
        Serializable[] value = new Serializable[2];
        value[0] = getTag();
        value[1] = payload;
        setValue(value);
    }

    /**
     * Gets the payload data of the element.
     *
     */
    public JsonValue getPyload() {
        Serializable[] value = getValue();
        return value == null ? null : (JsonValue) value[1];
    }

    @Override
    public void collectChanges(Consumer<NodeChange> collector) {
        List<NodeChange> changes = new ArrayList<>(1);
        super.collectChanges(changes::add);

        Serializable[] previousValue;
        Serializable tracker = getNode().getChangeTracker(this, () -> null);

        if (tracker instanceof Serializable[]) {
            previousValue = (Serializable[]) tracker;
        } else {
            previousValue = new Serializable[2];
        }

        NodeChange change = changes.get(0);
        if (change instanceof MapPutChange) {
            if (!Objects.equals(previousValue[0], getTag())) {
                collector.accept(new MapPutChange(this, getKey(), getTag()));
            }
            if (!Objects.equals(previousValue[1], getPyload())) {
                collector.accept(new MapPutChange(this, NodeProperties.PAYLOAD,
                        getPyload()));
            }
        } else if (change instanceof EmptyChange) {
            collector.accept(change);
        }
    }
}
