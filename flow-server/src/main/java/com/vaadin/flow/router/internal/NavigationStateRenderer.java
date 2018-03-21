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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveEvent.ContinueNavigationAction;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.ErrorNavigationEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.EventUtil;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.LocationChangeEvent;
import com.vaadin.flow.router.NavigationEvent;
import com.vaadin.flow.router.NavigationHandler;
import com.vaadin.flow.router.NavigationState;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.ParameterDeserializer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteNotFoundError;
import com.vaadin.flow.router.RouterLayout;

/**
 * Handles navigation events by rendering a contained NavigationState in the
 * target UI.
 *
 * @see NavigationState
 * @see Route
 */
public class NavigationStateRenderer implements NavigationHandler {

    private enum TransitionOutcome {
        FINISHED, REROUTED, POSTPONED
    }

    private final NavigationState navigationState;
    private Postpone postponed = null;

    /**
     * Constructs a new NavigationStateRenderer that handles the given
     * navigation state.
     *
     * @param navigationState
     *            the navigation state handled by this instance
     */
    public NavigationStateRenderer(NavigationState navigationState) {
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

    @SuppressWarnings("unchecked")
    @Override
    public int handle(NavigationEvent event) {
        UI ui = event.getUI();

        Class<? extends Component> routeTargetType = navigationState
                .getNavigationTarget();
        List<Class<? extends RouterLayout>> routeLayoutTypes = getRouterLayoutTypes(
                routeTargetType);

        assert routeTargetType != null;
        assert routeLayoutTypes != null;

        clearContinueNavigationAction(ui);
        RouterUtil.checkForDuplicates(routeTargetType, routeLayoutTypes);

        BeforeLeaveEvent beforeNavigationDeactivating = new BeforeLeaveEvent(
                event, routeTargetType);

        Deque<BeforeLeaveHandler> leaveHandlers;
        if (postponed != null) {
            leaveHandlers = postponed.getLeaveObservers();
            if (!leaveHandlers.isEmpty()) {
                postponed = null;
            }
        } else {
            List<BeforeLeaveHandler> beforeLeaveHandlers = new ArrayList<>(
                    ui.getNavigationListeners(BeforeLeaveHandler.class));
            beforeLeaveHandlers.addAll(
                    EventUtil.collectBeforeLeaveObservers(ui.getElement()));
            leaveHandlers = new ArrayDeque<>(beforeLeaveHandlers);
        }
        TransitionOutcome transitionOutcome = executeBeforeLeaveNavigation(
                beforeNavigationDeactivating, leaveHandlers);

        if (transitionOutcome == TransitionOutcome.REROUTED) {
            return reroute(event, beforeNavigationDeactivating);
        } else if (transitionOutcome == TransitionOutcome.POSTPONED) {
            ContinueNavigationAction currentAction = beforeNavigationDeactivating
                    .getContinueNavigationAction();
            currentAction.setReferences(this, event);
            storeContinueNavigationAction(ui, currentAction);
            return HttpServletResponse.SC_OK;
        }

        Component componentInstance = getRouteTarget(routeTargetType, event);
        List<HasElement> chain = new ArrayList<>();
        chain.add(componentInstance);

        for (Class<? extends RouterLayout> parentType : routeLayoutTypes) {
            chain.add(getRouteTarget(parentType, event));
        }

        BeforeEnterEvent beforeNavigationActivating = new BeforeEnterEvent(
                event, routeTargetType);

        Optional<List<String>> urlParameters = navigationState
                .getUrlParameters();
        if (urlParameters.isPresent()) {
            final Object deserializedParameter;
            try {
                deserializedParameter = ParameterDeserializer
                        .deserializeUrlParameters(routeTargetType,
                                urlParameters.get());
            } catch (Exception e) {
                beforeNavigationActivating.rerouteToError(
                        NotFoundException.class,
                        String.format(
                                "Failed to parse url parameter, exception: %s",
                                e));
                return reroute(event, beforeNavigationActivating);
            }
            ((HasUrlParameter) componentInstance).setParameter(
                    beforeNavigationActivating, deserializedParameter);
        }

        if (beforeNavigationActivating.hasRerouteTarget()) {
            return reroute(event, beforeNavigationActivating);
        }

        List<RouterLayout> routerLayouts = (List<RouterLayout>) (List<?>) chain
                .subList(1, chain.size());

        List<BeforeEnterHandler> enterHandlers = new ArrayList<>(
                ui.getNavigationListeners(BeforeEnterHandler.class));
        enterHandlers.addAll(EventUtil.collectEnterObservers(componentInstance,
                routerLayouts));
        transitionOutcome = executeBeforeEnterNavigation(
                beforeNavigationActivating, enterHandlers);

        if (TransitionOutcome.REROUTED.equals(transitionOutcome)) {
            return reroute(event, beforeNavigationActivating);
        }

        ui.getInternals().showRouteTarget(event.getLocation(),
                navigationState.getResolvedPath(), componentInstance,
                routerLayouts);

        RouterUtil.updatePageTitle(event, componentInstance);

        LocationChangeEvent locationChangeEvent = RouterUtil.createEvent(event,
                chain);

        if (locationChangeEvent.getStatusCode() == HttpServletResponse.SC_OK) {
            List<AfterNavigationHandler> afterNavigationHandlers = new ArrayList<>(
                    ui.getNavigationListeners(AfterNavigationHandler.class));
            afterNavigationHandlers.addAll(
                    EventUtil.collectAfterNavigationObservers(componentInstance,
                            routerLayouts));
            fireAfterNavigationListeners(
                    new AfterNavigationEvent(locationChangeEvent),
                    afterNavigationHandlers);
        }

        if (componentInstance instanceof RouteNotFoundError) {
            locationChangeEvent.setStatusCode(HttpServletResponse.SC_NOT_FOUND);
        }
        return locationChangeEvent.getStatusCode();
    }

    private void clearContinueNavigationAction(UI ui) {
        storeContinueNavigationAction(ui, null);
    }

    private void storeContinueNavigationAction(UI ui,
            ContinueNavigationAction currentAction) {
        ContinueNavigationAction previousAction = ui.getInternals()
                .getContinueNavigationAction();
        if (previousAction != null && previousAction != currentAction) {
            // Any earlier action is now obsolete, so it must be defused
            // to prevent it from wreaking havoc if it's ever called
            previousAction.setReferences(null, null);
        }
        ui.getInternals().setContinueNavigationAction(currentAction);
    }

    private void fireAfterNavigationListeners(AfterNavigationEvent event,
            List<AfterNavigationHandler> afterNavigationHandlers) {
        afterNavigationHandlers
                .forEach(listener -> listener.afterNavigation(event));
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

        return RouterUtil.getParentLayouts(targetType,
                navigationState.getResolvedPath());
    }

    /**
     * Inform any {@link BeforeLeaveObserver}s in detaching element chain.
     *
     * @param beforeNavigation
     *            navigation event sent to observers
     * @param leaveHandlers
     *            handlers for before leave event
     * @return result of observer events
     */
    private TransitionOutcome executeBeforeLeaveNavigation(
            BeforeLeaveEvent beforeNavigation,
            Deque<BeforeLeaveHandler> leaveHandlers) {
        while (!leaveHandlers.isEmpty()) {
            BeforeLeaveHandler listener = leaveHandlers.remove();
            listener.beforeLeave(beforeNavigation);

            if (beforeNavigation.hasRerouteTarget()) {
                return TransitionOutcome.REROUTED;
            } else if (beforeNavigation.isPostponed()) {
                postponed = Postpone.withLeaveObservers(leaveHandlers);
                return TransitionOutcome.POSTPONED;
            }
        }

        return TransitionOutcome.FINISHED;
    }

    /**
     * Inform any {@link BeforeEnterObserver}s in attaching element chain.
     *
     * @param beforeNavigation
     *            navigation event sent to observers
     * @param enterHandlers
     *            handlers for before enter event
     * @return result of observer events
     */
    private TransitionOutcome executeBeforeEnterNavigation(
            BeforeEnterEvent beforeNavigation,
            List<BeforeEnterHandler> enterHandlers) {

        for (BeforeEnterHandler eventHandler : enterHandlers) {
            eventHandler.beforeEnter(beforeNavigation);

            if (beforeNavigation.hasRerouteTarget()) {
                return TransitionOutcome.REROUTED;
            }
        }
        return TransitionOutcome.FINISHED;
    }

    private int reroute(NavigationEvent event, BeforeEvent beforeNavigation) {
        NavigationHandler handler = beforeNavigation.getRerouteTarget();

        NavigationEvent newNavigationEvent = getNavigationEvent(event,
                beforeNavigation);

        return handler.handle(newNavigationEvent);
    }

    private NavigationEvent getNavigationEvent(NavigationEvent event,
            BeforeEvent beforeNavigation) {
        if (beforeNavigation.hasErrorParameter()) {
            ErrorParameter<?> errorParameter = beforeNavigation
                    .getErrorParameter();

            return new ErrorNavigationEvent(event.getSource(),
                    event.getLocation(), event.getUI(),
                    NavigationTrigger.PROGRAMMATIC, errorParameter);
        }

        Location location = new Location(beforeNavigation.getRouteTargetType()
                .getAnnotation(Route.class).value());

        return new NavigationEvent(event.getSource(), location, event.getUI(),
                NavigationTrigger.PROGRAMMATIC);
    }
}
