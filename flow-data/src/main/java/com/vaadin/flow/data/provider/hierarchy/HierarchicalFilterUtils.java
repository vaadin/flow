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
package com.vaadin.flow.data.provider.hierarchy;

import java.util.stream.Stream;

import com.vaadin.flow.data.provider.ConfigurableFilterDataProviderWrapper;
import com.vaadin.flow.data.provider.DataProviderWrapper;
import com.vaadin.flow.function.SerializableBiFunction;

/**
 * @author Vaadin Ltd
 *
 */
final class HierarchicalFilterUtils {

    static abstract class HierarchialConfigurableFilterDataProviderWrapper<T, Q, C, F>
            extends ConfigurableFilterDataProviderWrapper<T, Q, C, F>
            implements HierarchicalDataProvider<T, Q> {

        HierarchialConfigurableFilterDataProviderWrapper(
                HierarchicalDataProvider<T, F> dataProvider,
                SerializableBiFunction<Q, C, F> filterCombiner) {
            super(dataProvider);
        }

        @Override
        public int getChildCount(HierarchicalQuery<T, Q> query) {
            return getDataProvider()
                    .getChildCount(adapt(query, getFilter(query)));
        }

        @Override
        public Stream<T> fetchChildren(HierarchicalQuery<T, Q> query) {
            return getDataProvider()
                    .fetchChildren(adapt(query, getFilter(query)));
        }

        @Override
        public boolean hasChildren(T item) {
            return getDataProvider().hasChildren(item);
        }

        private HierarchicalDataProvider<T, F> getDataProvider() {
            return (HierarchicalDataProvider<T, F>) dataProvider;
        }

    }

    static abstract class HierarchicalFilterDataProviderWrapper<T, F, M>
            extends DataProviderWrapper<T, F, M>
            implements HierarchicalDataProvider<T, F> {

        HierarchicalFilterDataProviderWrapper(
                HierarchicalDataProvider<T, M> dataProvider) {
            super(dataProvider);
        }

        @Override
        public int getChildCount(HierarchicalQuery<T, F> query) {
            return getDataProvider()
                    .getChildCount(adapt(query, getFilter(query)));
        }

        @Override
        public Stream<T> fetchChildren(HierarchicalQuery<T, F> query) {
            return getDataProvider()
                    .fetchChildren(adapt(query, getFilter(query)));
        }

        @Override
        public boolean hasChildren(T item) {
            return getDataProvider().hasChildren(item);
        }

        private HierarchicalDataProvider<T, M> getDataProvider() {
            return (HierarchicalDataProvider<T, M>) dataProvider;
        }

    }

    private HierarchicalFilterUtils() {
        // avoid instantiating utility class
    }

    private static <T, F, Q> HierarchicalQuery<T, F> adapt(
            HierarchicalQuery<T, Q> query, F filter) {
        return new HierarchicalQuery<T, F>(query.getOffset(), query.getLimit(),
                query.getSortOrders(), query.getInMemorySorting(), filter,
                query.getParent());
    }

}
