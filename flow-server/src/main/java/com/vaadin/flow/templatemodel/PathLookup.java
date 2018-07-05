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
package com.vaadin.flow.templatemodel;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A map for items that are looked up by hierarchical keys made up of period
 * separated strings.
 *
 * @author Vaadin Ltd
 * @since 1.0
 * @param <T>
 *            the item type
 */
public class PathLookup<T> {
    private final Map<String, T> items;

    private final String pathPrefix;

    private PathLookup() {
        items = new HashMap<>();
        pathPrefix = "";
    }

    private PathLookup(PathLookup<T> parentLookup, Map<String, T> newItems,
            String pathPrefix) {
        this.items = new HashMap<>(parentLookup.items);
        this.pathPrefix = pathPrefix;

        newItems.forEach(this::putItem);
    }

    private void putItem(String path, T item) {
        StringBuilder pathStringBuilder = new StringBuilder(pathPrefix);
        if (!path.isEmpty()) {
            pathStringBuilder.append(path);
            pathStringBuilder.append('.');
        }
        items.put(pathStringBuilder.toString(), item);
    }

    /**
     * Composes a new path lookup that contains all items of this path lookup
     * and all provided items supplemented with the provided path prefix.
     *
     * @param newItems
     *            new items to include in the composition
     * @param pathPrefix
     *            the prefix to include in the key of all new items
     * @return a new path lookup composition
     */
    public PathLookup<T> compose(Map<String, T> newItems, String pathPrefix) {
        return new PathLookup<>(this, newItems, pathPrefix);
    }

    /**
     * Creates an empty path lookup.
     *
     * @param <T>
     *            the item type
     * @return and emtpy path lookup
     */
    public static <T> PathLookup<T> empty() {
        return new PathLookup<>();
    }

    /**
     * Gets the item for the provided full path.
     * 
     * @param fullPath
     *            the fully qualified path for which to find an item
     * @return the item corresponding to the path, or an empty optional if there
     *         is no item
     */
    public Optional<T> getItem(String fullPath) {
        return Optional.ofNullable(items.get(fullPath));
    }

}
