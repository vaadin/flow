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

import com.vaadin.flow.function.SerializablePredicate;

import java.util.Collection;
import java.util.Objects;

/**
 * Data size change aware implementation for {@link ListDataProvider}.
 * Intercepts {@link #size(Query)} invocations and triggers size change handler.
 *
 * @param <T>
 *        data type
 *
 * @since
 */
public class SizeChangeAwareListDataProvider<T> extends ListDataProvider<T> implements SizeChangeAware {

    private SizeChangeHandler sizeChangeHandler = size -> {};

    public SizeChangeAwareListDataProvider(Collection<T> items) {
        super(items);
    }

    @Override
    public int size(Query<T, SerializablePredicate<T>> query) {
        int size = super.size(query);
        sizeChangeHandler.sizeEvent(size);
        return size;
    }

    @Override
    public void setSizeChangeHandler(SizeChangeHandler sizeChangeHandler) {
        this.sizeChangeHandler = sizeChangeHandler;
    }

    /**
     * Creates a new instance of {@link SizeChangeAwareListDataProvider}
     * on top of given {@link ListDataProvider} instance.
     *
     * @param dataProvider
     *        {@link DataProvider} which data size to be monitored
     * @param <T>
     *        data type
     * @return
     *        new instance of {@link SizeChangeAwareListDataProvider}
     */
    public static <T> SizeChangeAwareListDataProvider<T> fromListDataProvider(ListDataProvider<T> dataProvider) {
        Objects.requireNonNull(dataProvider, "ListDataProvider cannot be null");
        return new SizeChangeAwareListDataProvider<>(dataProvider.getItems());
    }
}
