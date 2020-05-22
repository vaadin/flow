/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import java.util.Comparator;
import java.util.List;

/**
 * Immutable query object used to get the estimated size of the data set with
 * {@link LazyDataView}. In addition to normal query parameters, contains the
 * previous size estimate that was used.
 * 
 * @param <T>
 *            the data type
 * @param <F>
 *            the filter type
 * @since
 */
public class SizeEstimateQuery<T, F> extends Query<T, F> {

    private final int previousSizeEstimate;
    private final boolean reset;

    /**
     * Constructs a size estimate query with given previous size, offset, limit,
     * sorting and filtering.
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
     * @param previousSizeEstimate
     *            the previous size estimate
     * @param reset
     *            whether the data set was reset
     */
    public SizeEstimateQuery(int offset, int limit,
            List<QuerySortOrder> sortOrders, Comparator<T> inMemorySorting,
            F filter, int previousSizeEstimate, boolean reset) {
        super(offset, limit, sortOrders, inMemorySorting, filter);
        this.previousSizeEstimate = previousSizeEstimate;
        this.reset = reset;
    }

    /**
     * Returns whether the data set was reset or not. The reset happens when the
     * data was updated due to addition, deleting or filter change.
     * 
     * @return {@code true} for reset, {@code false} for not
     */
    public boolean isReset() {
        return reset;
    }

    /**
     * Returns the previous size estimate used. If no previous size estimate was
     * used, returns 0 and at the same time {@link #isReset()} will return
     * {@code true}.
     * 
     * @return the previous size estimate
     */
    public int getPreviousSizeEstimate() {
        return previousSizeEstimate;
    }

}
