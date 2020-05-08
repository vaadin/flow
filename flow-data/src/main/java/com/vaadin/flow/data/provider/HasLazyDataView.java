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
 * Interface that defines methods for setting in memory data.
 * This will return a {@link LazyDataView}.
 *
 * @param <T>
 *         data type
 * @param <V>
 *         DataView type
 * @since
 */
public interface HasLazyDataView<T, V extends LazyDataView<T>> extends
        Serializable {
    /**
     * Supply data through a callback provider.
     *
     * @param fetchCallback
     *         function that returns a stream of items from the back end for
     *         a query
     * @return LazyDataView instance
     */
    V setDataProvider(
            CallbackDataProvider.FetchCallback<T, Void> fetchCallback);

    /**
     * Supply data through a callback provider with a count callback.
     *
     * @param fetchCallback
     *         function that returns a stream of items from the back end for
     *         a query
     * @param countCallback
     *         function that return the number of items in the back end for a
     *         query
     * @return LazyDataView instance
     */
    V setDataProvider(CallbackDataProvider.FetchCallback<T, Void> fetchCallback,
            CallbackDataProvider.CountCallback<T, Void> countCallback);

    // Using a more distinct type so that existing data provider API of HasDataProvider::setDataProvider can be overridden

    /**
     * Supply data through a BackendDataProvider that lazy loads items from a
     * back end.
     *
     * @param dataProvider
     *         BackendDataProvider instance
     * @return LazyDataView instance
     */
    V setDataProvider(BackEndDataProvider<T, Void> dataProvider);

    /**
     * Get the LazyDataView for the component. Throws if the data is not lazy
     * and should use another data view type.
     *
     * @return LazyDataView instance
     * @throws IllegalStateException
     *         when list data view is not applicable
     */
    V getLazyDataView();
}
