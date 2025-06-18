/*
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.data.provider;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Immutable query object used to request data from a backend. Contains index
 * limits, sorting and filtering information.
 *
 * @param <T>
 *            bean type
 * @param <F>
 *            filter type
 * @since 1.0
 */
public class Query<T, F> implements Serializable {

    private final int offset;
    private final int limit;
    private final List<QuerySortOrder> sortOrders;
    private final Comparator<T> inMemorySorting;
    private final F filter;
    private Integer pageSize;

    /**
     * Constructs a Query for all rows from 0 to {@link Integer#MAX_VALUE}
     * without sorting and filtering.
     */
    public Query() {
        offset = 0;
        limit = Integer.MAX_VALUE;
        sortOrders = Collections.emptyList();
        inMemorySorting = null;
        filter = null;
    }

    /**
     * Constructs a Query for all rows from 0 to {@link Integer#MAX_VALUE} with
     * filtering.
     *
     * @param filter
     *            back end filter of a suitable type for the data provider; can
     *            be null
     */
    public Query(F filter) {
        offset = 0;
        limit = Integer.MAX_VALUE;
        sortOrders = Collections.emptyList();
        inMemorySorting = null;
        this.filter = filter;
    }

    /**
     * Constructs a new Query object with given offset, limit, sorting and
     * filtering.
     *
     * @param offset
     *            first index to fetch
     * @param limit
     *            fetched item count
     * @param sortOrders
     *            sorting order for fetching; used for sorting backends
     * @param inMemorySorting
     *            comparator for sorting in-memory data
     * @param filter
     *            filtering for fetching; can be null
     */
    public Query(int offset, int limit, List<QuerySortOrder> sortOrders,
            Comparator<T> inMemorySorting, F filter) {
        this.offset = offset;
        this.limit = limit;
        this.sortOrders = sortOrders;
        this.inMemorySorting = inMemorySorting;
        this.filter = filter;
    }

    /**
     * Gets the first index of items to fetch. The offset is only used when
     * fetching items, but not when counting the number of available items.
     *
     * @return offset for data request
     */
    public int getOffset() {
        return offset;
    }

    /**
     * Gets the number of items to fetch. The limit is only used when fetching
     * items, but not when counting the number of available items.
     * <p>
     * <strong>Note: </strong>It is possible that
     * {@code offset + limit > item count}
     *
     * @return number of items to fetch
     */
    public int getLimit() {
        return limit;
    }

    /**
     * Returns a zero-based page index to be retrieved.
     * <p>
     * Vaadin asks data from the backend in paged manner. This shorthand
     * calculates the page index for backends using paged data access, such as
     * Spring Data repositories.
     * <p>
     * If page offset is not evenly divisible with pageSize raise pageSize until
     * it is. Updates the pageSize value if it has been raised.
     *
     * @return the zero-based page index
     */
    public int getPage() {
        int pageSize = getPageSize();
        int pageOffset = getOffset();
        // If page offset is not evenly divisible with pageSize raise
        // pageSize until it is.
        // Else we will on the end pick the wrong items due to rounding error.
        if (pageOffset > pageSize && pageOffset % pageSize != 0) {
            while (pageOffset % pageSize != 0) {
                pageSize++;
            }
            setPageSize(pageSize);
        }
        return pageOffset / pageSize;
    }

    private void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * Returns the page size that should be returned. The amount of items can be
     * smaller if there is no more items available in the backend.
     * <p>
     * Vaadin asks data from the backend in paged manner.
     * <p>
     * This is an alias for {@link #getLimit()} if the page size has not been
     * set.
     * <p>
     * {@link #getPage()} will increase page size to evenly divide offset so the
     * items skip for page will go to the correct item, in case the offset can
     * not be evenly divided by the limit.
     *
     * @return the page size used for data access
     */
    public int getPageSize() {
        if (pageSize != null) {
            return pageSize;
        }
        return getLimit();
    }

    /**
     * Gets the sorting for items to fetch. This list of sort orders is used for
     * sorting backends. The sort orders are only used when fetching items, but
     * not when counting the number of available items.
     * <p>
     * <strong>Note: </strong> Sort orders and in-memory sorting are mutually
     * exclusive. If the {@link DataProvider} handles one, it should ignore the
     * other.
     * <p>
     * <strong>Note: </strong> Sort orders are populated in the Vaadin Grid
     * component only for the columns that have key set using either
     * {@code Column#setKey} or {@code Grid#setColumns} methods.
     *
     * @return list of sort orders
     */
    public List<QuerySortOrder> getSortOrders() {
        return sortOrders;
    }

    /**
     * Gets the filter for items to fetch.
     *
     * @return optional filter
     */
    public Optional<F> getFilter() {
        return Optional.ofNullable(filter);
    }

    /**
     * Gets the comparator for sorting in-memory data. The comparator is only
     * used when fetching items, but not when counting the number of available
     * items.
     * <p>
     * <strong>Note: </strong> Sort orders and in-memory sorting are mutually
     * exclusive. If the {@link DataProvider} handles one, it should ignore the
     * other.
     *
     * @return sorting comparator
     */
    public Comparator<T> getInMemorySorting() {
        return inMemorySorting;
    }

    /**
     * Gets the optional comparator for sorting data. The comparator is only
     * used when fetching items, but not when counting the number of available
     * items.
     * <p>
     * <strong>Note: </strong> Sort orders and comparator sorting are mutually
     * exclusive. If the {@link DataProvider} handles one, it should ignore the
     * other.
     *
     * @return optional sorting comparator
     */
    public Optional<Comparator<T>> getSortingComparator() {
        return Optional.ofNullable(inMemorySorting);
    }

    /**
     * Gets the requested range end. This is a shorthand for
     * {@code getOffset() + getLimit()} where the end is exclusive.
     *
     * @return the requested range end
     */
    public int getRequestedRangeEnd() {
        return getOffset() + getLimit();
    }
}
