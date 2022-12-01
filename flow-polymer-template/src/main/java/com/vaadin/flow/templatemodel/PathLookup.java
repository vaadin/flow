/**
 * Copyright (C) 2022 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.templatemodel;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A map for items that are looked up by hierarchical keys made up of period
 * separated strings.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 * @param <T>
 *            the item type
 * @deprecated This functionality is internal and bound to template model which
 *             is not supported for lit template. Polymer template support is
 *             deprecated - we recommend you to use {@code LitTemplate} instead.
 *             Read more details from <a href=
 *             "https://vaadin.com/blog/future-of-html-templates-in-vaadin">the
 *             Vaadin blog.</a>
 */
@Deprecated
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
     * @return and empty path lookup
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
