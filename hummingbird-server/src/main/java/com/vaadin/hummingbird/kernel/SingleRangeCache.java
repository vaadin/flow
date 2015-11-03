package com.vaadin.hummingbird.kernel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import com.vaadin.shared.ui.grid.Range;

public class SingleRangeCache<T> implements Serializable {

    private Range range = Range.between(0, 0);
    private List<T> data = new ArrayList<>();
    private SingleRangeCache.AddListener<T> addListener;
    private SingleRangeCache.RemoveListener<T> removeListener;

    public interface AddListener<T> {
        public void addedToCache(List<T> added);
    }

    public interface RemoveListener<T> {
        public void removedFromCache(List<T> removed);
    }

    public SingleRangeCache(SingleRangeCache.AddListener<T> addListener,
            SingleRangeCache.RemoveListener<T> removeListener) {
        this.addListener = addListener;
        this.removeListener = removeListener;
    }

    private int getDataIndex(int index) {
        return index - range.getStart();
    }

    public T get(int index) {
        if (!range.contains(index)) {
            throw new IllegalArgumentException("The given index " + index
                    + " is not in the range of the cache " + range.getStart()
                    + "-" + range.getEnd());
        }

        return data.get(getDataIndex(index));
    }

    public List<T> get(int firstIndexInclusive, int endIndexExclusive) {
        return data.subList(getDataIndex(firstIndexInclusive),
                getDataIndex(endIndexExclusive));
    }

    public void set(int index, T value) {
        set(index, Collections.singletonList(value));

    }

    public void set(int index, List<T> values) {
        getLogger().info("cache.set(" + index + "," + values + ")");

        if (values.isEmpty()) {
            return;
        }

        ArrayList<T> reallyRemoved = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            int cacheIndex = getDataIndex(index + i);
            T previous = data.set(cacheIndex, values.get(i));
            if (previous != null) {
                reallyRemoved.add(previous);
            }
        }
        if (!reallyRemoved.isEmpty()) {
            removeListener.removedFromCache(reallyRemoved);
        }

        addListener.addedToCache(values);
    }

    public int getRangeStart() {
        return range.getStart();
    }

    public int getRangeEnd() {
        return range.getEnd();
    }

    public void setRangeStart(int rangeStart) {
        assert rangeStart >= 0 : "Range start must be positive";

        getLogger().info("cache.setRangeStart(" + rangeStart + ")");
        if (rangeStart > range.getEnd()) {
            throw new IllegalArgumentException(
                    "Start must always be before end");
        }
        if (rangeStart == range.getStart()) {
            return;
        } else if (rangeStart > range.getStart()) {
            // Was 0-10
            // Is 5-10

            // Drop [0,4]
            // The item at index rangeStart should remain as start is
            // inclusive
            ArrayList<T> reallyRemoved = new ArrayList<>();
            for (int i = range.getStart(); i < rangeStart; i++) {
                if (hasAvailableData(i)) {
                    T value = get(i);
                    if (value != null) {
                        reallyRemoved.add(value);
                    }
                }
            }
            if (!reallyRemoved.isEmpty()) {
                removeListener.removedFromCache(reallyRemoved);
            }

            // Contract cache array
            if (range.getEnd() > rangeStart) {
                data = new ArrayList<T>(data.subList(getDataIndex(rangeStart),
                        getDataIndex(range.getEnd())));
                range = Range.between(rangeStart, range.getEnd());
            } else {
                // Old 0-10
                // New 15-10
                // This should only be a temporary state, when start is set
                // before end
                data = new ArrayList<>();
                range = Range.between(rangeStart, rangeStart);
            }
        } else {
            // Was 5-10
            // Is 0-10
            int nrNewItems = range.getStart() - rangeStart;
            // Expand cache array
            ArrayList<T> newData = new ArrayList<T>();
            for (int i = 0; i < nrNewItems; i++) {
                newData.add(null);
            }

            data.addAll(0, newData);
            range = Range.between(rangeStart, range.getEnd());
        }

        assert data.size() == range
                .length() : "Size of the internal cache should always match the active range";

    }

    public void setRangeEnd(int rangeEnd) {
        assert rangeEnd >= 0 : "Range end must be positive";

        getLogger().info("cache.setRangeEnd(" + rangeEnd + ")");
        if (rangeEnd == range.getEnd()) {
            return;
        } else if (rangeEnd < range.getEnd()) {
            // Was 0-10
            // Is 0-4

            // Drop [4,9]
            ArrayList<T> reallyRemoved = new ArrayList<>();
            for (int i = rangeEnd; i < range.getEnd(); i++) {
                if (hasAvailableData(i)) {
                    T value = get(i);
                    if (value != null) {
                        reallyRemoved.add(value);
                    }
                }
            }
            if (!reallyRemoved.isEmpty()) {
                removeListener.removedFromCache(reallyRemoved);
            }

            // Contract cache array
            data = new ArrayList<T>(data.subList(getDataIndex(range.getStart()),
                    getDataIndex(rangeEnd)));
            range = Range.between(range.getStart(), rangeEnd);

        } else {
            // Was 0-10
            // Is 0-15
            int nrNewItems = rangeEnd - range.getEnd();
            // Expand cache array
            for (int i = 0; i < nrNewItems; i++) {
                data.add(null);
            }
            range = Range.between(range.getStart(), rangeEnd);
        }
    }

    public boolean hasAvailableData(int index) {
        if (!range.contains(index)) {
            return false;
        }

        int dataIndex = getDataIndex(index);
        return (data.size() > dataIndex && data.get(dataIndex) != null);
    }

    public Range getAvailableDataRange(int start, int end) {
        assert start >= range.getStart();
        assert end <= range.getEnd();

        int firstAvailable = -1;

        int dataSize = data.size();
        for (int realIndex = start; realIndex < end; realIndex++) {
            int dataIndex = getDataIndex(realIndex);
            boolean hasData = dataSize > (dataIndex)
                    && (data.get(dataIndex) != null);

            if (hasData && firstAvailable == -1) {
                firstAvailable = realIndex;
            } else if (!hasData && firstAvailable != -1) {
                return Range.between(firstAvailable, realIndex);
            }
        }
        if (firstAvailable != -1) {
            return Range.between(firstAvailable, end);
        } else {
            return null;
        }
    }

    public boolean isActive(int index) {
        return range.contains(index);
    }

    private static Logger getLogger() {
        return Logger.getLogger(SingleRangeCache.class.getName());
    }

    public Range getRange() {
        return range;
    }

}