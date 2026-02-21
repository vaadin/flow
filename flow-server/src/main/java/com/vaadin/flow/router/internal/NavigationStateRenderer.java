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
package com.vaadin.flow.router.internal;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.LocationChangeEvent;
import com.vaadin.flow.router.NavigationEvent;
import com.vaadin.flow.router.NavigationState;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.ParameterDeserializer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.router.RouterLayout;

/**
 * Handles navigation events by rendering a contained NavigationState in the
 * target UI.
 *
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @see NavigationState
 * @see Route
 * @since 1.0
 */
public class NavigationStateRenderer extends AbstractNavigationStateRenderer {

    private @Nullable LocationChangeEvent ongoingLocationChangeEvent;

    /**
     * Constructs a new NavigationStateRenderer that handles the given
     * navigation state.
     *
     * @param navigationState
     *            the navigation state handled by this instance
     */
    public NavigationStateRenderer(NavigationState navigationState) {
        super(navigationState);
    }

    void setOngoingLocationChangeEvent(
            @Nullable LocationChangeEvent ongoingLocationChangeEvent) {
        this.ongoingLocationChangeEvent = ongoingLocationChangeEvent;
    }

    @SuppressWarnings("NullAway") // getResolvedPath() is non-null when a
                                  // navigation target has been resolved
    @Override
    protected List<Class<? extends RouterLayout>> getRouterLayoutTypes(
            Class<? extends Component> targetType, Router router) {
        NavigationState navigationState = getNavigationState();
        assert targetType == navigationState.getNavigationTarget()
                : "Trying to get layouts for wrong route target";

        NavigationRouteTarget target = router.getRegistry()
                .getNavigationRouteTarget(navigationState.getResolvedPath());

        @Nullable
        RouteTarget routeTarget = target.getRouteTarget();
        @Nullable
        String path = target.getPath();
        if (routeTarget != null && path != null) {
            return getTargetParentLayouts(routeTarget, router.getRegistry(),
                    path);
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    protected void notifyNavigationTarget(Component componentInstance,
            NavigationEvent navigationEvent, BeforeEnterEvent beforeEnterEvent,
            LocationChangeEvent locationChangeEvent) {

        if (!(componentInstance instanceof HasUrlParameter)) {
            return;
        }

        NavigationState navigationState = getNavigationState();
        Class<? extends Component> routeTargetType = navigationState
                .getNavigationTarget();
        if (routeTargetType == null) {
            return;
        }

        List<String> parameters = navigationState.getUrlParameters()
                .orElse(Collections.emptyList());

        @Nullable
        Object deserializedParameter = null;
        try {
            deserializedParameter = ParameterDeserializer
                    .deserializeRouteParameters(routeTargetType, parameters);
        } catch (Exception e) {
            beforeEnterEvent.rerouteToError(NotFoundException.class, String
                    .format("Failed to parse url parameter, exception: %s", e));
            return;
        }

        HasUrlParameter<Object> hasUrlParameter = (HasUrlParameter<Object>) componentInstance;
        hasUrlParameter.setParameter(beforeEnterEvent, deserializedParameter);
    }

    @Override
    protected Optional<HasElement> findActiveRouteTarget(NavigationEvent event,
            Predicate<HasElement> isRouteTargetType) {
        Optional<HasElement> currentInstance;
        if (event.isForwardTo() && ongoingLocationChangeEvent != null) {
            // When forwarding, first check the current navigation chain
            // A view might forward to itself with different parameters,
            // but at this point the active navigation chain is not yet updated
            currentInstance = ongoingLocationChangeEvent.getRouteTargetChain()
                    .stream().filter(isRouteTargetType).findAny();
        } else {
            currentInstance = Optional.empty();
        }
        // then check the active navigation chain
        return currentInstance.or(
                () -> super.findActiveRouteTarget(event, isRouteTargetType));
    }

}
