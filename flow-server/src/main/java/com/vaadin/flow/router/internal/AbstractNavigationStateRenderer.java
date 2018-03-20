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
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletResponse;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveEvent.ContinueNavigationAction;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.ErrorNavigationEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.EventUtil;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.LocationChangeEvent;
import com.vaadin.flow.router.NavigationEvent;
import com.vaadin.flow.router.NavigationHandler;
import com.vaadin.flow.router.NavigationState;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;

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
     * Creates a new renderer for the given navigation state
     * 
     * @param navigationState
     *            the target navigation state
     */
    public AbstractNavigationStateRenderer(NavigationState navigationState) {
        this.navigationState = navigationState;
    }

    /**
     * Gets the targeted navigation state
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

        BeforeLeaveEvent beforeNavigationDeactivating = new BeforeLeaveEvent(
                event, routeTargetType);

        TransitionOutcome transitionOutcome = executeBeforeLeaveNavigation(
                beforeNavigationDeactivating, ui.getElement());

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

        transitionOutcome = executeBeforeEnterNavigation(
                beforeNavigationActivating, componentInstance, routerLayouts);

        if (TransitionOutcome.REROUTED.equals(transitionOutcome)) {
            return reroute(event, beforeNavigationActivating);
        }

        ui.getInternals().showRouteTarget(event.getLocation(),
                navigationState.getResolvedPath(), componentInstance,
                routerLayouts);

        RouterUtil.updatePageTitle(event, componentInstance);

        int statusCode = locationChangeEvent.getStatusCode();
        validateStatusCode(statusCode, routeTargetType);

        if (statusCode == HttpServletResponse.SC_OK) {
            fireAfterNavigationListeners(componentInstance, routerLayouts,
                    new AfterNavigationEvent(locationChangeEvent));
        }

        return statusCode;
    }

    /**
     * Notified the navigation target about the status of the navigation
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

    private void fireAfterNavigationListeners(HasElement observersRoot,
            List<? extends HasElement> elements, AfterNavigationEvent event) {
        Stream<AfterNavigationObserver> observerRootDescendants = EventUtil
                .collectAfterNavigationObservers(
                        Collections.singletonList(observersRoot))
                .stream();
        Stream<AfterNavigationObserver> otherObservers = EventUtil
                .getImplementingComponents(
                        elements.stream().map(HasElement::getElement),
                        AfterNavigationObserver.class);
        Stream.concat(observerRootDescendants, otherObservers)
                .forEach(listener -> listener.afterNavigation(event));
    }

    /**
     * Inform any {@link BeforeLeaveObserver}s in detaching element chain.
     *
     * @param beforeNavigation
     *            navigation event sent to observers
     * @param element
     *            element for which to handle observers
     * @return result of observer events
     */
    private TransitionOutcome executeBeforeLeaveNavigation(
            BeforeLeaveEvent beforeNavigation, Element element) {
        Deque<BeforeLeaveObserver> leaveObservers;
        if (postponed != null) {
            leaveObservers = postponed.getLeaveObservers();
            if (!leaveObservers.isEmpty()) {
                postponed = null;
            }
        } else {
            leaveObservers = new ArrayDeque<>(
                    EventUtil.collectBeforeLeaveObservers(element));
        }

        while (!leaveObservers.isEmpty()) {
            BeforeLeaveObserver listener = leaveObservers.remove();
            listener.beforeLeave(beforeNavigation);

            if (beforeNavigation.hasRerouteTarget()) {
                return TransitionOutcome.REROUTED;
            } else if (beforeNavigation.isPostponed()) {
                postponed = Postpone.withLeaveObservers(leaveObservers);
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
     * @param observersRoot
     *            element which is used as a root to traverse for obeservers
     * @param elements
     *            elements for which to handle observers
     * @return result of observer events
     */
    private TransitionOutcome executeBeforeEnterNavigation(
            BeforeEnterEvent beforeNavigation, HasElement observersRoot,
            List<? extends HasElement> elements) {
        List<BeforeEnterObserver> enterObservers = Stream
                .concat(EventUtil
                        .collectBeforeEnterObservers(
                                Collections.singletonList(observersRoot))
                        .stream(),
                        EventUtil.getImplementingComponents(
                                elements.stream().map(HasElement::getElement),
                                BeforeEnterObserver.class))
                .collect(Collectors.toList());

        for (BeforeEnterObserver observer : enterObservers) {
            observer.beforeEnter(beforeNavigation);

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
