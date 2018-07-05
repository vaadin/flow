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
