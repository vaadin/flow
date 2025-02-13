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
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

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
        protected Node node(Id nodeId) {
            if (detachedNodes.contains(nodeId)) {
                return null;
            }

            Node node = updatedNodes.get(nodeId);
            if (node != null) {
                return node;
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

            LinkedHashMap<String, Id> map = new LinkedHashMap<>(
                    node.mapChildren());
            if (mapUpdater.apply(map) instanceof Reject error) {
                return error;
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
            if (listUpdater.apply(list) instanceof Reject error) {
                return error;
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

            if (attacher.apply(resolvedParentId,
                    resolvedChildId) instanceof Reject error) {
                return error;
            }

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

        private Reject createNode(Id nodeId, JsonNode value, Id scopeOwner) {
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

        private NodeModification createModification(Id id, Node newNode) {
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
        // Custom logic for transactions that can produce multiple results
        if (command instanceof TransactionCommand transaction) {
            Map<Id, CommandResult> results = handleTransaction(transaction);

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

        if (builder.detach(childId) instanceof Reject error) {
            return error;
        }

        if (builder.attachAs(nodeId, key, childId) instanceof Reject error) {
            return error;
        }

        return builder.build();
    }

    private CommandResult handleAdoptAt(AdoptAtCommand adoptAt) {
        Id nodeId = adoptAt.targetNodeId();
        Id childId = adoptAt.childId();
        ListPosition pos = adoptAt.position();

        var builder = new ResultBuilder(adoptAt);

        if (builder.detach(childId) instanceof Reject error) {
            return error;
        }

        if (builder.attachAt(nodeId, pos, childId) instanceof Reject error) {
            return error;
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

            if (builder.createNode(commandId, value,
                    null) instanceof Reject error) {
                return error;
            }

            if (builder.attachAs(nodeId, key,
                    commandId) instanceof Reject error) {
                return error;
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
            if (builder.createNode(commandId, putIfAbsent.value(),
                    putIfAbsent.scopeOwner()) instanceof Reject error) {
                return error;
            }

            if (builder.attachAs(nodeId, key,
                    commandId) instanceof Reject error) {
                return error;
            }
        }

        return builder.build();
    }

    private CommandResult handleInsert(InsertCommand insert) {
        Id commandId = insert.commandId();

        var builder = new ResultBuilder(insert);

        if (builder.createNode(commandId, insert.value(),
                insert.scopeOwner()) instanceof Reject error) {
            return error;
        }

        if (builder.attachAt(insert.targetNodeId(), insert.position(),
                commandId) instanceof Reject error) {
            return error;
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

        if (builder.detach(nodeId) instanceof Reject error) {
            return error;
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
