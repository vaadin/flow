/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.data.provider;

import java.util.Comparator;
import java.util.List;

/**
 * Allows to trace {@link Query#getOffset()} and {@link Query#getLimit()} method
 * calls.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
class QueryTrace<T, F> extends Query<T, F> {

    private boolean isOffsetCalled;

    private boolean isLimitCalled;

    /**
     * {@inheritDoc}
     */
    QueryTrace(int offset, int limit, List<QuerySortOrder> sortOrders,
            Comparator<T> inMemorySorting, F filter) {
        super(offset, limit, sortOrders, inMemorySorting, filter);
    }

    @Override
    public int getOffset() {
        isOffsetCalled = true;
        return super.getOffset();
    }

    @Override
    public int getLimit() {
        isLimitCalled = true;
        return super.getLimit();
    }

    boolean isOffsetCalled() {
        return isOffsetCalled;
    }

    boolean isLimitCalled() {
        return isLimitCalled;
    }
}
