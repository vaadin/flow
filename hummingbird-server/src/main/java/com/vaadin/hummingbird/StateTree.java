package com.vaadin.hummingbird;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.vaadin.hummingbird.change.NodeChange;
import com.vaadin.hummingbird.namespace.Namespace;

public class StateTree extends NodeOwner {

    private final Map<Integer, StateNode> idToNode = new HashMap<>();
    private int nextId = 0;

    private final StateNode rootNode;

    @SafeVarargs
    public StateTree(Class<? extends Namespace>... namespaces) {
        rootNode = new StateNode(Arrays.asList(namespaces)) {
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
        rootNode.setOwner(this);
    }

    @Override
    public Collection<StateNode> getNodes() {
        return Collections.unmodifiableCollection(idToNode.values());
    }

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

    public StateNode getNodeById(int id) {
        return idToNode.get(Integer.valueOf(id));
    }

    public void collectChanges(Consumer<NodeChange> collector) {
        // TODO fire preCollect events

        collectDirtyNodes().forEach(n -> n.collectChanges(collector));
    }
}
