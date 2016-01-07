package com.vaadin.hummingbird.kernel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.hummingbird.kernel.RootNode.TransactionHandler;
import com.vaadin.hummingbird.kernel.ValueType.ArrayType;
import com.vaadin.hummingbird.kernel.change.IdChange;
import com.vaadin.hummingbird.kernel.change.NodeChange;
import com.vaadin.hummingbird.kernel.change.ParentChange;
import com.vaadin.hummingbird.kernel.change.PutChange;
import com.vaadin.hummingbird.kernel.change.RemoveChange;
import com.vaadin.server.communication.ServerOnlyKey;

public abstract class AbstractStateNode implements StateNode {
    private enum Keys implements ServerOnlyKey {
        TRANSACTION_LOG, NEXT_UNPREVIEWED_LOG_INDEX, COMPUTED_PENDING_FLUSH, COMPUTED, COMPUTED_CACHE, DEPENDENTS;
    }

    private static final Object EMPTY_FLUSH_MARKER = new Object();

    private StateNode parent;
    private int id = 0;

    protected RootNode rootNode;
    private List<Runnable> runOnAttach = null;

    protected AbstractStateNode() {
        // Empty
    }

    public <T> T get(Class<T> key) {
        return get(key, key);
    }

    @Override
    public void attachChild(Object value) {
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

    @Override
    public void setParent(StateNode parent) {
        StateNode oldParent = this.parent;
        this.parent = parent;
        logChange(new ParentChange(oldParent, parent));
    }

    @Override
    public void logChange(NodeChange change) {
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
                StateNode node = AbstractStateNode.this;
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

    @Override
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

    protected void updateDependents(Object key,
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
            dependents = (HashSet<Runnable>) ((AbstractStateNode) map)
                    .doGet(key);
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

    @Override
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

    @Override
    public Map<String, ComputedProperty> getComputedProperties() {
        return (Map<String, ComputedProperty>) doGet(Keys.COMPUTED);
    }

    protected abstract boolean doesContainKey(Object key);

    protected abstract Object removeValue(Object key);

    protected abstract Object setValue(Object key, Object value);

    private Stream<Object> getKeysStream() {
        Map<String, ComputedProperty> computed = getComputedProperties();
        Stream<Object> keys = doGetKeys();
        if (computed == null) {
            return keys;
        } else {
            return Stream.concat(keys, computed.keySet().stream());
        }

    }

    @Override
    public Set<Object> getKeys() {
        return getKeysStream().collect(Collectors.toSet());
    }

    protected abstract Stream<Object> doGetKeys();

    public abstract Class<?> getType(Object key);

    @Override
    public void setRoot(RootNode root) {
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

    protected void forEachChildNode(Consumer<StateNode> consumer) {
        Consumer<Object> action = new Consumer<Object>() {
            @Override
            public void accept(Object v) {
                if (v instanceof StateNode) {
                    StateNode childNode = (StateNode) v;
                    consumer.accept(childNode);
                }
            }
        };
        getKeysStream().map(this::doGet).forEach(action);
    }

    @Override
    public void detachChild(Object value) {
        if (value instanceof StateNode) {
            StateNode childNode = (StateNode) value;
            assert childNode.getParent() == this;
            childNode.setParent(null);

            if (isAttached()) {
                childNode.unregister();
            }
        } else if (value instanceof ListNode) {
            ((ListNode) value).detach();
        }
    }

    @Override
    public void register() {
        RootNode root = getRoot();
        assert root != null;
        assert !isAttached();

        setId(root.register(this));
        forEachChildNode(StateNode::register);
    }

    @Override
    public void unregister() {
        assert getRoot() != null;
        assert isAttached();

        setId(getRoot().unregister(this));
        forEachChildNode(StateNode::unregister);
    }

    @Override
    public boolean isAttached() {
        return getId() > 0;
    }

    @Override
    public StateNode getParent() {
        return parent;
    }

    @Override
    public List<Object> getMultiValued(Object key) {
        Object value = get(key);
        if (value instanceof ListNode) {
            return new ListNodeAsList((ListNode) value);
        } else if (value instanceof LazyList) {
            return (List) new LazyListActiveRangeView<StateNode>(
                    (LazyList<StateNode>) value);
        } else {
            ValueType propertyType = getType().getPropertyTypes().get(key);
            if (propertyType == null) {
                propertyType = ValueType.UNDEFINED;
            } else if (propertyType instanceof ArrayType) {
                propertyType = ((ArrayType) propertyType).getMemberType();
            } else {
                throw new RuntimeException("Can't get multi-valued for " + key
                        + " which is typed as " + propertyType);
            }

            // FIXME Why should these be auto-created?
            ListNode nodeList = new ListNode(propertyType);
            put(key, nodeList);
            if (isServerOnlyKey(key)) {
                nodeList.put(AbstractElementTemplate.Keys.SERVER_ONLY,
                        Boolean.TRUE);
            }
            return new ListNodeAsList(nodeList);
        }
    }

    @Override
    public Object put(Object key, Object value) {
        boolean contained = doesContainKey(key);
        Object previous = setValue(key, value);
        if (contained) {
            logChange(new RemoveChange(key, previous));
            detachChild(previous);
        }
        logChange(new PutChange(key, value));
        attachChild(value);

        if ((!contained || !Objects.equals(previous, value))
                && hasDependents()) {
            updateDependents(key, Reactive::registerWrite);
        }

        return previous;
    }

    protected boolean hasDependents() {
        return doesContainKey(Keys.DEPENDENTS);
    }

    public <T> void put(Class<T> type, T value) {
        // Explicit variable since Eclipse determined that a (Object) cast was
        // unnecessary and could be removed
        Object key = type;
        put(key, value);
    }

    @Override
    public Object remove(Object key) {
        if (containsKey(key)) {
            updateDependents(key, Reactive::registerWrite);

            Object removed = removeValue(key);
            logChange(new RemoveChange(key, removed));
            detachChild(removed);
            return removed;
        } else {
            return null;
        }
    }

    @Override
    public Set<String> getStringKeys() {
        Set<?> filteredKeys = getKeysStream().filter(k -> k instanceof String)
                .collect(Collectors.toSet());

        @SuppressWarnings("unchecked")
        Set<String> stringKeys = (Set<String>) filteredKeys;

        return Collections.unmodifiableSet(stringKeys);
    }

    @Override
    public RootNode getRoot() {
        return rootNode;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void addChangeListener(NodeChangeListener listener) {
        getMultiValued(NodeChangeListener.class).add(listener);
    }

    @Override
    public void removeChangeListener(NodeChangeListener listener) {
        getMultiValued(NodeChangeListener.class).remove(listener);
    }

    @Override
    public void enqueueRpc(String string, Object... params) {
        runAttached(() -> {
            getRoot().enqueueRpc(this, string, params);
        });

    }

    @Override
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

    @Override
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

    @Override
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

    private static Logger getLogger() {
        return Logger.getLogger(StateNode.class.getName());
    }

    @Override
    public boolean isServerOnly() {
        if (containsKey(AbstractElementTemplate.Keys.SERVER_ONLY)) {
            return true;
        }
        if (getParent() != null) {
            return getParent().isServerOnly();
        }
        return false;
    }

    @Override
    public boolean isServerOnlyKey(Object key) {
        assert key != null;
        if (key instanceof Class) {
            return true;
        } else if (key instanceof ServerOnlyKey) {
            return true;
        } else {
            return false;
        }

    }

    @Override
    public void rollback(NodeChange change) {
        if (change instanceof IdChange) {
            id = ((IdChange) change).getOldId();
        } else if (change instanceof ParentChange) {
            parent = ((ParentChange) change).getOldParent();
        } else if (change instanceof PutChange) {
            removeValue(((PutChange) change).getKey());
        } else if (change instanceof RemoveChange) {
            RemoveChange removeChange = (RemoveChange) change;
            setValue(removeChange.getKey(), removeChange.getValue());
        } else {
            throw new IllegalArgumentException("Unkown change type "
                    + change.getClass().getName() + " passed to rollback");
        }
    }
}
