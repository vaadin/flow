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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.internal.StateTree.BeforeClientResponseEntry;
import com.vaadin.flow.internal.StateTree.ExecutionRegistration;
import com.vaadin.flow.internal.change.NodeAttachChange;
import com.vaadin.flow.internal.change.NodeChange;
import com.vaadin.flow.internal.change.NodeDetachChange;
import com.vaadin.flow.internal.nodefeature.NodeFeature;
import com.vaadin.flow.internal.nodefeature.NodeFeatureRegistry;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.shared.Registration;

/**
 * A node in the state tree that is synchronized with the client-side. Data
 * stored in nodes is structured into different features to provide isolation.
 * The features available for a node are defined when the node is created.
 *
 * @see StateTree
 * @author Vaadin Ltd
 * @since 1.0
 */
public class StateNode implements Serializable {
    private static class FeatureSetKey implements Serializable {
        private final Set<Class<? extends NodeFeature>> reportedFeatures;
        private final Set<Class<? extends NodeFeature>> nonReportableFeatures;

        public FeatureSetKey(
                Collection<Class<? extends NodeFeature>> reportableFeatureTypes,
                Class<? extends NodeFeature>[] additionalFeatureTypes) {
            reportedFeatures = new HashSet<>(reportableFeatureTypes);
            nonReportableFeatures = Stream.of(additionalFeatureTypes)
                    /*
                     * Should preferably require consistency in whether
                     * reportable are also included in additional, but this is
                     * not practical since both alternatives are currently used
                     * in different implementations.
                     */
                    .filter(type -> !reportableFeatureTypes.contains(type))
                    .collect(Collectors.toSet());

            assert !nonReportableFeatures.removeAll(
                    reportedFeatures) : "No reportable feature should also be non-reportable";
            assert !reportedFeatures.removeAll(
                    nonReportableFeatures) : "No non-reportable feature should also be reportable";
        }

        @Override
        public int hashCode() {
            return Objects.hash(reportedFeatures, nonReportableFeatures);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (obj instanceof FeatureSetKey) {
                FeatureSetKey that = (FeatureSetKey) obj;
                return that.nonReportableFeatures.equals(nonReportableFeatures)
                        && that.reportedFeatures.equals(reportedFeatures);
            } else {
                return false;
            }
        }

        public Stream<Class<? extends NodeFeature>> getAllFeatures() {
            return Stream.concat(nonReportableFeatures.stream(),
                    reportedFeatures.stream());
        }
    }

    private static class FeatureSet implements Serializable {
        private final Set<Class<? extends NodeFeature>> reportedFeatures;

        /**
         * Maps from a node feature type to its index in the {@link #features}
         * array. This instance is cached per unique set of used node feature
         * types in {@link #featureSetCache}.
         */
        private final Map<Class<? extends NodeFeature>, Integer> mappings = new HashMap<>();

        public FeatureSet(FeatureSetKey featureSetKey) {
            reportedFeatures = featureSetKey.reportedFeatures;

            featureSetKey.getAllFeatures()
                    .sorted(NodeFeatureRegistry.PRIORITY_COMPARATOR)
                    .forEach(key -> mappings.put(key,
                            Integer.valueOf(mappings.size())));
        }
    }

    /**
     * Cache of immutable node feature type set instances.
     */
    private static final Map<FeatureSetKey, FeatureSet> featureSetCache = new ConcurrentHashMap<>();

    private final FeatureSet featureSet;

    /**
     * Node feature instances for this node, or a single item.
     */
    private Serializable features;

    private Map<Class<? extends NodeFeature>, Serializable> changes;

    private List<Command> attachListeners;

    private List<Command> detachListeners;

    private NodeOwner owner = NullOwner.get();

    private StateNode parent;

    private int id = -1;

    // Only the root node is attached at this point
    private boolean wasAttached = isAttached();

    private boolean hasBeenAttached;
    private boolean hasBeenDetached;

    private boolean isInactiveSelf;

    private boolean isInitialChanges = true;

    private ArrayList<StateTree.BeforeClientResponseEntry> beforeClientResponseEntries;
    private boolean enabled = true;

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
        this(new ArrayList<>(node.featureSet.reportedFeatures),
                getNonRepeatebleFeatures(node));
    }

    /**
     * Creates a state node with the given feature types and required features
     * that are always sent to the client side.
     *
     * @param reportableFeatureTypes
     *            the list of the features that are required on the client side
     *            (populated even if they are empty)
     * @param additionalFeatureTypes
     *            a collection of feature classes that the node should support.
     *            May, but is not required to, also include reportable feature
     *            types.
     */
    @SafeVarargs
    public StateNode(List<Class<? extends NodeFeature>> reportableFeatureTypes,
            Class<? extends NodeFeature>... additionalFeatureTypes) {
        featureSet = featureSetCache
                .computeIfAbsent(new FeatureSetKey(reportableFeatureTypes,
                        additionalFeatureTypes), FeatureSet::new);

        features = null;
        // Eagerly initialize features that should always be sent
        reportableFeatureTypes.forEach(this::getFeature);
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
        if (hasDetached()) {
            return;
        }
        boolean attachedBefore = isRegistered();
        boolean attachedAfter = false;

        if (parent != null) {
            assert this.parent == null : "Node is already attached to a parent: "
                    + this.parent;
            assert parent.hasChildAssert(this);

            if (isAncestorOf(parent)) {
                throw new IllegalStateException(
                        "Can't set own child as parent");
            }

            attachedAfter = parent.isRegistered();

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

    private boolean hasDetached() {
        return isAttached() && !owner.hasNode(this);
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
        List<Pair<StateNode, Boolean>> attachedNodes = new ArrayList<>();
        visitNodeTreeBottomUp(node -> attachedNodes
                .add(new Pair<>(node, node.handleOnAttach())));
        for (Pair<StateNode, Boolean> pair : attachedNodes) {
            final boolean isInitial = pair.getSecond();
            final StateNode node = pair.getFirst();
            if (node.isRegistered() && (isInitial || node.hasBeenDetached)) {
                node.hasBeenAttached = true;
                node.fireAttachListeners(isInitial);
            }
        }
    }

    /**
     * Called when this node has been detached from its state tree.
     */
    private void onDetach() {
        List<StateNode> nodes = new ArrayList<>();
        visitNodeTreeBottomUp(nodes::add);
        nodes.forEach(StateNode::handleOnDetach);
        for (StateNode node : nodes) {
            if (node.hasBeenAttached) {
                node.hasBeenDetached = true;
                node.fireDetachListeners();
            }
        }
    }

    private void forEachChild(Consumer<StateNode> action) {
        forEachFeature(n -> n.forEachChild(action));
    }

    private void forEachFeature(Consumer<NodeFeature> action) {
        getInitializedFeatures().forEach(action::accept);
    }

    private Stream<NodeFeature> getInitializedFeatures() {
        if (features == null) {
            return Stream.empty();
        } else if (features instanceof NodeFeature) {
            return Stream.of((NodeFeature) features);
        } else {
            return Stream.of((NodeFeature[]) features).filter(Objects::nonNull);
        }
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
     * Removes the node from its parent and unlinks the node (and children) from
     * the state tree.
     */
    public void removeFromTree() {
        visitNodeTree(StateNode::reset);
        setParent(null);
    }

    /**
     * Resets the node to the initial state where it is not owned by a state
     * tree.
     */
    private void reset() {
        owner = NullOwner.get();
        id = -1;
        wasAttached = false;
        hasBeenAttached = false;
        hasBeenDetached = false;
    }

    /**
     * Gets the feature of the given type, creating one if necessary. This
     * method throws {@link IllegalStateException} if this node isn't configured
     * to use the desired feature. Use {@link #hasFeature(Class)} to check
     * whether a node is configured to use a specific feature.
     *
     * @param <T>
     *            the desired feature type
     * @param featureType
     *            the desired feature type, not <code>null</code>
     * @return a feature instance, not <code>null</code>
     */
    public <T extends NodeFeature> T getFeature(Class<T> featureType) {
        int featureIndex = getFeatureIndex(featureType);

        /*
         * To limit memory use, the features array is kept as short as possible
         * and the size is increased when needed.
         *
         * Furthermore, instead of a one-item array, the single item is stored
         * as the field value. This further optimizes the case of text nodes and
         * template model nodes.
         */
        NodeFeature feature;
        if (featureIndex == 0 && features instanceof NodeFeature) {
            feature = (NodeFeature) features;
        } else if (featureIndex == 0 && features == null) {
            feature = NodeFeatureRegistry.create(featureType, this);
            features = feature;
        } else {
            NodeFeature[] featuresArray;
            if (features instanceof NodeFeature[]) {
                featuresArray = (NodeFeature[]) features;
            } else {
                assert features == null || features instanceof NodeFeature;

                featuresArray = new NodeFeature[featureIndex + 1];
                if (features instanceof NodeFeature) {
                    featuresArray[0] = (NodeFeature) features;
                }
                features = featuresArray;
            }

            // Increase size if necessary
            if (featureIndex >= featuresArray.length) {
                featuresArray = Arrays.copyOf(featuresArray, featureIndex + 1);
                features = featuresArray;
            }

            feature = featuresArray[featureIndex];

            if (feature == null) {
                feature = NodeFeatureRegistry.create(featureType, this);
                featuresArray[featureIndex] = feature;
            }
        }

        return featureType.cast(feature);
    }

    private <T extends NodeFeature> int getFeatureIndex(Class<T> featureType) {
        assert featureType != null;

        Integer featureIndex = featureSet.mappings.get(featureType);
        if (featureIndex == null) {
            throw new IllegalStateException(
                    "Node does not have the feature " + featureType);
        }

        return featureIndex.intValue();
    }

    /**
     * Gets the feature of the given type if it has been initialized. This
     * method throws {@link IllegalStateException} if this node isn't configured
     * to use the desired feature. Use {@link #hasFeature(Class)} to check
     * whether a node is configured to use a specific feature.
     *
     * @param <T>
     *            the desired feature type
     * @param featureType
     *            the desired feature type, not <code>null</code>
     * @return a feature instance, or an empty optional if the feature is not
     *         yet initialized for this node
     */
    public <T extends NodeFeature> Optional<T> getFeatureIfInitialized(
            Class<T> featureType) {
        if (features == null) {
            return Optional.empty();
        }
        int featureIndex = getFeatureIndex(featureType);

        if (features instanceof NodeFeature) {
            if (featureIndex == 0) {
                return Optional.of(featureType.cast(features));
            } else {
                return Optional.empty();
            }
        }

        NodeFeature[] featuresArray = (NodeFeature[]) features;

        if (featureIndex >= featuresArray.length) {
            return Optional.empty();
        }

        return Optional.ofNullable(featuresArray[featureIndex])
                .map(featureType::cast);
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

        return featureSet.mappings.containsKey(featureType);
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
     * Gets whether the client side has been initialized for this node.
     * <p>
     * This is used internally by the state tree when processing
     * beforeClientResponse callbacks.
     *
     * @return <code>true</code> if the node has a initialized client side and
     *         <code>false</code> if the client side is not initialized yet
     */
    boolean isClientSideInitialized() {
        return wasAttached;
    }

    /**
     * Collects all changes made to this node since the last time
     * {@link #collectChanges(Consumer)} has been called. If the node is
     * recently attached, then the reported changes will be relative to a newly
     * created node.
     * <p>
     * <b>WARNING:</b> this is in fact an internal (private method) which is
     * expected to be called from {@link StateTree#collectChanges(Consumer)}
     * method only (which is effectively private itself). Don't call this method
     * from any other place because it will break the expected {@link UI} state.
     *
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
                forEachFeature(NodeFeature::generateChangesFromEmpty);
            } else {
                collector.accept(new NodeDetachChange(this));
            }
            wasAttached = isAttached;
        }

        if (!isAttached()) {
            return;
        }
        if (isInactive()) {
            if (isInitialChanges) {
                // send only required (reported) features updates
                Stream<NodeFeature> initialFeatures = Stream
                        .concat(featureSet.mappings.keySet().stream()
                                .filter(this::isReportedFeature)
                                .map(this::getFeature), getDisalowFeatures());
                doCollectChanges(collector, initialFeatures);
            } else {
                doCollectChanges(collector, getDisalowFeatures());
            }
        } else {
            doCollectChanges(collector, getInitializedFeatures());
        }
    }

    private void doCollectChanges(Consumer<NodeChange> collector,
            Stream<NodeFeature> features) {
        features.filter(this::hasChangeTracker).forEach(feature -> {
            feature.collectChanges(collector);
            changes.remove(feature.getClass());
        });
        isInitialChanges = false;
        if (changes != null && changes.isEmpty()) {
            changes = null;
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
                    "Can't move a node from one state tree to another. "
                            + "If this is intentional, first remove the "
                            + "node from its current state tree by calling "
                            + "removeFromTree");
        }
        owner = tree;
    }

    private boolean handleOnAttach() {
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

        return initialAttach;
    }

    private void handleOnDetach() {
        assert isAttached();
        // Ensure detach change is sent
        markAsDirty();

        owner.unregister(this);
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
            List<Command> copy = new ArrayList<>(attachListeners);

            copy.forEach(Command::execute);
        }

        forEachFeature(f -> f.onAttach(initialAttach));
    }

    private void fireDetachListeners() {
        if (detachListeners != null) {
            List<Command> copy = new ArrayList<>(detachListeners);

            copy.forEach(Command::execute);
        }

        forEachFeature(NodeFeature::onDetach);
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
    public void runWhenAttached(SerializableConsumer<UI> command) {

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
        return featureSet.reportedFeatures.contains(featureType);
    }

    /**
     * Update "active"/"inactive" state of the node.
     * <p>
     * The node is considered as inactive if there is at least one feature whose
     * {@link NodeFeature#allowsChanges()} method returns false or it has
     * inactive ascendant.
     * <p>
     * Inactive nodes should restrict their RPC communication with client: only
     * features that returns {@code false} via their method
     * <code>allowsChanges()</code> and reported features send their changes
     * while the node is inactive (the latter features are necessary on the
     * client side to be able to find a strategy which has to be selected to
     * handle the node).
     *
     * <p>
     * Implementation Note: this is done as a separate method instead of
     * calculating the state on the fly (checking all features) because each
     * node needs to check this status on its own <em>AND</em> on its parents
     * (may be all parents up to the root).
     *
     * @see NodeFeature#allowsChanges()
     */
    public void updateActiveState() {
        setInactive(getDisalowFeatures().count() != 0);
    }

    /**
     * Checks whether the node is active.
     * <p>
     * Inactive node should not participate in any RPC communication.
     *
     * @return {@code true} if the node is inactive
     */
    public boolean isInactive() {
        if (isInactiveSelf || getParent() == null) {
            return isInactiveSelf;
        }
        return getParent().isInactive();
    }

    private Stream<NodeFeature> getDisalowFeatures() {
        return getInitializedFeatures()
                .filter(feature -> !feature.allowsChanges());
    }

    private void setInactive(boolean inactive) {
        if (isInactiveSelf != inactive) {
            isInactiveSelf = inactive;

            visitNodeTree(child -> {
                if (!equals(child) && !child.isInactiveSelf) {
                    /*
                     * We are here if: the child node itself is not inactive but
                     * it has some ascendant which is inactive.
                     *
                     * In this case we send only some subset of changes (not
                     * from all the features). But we should send changes for
                     * all remaining features. Normally it automatically happens
                     * if the node becomes "visible". But if it was visible with
                     * some invisible parent then only the parent becomes dirty
                     * (when it's set visible) and this child will never
                     * participate in collection of changes since it's not
                     * marked as dirty.
                     *
                     * So here such node (which is active itself but its
                     * ascendant is inactive) we mark as dirty again to be able
                     * to collect its changes later on when its ascendant
                     * becomes active.
                     */
                    child.markAsDirty();
                }
            });
        }
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

    @SuppressWarnings("rawtypes")
    private static Class[] getNonRepeatebleFeatures(StateNode node) {
        if (node.featureSet.reportedFeatures.isEmpty()) {
            Set<Class<? extends NodeFeature>> set = node.featureSet.mappings
                    .keySet();
            return set.toArray(new Class[set.size()]);
        }
        return node.featureSet.mappings.keySet().stream().filter(
                clazz -> !node.featureSet.reportedFeatures.contains(clazz))
                .toArray(Class[]::new);
    }

    /**
     * Checks whether there are pending executions for this node.
     *
     * @see StateTree#beforeClientResponse(StateNode,
     *      com.vaadin.flow.function.SerializableConsumer)
     *
     * @return <code>true</code> if there are pending executions, otherwise
     *         <code>false</code>
     */
    public boolean hasBeforeClientResponseEntries() {
        return beforeClientResponseEntries != null;
    }

    /**
     * Gets the current list of pending execution entries for this node and
     * clears the current list.
     *
     * @see StateTree#beforeClientResponse(StateNode,
     *      com.vaadin.flow.function.SerializableConsumer)
     *
     * @return the current list of entries, or and empty list if there are no
     *         entries
     */
    public List<StateTree.BeforeClientResponseEntry> dumpBeforeClientResponseEntries() {
        ArrayList<BeforeClientResponseEntry> entries = beforeClientResponseEntries;

        beforeClientResponseEntries = null;

        return !entries.isEmpty() ? entries : Collections.emptyList();
    }

    /**
     * Adds an entry to be executed before the next client response for this
     * node. Entries should always be created through
     * {@link StateTree#beforeClientResponse(StateNode, com.vaadin.flow.function.SerializableConsumer)}
     * to ensure proper ordering.
     *
     * @param entry
     *            the entry to add, not <code>null</code>
     * @return an execution registration that can be used to cancel the
     *         execution
     */
    public ExecutionRegistration addBeforeClientResponseEntry(
            BeforeClientResponseEntry entry) {
        assert entry != null;

        if (beforeClientResponseEntries == null) {
            beforeClientResponseEntries = new ArrayList<>();
        }

        // Effectively final local variable for the lambda
        List<BeforeClientResponseEntry> localEntries = beforeClientResponseEntries;
        localEntries.add(entry);

        return () -> localEntries.remove(entry);
    }

    /**
     * Enables/disables the node.
     *
     * @param enabled
     *            a new enabled state
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Returns enabled state respecting ascendants state.
     * <p>
     * The node may be explicitly disabled via its {@link #setEnabled(boolean)}
     * method (with {@code false} argument value). Also it may be implicitly
     * disabled if its ascendant is explicitly disabled. The method returns the
     * state which may be either explicit or implicit.
     * <p>
     * The method {@link #isEnabledSelf()} returns only explicit enabled state
     * of the node.
     *
     * @see #isEnabledSelf()
     *
     * @return enabled state respecting ascendants state
     */
    public boolean isEnabled() {
        boolean isEnabledSelf = isEnabledSelf();
        if (getParent() != null && isEnabledSelf) {
            return getParent().isEnabled();
        }
        return isEnabledSelf;
    }

    /**
     * Returns the enabled state only for this node.
     * <p>
     * The node may be implicitly or explicitly disabled (see
     * {@link #isEnabled()} method). This method doesn't respect ascendants
     * enabled state. It returns the own state for the node only.
     *
     * @see #isEnabled()
     *
     * @return the node enabled own state
     */
    public boolean isEnabledSelf() {
        return enabled;
    }

    /**
     * This is internal method which may differ from {@link #isAttached()} only
     * during attach/detach event dispatching (inside listeners) when some node
     * still has a parent (all the ascendant) but it's already unregistered.
     *
     * This is intermediate state which may happen only when tree is changed
     * inside listeners.
     *
     * Outside of the listeners this method is effectively the same as
     * {@link #isAttached()}.
     */
    private boolean isRegistered() {
        return isAttached() && getOwner().hasNode(this);
    }

}
