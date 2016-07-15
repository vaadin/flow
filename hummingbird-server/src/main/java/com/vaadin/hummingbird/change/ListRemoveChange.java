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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import com.vaadin.hummingbird.ConstantPool;
import com.vaadin.hummingbird.nodefeature.NodeList;
import com.vaadin.hummingbird.util.JsonUtil;
import com.vaadin.shared.JsonConstants;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonNumber;
import elemental.json.JsonObject;

/**
 * Change describing a remove operation in a {@link NodeList list} node feature.
 *
 * @author Vaadin Ltd
 */
public class ListRemoveChange extends NodeFeatureChange {

    private List<Integer> removedIndices;

    /**
     * Creates a new list remove change.
     *
     * @param list
     *            the changed list
     */
    public ListRemoveChange(NodeList<?> list) {
        super(list);
    }

    /**
     * Gets the removed indices.
     * <p>
     * This method is public only for testing reasons.
     *
     * @return the removed indices as a unmodifiable list
     */
    public List<Integer> getRemovedIndices() {
        return Collections.unmodifiableList(removedIndices);
    }

    /**
     * Adds a new removed item index.
     *
     * @param newRemovedIndex
     *            the item index to add
     */
    public void add(Integer newRemovedIndex) {
        if (removedIndices == null) {
            removedIndices = new ArrayList<>(1);
        }
        removedIndices.add(newRemovedIndex);
    }

    @Override
    protected void populateJson(JsonObject json, ConstantPool constantPool) {
        if (removedIndices.isEmpty()) {
            return;
        }

        json.put(JsonConstants.CHANGE_TYPE, JsonConstants.CHANGE_TYPE_SPLICE);

        super.populateJson(json, constantPool);
        Function<Integer, JsonNumber> mapper = Json::create;
        JsonArray descendingIndices = removedIndices.stream().map(mapper)
                .collect(JsonUtil.asArray());
        json.put(JsonConstants.CHANGE_SPLICE_REMOVE, descendingIndices);
    }
}
