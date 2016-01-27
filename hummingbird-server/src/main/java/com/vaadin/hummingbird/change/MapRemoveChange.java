package com.vaadin.hummingbird.change;

import com.vaadin.hummingbird.namespace.MapNamespace;

import elemental.json.JsonObject;

/**
 * Change describing a value removed from a map namespace.
 *
 * @since
 * @author Vaadin Ltd
 */
public class MapRemoveChange extends NamespaceChange {

    private final String key;

    /**
     * Creates a new remove change.
     *
     * @param namespace
     *            the changed namespace
     * @param key
     *            the removed key
     */
    public MapRemoveChange(MapNamespace namespace, String key) {
        super(namespace);

        this.key = key;
    }

    /**
     * Gets the removed key.
     *
     * @return the removed key
     */
    public String getKey() {
        return key;
    }

    @Override
    protected void populateJson(JsonObject json) {
        json.put("type", "remove");

        super.populateJson(json);

        json.put("key", key);
    }
}
