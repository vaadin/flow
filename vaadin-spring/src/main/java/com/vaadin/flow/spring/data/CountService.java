package com.vaadin.flow.spring.data;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.spring.data.filter.Filter;

/**
 * A service that can count the number of items with a given filter.
 */
public interface CountService {

    /**
     * Counts the number of items that match the given filter.
     *
     * @param filter
     *            the filter, or {@code null} to use no filter
     * @return the number of items in the service that match the filter
     */
   long count(@Nullable Filter filter);

}
