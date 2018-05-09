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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.router.*;
import com.vaadin.flow.router.BeforeLeaveEvent.ContinueNavigationAction;

/**
 * Base class for navigation handlers that target a navigation state.
 *
 * @author Vaadin Ltd
 */
public abstract class AbstractNavigationStateRenderer
        implements NavigationHandler {
    private enum TransitionOutcome {
        FINISHED, REROUTED, POSTPONED
    }

    private static List<Integer> statusCodes = ReflectTools
            .getConstantIntValues(HttpServletResponse.class);

    private final NavigationState navigationState;

    private Postpone postponed = null;

    /**
     * Creates a new renderer for the given navigation state.
     *
     * @param navigationState
     *            the target navigation state
     */
    public AbstractNavigationStateRenderer(NavigationState navigationState) {
        this.navigationState = navigationState;
    }

    /**
     * Gets the targeted navigation state.
     *
     * @return the targeted navigation state
     */
    public NavigationState getNavigationState() {
        return navigationState;
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
    // Non-private for testing purposes
    static <T extends HasElement> T getRouteTarget(Class<T> routeTargetType,
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
        UI ui = event.getUI();

        Class<? extends Component> routeTargetType = navigationState
                .getNavigationTarget();
        List<Class<? extends RouterLayout>> routeLayoutTypes = getRouterLayoutTypes(
                routeTargetType);

        assert routeTargetType != null;
        assert routeLayoutTypes != null;

        clearContinueNavigationAction(ui);
        RouterUtil.checkForDuplicates(routeTargetType, routeLayoutTypes);

        if (eventActionsSupported()) {

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
                beforeLeaveHandlers
                        .addAll(EventUtil.collectBeforeLeaveObservers(ui));
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
        }

        Component componentInstance = getRouteTarget(routeTargetType, event);
        List<HasElement> chain = new ArrayList<>();
        chain.add(componentInstance);

        for (Class<? extends RouterLayout> parentType : routeLayoutTypes) {
            chain.add(getRouteTarget(parentType, event));
        }

        BeforeEnterEvent beforeNavigationActivating = new BeforeEnterEvent(
                event, routeTargetType);

        LocationChangeEvent locationChangeEvent = RouterUtil.createEvent(event,
                chain);

        notifyNavigationTarget(componentInstance, event,
                beforeNavigationActivating, locationChangeEvent);

        if (beforeNavigationActivating.hasRerouteTarget()) {
            return reroute(event, beforeNavigationActivating);
        }

        @SuppressWarnings("unchecked")
        List<RouterLayout> routerLayouts = (List<RouterLayout>) (List<?>) chain
                .subList(1, chain.size());

        List<BeforeEnterHandler> enterHandlers = new ArrayList<>(
                ui.getNavigationListeners(BeforeEnterHandler.class));
        enterHandlers.addAll(EventUtil.collectBeforeEnterObservers(
                ui.getInternals().getActiveRouterTargetsChain(), chain));
        TransitionOutcome transitionOutcome = executeBeforeEnterNavigation(
                beforeNavigationActivating, enterHandlers);

        if (eventActionsSupported()
                && TransitionOutcome.REROUTED.equals(transitionOutcome)) {
            return reroute(event, beforeNavigationActivating);
        }

        ui.getInternals().showRouteTarget(event.getLocation(),
                navigationState.getResolvedPath(), componentInstance,
                routerLayouts);

        RouterUtil.updatePageTitle(event, componentInstance);

        int statusCode = locationChangeEvent.getStatusCode();
        validateStatusCode(statusCode, routeTargetType);

        List<AfterNavigationHandler> afterNavigationHandlers = new ArrayList<>(
                ui.getNavigationListeners(AfterNavigationHandler.class));
        afterNavigationHandlers
                .addAll(EventUtil.collectAfterNavigationObservers(ui));

        fireAfterNavigationListeners(
                new AfterNavigationEvent(locationChangeEvent),
                afterNavigationHandlers);

        return statusCode;
    }

    /**
     * Notified the navigation target about the status of the navigation.
     *
     * @param componentInstance
     *            the navigation target instance
     * @param navigationEvent
     *            the low level navigation event that is being processed
     * @param beforeEnterEvent
     *            the before enter event that will be fired unless navigation is
     *            rerouted
     * @param locationChangeEvent
     *            the location change event that will be fired unless navigation
     *            is rerouted
     */
    protected abstract void notifyNavigationTarget(Component componentInstance,
            NavigationEvent navigationEvent, BeforeEnterEvent beforeEnterEvent,
            LocationChangeEvent locationChangeEvent);

    /**
     * Gets the router layout types to show for the given route target type,
     * starting from the parent layout immediately wrapping the route target
     * type.
     *
     * @param routeTargetType
     *            component type that will be shown
     *
     * @return a list of parent {@link RouterLayout} types, not
     *         <code>null</code>
     */
    protected abstract List<Class<? extends RouterLayout>> getRouterLayoutTypes(
            Class<? extends Component> routeTargetType);

    /**
     * Checks whether this renderer should reroute or postpone navigation based
     * on results from event listeners. Furthermore, the before leave event is
     * not fired at all if event actions are not supported.
     *
     * @return <code>true</code> to support event actions; <code>false</code>
     *         otherwise.
     */
    protected abstract boolean eventActionsSupported();

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

        final Class<?> routeTargetType = beforeNavigation.getRouteTargetType();
        Location location = new Location(Router.resolve(routeTargetType, routeTargetType
                .getAnnotation(Route.class)));

        return new NavigationEvent(event.getSource(), location, event.getUI(),
                NavigationTrigger.PROGRAMMATIC);
    }

    private static void validateStatusCode(int statusCode,
            Class<? extends Component> targetClass) {
        if (!statusCodes.contains(statusCode)) {
            String msg = String.format(
                    "Error state code must be a valid HttpServletResponse value. Received invalid value of '%s' for '%s'",
                    statusCode, targetClass.getName());
            throw new IllegalStateException(msg);
        }
    }

}
