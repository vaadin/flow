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
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.hummingbird.kernel.RootNode.TransactionHandler;
import com.vaadin.hummingbird.kernel.change.IdChange;
import com.vaadin.hummingbird.kernel.change.ListInsertChange;
import com.vaadin.hummingbird.kernel.change.ListInsertManyChange;
import com.vaadin.hummingbird.kernel.change.ListRemoveChange;
import com.vaadin.hummingbird.kernel.change.ListReplaceChange;
import com.vaadin.hummingbird.kernel.change.NodeChange;
import com.vaadin.hummingbird.kernel.change.ParentChange;
import com.vaadin.hummingbird.kernel.change.PutChange;
import com.vaadin.hummingbird.kernel.change.RangeEndChange;
import com.vaadin.hummingbird.kernel.change.RangeStartChange;
import com.vaadin.hummingbird.kernel.change.RemoveChange;
import com.vaadin.server.communication.ServerOnlyKey;
import com.vaadin.shared.ui.grid.Range;
import com.vaadin.ui.Template.Model;

public abstract class StateNode implements Serializable {
    private enum Keys implements ServerOnlyKey {
        TRANSACTION_LOG, NEXT_UNPREVIEWED_LOG_INDEX, COMPUTED_PENDING_FLUSH, COMPUTED, COMPUTED_CACHE, DEPENDENTS;
    }

    private static final Object EMPTY_FLUSH_MARKER = new Object();

    private class ListView extends AbstractList<Object>
            implements Serializable {
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
        assert !(value instanceof ListView);

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
        assert !isAttached();

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
        } else if (value instanceof LazyList) {
            return (List) new LazyListActiveRangeView<StateNode>(
                    (LazyList<StateNode>) value);
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

    public interface DataProvider<T> {
        public List<T> getValues(int index, int count);

        public Class<T> getType();
    }

    /**
     * Class for providing the active range of a LazyList in a List format. The
     * list is offset so that index 0 corresponds to the start of the active
     * range.
     */
    public static class LazyListActiveRangeView<T extends StateNode>
            extends AbstractList<T> {

        private LazyList<T> lazyList;

        public LazyListActiveRangeView(LazyList<T> lazyList) {
            this.lazyList = lazyList;
        }

        @Override
        public T get(int index) {
            if (index < 0 || index >= size()) {
                throw new ArrayIndexOutOfBoundsException(index);
            }
            return lazyList.get(index + lazyList.getActiveRangeStart());
        }

        @Override
        public int size() {
            return lazyList.getActiveRangeEnd()
                    - lazyList.getActiveRangeStart();
        }

    }

    /**
     * A list like class which lazily fetches data from its DataProvider for the
     * range defined as active.
     *
     * @param <T>
     *            the type of list, currently must be StateNode
     */
    public interface LazyList<T extends StateNode> {

        /**
         * Returns the item stored at active index {@code activeIndex}
         *
         * @param activeIndex
         *            the index of the item, must be greater or equal than
         *            {@link #getActiveRangeStart()} and less than
         *            {@link #getActiveRangeEnd()}
         * @return
         */
        default public T get(int activeIndex) {
            return get(activeIndex, 1).get(0);
        }

        /**
         * Returns the {@code count} items stored at the active index
         * {@code activeIndex}
         *
         * @param activeIndex
         *            the index of the first item, must be greater or equal than
         *            {@link #getActiveRangeStart()}
         * @param count
         *            the number of items to retrieve. The index of the last
         *            item must be less than {@link #getActiveRangeEnd()}
         * @return
         */
        public List<T> get(int activeIndex, int count);

        /**
         * Returns the (inclusive) start of the active range.
         *
         * If the list contains rows 0-9, then {@link #getActiveRangeStart()}
         * will return 0 and {@link #getActiveRangeEnd()} will return 10.
         *
         * @return the start of the active range (inclusive)
         */
        public int getActiveRangeStart();

        /**
         * Sets the (inclusive) start of the active range
         *
         * @param activeRangeStart
         */
        public LazyList<T> setActiveRangeStart(int activeRangeStart);

        /**
         * Returns the (exclusive) end of the active range
         *
         * If the list contains rows 0-9, then {@link #getActiveRangeStart()}
         * will return 0 and {@link #getActiveRangeEnd()} will return 10.
         *
         * @return the end of the active range (exclusive)
         */
        public int getActiveRangeEnd();

        /**
         * Sets the (exclusive) end of the active range
         *
         * @param activeRangeEnd
         */
        public LazyList<T> setActiveRangeEnd(int activeRangeEnd);

        /**
         * Increases the start of the range by the given number of items
         *
         * @param increaseBy
         *            the number of items to include before the current start of
         *            the range
         */
        default public void increaseActiveRangeStart(int increaseBy) {
            setActiveRangeStart(getActiveRangeStart() + increaseBy);
        };

        /**
         * Decreases the start of the range by the given number of items
         *
         * @param decreaseBy
         *            the number of items to remove from the current start of
         *            the range
         */
        default public void decreaseActiveRangeStart(int decreaseBy) {
            int newStart = getActiveRangeStart() - decreaseBy;
            if (newStart < 0) {
                newStart = 0;
            }
            setActiveRangeStart(newStart);
        }

        /**
         * Increases the end of the range by the given number of items
         *
         * @param increaseBy
         *            the number of items to include after the current end of
         *            the range
         */
        default public void increaseActiveRangeEnd(int increaseBy) {
            setActiveRangeEnd(getActiveRangeEnd() + increaseBy);
        }

        /**
         * Decreases the end of the range by the given number of items
         *
         * @param decreaseBy
         *            the number of items to remove from the current end of the
         *            range
         */
        default public void decreaseActiveRangeEnd(int decreaseBy) {
            int newEnd = getActiveRangeEnd() - decreaseBy;
            if (newEnd < 0) {
                newEnd = 0;
            }
            setActiveRangeEnd(newEnd);
        }

        /**
         * Creates a lazy list using the given data provider.
         *
         * @param dataProvider
         *            The data provider which will provide the contents of the
         *            list on demand
         * @return a LazyList connected to the given data provider
         */
        public static LazyList create(DataProvider dataProvider) {
            return new LazyListImpl(dataProvider);
        }

        /**
         * Attach the lazy list to the given state node using the given key.
         *
         * @deprecated This method should be refactored and ultimately removed
         *
         * @param stateNode
         * @param key
         */
        @Deprecated
        public void attach(StateNode stateNode, Object key);

    }

    private static class LazyListImpl
            implements Serializable, LazyList<StateNode> {
        private DataProvider<Object> dataProvider;
        /**
         * The range which the client currently has data for
         */
        private Range clientRange = Range.between(0, 0); // empty
        private Runnable sendPendingData = this::sendPendingData;
        private SingleRangeCache<StateNode> cache;

        private StateNode node;
        private Object key;
        private Class<Object> type;

        public LazyListImpl(DataProvider<Object> dataProvider) {
            type = dataProvider.getType();
            this.dataProvider = dataProvider;
            cache = new SingleRangeCache<>(addedObject -> {
                assert node != null : "Node must be set";
                addedObject.forEach(o -> node.attach(o));
            } , removedObject -> {
                assert node != null : "Node must be set";
                removedObject.forEach(o -> node.detach(o));
            });
        }

        @Override
        public void attach(StateNode node, Object key) {
            assert this.node == null : "Node cannot be changed";
            assert node != null : "Node cannot be null";
            assert this.key == null : "Key cannot be changed";
            assert key != null : "Key cannot be null";
            this.key = key;
            this.node = node;
        }

        @Override
        public LazyList<StateNode> setActiveRangeStart(int activeRangeStart) {
            assert node != null;
            int oldRangeStart = getActiveRangeStart();
            getLogger().info("setActiveRangeStart(" + activeRangeStart + ")");
            cache.setRangeStart(activeRangeStart);
            node.logChange(new RangeStartChange(key, activeRangeStart));
            if (activeRangeStart > oldRangeStart) {
                if (!clientRange.isEmpty()
                        && clientRange.getStart() < activeRangeStart) {
                    // Client has 0-10
                    // New range is 5-10
                    // Drop [0,4]

                    for (int i = clientRange
                            .getStart(); i < activeRangeStart; i++) {
                        // Client side maps [0,N] to
                        // [clientRangeStart,clientRangeEnd]
                        node.logChange(new ListRemoveChange(0, key, null));
                    }
                    clientRange = Range.between(activeRangeStart,
                            clientRange.getEnd());
                }
            }
            node.runBeforeNextClientResponse(sendPendingData);
            return this;
        }

        @Override
        public LazyList<StateNode> setActiveRangeEnd(int activeRangeEnd) {
            assert node != null : "Node must be set";
            int oldRangeEnd = getActiveRangeEnd();

            getLogger().info("setActiveRangeEnd(" + activeRangeEnd + ")");
            cache.setRangeEnd(activeRangeEnd);
            node.logChange(new RangeEndChange(key, activeRangeEnd));
            if (activeRangeEnd < oldRangeEnd) {
                if (!clientRange.isEmpty()
                        && clientRange.getEnd() > activeRangeEnd) {
                    // Client has 0-10
                    // New range is 0-5
                    // Drop [5,9]

                    for (int i = activeRangeEnd; i < clientRange
                            .getEnd(); i++) {
                        // Client side maps [0,N] to
                        // [clientRangeStart,clientRangeEnd]
                        node.logChange(new ListRemoveChange(
                                activeRangeEnd - clientRange.getStart(), key,
                                null));
                    }
                    clientRange = Range.between(clientRange.getStart(),
                            activeRangeEnd);
                }
            }
            node.runBeforeNextClientResponse(sendPendingData);
            return this;
        }

        /**
         * Fetches the given items from the data provider, wraps them and adds
         * them to the cache
         *
         * @param index
         * @param count
         * @return
         */
        private List<StateNode> fetch(int index, int count) {
            assert getActiveRangeStart() <= index : "Tried to fetch " + index
                    + " when range is " + getActiveRangeStart() + "-"
                    + getActiveRangeEnd();
            assert (index + count) <= getActiveRangeEnd() : "Tried to fetch "
                    + index + " when range is " + getActiveRangeStart() + "-"
                    + getActiveRangeEnd();

            getLogger().info("fetch(" + index + "," + count + ")");

            List<Object> dataProviderValues = dataProvider.getValues(index,
                    count);
            int dataProviderValuesCount = dataProviderValues.size();
            if (dataProviderValuesCount != count) {
                getLogger().warning("Asked for index: " + index + ", count: "
                        + count + " but got only " + dataProviderValuesCount
                        + " values");
            }

            List<StateNode> nodes = new ArrayList<>(dataProviderValuesCount);
            for (int i = 0; i < dataProviderValuesCount; i++) {
                nodes.add(dataToStateNode(type, dataProviderValues.get(i)));
            }

            // Add to cache
            cache.set(index, nodes);
            return nodes;
        }

        private <T> StateNode dataToStateNode(Class<T> type, T object) {
            return Model.beanToStateNode(type, object);
        }

        @Override
        public int getActiveRangeStart() {
            return cache.getRangeStart();
        }

        @Override
        public int getActiveRangeEnd() {
            return cache.getRangeEnd();
        }

        @Override
        public List<StateNode> get(int index, int count) {
            Range requested = Range.between(index, index + count);
            Range availableRange = cache.getAvailableDataRange(
                    requested.getStart(), requested.getEnd());
            if (availableRange == null) {
                // Nothing available
                return fetch(index, count);
            } else {
                // Part of the range is already available
                ArrayList<StateNode> result = new ArrayList<>();
                if (requested.startsBefore(availableRange)) {
                    int countBefore = availableRange.getStart()
                            - requested.getStart();
                    result.addAll(fetch(requested.getStart(), countBefore));
                }
                result.addAll(cache.get(availableRange.getStart(),
                        availableRange.getEnd()));

                // available: 0-10
                // requested: 0-20
                // fetch 10-20 (count: 10)
                if (requested.endsAfter(availableRange)) {
                    int afterIndex = availableRange.getEnd();
                    int afterCount = requested.getEnd()
                            - availableRange.getEnd();
                    result.addAll(fetch(afterIndex, afterCount));
                }
                return result;
            }

        }

        public void sendPendingData() {
            Range activeRange = Range.between(getActiveRangeStart(),
                    getActiveRangeEnd());
            // Client has some data
            if (activeRange.startsBefore(clientRange)) {
                // New data in the beginning

                // New range 5-30, client has 10-20
                // -> fetch 5-9 (count: 5)
                // rangeEnd: 30, clientRangeStart: 10

                // New range 3-5, client has 10-20
                // -> fetch 3-4 (count: 2)
                // rangeEnd: 5, clientRangeStart: 10
                int firstInclusive = activeRange.getStart();
                int toExclusive = clientRange.getStart();
                if (activeRange.endsBefore(clientRange)) {
                    toExclusive = activeRange.getEnd();
                }
                sendData(Range.between(firstInclusive, toExclusive));
            }
            if (activeRange.endsAfter(clientRange)) {
                // New range 5-30, client has 10-20
                // -> fetch 21-30 (count: 10)

                // New range 25-30, client has 10-20
                // -> fetch 25-30 (count: 5)

                int firstInclusive = clientRange.getEnd();
                if (activeRange.startsAfter(clientRange)) {
                    firstInclusive = activeRange.getStart();
                }
                int toExclusive = activeRange.getEnd();
                sendData(Range.between(firstInclusive, toExclusive));
            }
            clientRange = activeRange;
        }

        private void sendData(Range range) {
            assert node != null : "Node must be set";
            assert key != null;

            List<StateNode> data = get(range.getStart(), range.length());
            getLogger().info("Sending data for " + range + ": " + data);

            // Client uses indexes [0,N] for [activeRangeStart,activeRangeEnd]
            node.logChange(new ListInsertManyChange(
                    range.getStart() - getActiveRangeStart(), key,
                    data.toArray()));
        }

    }

    public LazyList<StateNode> getLazyMultiValued(Object key) {
        if (!containsKey(key)) {
            return null;
        }
        return get(key, LazyList.class);
    }

    public Object put(Object key, Object value) {
        if (value instanceof LazyList) {
            if (containsKey(key)) {
                throw new IllegalStateException(
                        "Key '" + key + "' already exists");
            }

            LazyList list = (LazyList) value;
            list.attach(this, key);
            setValue(key, list);
            return null;

        }
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

    private static Logger getLogger() {
        return Logger.getLogger(StateNode.class.getName());
    }

    public boolean isServerOnly() {
        if (containsKey(AbstractElementTemplate.Keys.SERVER_ONLY)) {
            return true;
        }
        if (getParent() != null) {
            return getParent().isServerOnly();
        }
        return false;
    }

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
}
