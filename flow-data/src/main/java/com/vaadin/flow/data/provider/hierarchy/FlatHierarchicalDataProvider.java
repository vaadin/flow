package com.vaadin.flow.data.provider.hierarchy;

public interface FlatHierarchicalDataProvider<T, F>
        extends HierarchicalDataProvider<T, F> {
    @Override
    T getParentItem(T item);

    @Override
    int getDepth(T item);

    @Override
    default boolean isFlatHierarchy() {
        return true;
    }
}
