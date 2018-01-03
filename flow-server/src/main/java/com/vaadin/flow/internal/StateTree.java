/*
 * Copyright 2000-2017 Vaadin Ltd.
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

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.change.NodeChange;
import com.vaadin.flow.internal.nodefeature.NodeFeature;
import com.vaadin.flow.shared.Registration;

/**
 * The state tree that is synchronized with the client-side.
 *
 * @author Vaadin Ltd
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
            throw new IllegalStateException(
                    "Can't set the parent of the tree root");
        }

        @Override
        public boolean isAttached() {
            // Root is always attached
            return true;
        }

    }

    /**
     * Object that holds a {@link Runnable} to be executed before the client
     * response, using a {@link StateNode} as context.
     *
     * @see StateTree#beforeClientResponse(StateNode, Runnable) See
     *      {@link StateTree#runExecutionsBeforeClientResponse()}
     */
    private static final class StateNodeOnBeforeClientResponse {
        private WeakReference<StateNode> stateNodeReference;
        private Runnable runnable;

        StateNodeOnBeforeClientResponse(StateNode stateNode,
                Runnable runnable) {
            stateNodeReference = new WeakReference<>(stateNode);
            this.runnable = runnable;
        }

        boolean isStateNodeAttached() {
            StateNode stateNode = stateNodeReference.get();
            return stateNode != null && stateNode.isAttached();
        }

        boolean isAvailable() {
            return stateNodeReference.get() != null;
        }

        void setUnavailable() {
            stateNodeReference.clear();
        }

        Runnable getRunnable() {
            return runnable;
        }
    }

    /**
     * A registration object for removing a runnable registered for execution
     * before the client response.
     */
    @FunctionalInterface
    public interface ExecutionRegistration extends Registration {
        /**
         * Removes the associated runnable from the execution queue.
         */
        @Override
        void remove();
    }

    private Set<StateNode> dirtyNodes = new LinkedHashSet<>();

    private final Map<Integer, StateNode> idToNode = new HashMap<>();

    private LinkedList<StateNodeOnBeforeClientResponse> executionsToProcessBeforeResponse = new LinkedList<>();

    private int nextId = 1;

    private final StateNode rootNode;

    private final UI ui;

    /**
     * Creates a new state tree with a set of features defined for the root
     * node.
     *
     * @param features
     *            the features of the root node
     * @param ui
     *            the UI that this tree belongs to
     */
    @SafeVarargs
    public StateTree(UI ui, Class<? extends NodeFeature>... features) {
        rootNode = new RootNode(features);
        this.ui = ui;
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
        if (id > 0 && !idToNode.containsKey(Integer.valueOf(id))) {
            // Node already had an id, continue using it

            // Don't accept an id that we haven't yet handed out
            assert id < nextId;

            nodeId = id;
        } else {
            nodeId = nextId++;
        }

        idToNode.put(Integer.valueOf(nodeId), node);
        return nodeId;
    }

    @Override
    public void unregister(StateNode node) {
        assert node.getOwner() == this;

        Integer id = Integer.valueOf(node.getId());

        StateNode removedNode = idToNode.remove(id);

        if (removedNode != node) {
            // Remove by id didn't remove the expected node
            if (removedNode != null) {
                // Put the old node back
                idToNode.put(Integer.valueOf(removedNode.getId()), removedNode);
            }
            throw new IllegalStateException(
                    "Unregistered node was not found based on its id. The tree is most likely corrupted.");
        }
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
        return idToNode.get(Integer.valueOf(id));
    }

    /**
     * Collects all changes made to this tree since the last time
     * {@link #collectChanges(Consumer)} has been called.
     *
     * @param collector
     *            a consumer accepting node changes
     */
    public void collectChanges(Consumer<NodeChange> collector) {
        Set<StateNode> dirtyNodesSet = collectDirtyNodes();

        dirtyNodesSet.forEach(StateNode::updateActiveState);

        // TODO fire preCollect events

        dirtyNodesSet.forEach(node -> node.collectChanges(collector));
    }

    @Override
    public void markAsDirty(StateNode node) {
        assert node.getOwner() == this;

        dirtyNodes.add(node);
    }

    /**
     * Gets all the nodes that have been marked as dirty since the last time
     * this method was invoked.
     *
     * @return a set of dirty nodes, in the order they were marked dirty
     */
    public Set<StateNode> collectDirtyNodes() {
        Set<StateNode> collectedNodes = dirtyNodes;
        dirtyNodes = new LinkedHashSet<>();
        return collectedNodes;
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
        return ui;
    }

    /**
     * Registers a {@link Runnable} to be executed before the response is sent
     * to the client. The runnables are executed in order of registration. If
     * runnables register more runnables, they are executed after all already
     * registered executions for the moment.
     * <p>
     * Example: three tasks are submitted, {@code A}, {@code B} and {@code C},
     * where {@code B} produces two more tasks during execution, {@code D} and
     * {@code E}. The resulting execution would be {@code ABCDE}.
     * <p>
     * If the {@link StateNode} related to the runnable is not attached to the
     * document by the time the runnable is evaluated, the execution is
     * postponed to before the next response.
     *
     * @param context
     *            the StateNode relevant for the execution. Can not be
     *            <code>null</code>
     * @param execution
     *            the Runnable to be executed. Can not be <code>null</code>
     * @return a registration that can be used to cancel the execution of the
     *         runnable
     */
    public ExecutionRegistration beforeClientResponse(StateNode context,
            Runnable execution) {
        assert context != null : "The 'context' parameter can not be null";
        assert execution != null : "The 'execution' parameter can not be null";

        StateNodeOnBeforeClientResponse reference = new StateNodeOnBeforeClientResponse(
                context, execution);
        executionsToProcessBeforeResponse.add(reference);
        return reference::setUnavailable;
    }

    /**
     * Called internally by the framework before the response is sent to the
     * client. All runnables registered at
     * {@link #beforeClientResponse(StateNode, Runnable)} are evaluated and
     * executed if able.
     */
    public void runExecutionsBeforeClientResponse() {
        boolean newTasksPossiblyAddedOrNodesAttached = false;
        do {
            newTasksPossiblyAddedOrNodesAttached = false;
            for (StateNodeOnBeforeClientResponse reference : flushCallbacks()) {
                newTasksPossiblyAddedOrNodesAttached = executeRunnableIfAble(
                        reference) || newTasksPossiblyAddedOrNodesAttached;
            }
        } while (newTasksPossiblyAddedOrNodesAttached);
    }

    /**
     * Verifies whether the runnable is able to be executed, by analyzing two
     * scenarios:
     * <ol>
     * <li>If the StateNode associated with the Runnable was garbage collected,
     * the runnable is not considered able to be executed, and is
     * discarded.</li>
     * <li>If the StateNode is present but not attached to the document, the
     * runnable is not considered able to be executed, and it is saved be to
     * executed later. It is evaluated again before the next client
     * response.</li>
     * </ol>
     *
     * @param reference
     *            the StateNodeOnBeforeClientResponse object containing the
     *            StateNode and the Runnable
     * @return <code>true</code> if the runnable was executed right away,
     *         <code>false</code> otherwise
     */
    private boolean executeRunnableIfAble(
            StateNodeOnBeforeClientResponse reference) {
        if (!reference.isAvailable()) {
            return false;
        }
        if (!reference.isStateNodeAttached()) {
            executionsToProcessBeforeResponse.add(reference);
            return false;
        }
        reference.getRunnable().run();
        return true;
    }

    private List<StateNodeOnBeforeClientResponse> flushCallbacks() {
        if (executionsToProcessBeforeResponse.isEmpty()) {
            return Collections.emptyList();
        }
        List<StateNodeOnBeforeClientResponse> flushed = executionsToProcessBeforeResponse;
        executionsToProcessBeforeResponse = new LinkedList<>();
        return flushed;
    }
}
