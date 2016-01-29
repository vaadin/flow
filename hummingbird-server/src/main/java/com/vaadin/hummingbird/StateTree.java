package com.vaadin.hummingbird;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.vaadin.hummingbird.change.NodeChange;

/**
 * The state tree that is synchronized with the client-side.
 *
 * @since
 * @author Vaadin Ltd
 */
public class StateTree extends NodeOwner {

    private final Map<Integer, StateNode> idToNode = new HashMap<>();
    private int nextId = 0;

    private StateNode rootNode = new StateNode(Collections.emptyList()) {
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
    };

    /**
     * Creates a new state tree.
     */
    public StateTree() {
        rootNode.setOwner(this);
    }

    @Override
    public Collection<StateNode> getNodes() {
        return Collections.unmodifiableCollection(idToNode.values());
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
    public int doRegister(StateNode node) {
        int nodeId = nextId++;
        idToNode.put(Integer.valueOf(nodeId), node);
        return nodeId;
    }

    @Override
    public void doUnregister(StateNode node) {
        Integer id = Integer.valueOf(node.getId());

        StateNode removedNode = idToNode.remove(id);

        if (removedNode != node) {
            if (removedNode != null) {
                idToNode.put(Integer.valueOf(removedNode.getId()), removedNode);
            }
            throw new IllegalStateException(
                    "Invalid node id when removing node");
        }
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
        // TODO fire preCollect events

        collectDirtyNodes().forEach(n -> n.collectChanges(collector));
    }
}
