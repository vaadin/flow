package com.vaadin.hummingbird.change;

import com.vaadin.hummingbird.namespace.MapNamespace;

/**
 * Change describing a changed value in a map namespace.
 *
 * @since
 * @author Vaadin Ltd
 */
public class MapPutChange extends NamespaceChange {

    private final String key;
    private final Object value;

    /**
     * Creates a new put change.
     *
     * @param namespace
     *            the changed namespace
     * @param key
     *            the key of the changed value
     * @param value
     *            the new value
     */
    public MapPutChange(MapNamespace namespace, String key, Object value) {
        super(namespace);
        this.key = key;
        this.value = value;
    }

    /**
     * Gets the key of the change.
     * 
     * @return the key of the change
     */
    public String getKey() {
        return key;
    }

    /**
     * Gets the new value.
     * 
     * @return the new value
     */
    public Object getValue() {
        return value;
    }
}
