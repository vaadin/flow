package com.vaadin.hummingbird.kernel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.hummingbird.kernel.change.NodeChange;
import com.vaadin.hummingbird.kernel.change.NodeChangeVisitor;

public class RootNode extends MapStateNode {
    public interface TransactionHandler {
        public List<NodeChange> commit();

        public void rollback();

        public List<NodeChange> previewChanges();
    }

    public class PendingRpc {
        private final StateNode context;
        private final String javascript;
        private final Object[] params;

        public PendingRpc(StateNode context, String javascript,
                Object[] params) {
            this.context = context;
            this.javascript = javascript;
            this.params = params;
        }

        public StateNode getContext() {
            return context;
        }

        public String getJavascript() {
            return javascript;
        }

        public Object[] getParams() {
            return params;
        }
    }

    private int nextId = 1;
    private Map<Integer, StateNode> idToNode = new HashMap<>();
    private Map<Integer, StateNode> transactionIdToNode = new HashMap<>();

    private Map<StateNode, TransactionHandler> dirtyInTransaction = new LinkedHashMap<>();
    private Set<NodeChangeVisitor> commitVisitors = new HashSet<>();

    private List<PendingRpc> pendingRpc = new ArrayList<>();

    private Set<StateNode> preCommitChanges = new HashSet<>();

    public RootNode() {
        rootNode = this;
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

        do {
            Set<StateNode> changes = new HashSet<>(preCommitChanges);
            preCommitChanges.clear();

            for (StateNode stateNode : changes) {
                ((AbstractStateNode) stateNode).flushComputedProperties();
                if (stateNode.containsKey(NodeChangeListener.class)) {
                    List<Object> listeners = stateNode
                            .getMultiValued(NodeChangeListener.class);
                    List<NodeChange> previewChanges = new ArrayList<>(
                            dirtyInTransaction.get(stateNode).previewChanges());
                    for (Object o : new ArrayList<>(listeners)) {
                        NodeChangeListener l = (NodeChangeListener) o;
                        l.onChange(stateNode, previewChanges);
                    }
                }
            }
        } while (!preCommitChanges.isEmpty());

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
                for (Entry<StateNode, List<NodeChange>> entry : changes
                        .entrySet()) {
                    for (NodeChange change : entry.getValue()) {
                        change.accept(entry.getKey(), visitor);
                    }
                }
            }
        }
    }

    public void rollback() {
        transactionIdToNode.clear();

        dirtyInTransaction.values().forEach(TransactionHandler::rollback);
        dirtyInTransaction.clear();

        pendingRpc.clear();
    }

    public void registerTransactionHandler(StateNode node,
            TransactionHandler handler) {
        // Second case is when a nodes is marked as dirty because it's about to
        // become attached
        // assert node.isAttached() || node.getId() == 0;
        assert node.getRoot() == this;
        assert !dirtyInTransaction.containsKey(node);

        dirtyInTransaction.put(node, handler);
    }

    public void markAsDirty(StateNode node) {
        preCommitChanges.add(node);
    }

    public List<PendingRpc> flushRpcQueue() {
        List<PendingRpc> collect = pendingRpc.stream()
                .filter(r -> r.context.isAttached())
                .collect(Collectors.toList());
        pendingRpc.clear();
        return collect;
    }

    public void enqueueRpc(StateNode context, String javascript,
            Object... params) {
        pendingRpc.add(new PendingRpc(context, javascript, params));
    }

    public boolean isDirty() {
        return !dirtyInTransaction.isEmpty();
    }

}
