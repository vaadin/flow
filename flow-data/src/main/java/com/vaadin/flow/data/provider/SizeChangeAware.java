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

/**
 * Monitors the component data size change and delegates a new size value
 * handling to {@link SizeChangeHandler}.
 * Data size to be monitored is normally refers to {@link DataProvider}
 * or {@link DataCommunicator}, but not limited to them.
 *
 * @since
 */
public interface SizeChangeAware {

    /**
     * Sets the handler to be invoked if new data size value has been obtained.
     * @param sizeChangeHandler
     *        data size change event handler
     */
    void setSizeChangeHandler(SizeChangeHandler sizeChangeHandler);
}
