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
package com.vaadin.signals.impl;

import java.util.HashMap;
import java.util.Map;

import com.vaadin.signals.Id;
import com.vaadin.signals.Node;
import com.vaadin.signals.Node.Data;
import com.vaadin.signals.SignalCommand;

/**
 * The result of applying a signal command against a tree revision. The result
 * is either to accept or reject the command.
 *
 * @see SignalCommand
 * @see TreeRevision
 */
public sealed interface CommandResult {
    /**
     * A data node update in an accepted command result.
     *
     * @param oldNode
     *            the old node instance, or <code>null</code> if the command
     *            created a new node
     * @param newNode
     *            the new node instance or null if the command removed the node
     */
    record NodeModification(Node oldNode, Node newNode) {
    }

    /**
     * An accepted command. Contains a collection of node updates that are
     * performed as a result of the command and any applied insert commands with
     * a {@link Data#scopeOwner()} that matches the tree to which the command
     * was applied.
     * <p>
     * Note that due to the way aliases are resolved, the node id in the update
     * map might not match the node id in the applied signal command.
     *
     * @param updates
     *            a map from node ids to modifications to apply, not
     *            <code>null</code>. The map is empty for condition commands
     *            that do not apply any changes even if the test passes.
     * @param originalInserts
     *            a map from inserted node id to the originating signal command
     *            for new nodes with a matching scope owner. Not
     *            <code>null</code>.
     */
    record Accept(Map<Id, NodeModification> updates,
            Map<Id, SignalCommand.ScopeOwnerCommand> originalInserts)
            implements
                CommandResult {
        @Override
        public boolean accepted() {
            return true;
        }

        /**
         * Asserts that this result contains exactly one modification and
         * returns it.
         *
         * @return the single modification, not <code>null</code>
         */
        public NodeModification onlyUpdate() {
            assert updates.size() == 1;
            return updates.values().iterator().next();
        }
    }

    /**
     * A rejected command, together with the reason for the rejection.
     *
     * @param reason
     *            a string that describes the rejection reason, not
     *            <code>null</code>
     */
    record Reject(String reason) implements CommandResult {
        @Override
        public boolean accepted() {
            return false;
        }
    }

    /**
     * Tests whether this command result is accepted or rejected.
     *
     * @return <code>true</code> if the command is accepted, <code>false</code>
     *         if it's rejected
     */
    boolean accepted();

    /**
     * Creates a copy of the given map of command results where all accepted
     * results are replaced with the same rejection.
     *
     * @param results
     *            the original map from ids to command results, not
     *            <code>null</code>
     * @param reason
     *            the rejection reason string, not <code>null</code>
     * @return a map with all accepted results replaced with rejections
     */
    public static Map<Id, CommandResult> rejectAll(
            Map<Id, CommandResult> results, String reason) {
        Map<Id, CommandResult> failed = new HashMap<>();

        results.forEach((key, original) -> {
            if (original instanceof Reject failure) {
                failed.put(key, failure);
            } else {
                failed.put(key, fail(reason));
            }
        });

        return failed;
    }

    /**
     * Creates a simple accepted result without modifications or original
     * inserts.
     *
     * @return the accepted result, not <code>null</code>
     */
    public static Accept ok() {
        return new Accept(Map.of(), Map.of());
    }

    /**
     * Creates a new rejected result with the given reason.
     *
     * @param reason
     *            the reason string to use, not <code>null</code>
     * @return a new rejected result, not <code>null</code>
     */
    public static Reject fail(String reason) {
        return new Reject(reason);
    }

    /**
     * Creates an accepted or rejected result depending on the provided
     * condition.
     *
     * @param condition
     *            the condition to check
     * @param reasonIfFailed
     *            the reason string to use if rejected, not <code>null</code>
     * @return an accepted result if the condition is <code>true</code>, a
     *         rejected result if the condition is <code>false</code>
     */
    public static CommandResult conditional(boolean condition,
            String reasonIfFailed) {
        return condition ? ok() : fail(reasonIfFailed);
    }
}
