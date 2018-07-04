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
package com.vaadin.flow.router.internal;

import java.io.Serializable;
import java.util.Objects;

import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.Router;

/**
 * An object used to encapsulate data used in resolving routing requests.
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
