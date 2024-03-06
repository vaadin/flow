/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server;

import java.io.Serializable;
import java.util.UUID;

/**
 * Abstract stream resource class. Extending class may be used for data transfer
 * in either to the client (dynamic data) or from the client (file upload).
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public abstract class AbstractStreamResource implements Serializable {

    private long cacheTime = 0L;

    private final String id = UUID.randomUUID().toString();

    /**
     * Gets the length of cache expiration time. This gives the possibility to
     * cache the resource. "Cache-Control" HTTP header will be set based on this
     * value.
     * <p>
     * Default value is {@code 0}. So caching is disabled.
     *
     * @return cache time in milliseconds.
     */
    public long getCacheTime() {
        return cacheTime;
    }

    /**
     * Set cache time in millis. Zero or negative value disables the caching of
     * this stream.
     *
     * @param cacheTime
     *            cache time
     * @return this resource
     */
    public AbstractStreamResource setCacheTime(long cacheTime) {
        this.cacheTime = cacheTime;
        return this;
    }

    /**
     * Gets unique identifier of the resource.
     *
     * @return the resource unique id
     */
    public String getId() {
        return id;
    }

    /**
     * Get the resource name.
     * <p>
     * The value will be used in URI (generated when resource is registered) in
     * a way that the {@code name} is the last segment of the path. So this is a
     * synthetic name.
     *
     * @return resource name
     */
    public abstract String getName();
}
