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

package com.vaadin.flow;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.vaadin.flow.change.NodeAttachChange;
import com.vaadin.flow.change.NodeChange;
import com.vaadin.flow.change.NodeDetachChange;
import com.vaadin.flow.nodefeature.NodeFeature;
import com.vaadin.flow.nodefeature.NodeFeatureRegistry;
import com.vaadin.server.Command;
import com.vaadin.shared.Registration;
import com.vaadin.ui.UI;

/**
 * A node in the state tree that is synchronized with the client-side. Data
 * stored in nodes is structured into different features to provide isolation.
 * The features available for a node are defined when the node is created.
 *
 * @see StateTree
 * @author Vaadin Ltd
 */
public class StateNode implements Serializable {
    private final Map<Class<? extends NodeFeature>, NodeFeature> features = new HashMap<>();

    private final Set<Class<? extends NodeFeature>> reportedFeatures = new HashSet<>();

    private Map<Class<? extends NodeFeature>, Serializable> changes;

    private ArrayList<Command> attachListeners;

    private ArrayList<Command> detachListeners;

    private NodeOwner owner = NullOwner.get();

    private StateNode parent;

    private int id = -1;

    // Only the root node is attached at this point
    private boolean wasAttached = isAttached();

    /**
     * Creates a state node with the given feature types.
     *
     * @param featureTypes
     *            a collection of feature classes that the node should support
     */
    @SafeVarargs
    public StateNode(Class<? extends NodeFeature>... featureTypes) {
        this(Collections.emptyList(), featureTypes);
    }

    /**
     * Create a new instance using the same features as provided {@code node}
     * declares.
     *
     * @param node
     *            the node whose features set will be copied
     */
    @SuppressWarnings("unchecked")
    public StateNode(StateNode node) {
        this(new ArrayList<>(node.reportedFeatures),
                getNonRepeatebleFeatures(node));
    }

    /**
     * Creates a state node with the given feature types and required features
     * that are always sent to the client side.
     *
     * @param reportableFeatureTypes
     *            the list of the features that are required on the client side
     *            (populated even if they are empty)
     * @param nonReportableFeatureTypes
     *            a collection of feature classes that the node should support
     */
    @SafeVarargs
    public StateNode(List<Class<? extends NodeFeature>> reportableFeatureTypes,
            Class<? extends NodeFeature>... nonReportableFeatureTypes) {
        reportedFeatures.addAll(reportableFeatureTypes);
        Stream.concat(reportableFeatureTypes.stream(),
                Stream.of(nonReportableFeatureTypes)).forEach(this::addFeature);
    }

    /**
     * Gets the node owner that this node currently belongs to.
     *
     * @return the node owner
     */
    public NodeOwner getOwner() {
        return owner;
    }

    /**
     * Gets the parent node that this node belongs to.
     *
     * @return the current parent node; <code>null</code> if the node is not
     *         attached to a parent node, or if this node is the root of a state
     *         tree.
     */
    public StateNode getParent() {
        return parent;
    }

    /**
     * Sets the parent node that this node belongs to. This node is set to
     * belong to the node owner of the parent node. The node still retains its
     * owner when the parent is set to <code>null</code>.
     *
     * @param parent
     *            the new parent of this node; or <code>null</code> if this node
     *            is not attached to another node
     */
    public void setParent(StateNode parent) {
        boolean attachedBefore = isAttached();
        boolean attachedAfter = false;

        if (parent != null) {
            assert this.parent == null : "Node is already attached to a parent: "
                    + this.parent;
            assert parent.hasChildAssert(this);

            if (isAncestorOf(parent)) {
                throw new IllegalStateException(
                        "Can't set own child as parent");
            }

            attachedAfter = parent.isAttached();

            NodeOwner parentOwner = parent.getOwner();
            if (parentOwner != owner && parentOwner instanceof StateTree) {
                setTree((StateTree) parentOwner);
            }
        }

        if (!attachedBefore && attachedAfter) {
            this.parent = parent;
            onAttach();
        } else if (attachedBefore && !attachedAfter) {
            onDetach();
            this.parent = parent;
        } else {
            this.parent = parent;
        }
    }

    private boolean isAncestorOf(StateNode node) {
        while (node != null) {
            if (node == this) {
                return true;
            }
            node = node.getParent();
        }

        return false;
    }

    // Should only be used for debugging
    private boolean hasChildAssert(StateNode child) {
        AtomicBoolean found = new AtomicBoolean(false);
        forEachChild(c -> {
            if (c == child) {
                found.set(true);
            }
        });
        return found.get();
    }

    /**
     * Called when this node has been attached to a state tree.
     */
    // protected only to get the root node attached
    protected void onAttach() {
        visitNodeTreeBottomUp(StateNode::handleOnAttach);
    }

    /**
     * Called when this node has been detached from its state tree.
     */
    private void onDetach() {
        visitNodeTreeBottomUp(StateNode::handleOnDetach);
    }

    private void forEachChild(Consumer<StateNode> action) {
        getFeatures().values().forEach(n -> n.forEachChild(action));
    }

    /**
     * Sets the state tree that this node belongs to.
     *
     * @param tree
     *            the state tree
     */
    // protected only to get the root node attached
    protected void setTree(StateTree tree) {
        visitNodeTree(node -> node.doSetTree(tree));
    }

    /**
     * Gets the feature of the given type. This method throws
     * {@link IllegalStateException} if this node does not contain the desired
     * feature. Use {@link #hasFeature(Class)} to check whether a node contains
     * a specific feature.
     *
     * @param <T>
     *            the desired feature type
     * @param featureType
     *            the desired feature type, not <code>null</code>
     * @return a feature instance, not <code>null</code>
     */
    public <T extends NodeFeature> T getFeature(Class<T> featureType) {
        assert featureType != null;

        NodeFeature feature = getFeatures().get(featureType);
        if (feature == null) {
            throw new IllegalStateException(
                    "Node does not have the feature " + featureType);
        }

        return featureType.cast(feature);
    }

    /**
     * Checks whether this node contains a feature.
     *
     * @param featureType
     *            the feature type to check for
     * @return <code>true</code> if this node contains the feature; otherwise
     *         <code>false</code>
     */
    public boolean hasFeature(Class<? extends NodeFeature> featureType) {
        assert featureType != null;

        return getFeatures().containsKey(featureType);
    }

    /**
     * Gets the id of this node. The id is unique within the state tree that the
     * node belongs to. The id is 0 if the node does not belong to any state
     * tree.
     *
     * @see StateTree#getNodeById(int)
     *
     * @return the node id
     */
    public int getId() {
        return id;
    }

    /**
     * Marks this node as dirty.
     *
     * @see StateTree#collectDirtyNodes()
     */
    public void markAsDirty() {
        owner.markAsDirty(this);
    }

    /**
     * Checks whether this node is attached to a state tree.
     *
     * @return <code>true</code> if this node is attached; <code>false</code> if
     *         this node is not attached
     */
    public boolean isAttached() {
        return parent != null && parent.isAttached();
    }

    /**
     * Collects all changes made to this node since the last time
     * {@link #collectChanges(Consumer)} has been called. If the node is
     * recently attached, then the reported changes will be relative to a newly
     * created node.
     *
     * @param collector
     *            a consumer accepting node changes
     */
    public void collectChanges(Consumer<NodeChange> collector) {
        boolean isAttached = isAttached();
        if (isAttached != wasAttached) {
            if (isAttached) {
                collector.accept(new NodeAttachChange(this));

                // Make all changes show up as if the node was recently attached
                clearChanges();
                getFeatures().values()
                        .forEach(NodeFeature::generateChangesFromEmpty);
            } else {
                collector.accept(new NodeDetachChange(this));
            }

            wasAttached = isAttached;
        }

        if (isAttached) {
            getFeatures().values().stream().filter(this::hasChangeTracker)
                    .forEach(feature -> feature.collectChanges(collector));
            clearChanges();
        }
    }

    private boolean hasChangeTracker(NodeFeature nodeFeature) {
        return changes != null && changes.containsKey(nodeFeature.getClass());
    }

    /**
     * Clears all changes recorded for this node. This method is public only for
     * testing purposes.
     */
    public void clearChanges() {
        changes = null;
    }

    /**
     * Applies the {@code visitor} to this node and all its descendants.
     * <p>
     * The visitor is first applied to this node (root) and then to children.
     *
     * @param visitor
     *            visitor to apply
     */
    public void visitNodeTree(Consumer<StateNode> visitor) {
        LinkedList<StateNode> stack = new LinkedList<>();
        stack.add(this);
        while (!stack.isEmpty()) {
            StateNode node = stack.removeFirst();
            visitor.accept(node);
            node.forEachChild(child -> stack.add(0, child));
        }
    }

    /**
     * Applies the {@code visitor} to this node and all its descendants.
     * <p>
     * The visitor is recursively applied to the child nodes before it is
     * applied to this node.
     *
     * @param visitor
     *            visitor to apply
     */
    // package protected for testing
    void visitNodeTreeBottomUp(Consumer<StateNode> visitor) {
        LinkedList<StateNode> stack = new LinkedList<>();
        stack.add(this);
        // not done inside loop to please Sonarcube
        forEachChild(stack::addFirst);
        StateNode previousParent = this;

        while (!stack.isEmpty()) {
            StateNode current = stack.getFirst();
            assert current != null;
            if (current == previousParent) {
                visitor.accept(stack.removeFirst());
                previousParent = current.getParent();
            } else {
                current.forEachChild(stack::addFirst);
                previousParent = current;
            }
        }
    }

    private void doSetTree(StateTree tree) {
        if (tree == owner) {
            return;
        }

        if (owner instanceof StateTree) {
            throw new IllegalStateException(
                    "Can't move a node from one state tree to another");
        }
        owner = tree;
    }

    private void handleOnAttach() {
        assert isAttached();
        boolean initialAttach = false;

        int newId = owner.register(this);

        if (newId != -1) {
            if (id == -1) {
                // Didn't have an id previously, set one now
                id = newId;
                initialAttach = true;
            } else if (newId != id) {
                throw new IllegalStateException(
                        "Can't change id once it has been assigned");
            }

        }
        // Ensure attach change is sent
        markAsDirty();

        fireAttachListeners(initialAttach);
    }

    private void handleOnDetach() {
        assert isAttached();
        // Ensure detach change is sent
        markAsDirty();

        owner.unregister(this);

        fireDetachListeners();
    }

    /**
     * Adds a command as an attach listener. It is executed whenever this state
     * node is attached to the state tree.
     *
     * @param attachListener
     *            the attach listener to add
     * @return an event registration handle for removing the listener
     */
    public Registration addAttachListener(Command attachListener) {
        assert attachListener != null;

        if (attachListeners == null) {
            attachListeners = new ArrayList<>(1);
        }
        attachListeners.add(attachListener);

        return () -> removeAttachListener(attachListener);
    }

    /**
     * Adds a command as a detach listener. It is executed whenever this state
     * node is detached from the state tree.
     *
     * @param detachListener
     *            the detach listener to add
     * @return an event registration handle for removing the listener
     */
    public Registration addDetachListener(Command detachListener) {
        assert detachListener != null;

        if (detachListeners == null) {
            detachListeners = new ArrayList<>(1);
        }
        detachListeners.add(detachListener);

        return () -> removeDetachListener(detachListener);
    }

    private void removeAttachListener(Command attachListener) {
        assert attachListener != null;

        attachListeners.remove(attachListener);

        if (attachListeners.isEmpty()) {
            attachListeners = null;
        }
    }

    private void removeDetachListener(Command detachListener) {
        assert detachListener != null;

        detachListeners.remove(detachListener);

        if (detachListeners.isEmpty()) {
            detachListeners = null;
        }
    }

    private void fireAttachListeners(boolean initialAttach) {
        if (attachListeners != null) {
            ArrayList<Command> copy = new ArrayList<>(attachListeners);

            copy.forEach(Command::execute);
        }

        getFeatures().values().forEach(f -> f.onAttach(initialAttach));
    }

    private void fireDetachListeners() {
        if (detachListeners != null) {
            ArrayList<Command> copy = new ArrayList<>(detachListeners);

            copy.forEach(Command::execute);
        }

        getFeatures().values().forEach(NodeFeature::onDetach);
    }

    /**
     * Gets or creates a change tracker object for the provided feature.
     *
     * @param <T>
     *            the change tracker type
     * @param feature
     *            the feature for which to get a change tracker
     * @param factory
     *            a factory method used to create a new tracker if there isn't
     *            already one
     * @return the change tracker to use
     */
    @SuppressWarnings("unchecked")
    public <T extends Serializable> T getChangeTracker(NodeFeature feature,
            Supplier<T> factory) {
        if (changes == null) {
            changes = new HashMap<>();
        }

        return (T) changes.computeIfAbsent(feature.getClass(),
                k -> factory.get());
    }

    /**
     * Runs the command when the node is attached to a UI.
     * <p>
     * If the node is already attached when this method is called, the method is
     * run immediately.
     *
     * @param command
     *            the command to run immediately or when the node is attached
     */
    public void runWhenAttached(Consumer<UI> command) {

        if (isAttached()) {
            command.accept(getUI());
        } else {
            addAttachListener(new Command() {
                @Override
                public void execute() {
                    command.accept(getUI());
                    removeAttachListener(this);
                }
            });
        }
    }

    /**
     * Returns whether the {@code featureType} should be reported to the client
     * even if it doesn't contain any data.
     *
     * @param featureType
     *            feature type which needs to be populated on the client
     * @return whether the feature required by the client side
     */
    public boolean isReportedFeature(Class<? extends NodeFeature> featureType) {
        return reportedFeatures.contains(featureType);
    }

    /**
     * Internal helper for getting the UI instance for a node attached to a
     * StateTree. Assumes the node is attached.
     *
     * @return the UI this node is attached to
     */
    private UI getUI() {
        assert isAttached();
        assert getOwner() instanceof StateTree : "Attach should only be called when the node has been attached to the tree, not to a null owner";
        return ((StateTree) getOwner()).getUI();
    }

    private void addFeature(Class<? extends NodeFeature> featureType) {
        if (!features.containsKey(featureType)) {
            NodeFeature feature = NodeFeatureRegistry.create(featureType, this);
            features.put(featureType, feature);
        }
    }

    private Map<Class<? extends NodeFeature>, NodeFeature> getFeatures() {
        return features;
    }

    @SuppressWarnings("rawtypes")
    private static Class[] getNonRepeatebleFeatures(StateNode node) {
        if (node.reportedFeatures.isEmpty()) {
            Set<Class<? extends NodeFeature>> set = node.features.keySet();
            return set.toArray(new Class[set.size()]);
        }
        return node.features.keySet().stream()
                .filter(clazz -> !node.reportedFeatures.contains(clazz))
                .toArray(Class[]::new);
    }
}
