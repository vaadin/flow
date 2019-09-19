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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorNavigationEvent;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.LocationChangeEvent;
import com.vaadin.flow.router.NavigationEvent;
import com.vaadin.flow.router.NavigationState;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.router.RouterLayout;

/**
 * Handles error navigation rendering in the target UI.
 *
 * @see HasErrorParameter
 * @since 1.0
 */
public class ErrorStateRenderer extends AbstractNavigationStateRenderer {

    /**
     * Constructs a new state renderer for the given navigation state.
     *
     * @param navigationState
     *            the navigation state handled by this instance
     */
    public ErrorStateRenderer(NavigationState navigationState) {
        super(navigationState);
    }

    @Override
    public int handle(NavigationEvent event) {
        assert event instanceof ErrorNavigationEvent : "Error handling needs ErrorNavigationEvent";
        return super.handle(event);

    }

    @Override
    protected void notifyNavigationTarget(Component componentInstance,
            NavigationEvent navigationEvent, BeforeEnterEvent beforeEnterEvent,
            LocationChangeEvent locationChangeEvent) {
        @SuppressWarnings({ "rawtypes", "unchecked" })
        int statusCode = ((HasErrorParameter) componentInstance)

                .setErrorParameter(beforeEnterEvent,
                        ((ErrorNavigationEvent) navigationEvent)
                                .getErrorParameter());

        locationChangeEvent.setStatusCode(statusCode);
    }

    /**
     * Gets the router layout types to show for the given route target type,
     * starting from the parent layout immediately wrapping the route target
     * type.
     *
     * @param targetType
     *            component type to show
     *
     * @return a list of parent {@link RouterLayout} types, not
     *         <code>null</code>
     */
    @Override
    public List<Class<? extends RouterLayout>> getRouterLayoutTypes(
            Class<? extends Component> targetType, Router router) {
        assert targetType == getNavigationState().getNavigationTarget();

        return RouteUtil.getParentLayoutsForNonRouteTarget(targetType);
    }

    @Override
    protected boolean eventActionsSupported() {
        return false;
    }
}
