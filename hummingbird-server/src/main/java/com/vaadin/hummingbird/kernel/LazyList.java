package com.vaadin.hummingbird.kernel;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.vaadin.hummingbird.kernel.change.ListInsertManyChange;
import com.vaadin.hummingbird.kernel.change.ListRemoveChange;
import com.vaadin.hummingbird.kernel.change.RangeEndChange;
import com.vaadin.hummingbird.kernel.change.RangeStartChange;
import com.vaadin.shared.ui.grid.Range;
import com.vaadin.ui.Template.Model;

/**
 * A list like class which lazily fetches data from its DataProvider for the
 * range defined as active.
 *
 * @param <T>
 *            the type of list, currently must be StateNode
 */
public interface LazyList<T extends StateNode> {

    public interface DataProvider<T> {
        public List<T> getValues(int index, int count);

        public Class<T> getType();
    }

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
     *            the number of items to retrieve. The index of the last item
     *            must be less than {@link #getActiveRangeEnd()}
     * @return
     */
    public List<T> get(int activeIndex, int count);

    /**
     * Returns the (inclusive) start of the active range.
     *
     * If the list contains rows 0-9, then {@link #getActiveRangeStart()} will
     * return 0 and {@link #getActiveRangeEnd()} will return 10.
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
     * If the list contains rows 0-9, then {@link #getActiveRangeStart()} will
     * return 0 and {@link #getActiveRangeEnd()} will return 10.
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
     *            the number of items to include before the current start of the
     *            range
     */
    default public void increaseActiveRangeStart(int increaseBy) {
        setActiveRangeStart(getActiveRangeStart() + increaseBy);
    };

    /**
     * Decreases the start of the range by the given number of items
     *
     * @param decreaseBy
     *            the number of items to remove from the current start of the
     *            range
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
     *            the number of items to include after the current end of the
     *            range
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
     *            The data provider which will provide the contents of the list
     *            on demand
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

class LazyListImpl implements Serializable, LazyList<StateNode> {
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
            addedObject.forEach(o -> node.attachChild(o));
        } , removedObject -> {
            assert node != null : "Node must be set";
            removedObject.forEach(o -> node.detachChild(o));
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

                for (int i = activeRangeEnd; i < clientRange.getEnd(); i++) {
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
     * Fetches the given items from the data provider, wraps them and adds them
     * to the cache
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

        List<Object> dataProviderValues = dataProvider.getValues(index, count);
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

    private static Logger getLogger() {
        return Logger.getLogger(LazyListImpl.class.getName());
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
        Range availableRange = cache.getAvailableDataRange(requested.getStart(),
                requested.getEnd());
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
                int afterCount = requested.getEnd() - availableRange.getEnd();
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
                range.getStart() - getActiveRangeStart(), key, data.toArray()));
    }

}

/**
 * Class for providing the active range of a LazyList in a List format. The list
 * is offset so that index 0 corresponds to the start of the active range.
 */
class LazyListActiveRangeView<T extends StateNode> extends AbstractList<T> {

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
        return lazyList.getActiveRangeEnd() - lazyList.getActiveRangeStart();
    }

}
