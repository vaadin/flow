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
package com.vaadin.hummingbird.namespace;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.change.ListSpliceChange;
import com.vaadin.hummingbird.util.SerializableJson;

import elemental.json.JsonValue;

/**
 * A list namespace which contains {@link JsonValue}s.
 *
 * @author Vaadin
 * @since
 */
public abstract class JsonListNamespace extends ListNamespace<JsonValue> {

    /**
     * Creates a new namespace for the given node.
     *
     * @param node
     *            the node that the namespace belongs to
     */
    protected JsonListNamespace(StateNode node) {
        super(node);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected Serializable preWriteValues(List<JsonValue> list) {
        return SerializableJson.createSerializableList((List) list);
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected Serializable preWriteChanges(List<ListSpliceChange> list) {
        ArrayList<ListSpliceChange> copy = new ArrayList<>();
        for (ListSpliceChange change : list) {
            List newItemsCopy = SerializableJson
                    .createSerializableList((List) change.getNewItems());
            ListSpliceChange changeCopy = new ListSpliceChange(
                    change.getNode().getNamespace(change.getNamespace()),
                    isNodeValues(), change.getIndex(), change.getRemoveCount(),
                    newItemsCopy);
            copy.add(changeCopy);
        }
        return copy;
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void postReadChanges(List<ListSpliceChange> list) {
        list.forEach(change -> {
            SerializableJson.unwrapList((List) change.getNewItems());
        });
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void postReadList(List list) {
        SerializableJson.unwrapList(list);
    }
}
