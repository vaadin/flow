/*
 * Copyright 2000-2017 Vaadin Ltd.
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

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorNavigationEvent;
import com.vaadin.flow.router.EventUtil;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.NavigationEvent;
import com.vaadin.flow.router.NavigationHandler;
import com.vaadin.flow.router.NavigationState;
import com.vaadin.flow.router.RouterLayout;

/**
 * Handles error navigation rendering in the target UI.
 *
 * @see HasErrorParameter
 */
public class ErrorStateRenderer implements NavigationHandler {

    private final NavigationState navigationState;
    private static List<Integer> statusCodes = ReflectTools
            .getConstantIntValues(HttpServletResponse.class);

    /**
     * Constructs a new state renderer for the given navigation state.
     *
     * @param navigationState
     *            the navigation state handled by this instance
     */
    public ErrorStateRenderer(NavigationState navigationState) {
        this.navigationState = navigationState;
    }

    /**
     * Gets the component instance to use for the given type and the
     * corresponding navigation event.
     * <p>
     * Override this method to control the creation of view instances.
     * <p>
     * By default always creates new instances.
     *
     * @param <T>
     *            the route target type
     * @param routeTargetType
     *            the class of the route target component
     * @param event
     *            the navigation event that uses the route target
     * @return an instance of the route target component
     */
    @SuppressWarnings("unchecked")
    protected <T extends HasElement> T getRouteTarget(Class<T> routeTargetType,
            NavigationEvent event) {
        UI ui = event.getUI();
        Optional<HasElement> currentInstance = ui.getInternals()
                .getActiveRouterTargetsChain().stream()
                .filter(component -> component.getClass()
                        .equals(routeTargetType))
                .findAny();
        return (T) currentInstance.orElseGet(() -> Instantiator.get(ui)
                .createRouteTarget(routeTargetType, event));
    }

    @Override
    public int handle(NavigationEvent event) {
        assert event instanceof ErrorNavigationEvent : "Error handling needs ErrorNavigationEvent";

        UI ui = event.getUI();

        Class<? extends Component> routeTargetType = navigationState
                .getNavigationTarget();
        List<Class<? extends RouterLayout>> routeLayoutTypes = getRouterLayoutTypes(
                routeTargetType);

        assert routeTargetType != null;
        assert routeLayoutTypes != null;

        RouterUtil.checkForDuplicates(routeTargetType, routeLayoutTypes);

        Component componentInstance = getRouteTarget(routeTargetType, event);
        List<HasElement> chain = new ArrayList<>();
        chain.add(componentInstance);

        for (Class<? extends RouterLayout> parentType : routeLayoutTypes) {
            chain.add(getRouteTarget(parentType, event));
        }

        BeforeEnterEvent beforeNavigationActivating = new BeforeEnterEvent(
                event, routeTargetType);

        int statusCode = ((HasErrorParameter) componentInstance)
                .setErrorParameter(beforeNavigationActivating,
                        ((ErrorNavigationEvent) event).getErrorParameter());

        validateStatusCode(statusCode, routeTargetType);

        EventUtil.collectBeforeEnterObservers(chain).forEach(
                listener -> listener.beforeEnter(beforeNavigationActivating));

        @SuppressWarnings("unchecked")
        List<RouterLayout> routerLayouts = (List<RouterLayout>) (List<?>) chain
                .subList(1, chain.size());

        ui.getInternals().showRouteTarget(event.getLocation(),
                componentInstance, routerLayouts);

        RouterUtil.updatePageTitle(event, componentInstance);

        AfterNavigationEvent afterNavigationEvent = new AfterNavigationEvent(
                RouterUtil.createEvent(event, chain));

        EventUtil.collectAfterNavigationObservers(chain).forEach(
                listener -> listener.afterNavigation(afterNavigationEvent));

        return statusCode;
    }

    private void validateStatusCode(int statusCode,
            Class<? extends Component> targetClass) {
        if (!statusCodes.contains(statusCode)) {
            String msg = String.format(
                    "Error state code must be a valid HttpServletResponse value. Received invalid value of '%s' for '%s'",
                    statusCode, targetClass.getName());
            throw new IllegalStateException(msg);
        }
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
    public List<Class<? extends RouterLayout>> getRouterLayoutTypes(
            Class<? extends Component> targetType) {
        assert targetType == navigationState.getNavigationTarget();

        return RouterUtil.getParentLayoutsForNonRouteTarget(targetType);
    }
}
