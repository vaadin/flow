package com.vaadin.hummingbird.kernel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.vaadin.hummingbird.kernel.change.NodeChange;
import com.vaadin.hummingbird.kernel.change.NodeChangeVisitor;

public class RootNode extends MapStateNode {
    public interface TransactionHandler {
        public List<NodeChange> commit();

        public void rollback();
    }

    private int nextId = 1;
    private Map<Integer, StateNode> idToNode = new HashMap<>();
    private Map<Integer, StateNode> transactionIdToNode = new HashMap<>();

    private Map<StateNode, TransactionHandler> dirtyInTransaction = new LinkedHashMap<>();
    private Set<NodeChangeVisitor> commitVisitors = new HashSet<>();

    public RootNode() {
        this.rootNode = this;
        setId(register(this));
    }

    public StateNode getById(int id) {
        Integer key = Integer.valueOf(id);
        if (transactionIdToNode.containsKey(key)) {
            return transactionIdToNode.get(key);
        }
        return idToNode.get(key);
    }

    public int register(StateNode node) {
        assert node.getRoot() == this;
        int id = node.getId();
        assert id <= 0;

        if (id == 0) {
            id = nextId++;
        } else {
            id = -id;
        }

        assert getById(id) == null;

        transactionIdToNode.put(Integer.valueOf(id), node);
        return id;
    }

    public int unregister(StateNode node) {
        assert node.getRoot() == this;
        assert node.isAttached();

        Integer idObj = Integer.valueOf(node.getId());
        assert getById(node.getId()) == node;

        transactionIdToNode.put(idObj, null);

        return -node.getId();
    }

    public void addCommitVisitor(NodeChangeVisitor visitor) {
        commitVisitors.add(visitor);
    }

    public void removeCommitVisitor(NodeChangeVisitor visitor) {
        commitVisitors.remove(visitor);
    }

    public void commit(NodeChangeVisitor visitor) {
        assert !commitVisitors.contains(visitor);
        commitVisitors.add(visitor);
        commit();
        commitVisitors.remove(visitor);
    }

    public void commit() {

        transactionIdToNode.forEach((k, v) -> {
            if (v == null) {
                idToNode.remove(k);
            } else {
                idToNode.put(k, v);
            }
        });
        transactionIdToNode.clear();

        Map<StateNode, List<NodeChange>> changes = new LinkedHashMap<>();

        dirtyInTransaction.forEach((node, listener) -> {
            changes.put(node, listener.commit());
        });
        dirtyInTransaction.clear();

        if (!commitVisitors.isEmpty()) {
            for (NodeChangeVisitor visitor : new ArrayList<>(commitVisitors)) {
                for (Entry<StateNode, List<NodeChange>> entry : changes.entrySet()) {
                    for (NodeChange change : entry.getValue())
                        change.accept(entry.getKey(), visitor);
                }
            }
        }
    }

    public void rollback() {
        transactionIdToNode.clear();

        dirtyInTransaction.values().forEach(TransactionHandler::rollback);
        dirtyInTransaction.clear();
    }

    public void markAsDirty(StateNode node, TransactionHandler handler) {
        // Second case is when a nodes is marked as dirty because it's about to
        // become attached
        assert node.isAttached() || node.getId() == 0;
        assert node.getRoot() == this;
        assert !dirtyInTransaction.containsKey(node);

        dirtyInTransaction.put(node, handler);
    }
}
