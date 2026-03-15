/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.signals.shared.impl;

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
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.DoubleNode;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.NumericNode;

import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.function.SerializableBiFunction;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.signals.Id;
import com.vaadin.flow.signals.Node;
import com.vaadin.flow.signals.Node.Alias;
import com.vaadin.flow.signals.Node.Data;
import com.vaadin.flow.signals.SignalCommand;
import com.vaadin.flow.signals.SignalCommand.AdoptAsCommand;
import com.vaadin.flow.signals.SignalCommand.AdoptAtCommand;
import com.vaadin.flow.signals.SignalCommand.ClearCommand;
import com.vaadin.flow.signals.SignalCommand.ClearOwnerCommand;
import com.vaadin.flow.signals.SignalCommand.IncrementCommand;
import com.vaadin.flow.signals.SignalCommand.InsertCommand;
import com.vaadin.flow.signals.SignalCommand.KeyCondition;
import com.vaadin.flow.signals.SignalCommand.LastUpdateCondition;
import com.vaadin.flow.signals.SignalCommand.PositionCondition;
import com.vaadin.flow.signals.SignalCommand.PutCommand;
import com.vaadin.flow.signals.SignalCommand.PutIfAbsentCommand;
import com.vaadin.flow.signals.SignalCommand.RemoveByKeyCommand;
import com.vaadin.flow.signals.SignalCommand.RemoveCommand;
import com.vaadin.flow.signals.SignalCommand.ScopeOwnerCommand;
import com.vaadin.flow.signals.SignalCommand.SetCommand;
import com.vaadin.flow.signals.SignalCommand.SnapshotCommand;
import com.vaadin.flow.signals.SignalCommand.TransactionCommand;
import com.vaadin.flow.signals.SignalCommand.ValueCondition;
import com.vaadin.flow.signals.shared.SharedListSignal.ListPosition;
import com.vaadin.flow.signals.shared.impl.CommandResult.Accept;
import com.vaadin.flow.signals.shared.impl.CommandResult.NodeModification;
import com.vaadin.flow.signals.shared.impl.CommandResult.Reject;

/**
 * A tree revision that can be mutated by applying signal commands.
 */
public class MutableTreeRevision extends TreeRevision {
    /**
     * Helper for accessing nodes in a consistent way regardless of whether the
     * nodes are loaded directly from the map in a revision or also checked for
     * overrides in a result builder.
     */
    private static abstract class NodeLookup {
        protected abstract Node node(Id nodeId);

        protected boolean isChildAt(List<Id> children, int index,
                Id expectedChild) {
            assert expectedChild != null;

            if (index < 0 || index >= children.size()) {
                return false;
            }

            return isSameNode(children.get(index), expectedChild);
        }

        protected Data data(Id nodeId) {
            Node node = node(resolveAlias(nodeId));

            return (Data) node;
        }

        protected boolean isSameNode(Id a, Id b) {
            return Objects.equals(resolveAlias(a), resolveAlias(b));
        }

        protected Id resolveAlias(Id id) {
            Node node = node(id);

            if (node instanceof Alias alias) {
                return alias.target();
            } else {
                return id;
            }
        }

        protected ResolvedData resolveData(Id nodeId) {
            Id id = resolveAlias(nodeId);

            Node node = node(id);
            if (node == null) {
                return null;
            } else {
                return new ResolvedData(id, (Data) node);
            }
        }
    }

    private class DirectNodeLookup extends NodeLookup {
        @Override
        protected Node node(Id nodeId) {
            return nodes().get(nodeId);
        }
    }

    private record ResolvedData(Id resolvedId, Data data) {
    }

    /**
     * Gathers and collects all state related to applying a command that affects
     * multiple nodes. To help with commands that consist of multiple steps, any
     * node updates are tracked and subsequent node lookup uses the updates
     * nodes rather than the originals.
     */
<<<<<<< HEAD
    private class ResultBuilder extends NodeLookup {
=======
    @FunctionalInterface
    interface ChildAttacher extends Serializable {
        /**
         * Attaches a child to the parent node.
         *
         * @param parentNode
         *            the parent node data, not <code>null</code>
         * @param childId
         *            the child node ID to attach, not <code>null</code>
         * @return the modified parent node data, or <code>null</code> if the
         *         attach operation failed
         */
        @Nullable
        Data attach(Data parentNode, Id childId);
    }

    /**
     * Gathers and collects all state related to applying a single command. With
     * transactions, previously applied commands might end up rolled back if a
     * later command in the transaction is rejected. To deal with this, changes
     * are applied by collecting a set of changes so that later commands are
     * evaluated against the already collected changes. The same structure also
     * helps decompose complex single operations into individually evaluated
     * steps.
     */
    private class TreeManipulator implements Serializable {
>>>>>>> origin/main
        private final Map<Id, Node> updatedNodes = new HashMap<>();
        private final Set<Id> detachedNodes = new HashSet<>();
        private final Map<Id, SignalCommand.ScopeOwnerCommand> originalInserts = new HashMap<>();

        private final SignalCommand command;

<<<<<<< HEAD
        public ResultBuilder(SignalCommand command) {
=======
        /**
         * The operation result is tracked in an instance field to allow helper
         * methods to optionally set a result while also returning a regular
         * value.
         */
        private @Nullable CommandResult result;

        /**
         * Child results are collected for transactions and applied at the end
         * since the result of earlier operations might change if a later
         * operation is rejected.
         */
        private @Nullable Map<Id, CommandResult> subCommandResults;

        public TreeManipulator(SignalCommand command) {
>>>>>>> origin/main
            this.command = command;
        }

        private Reject fail(String reason) {
            return CommandResult.fail(reason);
        }

<<<<<<< HEAD
        @Override
        protected Node node(Id nodeId) {
            if (detachedNodes.contains(nodeId)) {
                return null;
            }

            Node node = updatedNodes.get(nodeId);
            if (node != null) {
                return node;
=======
        private void fail(String reason) {
            setResult(CommandResult.fail(reason));
        }

        private @Nullable Id resolveAlias(@Nullable Id nodeId) {
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
            Id id = Objects.requireNonNull(resolveAlias(nodeId));

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

            Id id = Objects.requireNonNull(resolveAlias(nodeId));
            data(id).ifPresentOrElse(node -> consumer.accept(node, id), () -> {
                fail("Node not found");
            });
        }

        private void updateData(Id nodeId, SignalUpdater<Data> updater) {
            useData(nodeId, (node, id) -> {
                Data updatedNode = updater.update(node);
                if (updatedNode != node) {
                    updatedNodes.put(id, updatedNode);
                }
            });
        }

        private @Nullable JsonNode value(Id nodeId) {
            return data(nodeId).map(Data::value).orElse(null);
        }

        private void setValue(Id nodeId, @Nullable JsonNode value) {
            updateData(nodeId, node -> {
                Objects.requireNonNull(node);
                return new Data(node.parent(), command.commandId(),
                        node.scopeOwner(), value, node.listChildren(),
                        node.mapChildren());
            });
        }

        private Optional<List<Id>> listChildren(Id parentId) {
            return data(parentId).map(Data::listChildren);
        }

        private boolean isChildAt(Id parentId, int index, Id expectedChild) {
            assert expectedChild != null;

            if (index < 0) {
                return false;
>>>>>>> origin/main
            }

            return nodes().get(nodeId);
        }

        private Reject detach(Id nodeId) {
            ResolvedData resolved = resolveData(nodeId);
            if (resolved == null) {
                return fail("Node not found");
            }

            Data node = resolved.data();
            Id id = resolved.resolvedId();

            if (id.equals(Id.ZERO)) {
                return fail("Cannot detach the root");
            }

            Id parentId = node.parent();
            if (parentId == null) {
                return fail("Node is not attached");
            }

            Data parentData = data(parentId);

            String key = parentData.mapChildren().entrySet().stream()
                    .filter(entry -> entry.getValue().equals(id)).findAny()
                    .map(Entry::getKey).orElse(null);

            if (key != null) {
                updateMapChildren(parentId, map -> {
                    map.remove(key);
                    return null;
                });
            } else {
                updateListChildren(parentId, list -> {
                    list.remove(id);
                    return null;
                });
            }

            detachedNodes.add(id);
            return null;
        }

        private Reject updateMapChildren(Id resolvedParentId,
                Function<Map<String, Id>, CommandResult.Reject> mapUpdater) {
            Data node = data(resolvedParentId);

<<<<<<< HEAD
            LinkedHashMap<String, Id> map = new LinkedHashMap<>(
                    node.mapChildren());
            Reject maybeError = mapUpdater.apply(map);
            if (maybeError != null) {
                return maybeError;
            }

            updatedNodes.put(resolvedParentId, new Data(node.parent(),
                    command.commandId(), node.scopeOwner(), node.value(),
                    node.listChildren(), Collections.unmodifiableMap(map)));

            return null;
        }

        private Reject updateListChildren(Id resolvedParentId,
                Function<List<Id>, CommandResult.Reject> listUpdater) {
            Data node = data(resolvedParentId);

            ArrayList<Id> list = new ArrayList<>(node.listChildren());
            Reject maybeError = listUpdater.apply(list);
            if (maybeError != null) {
                return maybeError;
            }

            updatedNodes.put(resolvedParentId, new Data(node.parent(),
                    command.commandId(), node.scopeOwner(), node.value(),
                    Collections.unmodifiableList(list), node.mapChildren()));
            return null;
        }

        private Reject attach(Id parentId, Id childId,
                BiFunction<Id, Id, Reject> attacher) {
            Id resolvedParentId = resolveAlias(parentId);
            Id resolvedChildId = resolveAlias(childId);
=======
        private boolean isSameNode(@Nullable Id a, @Nullable Id b) {
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
                SerializableConsumer<Map<String, Id>> mapUpdater) {
            LinkedHashMap<String, Id> map = new LinkedHashMap<>(
                    node.mapChildren());
            mapUpdater.accept(map);

            return new Data(node.parent(), command.commandId(),
                    node.scopeOwner(), node.value(), node.listChildren(),
                    Collections.unmodifiableMap(map));
        }

        private Data updateListChildren(Data node,
                SerializableConsumer<List<Id>> listUpdater) {
            ArrayList<Id> list = new ArrayList<>(node.listChildren());
            listUpdater.accept(list);

            return new Data(node.parent(), command.commandId(),
                    node.scopeOwner(), node.value(),
                    Collections.unmodifiableList(list), node.mapChildren());
        }

        private void attach(Id parentId, Id childId, ChildAttacher attacher) {
            if (result != null) {
                return;
            }

            Id resolvedParentId = Objects
                    .requireNonNull(resolveAlias(parentId));
            Id resolvedChildId = Objects.requireNonNull(resolveAlias(childId));
>>>>>>> origin/main

            if (!detachedNodes.contains(resolvedChildId)) {
                return fail("Node is not detached");
            }

            Id ancestor = resolvedParentId;
            while (ancestor != null) {
                if (ancestor.equals(resolvedChildId)) {
                    return fail("Cannot attach to own descendant");
                }

                ancestor = data(ancestor).parent();
            }

            detachedNodes.remove(resolvedChildId);

<<<<<<< HEAD
            Reject maybeError = attacher.apply(resolvedParentId,
                    resolvedChildId);
            if (maybeError != null) {
                return maybeError;
            }
=======
                Data updated = attacher.attach(node, resolvedChildId);
                if (result == null && updated != null) {
                    Data child = data(resolvedChildId).get();
>>>>>>> origin/main

            Data child = data(resolvedChildId);
            updatedNodes.put(resolvedChildId,
                    new Data(resolvedParentId, child.lastUpdate(),
                            child.scopeOwner(), child.value(),
                            child.listChildren(), child.mapChildren()));

            return null;
        }

        private Reject attachAs(Id parentId, String key, Id childId) {
            return attach(parentId, childId, (parentNode, resolvedChildId) -> {
                return updateMapChildren(parentNode, map -> {
                    Id previous = map.putIfAbsent(key, resolvedChildId);
                    if (previous != null) {
                        return fail("Key is in use");
                    } else {
                        return null;
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

        private Reject attachAt(Id parentId, ListPosition position,
                Id childId) {
            return attach(parentId, childId,
                    (resolvedParentId, resolvedChildId) -> {
                        int insertIndex = findInsertIndex(
                                data(resolvedParentId).listChildren(),
                                position);
                        if (insertIndex == -1) {
                            return fail("Insert position not matched");
                        }

                        return updateListChildren(resolvedParentId, list -> {
                            list.add(insertIndex, resolvedChildId);
                            return null;
                        });
                    });
        }

<<<<<<< HEAD
        private Reject createNode(Id nodeId, JsonNode value, Id scopeOwner) {
            if (node(nodeId) != null) {
                return fail("Node already exists");
=======
        private void createNode(Id nodeId, @Nullable JsonNode value,
                @Nullable Id scopeOwner) {
            if (data(nodeId).isPresent()) {
                fail("Node already exists");
                return;
>>>>>>> origin/main
            }

            // Mark as detached to make it eligible for attaching
            detachedNodes.add(nodeId);
            updatedNodes.put(nodeId, new Data(null, command.commandId(),
                    scopeOwner, value, List.of(), Map.of()));

            if (ownerId().equals(scopeOwner)) {
                originalInserts.put(nodeId, (ScopeOwnerCommand) command);
            }
            return null;
        }

        private NodeModification createModification(Id id,
                @Nullable Node newNode) {
            Data original = MutableTreeRevision.this.data(id).orElse(null);
            return new NodeModification(original, newNode);
        }

<<<<<<< HEAD
        private CommandResult build() {
=======
        private static Map<Class<? extends SignalCommand>, SerializableBiConsumer<TreeManipulator, ? extends SignalCommand>> handlers = new HashMap<>();

        private static <T extends SignalCommand> void addHandler(
                Class<T> commandType,
                SerializableBiConsumer<TreeManipulator, T> handler) {
            handlers.put(commandType, handler);
        }

        private static <T extends ConditionCommand> void addConditionHandler(
                Class<T> commandType,
                SerializableBiFunction<TreeManipulator, T, CommandResult> handler) {
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
            SerializableBiConsumer<TreeManipulator, SignalCommand> handler = (SerializableBiConsumer<TreeManipulator, SignalCommand>) handlers
                    .get(command.getClass());

            if (handler == null) {
                throw new IllegalStateException(
                        "No handler for " + command.getClass().getName());
            }
            handler.accept(this, command);

            if (result != null) {
                return result;
            }

>>>>>>> origin/main
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
<<<<<<< HEAD
=======

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
                Objects.requireNonNull(node);
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
                // Include parent node in updates so the callback can read the
                // existing mapping
                useData(nodeId, (node, id) -> updatedNodes.put(id, node));
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
                    if (op == null) {
                        throw new IllegalStateException(
                                "Missing result for command "
                                        + command.commandId());
                    }
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
>>>>>>> origin/main
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
<<<<<<< HEAD
            BiConsumer<Id, CommandResult> resultCollector) {
        // Custom logic for transactions that can produce multiple results
        if (command instanceof TransactionCommand transaction) {
            Map<Id, CommandResult> results = handleTransaction(transaction);
=======
            @Nullable CommandDispatcher resultCollector) {
        CommandResult result = data(command.targetNodeId()).map(data -> {
            TreeManipulator manipulator = new TreeManipulator(command);
            var opResult = manipulator.handleCommand(command);
            if (manipulator.subCommandResults != null
                    && resultCollector != null) {
                manipulator.subCommandResults
                        .forEach(resultCollector::dispatch);
            }
            return opResult;
        }).orElseGet(() -> CommandResult.fail("Node not found"));
>>>>>>> origin/main

            applyResult(results.get(transaction.commandId()));

            if (resultCollector != null) {
                results.forEach(resultCollector);
            }
        } else {
            CommandResult result;
            if (!nodes().containsKey(command.targetNodeId())) {
                result = CommandResult.fail("Node not found");
            } else {
                @SuppressWarnings("unchecked")
                var handler = (BiFunction<MutableTreeRevision, SignalCommand, CommandResult>) handlers
                        .get(command.getClass());

                result = handler.apply(this, command);
            }

            applyResult(result);

            if (resultCollector != null) {
                resultCollector.accept(command.commandId(), result);
            }
        }

        assert assertValidTree();
    }

    private void applyResult(CommandResult result) {
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
    }

    private static Map<Class<? extends SignalCommand>, BiFunction<MutableTreeRevision, ? extends SignalCommand, CommandResult>> handlers = new HashMap<>();

    private static <T extends SignalCommand> void addHandler(
            Class<T> commandType,
            BiFunction<MutableTreeRevision, T, CommandResult> handler) {
        handlers.put(commandType, handler);
    }

    static {
        addHandler(ValueCondition.class,
                MutableTreeRevision::handleValueCondition);
        addHandler(PositionCondition.class,
                MutableTreeRevision::handlePositionCondition);
        addHandler(KeyCondition.class, MutableTreeRevision::handleKeyCondition);
        addHandler(LastUpdateCondition.class,
                MutableTreeRevision::handleLastUpdateCondition);

        addHandler(AdoptAsCommand.class, MutableTreeRevision::handleAdoptAs);
        addHandler(AdoptAtCommand.class, MutableTreeRevision::handleAdoptAt);
        addHandler(IncrementCommand.class,
                MutableTreeRevision::handleIncrement);
        addHandler(ClearCommand.class, MutableTreeRevision::handleClear);
        addHandler(RemoveByKeyCommand.class,
                MutableTreeRevision::handleRemoveByKey);
        addHandler(PutCommand.class, MutableTreeRevision::handlePut);
        addHandler(PutIfAbsentCommand.class,
                MutableTreeRevision::handlePutIfAbsent);
        addHandler(InsertCommand.class, MutableTreeRevision::handleInsert);
        addHandler(SetCommand.class, MutableTreeRevision::handleSet);
        addHandler(RemoveCommand.class, MutableTreeRevision::handleRemove);
        addHandler(ClearOwnerCommand.class,
                MutableTreeRevision::handleClearOwner);
        addHandler(SnapshotCommand.class, MutableTreeRevision::handleSnapshot);
    }

    private CommandResult handleValueCondition(ValueCondition test) {
        JsonNode value = data(test.targetNodeId()).map(Data::value)
                .orElse(null);
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
        DirectNodeLookup nodeLookup = new DirectNodeLookup();

        List<Id> listChildren = data(test.targetNodeId()).get().listChildren();

        Id resolvedChild = nodeLookup.resolveAlias(test.childId());
        int indexOf = listChildren.indexOf(resolvedChild);

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
                if (!nodeLookup.isChildAt(listChildren, indexOf - 1, after)) {
                    return CommandResult.fail("Not after the provided child");
                }
            }
        }

        Id before = position.before();
        if (before != null) {
            if (before.equals(Id.EDGE)) {
                int childCount = listChildren.size();
                if (indexOf != childCount - 1) {
                    return CommandResult.fail("Not the last child");
                }
            } else {
                if (!nodeLookup.isChildAt(listChildren, indexOf + 1, before)) {
                    return CommandResult.fail("Not before the provided child");
                }
            }
        }

        return CommandResult.ok();
    }

    private CommandResult handleKeyCondition(KeyCondition keyTest) {
        Id nodeId = keyTest.targetNodeId();
        String key = keyTest.key();
        Id expectedChild = keyTest.expectedChild();

        Id actualChildId = data(nodeId).get().mapChildren().get(key);

        if (expectedChild == null) {
            return CommandResult.conditional(actualChildId != null,
                    "Key not present");
        } else if (Id.ZERO.equals(expectedChild)) {
            return CommandResult.conditional(actualChildId == null,
                    "A key is present");
        } else {
            DirectNodeLookup nodeLookup = new DirectNodeLookup();

            return CommandResult.conditional(
                    nodeLookup.isSameNode(actualChildId, expectedChild),
                    "Unexpected child");
        }
    }

    private CommandResult handleLastUpdateCondition(
            LastUpdateCondition lastUpdateTest) {
        Id lastUpdate = data(lastUpdateTest.targetNodeId()).get().lastUpdate();

        return CommandResult.conditional(
                Objects.equals(lastUpdate, lastUpdateTest.expectedLastUpdate()),
                "Unexpected last update");
    }

    private CommandResult handleAdoptAs(AdoptAsCommand adoptAs) {
        Id nodeId = adoptAs.targetNodeId();
        Id childId = adoptAs.childId();
        String key = adoptAs.key();

        var builder = new ResultBuilder(adoptAs);

        Reject maybeError = builder.detach(childId);
        if (maybeError != null) {
            return maybeError;
        }

        maybeError = builder.attachAs(nodeId, key, childId);
        if (maybeError != null) {
            return maybeError;
        }

        return builder.build();
    }

    private CommandResult handleAdoptAt(AdoptAtCommand adoptAt) {
        Id nodeId = adoptAt.targetNodeId();
        Id childId = adoptAt.childId();
        ListPosition pos = adoptAt.position();

        var builder = new ResultBuilder(adoptAt);

        Reject maybeError = builder.detach(childId);
        if (maybeError != null) {
            return maybeError;
        }

        maybeError = builder.attachAt(nodeId, pos, childId);
        if (maybeError != null) {
            return maybeError;
        }

        return builder.build();
    }

    private CommandResult handleIncrement(IncrementCommand increment) {
        DirectNodeLookup nodeLookup = new DirectNodeLookup();

        ResolvedData resolved = nodeLookup
                .resolveData(increment.targetNodeId());

        double delta = increment.delta();

        JsonNode oldValue = data(increment.targetNodeId()).get().value();

        double newValue;
        if (oldValue instanceof NumericNode value) {
            newValue = value.doubleValue() + delta;
        } else if (oldValue == null || oldValue instanceof NullNode) {
            newValue = delta;
        } else {
            return CommandResult.fail("Value is not numeric");
        }

        return createValueChange(increment, resolved, new DoubleNode(newValue));
    }

    private CommandResult handleClear(ClearCommand clear) {
        var builder = new ResultBuilder(clear);

        ResolvedData resolved = builder.resolveData(clear.targetNodeId());

        Data node = resolved.data;

        builder.detachedNodes.addAll(node.listChildren());
        builder.detachedNodes.addAll(node.mapChildren().values());

        if (builder.detachedNodes.isEmpty()) {
            return CommandResult.ok();
        }

        Data updatedNode = new Data(node.parent(), clear.commandId(),
                node.scopeOwner(), node.value(), List.of(), Map.of());

        builder.updatedNodes.put(resolved.resolvedId, updatedNode);

        return builder.build();
    }

    private CommandResult handleRemoveByKey(RemoveByKeyCommand removeByKey) {
        Id nodeId = removeByKey.targetNodeId();
        String key = removeByKey.key();

        DirectNodeLookup nodeLookup = new DirectNodeLookup();

        Id childId = nodeLookup.data(nodeId).mapChildren().get(key);

        if (childId == null) {
            return CommandResult.fail("Key not present");
        } else {
            var builder = new ResultBuilder(removeByKey);

            Reject result = builder.detach(childId);
            assert result == null;

            return builder.build();
        }
    }

    private CommandResult handlePut(PutCommand put) {
        Id commandId = put.commandId();
        Id nodeId = put.targetNodeId();
        String key = put.key();
        JsonNode value = put.value();

        Id childId = data(nodeId).get().mapChildren().get(key);
        if (childId != null) {
            DirectNodeLookup nodeLookup = new DirectNodeLookup();
            return createValueChange(put, nodeLookup.resolveData(childId),
                    value);
        } else {
            var builder = new ResultBuilder(put);

            Reject maybeError = builder.createNode(commandId, value, null);
            if (maybeError != null) {
                return maybeError;
            }

            maybeError = builder.attachAs(nodeId, key, commandId);
            if (maybeError != null) {
                return maybeError;
            }

            return builder.build();
        }
    }

    private CommandResult handlePutIfAbsent(PutIfAbsentCommand putIfAbsent) {
        Id commandId = putIfAbsent.commandId();
        Id nodeId = putIfAbsent.targetNodeId();
        String key = putIfAbsent.key();

        var builder = new ResultBuilder(putIfAbsent);

        Id childId = builder.data(nodeId).mapChildren().get(key);
        if (childId != null) {
            if (builder.node(commandId) != null) {
                return CommandResult.fail("Node already exists");
            }

            builder.updatedNodes.put(commandId, new Alias(childId));
        } else {
            Reject maybeError = builder.createNode(commandId,
                    putIfAbsent.value(), putIfAbsent.scopeOwner());
            if (maybeError != null) {
                return maybeError;
            }

            maybeError = builder.attachAs(nodeId, key, commandId);
            if (maybeError != null) {
                return maybeError;
            }
        }

        return builder.build();
    }

    private CommandResult handleInsert(InsertCommand insert) {
        Id commandId = insert.commandId();

        var builder = new ResultBuilder(insert);

        Reject maybeError = builder.createNode(commandId, insert.value(),
                insert.scopeOwner());
        if (maybeError != null) {
            return maybeError;
        }

        maybeError = builder.attachAt(insert.targetNodeId(), insert.position(),
                commandId);
        if (maybeError != null) {
            return maybeError;
        }

        return builder.build();
    }

    private CommandResult handleSet(SetCommand set) {
        DirectNodeLookup nodeLookup = new DirectNodeLookup();

        ResolvedData resolved = nodeLookup.resolveData(set.targetNodeId());

        return createValueChange(set, resolved, set.value());
    }

    private CommandResult handleRemove(RemoveCommand remove) {
        Id nodeId = remove.targetNodeId();

        Id expectedParentId = remove.expectedParentId();
        if (expectedParentId != null) {
            DirectNodeLookup nodeLookup = new DirectNodeLookup();

            Id actualParentId = nodeLookup.data(nodeId).parent();

            if (!nodeLookup.isSameNode(expectedParentId, actualParentId)) {
                return CommandResult.fail("Not a child");
            }
        }

        var builder = new ResultBuilder(remove);

        Reject maybeError = builder.detach(nodeId);
        if (maybeError != null) {
            return maybeError;
        }

        return builder.build();
    }

    private CommandResult handleClearOwner(ClearOwnerCommand clearOwner) {
        Id ownerId = clearOwner.ownerId();

        var builder = new ResultBuilder(clearOwner);

        nodes().forEach((id, nodeOrAlias) -> {
            if (nodeOrAlias instanceof Data node
                    && ownerId.equals(node.scopeOwner())) {
                Reject result = builder.detach(id);
                assert result == null;
            }
        });

        return builder.build();
    }

    private Map<Id, CommandResult> handleTransaction(
            TransactionCommand transaction) {
        List<SignalCommand> commands = transaction.commands();

        MutableTreeRevision scratchpad = new MutableTreeRevision(this);

        var results = new HashMap<Id, CommandResult>();

        Reject firstReject = null;
        for (SignalCommand command : commands) {
            scratchpad.apply(command, results::put);

            CommandResult subResult = results.get(command.commandId());
            if (subResult instanceof Reject reject) {
                firstReject = reject;
                break;
            }
        }

        if (firstReject == null) {
            Map<Id, NodeModification> updates = new HashMap<>();
            Map<Id, SignalCommand.ScopeOwnerCommand> originalInserts = new HashMap<>();

            // Iterate the command list to preserve order
            for (SignalCommand command : commands) {
                Accept op = (Accept) results.get(command.commandId());
                op.updates().forEach((nodeId, modification) -> {
                    NodeModification previous = updates.get(nodeId);
                    if (previous != null) {
                        updates.put(nodeId, new NodeModification(
                                previous.oldNode(), modification.newNode()));
                    } else {
                        updates.put(nodeId, modification);
                    }
                });

                originalInserts.putAll(op.originalInserts());
            }

            results.put(transaction.commandId(),
                    new Accept(updates, originalInserts));
        } else {
            for (SignalCommand command : commands) {
                CommandResult originalResult = results.get(command.commandId());
                if (!(originalResult instanceof Reject)) {
                    results.put(command.commandId(), firstReject);
                }
            }

            results.put(transaction.commandId(), firstReject);
        }

        return results;
    }

    private CommandResult handleSnapshot(SnapshotCommand snapshot) {
        ResultBuilder builder = new ResultBuilder(snapshot);

        builder.updatedNodes.putAll(snapshot.nodes());

        return builder.build();
    }

    private static CommandResult createValueChange(SignalCommand command,
            ResolvedData resolved, JsonNode value) {
        Data oldNode = resolved.data();
        Data newNode = new Node.Data(oldNode.parent(), command.commandId(),
                oldNode.scopeOwner(), value, oldNode.listChildren(),
                oldNode.mapChildren());

        return new Accept(Map.of(resolved.resolvedId(),
                new NodeModification(oldNode, newNode)), Map.of());
    }
}
