/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.router.internal;

import java.io.Serializable;
import java.util.Objects;

import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.Router;

/**
 * An object used to encapsulate data used in resolving routing requests.
 *
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class ResolveRequest implements Serializable {

    private final Router router;
    private final Location location;

    /**
     * Constructs a new ResolveRequest with the given Router and Location.
     *
     * @param router
     *            the router this request originated from, not {@code null}
     * @param location
     *            the location to resolve, not {@code null}
     */
    public ResolveRequest(Router router, Location location) {
        Objects.requireNonNull(router, "router cannot be null");
        Objects.requireNonNull(location, "location cannot be null");
        this.router = router;
        this.location = location;
    }

    /**
     * Gets the router that this request originates from.
     *
     * @return the router this request originates from
     */
    public Router getRouter() {
        return router;
    }

    /**
     * Gets the location that is requested to be resolved.
     *
     * @return the location to be resolved
     */
    public Location getLocation() {
        return location;
    }
}
