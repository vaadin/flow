/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import java.io.Serializable;
import java.util.List;

/**
 * Represents hierarchical data.
 * <p>
 * Typically used as a backing data source for {@link TreeDataProvider}.
 *
 * @author Vaadin Ltd
 * @since 25.2
 *
 * @param <T>
 *            data type
 */
public interface HierarchicalTreeData<T> extends Serializable {

    /**
     * Get the immediate child items for the given item.
     *
     * @param item
     *            the item for which to retrieve child items for, null to
     *            retrieve all root items
     * @return an unmodifiable list of child items for the given item
     *
     * @throws IllegalArgumentException
     *             if the item does not exist in this structure
     */
    List<T> getChildren(T item);

    /**
     * Get the parent item for the given item.
     *
     * @param item
     *            the item for which to retrieve the parent item for
     * @return parent item for the given item or {@code null} if the item is a
     *         root item.
     * @throws IllegalArgumentException
     *             if the item does not exist in this structure
     */
    T getParent(T item);

    /**
     * Check whether the given item is in this hierarchy.
     *
     * @param item
     *            the item to check
     * @return {@code true} if the item is in this hierarchy, {@code false} if
     *         not
     */
    boolean contains(T item);
}
