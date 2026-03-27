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

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.DoubleNode;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.NumericNode;

import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.function.SerializableBiFunction;
import com.vaadin.flow.function.SerializableFunction;
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
        protected abstract @Nullable Node node(Id nodeId);

        protected boolean isChildAt(List<Id> children, int index,
                Id expectedChild) {
            assert expectedChild != null;

            if (index < 0 || index >= children.size()) {
                return false;
            }

            return isSameNode(children.get(index), expectedChild);
        }

        protected @Nullable Data data(Id nodeId) {
            Node node = node(resolveAlias(nodeId));

            if (node instanceof Data data) {
                return data;
            }
            return null;
        }

        protected boolean isSameNode(@Nullable Id a, @Nullable Id b) {
            if (a == null || b == null) {
                return Objects.equals(a, b);
            }
            return Objects.equals(resolveAlias(a), resolveAlias(b));
        }

        protected Id resolveAlias(Id id) {
            Node node = node(id);

            if (node instanceof Alias(Id target)) {
                return target;
            } else {
                return id;
            }
        }

        protected @Nullable ResolvedData resolveData(Id nodeId) {
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
        protected @Nullable Node node(Id nodeId) {
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
    private class ResultBuilder extends NodeLookup {
        private final Map<Id, Node> updatedNodes = new HashMap<>();
        private final Set<Id> detachedNodes = new HashSet<>();
        private final Map<Id, SignalCommand.ScopeOwnerCommand> originalInserts = new HashMap<>();

        private final SignalCommand command;

        public ResultBuilder(SignalCommand command) {
            this.command = command;
        }

        private Reject fail(String reason) {
            return CommandResult.fail(reason);
        }

        @Override
        protected @Nullable Node node(Id nodeId) {
            if (detachedNodes.contains(nodeId)) {
                return null;
            }

            Node node = updatedNodes.get(nodeId);
            if (node != null) {
                return node;
            }

            return nodes().get(nodeId);
        }

        private @Nullable Reject detach(Id nodeId) {
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
            if (parentData == null) {
                return fail("Parent node not found");
            }

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

        private @Nullable Reject updateMapChildren(Id resolvedParentId,
                SerializableFunction<Map<String, Id>, CommandResult.@Nullable Reject> mapUpdater) {
            Data node = data(resolvedParentId);
            if (node == null) {
                return fail("Node not found");
            }

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

        private @Nullable Reject updateListChildren(Id resolvedParentId,
                SerializableFunction<List<Id>, CommandResult.@Nullable Reject> listUpdater) {
            Data node = data(resolvedParentId);
            if (node == null) {
                return fail("Node not found");
            }

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

        private @Nullable Reject attach(Id parentId, Id childId,
                SerializableBiFunction<Id, Id, @Nullable Reject> attacher) {
            Id resolvedParentId = resolveAlias(parentId);
            Id resolvedChildId = resolveAlias(childId);

            if (!detachedNodes.contains(resolvedChildId)) {
                return fail("Node is not detached");
            }

            // Validate child exists before modifying state
            Data child = data(resolvedChildId);
            if (child == null) {
                return fail("Child node not found");
            }

            Id ancestor = resolvedParentId;
            while (ancestor != null) {
                if (ancestor.equals(resolvedChildId)) {
                    return fail("Cannot attach to own descendant");
                }

                Data ancestorData = data(ancestor);
                if (ancestorData == null) {
                    return fail("Ancestor node not found");
                }
                ancestor = ancestorData.parent();
            }

            detachedNodes.remove(resolvedChildId);

            Reject maybeError = attacher.apply(resolvedParentId,
                    resolvedChildId);
            if (maybeError != null) {
                return maybeError;
            }

            updatedNodes.put(resolvedChildId,
                    new Data(resolvedParentId, child.lastUpdate(),
                            child.scopeOwner(), child.value(),
                            child.listChildren(), child.mapChildren()));

            return null;
        }

        private @Nullable Reject attachAs(Id parentId, String key, Id childId) {
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
            Id after = insertPosition.after();
            if (after != null) {
                after = resolveAlias(after);
            }
            Id before = insertPosition.before();
            if (before != null) {
                before = resolveAlias(before);
            }

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

        private @Nullable Reject attachAt(Id parentId, ListPosition position,
                Id childId) {
            return attach(parentId, childId,
                    (resolvedParentId, resolvedChildId) -> {
                        Data parentData = data(resolvedParentId);
                        if (parentData == null) {
                            return fail("Parent node not found");
                        }
                        int insertIndex = findInsertIndex(
                                parentData.listChildren(), position);
                        if (insertIndex == -1) {
                            return fail("Insert position not matched");
                        }

                        return updateListChildren(resolvedParentId, list -> {
                            list.add(insertIndex, resolvedChildId);
                            return null;
                        });
                    });
        }

        private @Nullable Reject createNode(Id nodeId, @Nullable JsonNode value,
                @Nullable Id scopeOwner) {
            if (node(nodeId) != null) {
                return fail("Node already exists");
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

        private CommandResult build() {

            Map<Id, NodeModification> updates = new HashMap<>();

            updatedNodes.forEach((id, newNode) -> {
                if (!detachedNodes.contains(id)) {
                    updates.put(id, createModification(id, newNode));
                }
            });

            if (!detachedNodes.isEmpty()) {
                Map<Id, List<Id>> reverseAliases = new HashMap<>();
                nodes().forEach((signalId, nodeOrAlias) -> {
                    if (nodeOrAlias instanceof Alias(Id target)) {
                        reverseAliases
                                .computeIfAbsent(target,
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

                    MutableTreeRevision.this.data(removed).ifPresent(node -> {
                        toDetach.addAll(node.listChildren());
                        toDetach.addAll(node.mapChildren().values());
                    });
                }
            }

            return new Accept(updates, originalInserts);
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
            @Nullable SerializableBiConsumer<Id, CommandResult> resultCollector) {
        // Custom logic for transactions that can produce multiple results
        if (command instanceof TransactionCommand transaction) {
            Map<Id, CommandResult> results = handleTransaction(transaction);

            CommandResult transactionResult = results
                    .get(transaction.commandId());
            if (transactionResult != null) {
                applyResult(transactionResult);
            }

            if (resultCollector != null) {
                results.forEach(resultCollector);
            }
        } else {
            CommandResult result;
            if (!nodes().containsKey(command.targetNodeId())) {
                result = CommandResult.fail("Node not found");
            } else {
                @SuppressWarnings("unchecked")
                SerializableBiFunction<MutableTreeRevision, SignalCommand, CommandResult> handler = (SerializableBiFunction<MutableTreeRevision, SignalCommand, CommandResult>) handlers
                        .get(command.getClass());

                if (handler == null) {
                    result = CommandResult.fail("Unknown command type");
                } else {
                    result = handler.apply(this, command);
                }
            }

            applyResult(result);

            if (resultCollector != null) {
                resultCollector.accept(command.commandId(), result);
            }
        }

        assert assertValidTree();
    }

    private void applyResult(CommandResult result) {
        if (result instanceof Accept(Map<Id, NodeModification> updates, Map<Id, ScopeOwnerCommand> originalInserts)) {
            updates.forEach((nodeId, update) -> {
                Node newNode = update.newNode();

                if (newNode == null) {
                    nodes().remove(nodeId);
                    originalInserts().remove(nodeId);
                } else {
                    nodes().put(nodeId, newNode);
                }
            });

            originalInserts().putAll(originalInserts);
        }
    }

    private static final Map<Class<? extends SignalCommand>, SerializableBiFunction<MutableTreeRevision, ? extends SignalCommand, CommandResult>> handlers = new HashMap<>();

    private static <T extends SignalCommand> void addHandler(
            Class<T> commandType,
            SerializableBiFunction<MutableTreeRevision, T, CommandResult> handler) {
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

        Data nodeData = data(test.targetNodeId()).orElse(null);
        if (nodeData == null) {
            return CommandResult.fail("Node not found");
        }
        List<Id> listChildren = nodeData.listChildren();

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

        Data data = data(nodeId).orElse(null);
        if (data == null) {
            return CommandResult.fail("Node not found");
        }
        Id actualChildId = data.mapChildren().get(key);

        if (expectedChild == null) {
            return CommandResult.conditional(actualChildId != null,
                    "Key not present");
        } else if (Id.ZERO.equals(expectedChild)) {
            return CommandResult.conditional(actualChildId == null,
                    "A key is present");
        } else {
            DirectNodeLookup nodeLookup = new DirectNodeLookup();

            boolean isSame = actualChildId != null
                    && nodeLookup.isSameNode(actualChildId, expectedChild);
            return CommandResult.conditional(isSame, "Unexpected child");
        }
    }

    private CommandResult handleLastUpdateCondition(
            LastUpdateCondition lastUpdateTest) {

        Data data = data(lastUpdateTest.targetNodeId()).orElse(null);
        if (data == null) {
            return CommandResult.fail("Node not found");
        }
        Id lastUpdate = data.lastUpdate();

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
        if (resolved == null) {
            return CommandResult.fail("Node not found");
        }

        double delta = increment.delta();

        JsonNode oldValue = resolved.data().value();

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
        if (resolved == null) {
            return CommandResult.fail("Node not found");
        }

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

        Data nodeData = nodeLookup.data(nodeId);
        if (nodeData == null) {
            return CommandResult.fail("Node not found");
        }
        Id childId = nodeData.mapChildren().get(key);

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

        Data data = data(nodeId).orElse(null);
        if (data == null) {
            return CommandResult.fail("Node not found");
        }
        Id childId = data.mapChildren().get(key);
        if (childId != null) {
            DirectNodeLookup nodeLookup = new DirectNodeLookup();
            ResolvedData resolved = nodeLookup.resolveData(childId);
            if (resolved == null) {
                return CommandResult.fail("Child node not found");
            }
            return createValueChange(put, resolved, value);
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

        Data nodeData = builder.data(nodeId);
        if (nodeData == null) {
            return CommandResult.fail("Node not found");
        }
        Id childId = nodeData.mapChildren().get(key);
        if (childId != null) {
            // Key already exists - mark parent as accessed but don't create
            // alias
            Id resolvedNodeId = builder.resolveAlias(nodeId);
            Data resolvedNodeData = builder.data(resolvedNodeId);
            if (resolvedNodeData != null) {
                builder.updatedNodes.put(resolvedNodeId, resolvedNodeData);
            }
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
        if (resolved == null) {
            return CommandResult.fail("Node not found");
        }

        return createValueChange(set, resolved, set.value());
    }

    private CommandResult handleRemove(RemoveCommand remove) {
        Id nodeId = remove.targetNodeId();

        Id expectedParentId = remove.expectedParentId();
        if (expectedParentId != null) {
            DirectNodeLookup nodeLookup = new DirectNodeLookup();

            Data nodeData = nodeLookup.data(nodeId);
            if (nodeData == null) {
                return CommandResult.fail("Node not found");
            }
            Id actualParentId = nodeData.parent();

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
                CommandResult commandResult = results.get(command.commandId());
                if (!(commandResult instanceof Accept(Map<Id, NodeModification> updates1, Map<Id, ScopeOwnerCommand> inserts))) {
                    continue;
                }
                updates1.forEach((nodeId, modification) -> {
                    NodeModification previous = updates.get(nodeId);
                    if (previous != null) {
                        updates.put(nodeId, new NodeModification(
                                previous.oldNode(), modification.newNode()));
                    } else {
                        updates.put(nodeId, modification);
                    }
                });

                originalInserts.putAll(inserts);
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
            ResolvedData resolved, @Nullable JsonNode value) {
        Data oldNode = resolved.data();
        Data newNode = new Node.Data(oldNode.parent(), command.commandId(),
                oldNode.scopeOwner(), value, oldNode.listChildren(),
                oldNode.mapChildren());

        return new Accept(Map.of(resolved.resolvedId(),
                new NodeModification(oldNode, newNode)), Map.of());
    }
}
