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

/**
 * Data change events listener.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 * @param <T>
 *            the data type
 * @see DataProvider
 */
@FunctionalInterface
public interface DataProviderListener<T> extends Serializable {

    /**
     * Invoked when this listener receives a data change event from a data
     * source to which it has been added.
     * <p>
     * This event is fired when something has changed in the underlying data. It
     * doesn't allow to distinguish different kind of events
     * (add/remove/update). It means that the method implementation normally
     * just reloads the whole data to refresh.
     *
     * @param event
     *            the received event, not null
     */
    void onDataChange(DataChangeEvent<T> event);
}
