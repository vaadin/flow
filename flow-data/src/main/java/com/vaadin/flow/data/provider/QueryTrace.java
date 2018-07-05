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
