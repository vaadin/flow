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
package com.vaadin.router;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.vaadin.flow.di.Instantiator;
import com.vaadin.router.event.ActivationState;
import com.vaadin.router.event.AfterNavigationEvent;
import com.vaadin.router.event.BeforeNavigationEvent;
import com.vaadin.router.event.ErrorNavigationEvent;
import com.vaadin.router.event.EventUtil;
import com.vaadin.router.event.NavigationEvent;
import com.vaadin.router.util.RouterUtil;
import com.vaadin.ui.Component;
import com.vaadin.ui.UI;
import com.vaadin.ui.common.HasElement;

/**
 * Handles error navigation rendering in the target UI.
 *
 * @see HasErrorParameter
 */
public class ErrorStateRenderer implements NavigationHandler {

    private final NavigationState navigationState;

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

        BeforeNavigationEvent beforeNavigationActivating = new BeforeNavigationEvent(
                event, routeTargetType, ActivationState.ACTIVATING);

        int statusCode = ((HasErrorParameter) componentInstance)
                .setErrorParameter(beforeNavigationActivating,
                        ((ErrorNavigationEvent) event).getErrorParameter());

        EventUtil.collectBeforeNavigationObservers(chain)
                .forEach(listener -> listener
                        .beforeNavigation(beforeNavigationActivating));
        EventUtil.collectBeforeEnterObservers(chain)
                .forEach(listener -> listener
                        .beforeEnter(beforeNavigationActivating));

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

        return RouterUtil.getParentLayouts(targetType);
    }
}
