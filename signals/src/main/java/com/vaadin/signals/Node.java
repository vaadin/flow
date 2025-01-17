package com.vaadin.signals;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;

/**
 * A node in a signal tree. Each node represents as signal entry. Nodes are
 * immutable and referenced by an {@link Id} rather than directly referencing
 * the node instance. The node is either a {@link Data} node carrying actual
 * signal data or an {@link Alias} node that allows multiple signal ids to
 * reference the same data.
 */
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
     * instance.
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
     *            a list of child ids, or the an list if the node has no list
     *            children
     * @param mapChildren
     *            a sequenced map from key to child id, or an empty map if the
     *            node has no map children
     */
    public record Data(Id parent, Id lastUpdate, Id scopeOwner, JsonNode value,
            List<Id> listChildren, Map<String, Id> mapChildren)
            implements Node {
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
         *            a list of child ids, or the an list if the node has no
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