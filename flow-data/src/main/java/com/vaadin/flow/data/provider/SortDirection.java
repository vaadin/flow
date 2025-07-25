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

/**
 * Describes sorting direction.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public enum SortDirection {

    /**
     * Ascending (e.g. A-Z, 1..9) sort order
     */
    ASCENDING,

    /**
     * Descending (e.g. Z-A, 9..1) sort order
     */
    DESCENDING;

    /**
     * Get the sort direction that is the direct opposite to this one.
     *
     * @return a sort direction value
     */
    public SortDirection getOpposite() {
        return ASCENDING.equals(this) ? DESCENDING : ASCENDING;
    }

    /**
     * Get the short name of the sort direction.
     *
     * @return The shortened representation of the sort direction, either "asc"
     *         or "desc"
     */
    public String getShortName() {
        return ASCENDING.equals(this) ? "asc" : "desc";
    }
}
