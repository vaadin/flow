/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.signals;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;

/**
 * A node in a signal tree. Each node represents as signal entry. Nodes are
 * immutable and referenced by an {@link Id} rather than directly referencing
 * the node instance. The node is either a {@link Data} node carrying actual
 * signal data or an {@link Alias} node that allows multiple signal ids to
 * reference the same data.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME)
@JsonSubTypes(value = { @Type(Node.Data.class), @Type(Node.Alias.class), })
public sealed interface Node {

    /**
     * An empty data node without parent, scope owner, value or children and the
     * initial last update id.
     */
    public static final Data EMPTY = new Data(null, Id.ZERO, null, null,
            List.of(), Map.of());

    /**
     * A node alias. An alias node allows multiple signal ids to reference the
     * same data.
     *
     * @param target
     *            the id of the alias target, not <code>null</code>
     */
    public record Alias(Id target) implements Node {
    }

    /**
     * A data node. The node represents the actual data behind a signal
     * instance. The value of a node is made up of three different components:
     * <ul>
     * <li>a leaf value for when the node is used as a value signal</li>
     * <li>a list of children for when the node is used as a list signal</li>
     * <li>a map of children for when the node is used as a map signal</li>
     * </ul>
     * Note that a child is always either a list child or a map child. A child
     * cannot have a list position and a map key at the same time.
     *
     * @param parent
     *            the parent id, or <code>null</code> for the root node
     * @param lastUpdate
     *            a unique id for the update that last updated this data node,
     *            not <code>null</code>
     * @param scopeOwner
     *            the id of the external owner of this node, or
     *            <code>null</code> if the node has no owner. Any node with an
     *            owner is deleted if the owner is disconnected.
     * @param value
     *            the JSON value of this node, or <code>null</code> if there is
     *            no value
     * @param listChildren
     *            a list of child ids, or an empty list if the node has no list
     *            children
     * @param mapChildren
     *            a sequenced map from key to child id, or an empty map if the
     *            node has no map children
     */
    public record Data(Id parent, Id lastUpdate, Id scopeOwner, JsonNode value,
            List<Id> listChildren,
            Map<String, Id> mapChildren) implements Node {
        /**
         * Creates a new data node.
         *
         * @param parent
         *            the parent id, or <code>null</code> for the root node
         * @param lastUpdate
         *            a unique id for the update that last updated this data
         *            node, not <code>null</code>
         * @param scopeOwner
         *            the id of the external owner of this node, or
         *            <code>null</code> if the node has no owner. Any node with
         *            an owner is deleted if the owner is disconnected.
         * @param value
         *            the JSON value of this node, or <code>null</code> if there
         *            is no value
         * @param listChildren
         *            a list of child ids, or an empty list if the node has no
         *            list children
         * @param mapChildren
         *            a sequenced map from key to child id, or an empty map if
         *            the node has no map children
         */
        /*
         * There's no point in copying the record components here since they are
         * already documented on the top level, but the Javadoc checker insist
         * that this constructor also has full documentation...
         */
        public Data {
            Objects.requireNonNull(lastUpdate);

            /*
             * Avoid accidentally making a distinction between the two different
             * nulls that will look the same after JSON deserialization
             */
            if (value instanceof NullNode) {
                value = null;
            }
        }
    }
}
