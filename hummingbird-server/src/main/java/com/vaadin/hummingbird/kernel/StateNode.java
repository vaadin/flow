package com.vaadin.hummingbird.kernel;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.hummingbird.kernel.RootNode.TransactionHandler;
import com.vaadin.hummingbird.kernel.change.IdChange;
import com.vaadin.hummingbird.kernel.change.ListInsertChange;
import com.vaadin.hummingbird.kernel.change.ListRemoveChange;
import com.vaadin.hummingbird.kernel.change.ListReplaceChange;
import com.vaadin.hummingbird.kernel.change.NodeChange;
import com.vaadin.hummingbird.kernel.change.ParentChange;
import com.vaadin.hummingbird.kernel.change.PutChange;
import com.vaadin.hummingbird.kernel.change.RemoveChange;

public abstract class StateNode {
    private enum Keys {
        TRANSACTION_LOG;
    }

    private class ListView extends AbstractList<Object> {
        private Object key;
        private ArrayList<Object> backing;

        public ListView(Object key, ArrayList<Object> backing) {
            assert key != null;
            this.key = key;
            this.backing = backing;
        }

        @Override
        public Object set(int index, Object element) {
            ensureAttached();

            Object previous = backing.set(index, element);
            logChange(new ListReplaceChange(index, key, previous, element));
            detach(previous);
            attach(element);
            return previous;
        }

        private void ensureAttached() {
            if (backing == null) {
                throw new IllegalStateException();
            }
        }

        @Override
        public void add(int index, Object element) {
            ensureAttached();

            backing.add(index, element);
            logChange(new ListInsertChange(index, key, element));
            attach(element);
        }

        @Override
        public Object remove(int index) {
            ensureAttached();
            Object removed = backing.remove(index);
            logChange(new ListRemoveChange(index, key, removed));
            detach(removed);

            return removed;
        }

        @Override
        public Object get(int index) {
            ensureAttached();

            return backing.get(index);
        }

        @Override
        public int size() {
            ensureAttached();

            return backing.size();
        }
    }

    private StateNode parent;
    private int id = 0;

    protected RootNode rootNode;

    protected StateNode() {
        // Empty
    }

    public <T> T get(Object key, Class<T> type) {
        return assertCast(get(key), type);
    }

    @SuppressWarnings("unchecked")
    private static final <T> T assertCast(Object value, Class<T> type) {
        assert value == null || type.isInstance(value);
        return (T) value;
    }

    private void attach(Object value) {
        assert!(value instanceof ListView);

        if (value instanceof StateNode) {
            StateNode stateNode = (StateNode) value;
            RootNode ownRoot = getRoot();
            RootNode childRoot = stateNode.getRoot();

            if (ownRoot != null) {
                if (ownRoot != childRoot) {
                    stateNode.setRoot(ownRoot);
                } else if (isAttached()) {
                    stateNode.register();
                }
            }

            StateNode parent = stateNode.getParent();
            if (parent == null) {
                stateNode.setParent(this);
            } else {
                throw new IllegalStateException();
            }
        }
    }

    private void setParent(StateNode parent) {
        StateNode oldParent = this.parent;
        this.parent = parent;
        logChange(new ParentChange(oldParent, parent));
    }

    private void logChange(NodeChange change) {
        getTransactionLog().add(change);
    }

    private List<NodeChange> getTransactionLog() {
        @SuppressWarnings("unchecked")
        List<NodeChange> log = (List<NodeChange>) get(Keys.TRANSACTION_LOG);
        if (log == null) {
            log = new ArrayList<>();
            setValue(Keys.TRANSACTION_LOG, log);
            if (rootNode != null) {
                rootNode.markAsDirty(this, createTransactionHandler());
            }
        }
        return log;
    }

    private TransactionHandler createTransactionHandler() {
        return new TransactionHandler() {
            @Override
            public void rollback() {
                List<NodeChange> log = getTransactionLog();
                StateNode node = StateNode.this;
                // Iterate in reverse order
                for (int i = log.size() - 1; i >= 0; i--) {
                    node.rollback(log.get(i));
                }

                removeValue(Keys.TRANSACTION_LOG);
            }

            @Override
            public List<NodeChange> commit() {
                List<NodeChange> log = getTransactionLog();
                removeValue(Keys.TRANSACTION_LOG);
                return log;
            }
        };
    }

    public abstract Object get(Object key);

    public abstract boolean containsKey(Object key);

    protected abstract Object removeValue(Object key);

    protected abstract Object setValue(Object key, Object value);

    protected abstract Stream<Object> getKeys();

    public abstract Class<?> getType(Object key);

    private static Map<Class<? extends NodeChange>, BiConsumer<StateNode, ? extends NodeChange>> rollbackHandlers = new HashMap<>();

    private static <T extends NodeChange> void addRollbackHandler(Class<T> type,
            BiConsumer<StateNode, T> handler) {
        rollbackHandlers.put(type, handler);
    }

    static {
        addRollbackHandler(IdChange.class,
                (node, change) -> node.id = change.getOldId());
        addRollbackHandler(ParentChange.class,
                (node, change) -> node.parent = change.getOldParent());
        addRollbackHandler(PutChange.class,
                (node, change) -> node.removeValue(change.getKey()));
        addRollbackHandler(RemoveChange.class, (node, change) -> node
                .setValue(change.getKey(), change.getValue()));
        addRollbackHandler(
                ListInsertChange.class, (node,
                        change) -> node.get(change.getKey(),
                                ListView.class).backing
                                        .remove(change.getIndex()));
        addRollbackHandler(ListRemoveChange.class,
                (node, change) -> node.get(change.getKey(),
                        ListView.class).backing.add(change.getIndex(),
                                change.getValue()));
        addRollbackHandler(ListReplaceChange.class,
                (node, change) -> node.get(change.getKey(),
                        ListView.class).backing.set(change.getIndex(),
                                change.getOldValue()));
    }

    private <T extends NodeChange> void rollback(T change) {
        // Can't delegate to change since it doesn't have access to private
        // values
        @SuppressWarnings("unchecked")
        BiConsumer<StateNode, T> handler = (BiConsumer<StateNode, T>) rollbackHandlers
                .get(change.getClass());
        if (handler == null) {
            throw new IllegalStateException(change.getClass().toString());
        } else {
            handler.accept(this, change);
        }
    }

    private void setRoot(RootNode root) {
        assert rootNode == null;
        assert root != null;
        assert getId() == 0;

        boolean hadTransactionLog = containsKey(Keys.TRANSACTION_LOG);

        rootNode = root;
        setId(root.register(this));

        if (hadTransactionLog) {
            root.markAsDirty(this, createTransactionHandler());
        }

        // Recursively set the root of all children as well
        forEachChildNode(n -> {
            if (n.getRoot() != root) {
                n.setRoot(root);
            }
        });
    }

    protected void setId(int newId) {
        int oldId = id;

        id = newId;
        logChange(new IdChange(oldId, newId));
    }

    private void forEachChildNode(Consumer<StateNode> consumer) {
        Consumer<Object> action = new Consumer<Object>() {
            @Override
            public void accept(Object v) {
                if (v instanceof StateNode) {
                    StateNode childNode = (StateNode) v;
                    consumer.accept(childNode);
                } else if (v instanceof ListView) {
                    ((ListView) v).backing.forEach(this);
                }
            }
        };
        getKeys().map(this::get).forEach(action);
    }

    private void detach(Object value) {
        if (value instanceof StateNode) {
            StateNode childNode = (StateNode) value;
            assert childNode.getParent() == this;
            childNode.setParent(null);

            if (isAttached()) {
                childNode.unregister();
            }
        } else if (value instanceof ListView) {
            ListView listView = (ListView) value;
            listView.backing.forEach(this::detach);
            listView.backing = null;
        }
    }

    private void register() {
        RootNode root = getRoot();
        assert root != null;
        assert!isAttached();

        setId(root.register(this));
        forEachChildNode(StateNode::register);
    }

    private void unregister() {
        assert getRoot() != null;
        assert isAttached();

        setId(getRoot().unregister(this));
        forEachChildNode(StateNode::unregister);
    }

    public boolean isAttached() {
        return getId() > 0;
    }

    public StateNode getParent() {
        return parent;
    }

    public List<Object> getMultiValued(Object key) {
        Object value = get(key);
        if (value instanceof ListView) {
            ListView listView = (ListView) value;
            assert listView.key.equals(key);
            assert listView.backing != null;
            return listView;
        } else {
            ArrayList<Object> backing = new ArrayList<>();
            if (containsKey(key)) {
                backing.add(get(key));
            }

            ListView listView = new ListView(key, backing);
            setValue(key, listView);
            return listView;
        }
    }

    public Object put(Object key, Object value) {
        boolean contained = containsKey(key);
        Object previous = setValue(key, value);
        if (contained) {
            logChange(new RemoveChange(key, previous));
            detach(previous);
        }
        logChange(new PutChange(key, value));
        attach(value);
        return previous;
    }

    public Object remove(Object key) {
        if (containsKey(key)) {
            Object removed = removeValue(key);
            logChange(new RemoveChange(key, removed));
            detach(removed);
            return removed;
        } else {
            return null;
        }
    }

    public Set<String> getStringKeys() {
        Set<?> filteredKeys = getKeys().filter(k -> k instanceof String)
                .collect(Collectors.toSet());

        @SuppressWarnings("unchecked")
        Set<String> stringKeys = (Set<String>) filteredKeys;

        return Collections.unmodifiableSet(stringKeys);
    }

    public RootNode getRoot() {
        return rootNode;
    }

    public int getId() {
        return id;
    }

    public static StateNode create() {
        return new MapStateNode();
    }

    public static StateNode create(Map<Object, Class<?>> explicitTypes) {
        return ClassBackedStateNode.create(explicitTypes);
    }

    public boolean hasAncestor(StateNode node) {
        StateNode n = this;
        while (n != null) {
            if (n == node) {
                return true;
            }
            n = n.getParent();
        }
        return false;
    }
}
