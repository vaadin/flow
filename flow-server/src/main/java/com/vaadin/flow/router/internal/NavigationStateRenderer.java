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

import java.util.List;
import java.util.Optional;

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
        assert targetType == navigationState
                .getNavigationTarget() : "Trying to get layouts for wrong route target";

        return router.getRegistry()
                .getRouteLayouts(navigationState.getResolvedPath(), targetType);
    }

    @Override
    protected void notifyNavigationTarget(Component componentInstance,
            NavigationEvent navigationEvent, BeforeEnterEvent beforeEnterEvent,
            LocationChangeEvent locationChangeEvent) {
        NavigationState navigationState = getNavigationState();
        Class<? extends Component> routeTargetType = navigationState
                .getNavigationTarget();

        Optional<List<String>> urlParameters = navigationState
                .getUrlParameters();
        if (urlParameters.isPresent()) {
            Object deserializedParameter = null;
            try {
                deserializedParameter = ParameterDeserializer
                        .deserializeUrlParameters(routeTargetType,
                                urlParameters.get());

            } catch (Exception e) {
                beforeEnterEvent.rerouteToError(NotFoundException.class,
                        String.format(
                                "Failed to parse url parameter, exception: %s",
                                e));
            }
            @SuppressWarnings("unchecked")
            HasUrlParameter<Object> hasUrlParameter = (HasUrlParameter<Object>) componentInstance;
            hasUrlParameter.setParameter(beforeEnterEvent,
                    deserializedParameter);
        }
    }

    @Override
    protected boolean eventActionsSupported() {
        return true;
    }

}
