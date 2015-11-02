package com.vaadin.hummingbird.kernel;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
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
import com.vaadin.server.communication.ServerOnlyKey;

public abstract class StateNode implements Serializable {
    private enum Keys implements ServerOnlyKey {
        TRANSACTION_LOG, NEXT_UNPREVIEWED_LOG_INDEX, COMPUTED_PENDING_FLUSH, COMPUTED, COMPUTED_CACHE, DEPENDENTS;
    }

    private static final Object EMPTY_FLUSH_MARKER = new Object();

    private class ListView extends AbstractList<Object>implements Serializable {
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

        private boolean isAttached() {
            return backing != null;
        }

        private void ensureAttached() {
            if (!isAttached()) {
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
            if (!isAttached()) {
                return 0;
            }

            return backing.size();
        }
    }

    private StateNode parent;
    private int id = 0;

    protected RootNode rootNode;
    private List<Runnable> runOnAttach = null;

    protected StateNode() {
        // Empty
    }

    public <T> T get(Object key, Class<T> type) {
        return assertCast(get(key), type);
    }

    public <T> T get(Class<T> key) {
        return get(key, key);
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
        List<NodeChange> transactionLog = markAsDirty();
        transactionLog.add(change);
    }

    private List<NodeChange> markAsDirty() {
        // Access transaction log so that we will mark ourselves as dirty later
        // on if we're not attached right now
        List<NodeChange> transactionLog = getTransactionLog();
        if (rootNode != null) {
            rootNode.markAsDirty(this);
        }
        return transactionLog;
    }

    private List<NodeChange> getTransactionLog() {
        @SuppressWarnings("unchecked")
        List<NodeChange> log = (List<NodeChange>) doGet(Keys.TRANSACTION_LOG);
        if (log == null) {
            log = new ArrayList<>();
            setValue(Keys.TRANSACTION_LOG, log);
            if (rootNode != null) {
                rootNode.registerTransactionHandler(this,
                        createTransactionHandler());
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

                clearTransactionData();
            }

            private void clearTransactionData() {
                removeValue(Keys.TRANSACTION_LOG);
                removeValue(Keys.NEXT_UNPREVIEWED_LOG_INDEX);
                removeValue(Keys.COMPUTED_PENDING_FLUSH);
            }

            @Override
            public List<NodeChange> commit() {
                List<NodeChange> log = getTransactionLog();
                clearTransactionData();
                return log;
            }

            @Override
            public List<NodeChange> previewChanges() {
                List<NodeChange> transactionLog = getTransactionLog();
                Integer nextUnpreviewedLogIndex = get(
                        Keys.NEXT_UNPREVIEWED_LOG_INDEX, Integer.class);
                if (nextUnpreviewedLogIndex == null) {
                    nextUnpreviewedLogIndex = Integer.valueOf(0);
                }
                List<NodeChange> subList = transactionLog.subList(
                        nextUnpreviewedLogIndex.intValue(),
                        transactionLog.size());

                // Non-transactional put
                setValue(Keys.NEXT_UNPREVIEWED_LOG_INDEX,
                        Integer.valueOf(transactionLog.size()));

                return subList;
            }
        };
    }

    public Object get(Object key) {
        Map<String, ComputedProperty> computed = getComputedProperties();
        if (computed != null && computed.containsKey(key)) {
            StateNode cache = getOrCreateInternalMap(Keys.COMPUTED_CACHE, true);
            if (cache.containsKey(key)) {
                return cache.get(key);
            } else {
                ComputedProperty property = computed.get(key);
                Reactive.compute(() -> {
                    Object value = property.compute(this);
                    cache.put(key, value);

                    Map<Object, Object> pendingFlush = getPendingFlush(false);
                    if (pendingFlush != null && pendingFlush.containsKey(key)) {
                        Object oldValue = pendingFlush.remove(key);
                        if (oldValue != EMPTY_FLUSH_MARKER) {
                            // Add to log without marking as dirty
                            getTransactionLog()
                                    .add(new RemoveChange(key, oldValue));
                        }
                    }
                    // Add to log without marking as dirty
                    getTransactionLog().add(new PutChange(key, value));
                } , () -> {
                    Object oldValue = cache.remove(key);

                    Map<Object, Object> pendingFlush = getPendingFlush(true);
                    pendingFlush.put(key, oldValue);
                    markAsDirty();
                });
                return cache.get(key);
            }
        } else {
            if (Reactive.inComputation()) {
                updateDependents(key, Reactive::registerRead);
            }
            return doGet(key);
        }
    }

    private Map<Object, Object> getPendingFlush(boolean create) {
        @SuppressWarnings("unchecked")
        Map<Object, Object> pendingFlush = (Map<Object, Object>) doGet(
                Keys.COMPUTED_PENDING_FLUSH);
        if (pendingFlush == null && create) {
            pendingFlush = new HashMap<>();
            setValue(Keys.COMPUTED_PENDING_FLUSH, pendingFlush);
        }
        return pendingFlush;
    }

    private void updateDependents(Object key,
            Function<HashSet<Runnable>, HashSet<Runnable>> updater) {
        StateNode map = getOrCreateInternalMap(Keys.DEPENDENTS, false);

        HashSet<Runnable> dependents;
        if (map == null) {
            dependents = null;
        } else {
            /*
             * The contents of the set is not transactional, but that's
             * acceptable since we only add entries, which means that after a
             * rollback there might be some redundant items, but nothing
             * missing.
             */
            dependents = (HashSet<Runnable>) map.doGet(key);
        }

        HashSet<Runnable> newDependents = updater.apply(dependents);

        if (dependents == newDependents) {
            return;
        }

        if (newDependents != null) {
            if (map == null) {
                map = getOrCreateInternalMap(Keys.DEPENDENTS, true);
            }
            map.put(key, newDependents);
        } else if (map != null) {
            map.remove(key);
        }
    }

    protected abstract Object doGet(Object key);

    public boolean containsKey(Object key) {
        if (key != AbstractElementTemplate.Keys.SERVER_ONLY) {
            Map<String, ComputedProperty> computed = getComputedProperties();
            if (computed != null && computed.containsKey(key)) {
                return true;
            }

            if (!(key instanceof Keys) && Reactive.inComputation()) {
                updateDependents(key, Reactive::registerRead);
            }
        }

        return doesContainKey(key);
    }

    @SuppressWarnings("unchecked")
    private Map<String, ComputedProperty> getComputedProperties() {
        return (Map<String, ComputedProperty>) doGet(Keys.COMPUTED);
    }

    protected abstract boolean doesContainKey(Object key);

    protected abstract Object removeValue(Object key);

    protected abstract Object setValue(Object key, Object value);

    protected Stream<Object> getKeys() {
        Map<String, ComputedProperty> computed = getComputedProperties();
        Stream<Object> keys = doGetKeys();
        if (computed == null) {
            return keys;
        } else {
            return Stream.concat(keys, computed.keySet().stream());
        }
    }

    protected abstract Stream<Object> doGetKeys();

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
            root.registerTransactionHandler(this, createTransactionHandler());
        }

        if (runOnAttach != null) {
            runOnAttach.forEach(Runnable::run);
            runOnAttach = null;
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
        getKeys().map(this::doGet).forEach(action);
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
        boolean contained = doesContainKey(key);
        Object previous = setValue(key, value);
        if (contained) {
            logChange(new RemoveChange(key, previous));
            detach(previous);
        }
        logChange(new PutChange(key, value));
        attach(value);

        if ((!contained || !Objects.equals(previous, value))
                && doesContainKey(Keys.DEPENDENTS)) {
            updateDependents(key, Reactive::registerWrite);
        }

        return previous;
    }

    public <T> void put(Class<T> type, T value) {
        // Explicit variable since Eclipse determined that a (Object) cast was
        // unnecessary and could be removed
        Object key = type;
        put(key, value);
    }

    public Object remove(Object key) {
        if (containsKey(key)) {
            updateDependents(key, Reactive::registerWrite);

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

    public void addChangeListener(NodeChangeListener listener) {
        getMultiValued(NodeChangeListener.class).add(listener);
    }

    public void removeChangeListener(NodeChangeListener listener) {
        getMultiValued(NodeChangeListener.class).remove(listener);
    }

    public void enqueueRpc(String string, Object... params) {
        runAttached(() -> {
            getRoot().enqueueRpc(this, string, params);
        });

    }

    public void runAttached(Runnable runnable) {
        if (isAttached()) {
            runnable.run();
        } else {
            if (runOnAttach == null) {
                runOnAttach = new ArrayList<>();
            }
            runOnAttach.add(runnable);
        }
    }

    public boolean get(String key, boolean defaultValue) {
        if (containsKey(key)) {
            return (boolean) get(key);
        } else {
            return defaultValue;
        }
    }

    public double get(String key, double defaultValue) {
        if (containsKey(key)) {
            return (double) get(key);
        } else {
            return defaultValue;
        }
    }

    public int get(String key, int defaultValue) {
        if (containsKey(key)) {
            return (int) get(key);
        } else {
            return defaultValue;
        }
    }

    public String get(String key, String defaultValue) {
        if (containsKey(key)) {
            return (String) get(key);
        } else {
            return defaultValue;
        }
    }

    public void setComputedProperties(
            Map<String, ComputedProperty> computedProperties) {
        assert isUnmodifiable(computedProperties);

        if (containsKey(Keys.COMPUTED)) {
            throw new IllegalStateException(
                    "Computed properties has already been set for this node");
        }

        put(Keys.COMPUTED, computedProperties);

        Map<Object, Object> pendingFlush = getPendingFlush(true);

        computedProperties.keySet()
                .forEach(key -> pendingFlush.put(key, EMPTY_FLUSH_MARKER));

        markAsDirty();
    }

    private static boolean isUnmodifiable(Map<String, ?> map) {
        try {
            map.put("foo", null);
            return false;
        } catch (Exception expected) {
            return true;
        }
    }

    private StateNode getOrCreateInternalMap(Object key,
            boolean createIfNeeded) {
        StateNode map = (StateNode) doGet(key);
        if (map == null && createIfNeeded) {
            map = StateNode.create();
            map.put(AbstractElementTemplate.Keys.SERVER_ONLY, Boolean.TRUE);
            put(key, map);
        }
        return map;
    }

    public void flushComputedProperties() {
        Map<Object, Object> pendingFlush = getPendingFlush(false);
        if (pendingFlush != null) {
            new ArrayList<>(pendingFlush.keySet()).forEach(t -> {
                get(t);
            });
            pendingFlush.clear();
        }
    }

    // Class used as a key to ensure the value is never sent to the client
    private static class RunBeforeClientResponseKey {
        // This class has intentionally been left empty
    }

    public void runBeforeNextClientResponse(Runnable runnable) {
        @SuppressWarnings("unchecked")
        LinkedHashSet<Runnable> pendingRunnables = get(
                RunBeforeClientResponseKey.class, LinkedHashSet.class);
        if (pendingRunnables == null) {
            pendingRunnables = new LinkedHashSet<>();
            put(RunBeforeClientResponseKey.class, pendingRunnables);
            addChangeListener(new NodeChangeListener() {
                @Override
                public void onChange(StateNode stateNode,
                        List<NodeChange> changes) {
                    removeChangeListener(this);
                    @SuppressWarnings("unchecked")
                    LinkedHashSet<Runnable> pendingRunnables = (LinkedHashSet<Runnable>) remove(
                            RunBeforeClientResponseKey.class);
                    assert pendingRunnables != null;
                    pendingRunnables.forEach(Runnable::run);
                }
            });
        }
        pendingRunnables.add(runnable);
    }

}
