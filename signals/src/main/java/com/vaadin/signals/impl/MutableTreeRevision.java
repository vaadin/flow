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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.vaadin.signals.Id;
import com.vaadin.signals.ListSignal.ListPosition;
import com.vaadin.signals.Node;
import com.vaadin.signals.Node.Alias;
import com.vaadin.signals.Node.Data;
import com.vaadin.signals.SignalCommand;
import com.vaadin.signals.SignalCommand.AdoptAsCommand;
import com.vaadin.signals.SignalCommand.AdoptAtCommand;
import com.vaadin.signals.SignalCommand.ClearCommand;
import com.vaadin.signals.SignalCommand.ClearOwnerCommand;
import com.vaadin.signals.SignalCommand.IncrementCommand;
import com.vaadin.signals.SignalCommand.InsertCommand;
import com.vaadin.signals.SignalCommand.KeyCondition;
import com.vaadin.signals.SignalCommand.ConditionCommand;
import com.vaadin.signals.SignalCommand.LastUpdateCondition;
import com.vaadin.signals.SignalCommand.PositionCondition;
import com.vaadin.signals.SignalCommand.PutCommand;
import com.vaadin.signals.SignalCommand.PutIfAbsentCommand;
import com.vaadin.signals.SignalCommand.RemoveByKeyCommand;
import com.vaadin.signals.SignalCommand.RemoveCommand;
import com.vaadin.signals.SignalCommand.ScopeOwnerCommand;
import com.vaadin.signals.SignalCommand.SetCommand;
import com.vaadin.signals.SignalCommand.SnapshotCommand;
import com.vaadin.signals.SignalCommand.TransactionCommand;
import com.vaadin.signals.SignalCommand.ValueCondition;
import com.vaadin.signals.impl.CommandResult.Accept;
import com.vaadin.signals.impl.CommandResult.Reject;
import com.vaadin.signals.impl.CommandResult.NodeModification;

/**
 * A tree revision that can be mutated by applying signal commands.
 */
public class MutableTreeRevision extends TreeRevision {
    /**
     * Gathers and collects all state related to applying a single command. With
     * transactions, previously applied commands might end up rolled back if a
     * later command in the transaction is rejected. To deal with this, changes
     * are applied by collecting a set of changes so that later commands are
     * evaluated against the already collected changes. The same structure also
     * helps decompose complex single operations into individually evaluated
     * steps.
     */
    private class TreeManipulator {
        private final Map<Id, Node> updatedNodes = new HashMap<>();
        private final Set<Id> detachedNodes = new HashSet<>();
        private final Map<Id, SignalCommand.ScopeOwnerCommand> originalInserts = new HashMap<>();

        private final SignalCommand command;

        /**
         * The operation result is tracked in an instance field to allow helper
         * methods to optionally set a result while also returning a regular
         * value.
         */
        private CommandResult result;

        /**
         * Child results are collected for transactions and applied at the end
         * since the result of earlier operations might change if a later
         * operation is rejected.
         */
        private Map<Id, CommandResult> subCommandResults;

        public TreeManipulator(SignalCommand command) {
            this.command = command;
        }

        private void setResult(CommandResult result) {
            assert this.result == null;
            this.result = result;
        }

        private void fail(String reason) {
            setResult(CommandResult.fail(reason));
        }

        private Id resolveAlias(Id nodeId) {
            Node dataOrAlias = updatedNodes.get(nodeId);
            if (dataOrAlias == null) {
                dataOrAlias = nodes().get(nodeId);
            }

            if (dataOrAlias instanceof Alias alias) {
                return alias.target();
            } else {
                return nodeId;
            }
        }

        private Optional<Data> data(Id nodeId) {
            Id id = resolveAlias(nodeId);

            if (detachedNodes.contains(id)) {
                return Optional.empty();
            } else if (updatedNodes.containsKey(id)) {
                return Optional.ofNullable((Data) updatedNodes.get(id));
            } else {
                return MutableTreeRevision.this.data(id);
            }
        }

        private void useData(Id nodeId, BiConsumer<Data, Id> consumer) {
            assert result == null;

            Id id = resolveAlias(nodeId);
            data(id).ifPresentOrElse(node -> consumer.accept(node, id), () -> {
                fail("Node not found");
            });
        }

        private void updateData(Id nodeId, UnaryOperator<Data> updater) {
            useData(nodeId, (node, id) -> {
                Data updatedNode = updater.apply(node);
                if (updatedNode != node) {
                    updatedNodes.put(id, updatedNode);
                }
            });
        }

        private JsonNode value(Id nodeId) {
            return data(nodeId).map(Data::value).orElse(null);
        }

        private void setValue(Id nodeId, JsonNode value) {
            updateData(nodeId,
                    node -> new Data(node.parent(), command.commandId(),
                            node.scopeOwner(), value, node.listChildren(),
                            node.mapChildren()));
        }

        private Optional<List<Id>> listChildren(Id parentId) {
            return data(parentId).map(Data::listChildren);
        }

        private boolean isChildAt(Id parentId, int index, Id expectedChild) {
            assert expectedChild != null;

            if (index < 0) {
                return false;
            }

            Id idAtIndex = listChildren(parentId).map(children -> {
                if (index >= children.size()) {
                    return null;
                }
                return children.get(index);
            }).orElse(null);

            return isSameNode(idAtIndex, expectedChild);
        }

        private Optional<Id> mapChild(Id nodeId, String key) {
            return data(nodeId).map(Data::mapChildren)
                    .map(children -> children.get(key));
        }

        private boolean isSameNode(Id a, Id b) {
            return Objects.equals(resolveAlias(a), resolveAlias(b));
        }

        private boolean detach(Id nodeId) {
            useData(nodeId, (node, id) -> {
                if (id.equals(Id.ZERO)) {
                    fail("Cannot detach the root");
                    return;
                }

                Id parentId = node.parent();
                if (parentId == null) {
                    fail("Node is not attached");
                    return;
                }

                Data parentData = data(parentId).get();

                String key = parentData.mapChildren().entrySet().stream()
                        .filter(entry -> entry.getValue().equals(id)).findAny()
                        .map(Entry::getKey).orElse(null);

                if (key != null) {
                    updatedNodes.put(parentId, updateMapChildren(parentData,
                            map -> map.remove(key)));
                } else {
                    updatedNodes.put(parentId, updateListChildren(parentData,
                            list -> list.remove(id)));
                }

                detachedNodes.add(id);
            });

            // Check if any error was reported
            return result == null;
        }

        private Data updateMapChildren(Data node,
                Consumer<Map<String, Id>> mapUpdater) {
            LinkedHashMap<String, Id> map = new LinkedHashMap<>(
                    node.mapChildren());
            mapUpdater.accept(map);

            return new Data(node.parent(), command.commandId(),
                    node.scopeOwner(), node.value(), node.listChildren(),
                    Collections.unmodifiableMap(map));
        }

        private Data updateListChildren(Data node,
                Consumer<List<Id>> listUpdater) {
            ArrayList<Id> list = new ArrayList<>(node.listChildren());
            listUpdater.accept(list);

            return new Data(node.parent(), command.commandId(),
                    node.scopeOwner(), node.value(),
                    Collections.unmodifiableList(list), node.mapChildren());
        }

        private void attach(Id parentId, Id childId,
                BiFunction<Data, Id, Data> attacher) {
            if (result != null) {
                return;
            }

            Id resolvedParentId = resolveAlias(parentId);
            Id resolvedChildId = resolveAlias(childId);

            if (!detachedNodes.contains(resolvedChildId)) {
                fail("Node is not detached");
                return;
            }

            Id ancestor = resolvedParentId;
            while (ancestor != null) {
                if (ancestor.equals(childId)) {
                    fail("Cannot attach to own descendant");
                    return;
                }

                ancestor = data(ancestor).map(Data::parent).orElse(null);
            }

            useData(parentId, (node, id) -> {
                // Mark as detached only after the last error condition check
                // done by useData
                detachedNodes.remove(resolvedChildId);

                Data updated = attacher.apply(node, resolvedChildId);
                if (result == null) {
                    Data child = data(resolvedChildId).get();

                    updatedNodes.put(id, updated);
                    updatedNodes.put(resolvedChildId,
                            new Data(id, child.lastUpdate(), child.scopeOwner(),
                                    child.value(), child.listChildren(),
                                    child.mapChildren()));
                }
            });
        }

        private void attachAs(Id parentId, String key, Id childId) {
            attach(parentId, childId, (parentNode, resolvedChildId) -> {
                return updateMapChildren(parentNode, map -> {
                    Id previous = map.putIfAbsent(key, resolvedChildId);
                    if (previous != null) {
                        fail("Key is in use");
                    }
                });
            });
        }

        private int findInsertIndex(List<Id> children,
                ListPosition insertPosition) {
            Id after = resolveAlias(insertPosition.after());
            Id before = resolveAlias(insertPosition.before());

            if (after != null) {
                int position;
                if (after.equals(Id.EDGE)) {
                    // After edge -> insert first
                    position = 0;
                } else {
                    int indexOf = children.indexOf(after);
                    if (indexOf == -1) {
                        return -1;
                    }
                    position = indexOf + 1;
                }

                // Validate before constraint if there is one
                if (before != null) {
                    Id atPosition = position < children.size()
                            ? children.get(position)
                            : Id.EDGE;
                    if (!atPosition.equals(before)) {
                        return -1;
                    }
                }

                return position;
            } else {
                // Invalid to not define any position
                if (before == null) {
                    return -1;
                }

                // Before edge -> insert last
                if (before.equals(Id.EDGE)) {
                    return children.size();
                }

                return children.indexOf(before);
            }
        }

        private void attachAt(Id parentId, ListPosition position, Id childId) {
            attach(parentId, childId, (node, resolvedChildId) -> {
                int insertIndex = findInsertIndex(node.listChildren(),
                        position);
                if (insertIndex == -1) {
                    fail("Insert position not matched");
                    return null;
                }

                return updateListChildren(node,
                        list -> list.add(insertIndex, resolvedChildId));
            });
        }

        private void createNode(Id nodeId, JsonNode value, Id scopeOwner) {
            if (data(nodeId).isPresent()) {
                fail("Node already exists");
                return;
            }

            // Mark as detached to make it eligible for attaching
            detachedNodes.add(nodeId);
            updatedNodes.put(nodeId, new Data(null, command.commandId(),
                    scopeOwner, value, List.of(), Map.of()));

            if (ownerId().equals(scopeOwner)) {
                originalInserts.put(nodeId, (ScopeOwnerCommand) command);
            }
        }

        private NodeModification createModification(Id id, Node newNode) {
            Data original = MutableTreeRevision.this.data(id).orElse(null);
            return new NodeModification(original, newNode);
        }

        private static Map<Class<? extends SignalCommand>, BiConsumer<TreeManipulator, ? extends SignalCommand>> handlers = new HashMap<>();

        private static <T extends SignalCommand> void addHandler(
                Class<T> commandType, BiConsumer<TreeManipulator, T> handler) {
            handlers.put(commandType, handler);
        }

        private static <T extends ConditionCommand> void addConditionHandler(
                Class<T> commandType,
                BiFunction<TreeManipulator, T, CommandResult> handler) {
            addHandler(commandType, (manipulator, command) -> manipulator
                    .setResult(handler.apply(manipulator, command)));
        }

        static {
            addConditionHandler(ValueCondition.class,
                    TreeManipulator::handleValueCondition);
            addConditionHandler(PositionCondition.class,
                    TreeManipulator::handlePositionCondition);
            addConditionHandler(KeyCondition.class,
                    TreeManipulator::handleKeyCondition);
            addConditionHandler(LastUpdateCondition.class,
                    TreeManipulator::handleLastUpdateCondition);

            addHandler(AdoptAsCommand.class, TreeManipulator::handleAdoptAs);
            addHandler(AdoptAtCommand.class, TreeManipulator::handleAdoptAt);
            addHandler(IncrementCommand.class,
                    TreeManipulator::handleIncrement);
            addHandler(ClearCommand.class, TreeManipulator::handleClear);
            addHandler(RemoveByKeyCommand.class,
                    TreeManipulator::handleRemoveByKey);
            addHandler(PutCommand.class, TreeManipulator::handlePut);
            addHandler(PutIfAbsentCommand.class,
                    TreeManipulator::handlePutIfAbsent);
            addHandler(InsertCommand.class, TreeManipulator::handleInsert);
            addHandler(SetCommand.class, TreeManipulator::handleSet);
            addHandler(RemoveCommand.class, TreeManipulator::handleRemove);
            addHandler(ClearOwnerCommand.class,
                    TreeManipulator::handleClearOwner);
            addHandler(TransactionCommand.class,
                    TreeManipulator::handleTransaction);
            addHandler(SnapshotCommand.class, TreeManipulator::handleSnapshot);
        }

        public CommandResult handleCommand(SignalCommand command) {
            @SuppressWarnings("unchecked")
            BiConsumer<TreeManipulator, SignalCommand> handler = (BiConsumer<TreeManipulator, SignalCommand>) handlers
                    .get(command.getClass());

            handler.accept(this, command);

            if (result != null) {
                return result;
            }

            Map<Id, NodeModification> updates = new HashMap<>();

            updatedNodes.forEach((id, newNode) -> {
                if (!detachedNodes.contains(id)) {
                    updates.put(id, createModification(id, newNode));
                }
            });

            if (!detachedNodes.isEmpty()) {
                Map<Id, List<Id>> reverseAliases = new HashMap<>();
                nodes().forEach((signalId, nodeOrAlias) -> {
                    if (nodeOrAlias instanceof Alias alias) {
                        reverseAliases
                                .computeIfAbsent(alias.target(),
                                        ignore -> new ArrayList<>())
                                .add(signalId);
                    }
                });

                LinkedList<Id> toDetach = new LinkedList<>(detachedNodes);
                while (!toDetach.isEmpty()) {
                    Id removed = toDetach.removeLast();
                    updates.put(removed, createModification(removed, null));

                    reverseAliases.getOrDefault(removed, List.of())
                            .forEach(aliasToRemove -> updates.put(aliasToRemove,
                                    createModification(aliasToRemove, null)));

                    Data node = MutableTreeRevision.this.data(removed).get();
                    toDetach.addAll(node.listChildren());
                    toDetach.addAll(node.mapChildren().values());
                }
            }

            return new Accept(updates, originalInserts);
        }

        private CommandResult handleValueCondition(ValueCondition test) {
            JsonNode value = value(test.targetNodeId());
            if (value == null) {
                value = NullNode.getInstance();
            }
            JsonNode expectedValue = test.expectedValue();

            if (expectedValue == null) {
                expectedValue = NullNode.getInstance();
            }

            return CommandResult.conditional(value.equals(expectedValue),
                    "Unexpected value");
        }

        private CommandResult handlePositionCondition(PositionCondition test) {
            Id nodeId = test.targetNodeId();
            Id resolvedChild = resolveAlias(test.childId());

            int indexOf = listChildren(nodeId)
                    .map(list -> list.indexOf(resolvedChild))
                    .orElseGet(() -> Integer.valueOf(-1));

            if (indexOf == -1) {
                return CommandResult.fail("Not a child");
            }

            ListPosition position = test.position();

            Id after = position.after();
            if (after != null) {
                if (after.equals(Id.EDGE)) {
                    if (indexOf != 0) {
                        return CommandResult.fail("Not the first child");
                    }
                } else {
                    if (!isChildAt(nodeId, indexOf - 1, after)) {
                        return CommandResult
                                .fail("Not after the provided child");
                    }
                }
            }

            Id before = position.before();
            if (before != null) {
                if (before.equals(Id.EDGE)) {
                    int childCount = listChildren(nodeId).map(List::size)
                            .orElse(0);
                    if (indexOf != childCount - 1) {
                        return CommandResult.fail("Not the last child");
                    }
                } else {
                    if (!isChildAt(nodeId, indexOf + 1, before)) {
                        return CommandResult
                                .fail("Not before the provided child");
                    }
                }
            }

            return CommandResult.ok();
        }

        private CommandResult handleKeyCondition(KeyCondition keyTest) {
            Id nodeId = keyTest.targetNodeId();
            String key = keyTest.key();
            Id expectedChild = keyTest.expectedChild();

            Id actualChildId = mapChild(nodeId, key).orElse(null);
            if (expectedChild == null) {
                return CommandResult.conditional(actualChildId != null,
                        "Key not present");
            } else if (Id.ZERO.equals(expectedChild)) {
                return CommandResult.conditional(actualChildId == null,
                        "A key is present");
            } else {
                return CommandResult.conditional(
                        isSameNode(actualChildId, expectedChild),
                        "Unexpected child");
            }
        }

        private CommandResult handleLastUpdateCondition(
                LastUpdateCondition lastUpdateTest) {
            Id lastUpdate = data(lastUpdateTest.targetNodeId())
                    .map(Data::lastUpdate).orElse(null);

            return CommandResult.conditional(
                    Objects.equals(lastUpdate,
                            lastUpdateTest.expectedLastUpdate()),
                    "Unexpected last update");
        }

        private void handleAdoptAs(AdoptAsCommand adoptAs) {
            Id nodeId = adoptAs.targetNodeId();
            String key = adoptAs.key();
            Id childId = adoptAs.childId();

            if (detach(childId)) {
                attachAs(nodeId, key, childId);
            }
        }

        private void handleAdoptAt(AdoptAtCommand adoptAt) {
            Id nodeId = adoptAt.targetNodeId();
            ListPosition position = adoptAt.position();
            Id childId = adoptAt.childId();

            if (detach(childId)) {
                attachAt(nodeId, position, childId);
            }
        }

        private void handleIncrement(IncrementCommand increment) {
            Id nodeId = increment.targetNodeId();
            double delta = increment.delta();

            JsonNode oldValue = value(nodeId);

            double newValue;
            if (oldValue instanceof NumericNode value) {
                newValue = value.doubleValue() + delta;
            } else if (oldValue == null || oldValue instanceof NullNode) {
                newValue = delta;
            } else {
                fail("Value is not numeric");
                return;
            }

            setValue(nodeId, new DoubleNode(newValue));
        }

        private void handleClear(ClearCommand clear) {
            updateData(clear.targetNodeId(), node -> {
                detachedNodes.addAll(node.listChildren());
                detachedNodes.addAll(node.mapChildren().values());

                if (detachedNodes.isEmpty()) {
                    return node;
                }

                return new Data(node.parent(), command.commandId(),
                        node.scopeOwner(), node.value(), List.of(), Map.of());
            });
        }

        private void handleRemoveByKey(RemoveByKeyCommand removeByKey) {
            mapChild(removeByKey.targetNodeId(), removeByKey.key())
                    .ifPresentOrElse(this::detach,
                            () -> fail("Key not present"));
        }

        private void handlePut(PutCommand put) {
            Id commandId = put.commandId();
            Id nodeId = put.targetNodeId();
            String key = put.key();
            JsonNode value = put.value();

            mapChild(nodeId, key).ifPresentOrElse(childId -> {
                setValue(childId, value);
            }, () -> {
                createNode(commandId, value, null);
                attachAs(nodeId, key, commandId);
            });
        }

        private void handlePutIfAbsent(PutIfAbsentCommand putIfAbsent) {
            Id commandId = putIfAbsent.commandId();
            Id nodeId = putIfAbsent.targetNodeId();
            String key = putIfAbsent.key();

            mapChild(nodeId, key).ifPresentOrElse(childId -> {
                if (data(commandId).isPresent()) {
                    fail("Node already exists");
                    return;
                }

                updatedNodes.put(commandId, new Alias(resolveAlias(childId)));
            }, () -> {
                createNode(commandId, putIfAbsent.value(),
                        putIfAbsent.scopeOwner());
                attachAs(nodeId, key, commandId);
            });
        }

        private void handleInsert(InsertCommand insert) {
            Id commandId = insert.commandId();

            createNode(commandId, insert.value(), insert.scopeOwner());
            attachAt(insert.targetNodeId(), insert.position(), commandId);
        }

        private void handleSet(SetCommand set) {
            setValue(set.targetNodeId(), set.value());
        }

        private void handleRemove(RemoveCommand remove) {
            Id nodeId = remove.targetNodeId();
            Id expectedParentId = remove.expectedParentId();

            if (expectedParentId != null) {
                Id parentId = data(nodeId).map(Data::parent).orElse(null);

                if (!isSameNode(expectedParentId, parentId)) {
                    fail("Not a child");
                    return;
                }
            }

            detach(nodeId);
        }

        private void handleClearOwner(ClearOwnerCommand clearOwner) {
            Id ownerId = clearOwner.ownerId();

            // TODO clear originalInserts that have been removed previously?
            nodes().forEach((id, nodeOrAlias) -> {
                if (nodeOrAlias instanceof Data node
                        && ownerId.equals(node.scopeOwner())) {
                    detach(id);
                }
            });
        }

        private void handleTransaction(TransactionCommand transaction) {
            List<SignalCommand> commands = transaction.commands();

            MutableTreeRevision scratchpad = new MutableTreeRevision(
                    MutableTreeRevision.this);

            subCommandResults = new HashMap<Id, CommandResult>();

            Reject firstReject = null;
            for (SignalCommand command : commands) {
                scratchpad.apply(command, subCommandResults::put);

                CommandResult childResult = subCommandResults
                        .get(command.commandId());
                if (childResult instanceof Reject reject) {
                    firstReject = reject;
                    break;
                }
            }

            if (firstReject == null) {
                Map<Id, NodeModification> updates = new HashMap<>();
                Map<Id, SignalCommand.ScopeOwnerCommand> originalInserts = new HashMap<>();

                // Iterate the command list to preserve order
                for (SignalCommand command : commands) {
                    Accept op = (Accept) subCommandResults
                            .get(command.commandId());
                    op.updates().forEach((nodeId, modification) -> {
                        if (updates.containsKey(nodeId)) {
                            updates.put(nodeId,
                                    new NodeModification(
                                            updates.get(nodeId).oldNode(),
                                            modification.newNode()));
                        } else {
                            updates.put(nodeId, modification);
                        }
                    });

                    originalInserts.putAll(op.originalInserts());
                }

                setResult(new Accept(updates, originalInserts));
            } else {
                for (SignalCommand command : commands) {
                    CommandResult originalResult = subCommandResults
                            .get(command.commandId());
                    if (originalResult == null
                            || originalResult instanceof Accept) {
                        subCommandResults.put(command.commandId(), firstReject);
                    }
                }

                setResult(firstReject);
            }
        }

        private void handleSnapshot(SnapshotCommand snapshot) {
            /*
             * We will have to add support for applying a snapshot to a
             * non-empty tree if we implement re-synchronization based on
             * snapshots.
             */
            assert updatedNodes.isEmpty();
            assert detachedNodes.isEmpty();

            updatedNodes.putAll(snapshot.nodes());
        }
    }

    /**
     * Creates a new mutable tree revision as a copy of the provided base
     * revision.
     *
     * @param base
     *            the base revision to copy, not <code>null</code>
     */
    public MutableTreeRevision(TreeRevision base) {
        super(base.ownerId(), new HashMap<>(base.nodes()),
                new HashMap<>(base.originalInserts()));
    }

    /**
     * Applies a sequence of commands and collects the results to a map.
     *
     * @param commands
     *            the list of commands to apply, not <code>null</code>
     * @return a map from command id to operation results, not <code>null</code>
     */
    public Map<Id, CommandResult> applyAndGetResults(
            List<SignalCommand> commands) {
        Map<Id, CommandResult> results = new HashMap<>();

        for (SignalCommand command : commands) {
            apply(command, results::put);
        }

        return results;
    }

    /**
     * Applies a sequence of commands and ignores the results.
     *
     * @param commands
     *            the list of commands to apply, not <code>null</code>
     */
    public void apply(List<SignalCommand> commands) {
        for (SignalCommand command : commands) {
            apply(command, null);
        }
    }

    /**
     * Applies a single command and passes the results to the provided handler.
     * Note that the handler will be invoked exactly once for most types of
     * commands but it will be invoked multiple times for transactions.
     *
     * @param command
     *            the command to apply, not <code>null</code>
     * @param resultCollector
     *            callback to collect command results, or <code>null</code> to
     *            ignore results
     */
    public void apply(SignalCommand command,
            BiConsumer<Id, CommandResult> resultCollector) {
        CommandResult result = data(command.targetNodeId()).map(data -> {
            TreeManipulator manipulator = new TreeManipulator(command);
            var opResult = manipulator.handleCommand(command);
            if (manipulator.subCommandResults != null
                    && resultCollector != null) {
                manipulator.subCommandResults.forEach(resultCollector);
            }
            return opResult;
        }).orElseGet(() -> CommandResult.fail("Node not found"));

        if (result instanceof Accept accept) {
            accept.updates().forEach((nodeId, update) -> {
                Node newNode = update.newNode();

                if (newNode == null) {
                    nodes().remove(nodeId);
                    originalInserts().remove(nodeId);
                } else {
                    nodes().put(nodeId, newNode);
                }
            });

            originalInserts().putAll(accept.originalInserts());
        }

        if (resultCollector != null) {
            resultCollector.accept(command.commandId(), result);
        }

        assert assertValidTree();
    }
}
