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

import java.io.Serializable;
import java.util.List;

import elemental.json.JsonValue;

/**
 * Array update strategy aware class.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public interface ArrayUpdater extends Serializable {

    /**
     * Array updater strategy.
     *
     */
    public interface Update extends Serializable {
        /**
         * Clears {@code length} elements in array from the {@code start}
         * position.
         *
         * @param start
         *            the start index
         * @param length
         *            the number of elements to clear
         */
        void clear(int start, int length);

        /**
         * Sets the {@code items} at the {@code start} position.
         *
         * @param start
         *            the start index
         * @param items
         *            the items to set
         */
        void set(int start, List<JsonValue> items);

        /**
         * Commits changes for the given {@code updateId}.
         *
         * @param updateId
         *            the update identifier of the commit
         */
        void commit(int updateId);
    }

    /**
     * Starts update of an array.
     *
     * @param sizeChange
     *            the size of the array where changes happened
     * @return array update strategy
     */
    Update startUpdate(int sizeChange);

    /**
     * Initialize the array.
     */
    void initialize();
}
