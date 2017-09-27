/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.data;

import com.vaadin.data.provider.DataCommunicator;
import com.vaadin.ui.Component;

/**
 * A base class for listing components. Provides common handling for fetching
 * backend data items, selection logic, and server-client communication.
 * <p>
 * <strong>Note: </strong> concrete component implementations should implement
 * the {@link HasDataProvider} or {@link HasFilterableDataProvider} interface.
 *
 * @author Vaadin Ltd.
 * @param <T>
 *            the item data type
 */
public abstract class AbstractListing<T> extends Component
        implements HasItems<T> {

    /**
     * Returns the data communicator of this listing.
     *
     * @return the data communicator, not null
     */
    public abstract DataCommunicator<T> getDataCommunicator();
}
