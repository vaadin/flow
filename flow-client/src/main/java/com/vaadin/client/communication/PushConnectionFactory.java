/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.client.communication;

import com.vaadin.client.Registry;

/**
 * Factory for {@link PushConnection}.
 *
 * Produces a {@link PushConnection} for the provided {@link Registry}
 *
 * @since 1.0
 */
@FunctionalInterface
public interface PushConnectionFactory {

    /**
     * Creates a new {@link PushConnection} instance for the given
     * {@code registry}.
     *
     * @param registry
     *            the global registry
     * @return the push connection instance
     */
    PushConnection create(Registry registry);
}
