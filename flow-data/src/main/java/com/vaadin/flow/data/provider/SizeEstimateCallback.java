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

import java.io.Serializable;

/**
 * Callback interface for estimating the number of items in a backend based on a
 * query.
 *
 * @param <T>
 *            the type of the items
 * @param <F>
 *            the type of the optional filter in the query, <code>Void</code> if
 *            filtering is not supported
 */
@FunctionalInterface
public interface SizeEstimateCallback<T, F> extends Serializable {

    /**
     * Returns the estimated size of the data set based on a query. This
     * callback is invoked for the initial size estimate, when reset or filter
     * has changed or when the previous size estimate is about to be reached.
     * <p>
     * The query provides the previous estimated size and optionally defines any
     * filtering to use through {@link Query#getFilter()}. The query also
     * contains information about paging and sorting although that information
     * is generally not applicable for determining the number of items.
     *
     * @param query
     *            the query that defines which items to count
     * @return the number of available items
     */
    // TODO refactor to return void and provide API to increase the size
    int sizeEstimate(SizeEstimateQuery<T, F> query);
}