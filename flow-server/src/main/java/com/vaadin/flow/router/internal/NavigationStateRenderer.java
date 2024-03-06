/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.router.internal;

import java.util.List;

import com.vaadin.flow.component.Component;
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

    @Override
    protected List<Class<? extends RouterLayout>> getRouterLayoutTypes(
            Class<? extends Component> targetType, Router router) {
        NavigationState navigationState = getNavigationState();
        assert targetType == navigationState.getNavigationTarget()
                : "Trying to get layouts for wrong route target";

        return router.getRegistry()
                .getRouteLayouts(navigationState.getResolvedPath(), targetType);
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

        List<String> parameters = navigationState.getUrlParameters()
                .orElse(null);

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

}
