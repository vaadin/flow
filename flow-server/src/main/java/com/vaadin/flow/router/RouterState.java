/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.router;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;

/**
 * An immutable snapshot of a UI's current navigation state.
 * <p>
 * Carried by {@link com.vaadin.flow.component.UI#routerStateSignal()} so that
 * components can observe the active route reactively, instead of registering an
 * {@link AfterNavigationListener} and manually fetching the initial state on
 * attach.
 * <p>
 * The snapshot covers the same data already available through
 * {@link AfterNavigationEvent} plus the navigation target class. It is produced
 * at the same point that {@code AfterNavigationEvent} is dispatched, so the
 * signal value and the listener notification are always consistent.
 *
 * @param location
 *            the location of the current view (path, query parameters,
 *            fragment); never {@code null}. Before the first navigation, this
 *            is an empty location ({@code new Location("")}).
 * @param routeParameters
 *            route parameters extracted from the URL; never {@code null}. May
 *            be {@link RouteParameters#empty()}.
 * @param activeChain
 *            unmodifiable list of the currently active route target and its
 *            parent layouts, leaf first. Never {@code null}; empty before the
 *            first navigation completes.
 * @param navigationTarget
 *            the class of the leaf route target. {@code null} before the first
 *            navigation completes. navigation completes. Use
 *            {@link #isNavigationPending()} to check for this state.
 * @since 25.2
 */
public record RouterState(Location location, RouteParameters routeParameters,
        List<HasElement> activeChain,
        Class<? extends Component> navigationTarget) implements Serializable {

    public RouterState {
        Objects.requireNonNull(location, "location cannot be null");
        Objects.requireNonNull(routeParameters,
                "routeParameters cannot be null");
        Objects.requireNonNull(activeChain, "activeChain cannot be null");
        activeChain = List.copyOf(activeChain);
    }

    /**
     * Returns the currently shown leaf view, if any.
     *
     * @return the leaf route target, or empty if no navigation has happened yet
     */
    public Optional<HasElement> currentView() {
        return activeChain.isEmpty() ? Optional.empty()
                : Optional.of(activeChain.get(0));
    }

    /**
     * Returns whether the first navigation is still pending.
     *
     * @return {@code true} before the first navigation completes, otherwise
     *         {@code false}
     */
    public boolean isNavigationPending() {
        return navigationTarget == null;
    }
}
