package com.vaadin.flow.data.provider.hierarchy;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;

import com.vaadin.flow.function.SerializableBiFunction;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.internal.Range;

public class HierarchyLevel<T> {
    private T parent;
    private int size;
    private SerializablePredicate<T> isExpanded;
    private SortedMap<Integer, HierarchyLevel<T>> subLevels = new TreeMap<>();
    private SortedMap<Integer, T> items = new TreeMap<>();

    public HierarchyLevel(T parent, int size,
            SerializablePredicate<T> isExpanded) {
        this.parent = parent;
        this.size = size;
        this.isExpanded = isExpanded;
    }

    public SortedMap<Integer, HierarchyLevel<T>> getSubLevels() {
        return subLevels;
    }

    public HierarchyLevel<T> getSubLevel(int index) {
        return subLevels.get(index);
    }

    public void setSubLevel(int index, HierarchyLevel<T> level) {
        subLevels.put(index, level);
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public T getParent() {
        return parent;
    }

    public void setParent(T parent) {
        this.parent = parent;
    }

    public int getFlatSize() {
        return parent == null || isExpanded.test(parent) ? size
                + subLevels.values().stream().map(HierarchyLevel::getFlatSize)
                        .reduce(0, Integer::sum)
                : 0;
    }

    public T getItem(int index) {
        return items.get(index);
    }

    public boolean hasItem(int index) {
        return items.containsKey(index);
    }

    public void setItemsRange(int start, List<T> items) {
        for (int i = 0; i < items.size(); i++) {
            this.items.put(start + i, items.get(i));
        }
    }
}
