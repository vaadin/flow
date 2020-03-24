package com.vaadin.flow.data.provider;

import java.util.List;
import java.util.stream.Stream;

/**
 * Used by {@link HasDataProviderTest} and {@link HasFilterableDataProviderTest}.
 */
class WildcardDataProvider<T>
        extends AbstractDataProvider<T, Void> {

    private final List<T> backend;

    public WildcardDataProvider(List<T> items) {
        backend = items;
    }

    @Override
    public boolean isInMemory() {
        return true;
    }

    @Override
    public int size(Query<T, Void> t) {
        return backend.size();
    }

    @Override
    public Stream<T> fetch(Query<T, Void> query) {
        return backend.stream().skip(query.getOffset()).limit(query.getLimit());
    }
}
