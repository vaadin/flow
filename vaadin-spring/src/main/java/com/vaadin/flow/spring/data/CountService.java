package com.vaadin.flow.spring.data;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.spring.data.filter.Filter;

/**
 * A service that can count the number of items with a given filter.
 */
public interface CountService {

    /**
     * Counts the number of items.
     *
     * @return
     *           the number of items in the service
     */
    public long count();
    
    /**
     * Counts the number of items that match the given filter.
     *
     * @param filter
     *            the filter, never {@code null}
     * @return
     *           the number of items in the service that match the filter
     */
    public long count(Filter filter);

}
