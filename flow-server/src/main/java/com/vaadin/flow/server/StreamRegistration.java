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
import java.net.URI;

/**
 * Stream registration result.
 * <p>
 * Use {@link #getResourceUri()} to get URI after {@link StreamResource} /
 * {@link StreamReceiver} is registered.
 * <p>
 * Also allows resource unregistering.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public interface StreamRegistration extends Serializable {

    /**
     * Get resource URI for registered {@link StreamResource} instance.
     * <p>
     * The URI is relative to the application base URI.
     *
     * @return resource URI
     */
    URI getResourceUri();

    /**
     * Unregister {@link StreamResource}.
     * <p>
     * The resource will be removed from the session and its URI won't be served
     * by the application anymore so that the resource becomes available for GC.
     * <p>
     * It's the developer's responsibility to call this method at the
     * appropriate time. Otherwise the resource instance will stay in memory
     * until the session expires.
     */
    void unregister();

    /**
     * Get the stream resource whose registration result is represented by this
     * {@link StreamRegistration} instance.
     *
     * @return resource, or null if resource has been already unregistered
     */
    AbstractStreamResource getResource();
}
