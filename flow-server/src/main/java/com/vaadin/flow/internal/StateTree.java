/*
 * Copyright 2000-2018 Vaadin Ltd.
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

package com.vaadin.flow.internal;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.internal.change.NodeChange;
import com.vaadin.flow.internal.nodefeature.NodeFeature;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.communication.UidlWriter;
import com.vaadin.flow.shared.Registration;

/**
 * The state tree that is synchronized with the client-side.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class StateTree implements NodeOwner {

    private final class RootNode extends StateNode {

        private RootNode(Class<? extends NodeFeature>[] features) {
            super(features);

            // Bootstrap
            setTree(StateTree.this);

            // Assign id
            onAttach();
        }

        @Override
        public void setParent(StateNode parent) {
            if (parent == null) {
                super.setParent(null);
                isRootAttached = false;
            } else {
                throw new IllegalStateException(
                        "Can't set the parent of the tree root");
            }
        }

        @Override
        public boolean isAttached() {
            // the method is called from the super class (which is bad: call
            // overridden method at the class init phase), so there the field
            // from enclosing class is used because it's already initialized at
            // the class constructor call.
            return isRootAttached;
        }

    }

    /**
     * A task to be executed before the client response, together with an
     * execution sequence number and context object.
     * <p>
     * While of this class are stored inside individual state nodes, code
     * outside {@link StateTree} should treat the those as opaque values.
     *
     * @see StateTree#beforeClientResponse(StateNode, SerializableConsumer)
     * @see StateTree#runExecutionsBeforeClientResponse()
     */
    public static final class BeforeClientResponseEntry
            implements Serializable {
        private static final Comparator<BeforeClientResponseEntry> COMPARING_INDEX = Comparator
                .comparingInt(BeforeClientResponseEntry::getIndex);

        private final SerializableConsumer<ExecutionContext> execution;
        private final StateNode stateNode;
        private final int index;

        private BeforeClientResponseEntry(int index, StateNode stateNode,
                SerializableConsumer<ExecutionContext> execution) {
            this.index = index;
            this.stateNode = stateNode;
            this.execution = execution;
        }

        private int getIndex() {
            return index;
        }

        public StateNode getStateNode() {
            return stateNode;
        }

        public SerializableConsumer<ExecutionContext> getExecution() {
            return execution;
        }
    }

    /**
     * A registration object for removing a task registered for execution before
     * the client response.
     */
    @FunctionalInterface
    public interface ExecutionRegistration extends Registration {
        /**
         * Removes the associated task from the execution queue.
         */
        @Override
        void remove();
    }

    private Set<StateNode> dirtyNodes = new LinkedHashSet<>();

    private final Map<Integer, StateNode> idToNode = new HashMap<>();

    private int nextId = 1;

    private Set<StateNode> pendingExecutionNodes = new HashSet<>();

    private int nextBeforeClientResponseIndex = 1;

    private final StateNode rootNode;

    private final UIInternals uiInternals;

    // This field actually belongs to RootNode class but it can'be moved there
    // because its method isAttached() is called before the RootNode class is
    // initialization is done.
    private boolean isRootAttached = true;

    /**
     * Creates a new state tree with a set of features defined for the root
     * node.
     *
     * @param features
     *            the features of the root node
     * @param uiInternals
     *            the internals for the UI that this tree belongs to
     */
    @SafeVarargs
    public StateTree(UIInternals uiInternals,
            Class<? extends NodeFeature>... features) {
        this.uiInternals = uiInternals;
        rootNode = new RootNode(features);
    }

    /**
     * Gets the root node of this state tree. The root node is created together
     * with the tree and can't be detached.
     *
     * @return the root node
     */
    public StateNode getRootNode() {
        return rootNode;
    }

    @Override
    public int register(StateNode node) {
        assert node.getOwner() == this;

        int id = node.getId();

        int nodeId;
        if (id > 0 && !idToNode.containsKey(id)) {
            // Node already had an id, continue using it

            // Don't accept an id that we haven't yet handed out
            assert id < nextId;

            nodeId = id;
        } else {
            nodeId = nextId++;
        }

        idToNode.put(nodeId, node);

        if (node.hasBeforeClientResponseEntries()) {
            pendingExecutionNodes.add(node);
        }

        return nodeId;
    }

    @Override
    public void unregister(StateNode node) {
        assert node.getOwner() == this;

        Integer id = node.getId();

        StateNode removedNode = idToNode.remove(id);

        if (removedNode != node) {
            // Remove by id didn't remove the expected node
            if (removedNode != null) {
                // Put the old node back
                idToNode.put(removedNode.getId(), removedNode);
            }
            throw new IllegalStateException(
                    "Unregistered node was not found based on its id. The tree is most likely corrupted.");
        }

        pendingExecutionNodes.remove(node);
    }

    @Override
    public boolean hasNode(StateNode node) {
        assert node.getOwner() == this;
        return idToNode.containsKey(node.getId());
    }

    /**
     * Finds a node with the given id.
     *
     * @see StateNode#getId()
     * @param id
     *            the node id to look for
     * @return the node with the given id; <code>null</code> if the id is not
     *         registered with this tree
     */
    public StateNode getNodeById(int id) {
        return idToNode.get(id);
    }

    /**
     * Collects all changes made to this tree since the last time
     * {@link #collectChanges(Consumer)} has been called.
     * <p>
     *
     * <b>WARNING</b>: This is an internal method which is not intended to be
     * used outside. The only proper caller of this method is {@link UidlWriter}
     * class (the {@code UidlWriter::encodeChanges} method). Any call of this
     * method in any other place will break the expected {@link UI} state.
     *
     * @param collector
     *            a consumer accepting node changes
     */
    public void collectChanges(Consumer<NodeChange> collector) {
        Set<StateNode> allDirtyNodes = new LinkedHashSet<>();
        boolean evaluateNewDirtyNodes = true;

        // The updateActiveState method can create new dirty nodes, so they need
        // to be collected as well
        while (evaluateNewDirtyNodes) {
            Set<StateNode> dirtyNodesSet = doCollectDirtyNodes(true);
            dirtyNodesSet.forEach(StateNode::updateActiveState);
            evaluateNewDirtyNodes = allDirtyNodes.addAll(dirtyNodesSet);
        }

        // TODO fire preCollect events

        allDirtyNodes.forEach(node -> node.collectChanges(collector));
    }

    @Override
    public void markAsDirty(StateNode node) {
        assert node.getOwner() == this;
        checkHasLock();

        dirtyNodes.add(node);
    }

    /**
     * Gets all the nodes that have been marked.
     *
     * @return a set of dirty nodes, in the order they were marked dirty
     */
    public Set<StateNode> collectDirtyNodes() {
        return doCollectDirtyNodes(false);
    }

    /**
     * Checks if there are nodes that have been marked as dirty since the last
     * time {@link #collectDirtyNodes()} was invoked.
     *
     * @return true if there are dirty nodes, false otherwise
     */
    public boolean hasDirtyNodes() {
        return !dirtyNodes.isEmpty();
    }

    /**
     * Gets the {@link UI} that this tree belongs to.
     *
     * @return the UI that this tree belongs to
     */
    public UI getUI() {
        return uiInternals.getUI();
    }

    /**
     * Registers a task to be executed before the response is sent to the
     * client. The tasks are executed in order of registration. If tasks
     * register more tasks, they are executed after all already registered tasks
     * for the moment.
     * <p>
     * Example: three tasks are submitted, {@code A}, {@code B} and {@code C},
     * where {@code B} produces two more tasks during execution, {@code D} and
     * {@code E}. The resulting execution would be {@code ABCDE}.
     * <p>
     * If the {@link StateNode} related to the task is not attached to the
     * document by the time the task is evaluated, the execution is postponed to
     * before the next response.
     * <p>
     * The task receives a {@link ExecutionContext} as parameter, which contains
     * information about the node state before the response.
     *
     * @param context
     *            the StateNode relevant for the execution. Can not be
     *            <code>null</code>
     * @param execution
     *            the task to be executed. Can not be <code>null</code>
     * @return a registration that can be used to cancel the execution of the
     *         task
     */
    public ExecutionRegistration beforeClientResponse(StateNode context,
            SerializableConsumer<ExecutionContext> execution) {
        checkHasLock();
        assert context != null : "The 'context' parameter can not be null";
        assert execution != null : "The 'execution' parameter can not be null";

        if (context.isAttached()) {
            pendingExecutionNodes.add(context);
        }

        BeforeClientResponseEntry entry = new BeforeClientResponseEntry(
                nextBeforeClientResponseIndex, context, execution);
        nextBeforeClientResponseIndex++;
        return context.addBeforeClientResponseEntry(entry);
    }

    /**
     * Called internally by the framework before the response is sent to the
     * client. All tasks registered at
     * {@link #beforeClientResponse(StateNode, SerializableConsumer)} are
     * evaluated and executed if able.
     */
    public void runExecutionsBeforeClientResponse() {
        while (true) {
            List<StateTree.BeforeClientResponseEntry> callbacks = flushCallbacks();
            if (callbacks.isEmpty()) {
                return;
            }
            callbacks.forEach(entry -> {
                ExecutionContext context = new ExecutionContext(getUI(),
                        entry.getStateNode().isClientSideInitialized());
                entry.getExecution().accept(context);
            });
        }
    }

    private List<StateTree.BeforeClientResponseEntry> flushCallbacks() {
        if (!hasCallbacks()) {
            return Collections.emptyList();
        }

        // Collect
        List<StateTree.BeforeClientResponseEntry> flushed = pendingExecutionNodes
                .stream().map(StateNode::dumpBeforeClientResponseEntries)
                .flatMap(List::stream)
                .sorted(BeforeClientResponseEntry.COMPARING_INDEX)
                .collect(Collectors.toList());

        // Reset bookeeping for the next round
        pendingExecutionNodes = new HashSet<>();

        return flushed;
    }

    private boolean hasCallbacks() {
        return !pendingExecutionNodes.isEmpty();
    }

    /**
     * Checks if there are changes waiting to be sent to the client side.
     *
     * @return <code>true</code> if there are pending changes,
     *         <code>false</code> otherwise
     */
    public boolean isDirty() {
        return hasDirtyNodes() || hasCallbacks();
    }

    private void checkHasLock() {
        VaadinSession session = uiInternals.getSession();
        if (session != null) {
            session.checkHasLock();
        }
    }

    /**
     * Gets all the nodes that have been marked as dirty.
     * <p>
     * If {@code reset} is {@code true} then dirty nodes collection is reset.
     *
     * @return a set of dirty nodes, in the order they were marked dirty
     */
    private Set<StateNode> doCollectDirtyNodes(boolean reset) {
        if (reset) {
            Set<StateNode> collectedNodes = dirtyNodes;
            dirtyNodes = new LinkedHashSet<>();
            return collectedNodes;
        } else {
            return Collections.unmodifiableSet(dirtyNodes);
        }

    }
}
