/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
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

    private static class ExceptionsTrace extends RuntimeException {
        private Set<Class<? extends Exception>> trace = new HashSet<>();

        void addException(Exception exception) {
            trace.add(exception.getClass());
        }

        boolean hasException(Exception exception) {
            return trace.contains(exception.getClass());
        }

        @Override
        public String getMessage() {
            return "Exceptions handled by "
                    + HasErrorParameter.class.getSimpleName() + " views are :"
                    + trace.stream().map(Class::getName)
                            .collect(Collectors.joining(", "));
        }
    }

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

        ExceptionsTrace trace = ComponentUtil.getData(event.getUI(),
                ExceptionsTrace.class);
        boolean isFirstCall = trace == null;
        Exception exception = ((ErrorNavigationEvent) event).getErrorParameter()
                .getCaughtException();
        if (isFirstCall) {
            trace = new ExceptionsTrace();
            ComponentUtil.setData(event.getUI(), ExceptionsTrace.class, trace);
        }
        if (trace.hasException(exception)) {
            trace.fillInStackTrace();
            LoggerFactory.getLogger(ErrorStateRenderer.class)
                    .error("The same exception {} "
                            + "has been thrown several times during navigation. "
                            + "Can't use any {} view for this error.",
                            exception.getClass().getName(),
                            HasErrorParameter.class.getSimpleName(), trace);
            return 500;
        }
        trace.addException(exception);
        try {
            return super.handle(event);
        } finally {
            if (isFirstCall) {
                ComponentUtil.setData(event.getUI(), ExceptionsTrace.class,
                        null);
            }
        }
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
        return true;
    }
}
