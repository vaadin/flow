/*
 * Copyright 2000-2018 Vaadin Ltd.
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

/**
 * Sorting information for {@link Query}.
 *
 * @see Query
 * @since 1.0
 */
public class QuerySortOrder extends SortOrder<String> {

    /**
     * Constructs sorting information for usage in a {@link Query}.
     *
     * @param sorted
     *            sorting information, usually field id
     * @param direction
     *            sorting direction
     */
    public QuerySortOrder(String sorted, SortDirection direction) {
        super(sorted, direction);
    }

    /**
     * Creates a new query sort builder with given sorting using ascending sort
     * direction.
     *
     * @param by
     *            the string to sort by
     *
     * @return the query sort builder
     */
    public static QuerySortOrderBuilder asc(String by) {
        return new QuerySortOrderBuilder().thenAsc(by);
    }

    /**
     * Creates a new query sort builder with given sorting using descending sort
     * direction.
     *
     * @param by
     *            the string to sort by
     *
     * @return the query sort builder
     */
    public static QuerySortOrderBuilder desc(String by) {
        return new QuerySortOrderBuilder().thenDesc(by);
    }
}
