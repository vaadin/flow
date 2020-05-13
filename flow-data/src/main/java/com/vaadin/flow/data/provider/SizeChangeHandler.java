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
 * Base interface for all component data size change handlers.
 *
 * @since
 */
public interface SizeChangeHandler {

    /**
     * Handles the data size and triggers {@link SizeChangeEvent} if
     * a size value has been changed.
     *
     * @param size
     *        new data size value
     */
    void sizeEvent(int size);
}
