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
package com.vaadin.signals;

/**
 * The rest of this class will be implemented later.
 */
public class ListSignal {

    /**
     * A list insertion position before and/or after the referenced entries. If
     * both entries are defined, then this position represents an exact match
     * that is valid only if the two entries are adjacent. If only one is
     * defined, then the position is relative to only that position. A position
     * with neither reference is not valid for inserts but it is valid to test a
     * parent-child relationship regardless of the child position.
     * {@link Id#ZERO} represents the edge of the list, i.e. the first or the
     * last position.
     *
     * @param after
     *            id of the node to insert immediately after, or
     *            <code>null</code> to not define a constraint
     * @param before
     *            id of the node to insert immediately before, or
     *            <code>null</code> to not define a constraint
     */
    public record ListPosition(Id after, Id before) {
        /**
         * Gets the insertion position that corresponds to the beginning of the
         * list.
         *
         * @return a list position for the beginning of the list, not
         *         <code>null</code>
         */
        public static ListPosition first() {
            // After edge
            return new ListPosition(Id.ZERO, null);
        }

        /**
         * Gets the insertion position that corresponds to the end of the list.
         *
         * @return a list position for the end of the list, not
         *         <code>null</code>
         */
        public static ListPosition last() {
            // Before edge
            return new ListPosition(null, Id.ZERO);
        }
    }
}
