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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.Pair;
import com.vaadin.flow.internal.ReflectTools;
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
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.LocationChangeEvent;
import com.vaadin.flow.router.NavigationEvent;
import com.vaadin.flow.router.NavigationHandler;
import com.vaadin.flow.router.NavigationState;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.VaadinSession;

/**
 * Base class for navigation handlers that target a navigation state.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public abstract class AbstractNavigationStateRenderer
        implements NavigationHandler {
    private enum TransitionOutcome {
        FORWARDED, FINISHED, REROUTED, POSTPONED
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
                routeTargetType, ui.getRouter());

        assert routeTargetType != null;
        assert routeLayoutTypes != null;

        clearContinueNavigationAction(ui);
        checkForDuplicates(routeTargetType, routeLayoutTypes);

        if (eventActionsSupported()) {
            BeforeLeaveEvent beforeNavigationDeactivating = new BeforeLeaveEvent(
                    event, routeTargetType, routeLayoutTypes);

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

            if (transitionOutcome == TransitionOutcome.FORWARDED) {
                return forward(event, beforeNavigationDeactivating);
            }

            if (transitionOutcome == TransitionOutcome.REROUTED) {
                return reroute(event, beforeNavigationDeactivating);
            }

            if (transitionOutcome == TransitionOutcome.POSTPONED) {
                ContinueNavigationAction currentAction = beforeNavigationDeactivating
                        .getContinueNavigationAction();
                currentAction.setReferences(this, event);
                storeContinueNavigationAction(ui, currentAction);

                return HttpServletResponse.SC_OK;
            }
        }

        final ArrayList<HasElement> chain;

        if (isPreserveOnRefreshTarget(routeTargetType, routeLayoutTypes)) {
            final Optional<ArrayList<HasElement>> maybeChain =
                    createOrRehandlePreserveOnRefreshComponent(event);
            if (!maybeChain.isPresent()) {
                return HttpServletResponse.SC_OK;
            } else {
                chain = maybeChain.get();
            }
        } else {
            // Non-preserving component, create fresh chain
            chain = createChain(event);

            // Has any preserved components already been created here? If so,
            // we don't want to navigate back to them ever so clear cache for
            // window.
            clearAllPreservedChains(ui);
        }

        final Component componentInstance = (Component) chain.get(0);

        BeforeEnterEvent beforeNavigationActivating = new BeforeEnterEvent(
                event, routeTargetType, routeLayoutTypes);

        LocationChangeEvent locationChangeEvent = new LocationChangeEvent(
                event.getSource(), event.getUI(), event.getTrigger(),
                event.getLocation(), chain);

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
                && TransitionOutcome.FORWARDED.equals(transitionOutcome)) {
            return forward(event, beforeNavigationActivating);
        }

        if (eventActionsSupported()
                && TransitionOutcome.REROUTED.equals(transitionOutcome)) {
            return reroute(event, beforeNavigationActivating);
        }

        ui.getInternals().showRouteTarget(event.getLocation(),
                navigationState.getResolvedPath(), componentInstance,
                routerLayouts);

        updatePageTitle(event, componentInstance);

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
     * @param router
     *            used router instance
     *
     * @return a list of parent {@link RouterLayout} types, not
     *         <code>null</code>
     */
    protected abstract List<Class<? extends RouterLayout>> getRouterLayoutTypes(
            Class<? extends Component> routeTargetType, Router router);

    /**
     * Checks whether this renderer should reroute or postpone navigation based
     * on results from event listeners. Furthermore, the before leave event is
     * not fired at all if event actions are not supported.
     *
     * @return <code>true</code> to support event actions; <code>false</code>
     *         otherwise.
     */
    protected abstract boolean eventActionsSupported();

    // The first element in the returned list is always a Component
    private ArrayList<HasElement> createChain(NavigationEvent event) {
        final Class<? extends Component> routeTargetType = navigationState
                .getNavigationTarget();
        final List<Class<? extends RouterLayout>> routeLayoutTypes =
                getRouterLayoutTypes(routeTargetType,
                        event.getUI().getRouter());

        final ArrayList<HasElement> chain = new ArrayList<>();
        chain.add(getRouteTarget(routeTargetType, event));
        for (Class<? extends RouterLayout> parentType : routeLayoutTypes) {
            chain.add(getRouteTarget(parentType, event));
        }
        return chain;
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

            validateBeforeEvent(beforeNavigation);
            if (beforeNavigation.hasForwardTarget()) {
                return TransitionOutcome.FORWARDED;
            }

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
            validateBeforeEvent(beforeNavigation);

            if (beforeNavigation.hasForwardTarget()) {
                return TransitionOutcome.FORWARDED;
            }

            if (beforeNavigation.hasRerouteTarget()) {
                return TransitionOutcome.REROUTED;
            }
        }
        return TransitionOutcome.FINISHED;
    }

    private int forward(NavigationEvent event, BeforeEvent beforeNavigation) {
        NavigationHandler handler = beforeNavigation.getForwardTarget();

        NavigationEvent newNavigationEvent = getNavigationEvent(event,
                beforeNavigation);
        newNavigationEvent.getUI().getPage().getHistory().replaceState(null,
                newNavigationEvent.getLocation());

        return handler.handle(newNavigationEvent);
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

        Class<? extends Component> targetType;
        if (beforeNavigation.hasForwardTarget()) {
            targetType = beforeNavigation.getForwardTargetType();
        } else {
            targetType = beforeNavigation.getRouteTargetType();
        }

        Location location = new Location(RouteConfiguration
                .forRegistry(event.getSource().getRegistry())
                .getUrlBase(targetType)
                .orElseThrow(() -> new IllegalStateException(String.format(
                        "The target component '%s' has no registered route",
                        targetType))));

        if (beforeNavigation.hasForwardTarget()) {
            List<String> segments = new ArrayList<>(location.getSegments());
            segments.addAll(beforeNavigation.getForwardTargetParameters());
            location = new Location(segments);
        }

        return new NavigationEvent(event.getSource(), location, event.getUI(),
                NavigationTrigger.PROGRAMMATIC);
    }

    /**
     * Creates a new instance of the target component for the route when the
     * target class is annotated with @PreserveOnRefresh. It checks if there
     * exists a cached component of the route location in the current window.
     * If retrieving the window name requires another round-trip, schedule it
     * and make a new call to the handle {@link #handle(NavigationEvent)} in
     * the callback. In this case, this method returns {@link Optional#empty()}.
     */
    private Optional<ArrayList<HasElement>> createOrRehandlePreserveOnRefreshComponent(
            NavigationEvent event) {
        final Location location = event.getLocation();
        final UI ui = event.getUI();
        final VaadinSession session = ui.getSession();

        final ArrayList<HasElement> chain;

        if (ui.getInternals().getExtendedClientDetails() == null) {
            if (hasPreservedChainOfLocation(session, location)) {
                // We may have a cached instance for this location, but we
                // need to retrieve the window name before we can determine
                // this, so execute a client-side request.
                ui.getPage().retrieveExtendedClientDetails(details ->
                        handle(event));
                return Optional.empty();
            } else {
                // We can immediately create the new component instance and
                // route to it. But we also want to retrieve the window
                // name in order to cache the component for later potential
                // refreshes.
                chain = createChain(event);
                ui.getPage().retrieveExtendedClientDetails(details -> {
                    final String windowName = ui.getInternals()
                            .getExtendedClientDetails().getWindowName();
                    setPreservedChain(session, windowName, location, chain);
                });
            }
        } else {
            final String windowName = ui.getInternals()
                    .getExtendedClientDetails().getWindowName();
            final Optional<ArrayList<HasElement>> maybePreserved =
                    getPreservedChain(session, windowName, event.getLocation());
            if (maybePreserved.isPresent()) {
                // Re-use preserved chain for this route
                chain = maybePreserved.get();
                final HasElement root = chain.get(chain.size()-1);
                final Component component = (Component) chain.get(0);
                final Optional<UI> maybePrevUI = component.getUI();

                // Remove the top-level component from the tree
                root.getElement().removeFromTree();

                // Transfer all remaining UI child elements (typically dialogs
                // and notifications) to the new UI
                maybePrevUI.ifPresent(prevUi ->
                        moveElementsToNewUI(prevUi, ui));
            } else {
                // Instantiate new chain for the route
                chain = createChain(event);
                setPreservedChain(session, windowName, location, chain);
            }
        }
        return Optional.of(chain);
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

    private static void validateBeforeEvent(BeforeEvent event) {
        if (event.hasForwardTarget() && event.hasRerouteTarget()) {
            throw new IllegalStateException(
                    "Error forward & reroute can not be set at the same time");
        }
    }

    private static void checkForDuplicates(
            Class<? extends Component> routeTargetType,
            Collection<Class<? extends RouterLayout>> routeLayoutTypes) {
        Set<Class<?>> duplicateCheck = new HashSet<>();
        duplicateCheck.add(routeTargetType);
        for (Class<?> parentType : routeLayoutTypes) {
            if (!duplicateCheck.add(parentType)) {
                throw new IllegalArgumentException(
                        parentType + " is used in multiple locations");
            }
        }
    }

    private static void updatePageTitle(NavigationEvent navigationEvent,
            Component routeTarget) {
        String title;

        if (routeTarget instanceof HasDynamicTitle) {
            title = ((HasDynamicTitle) routeTarget).getPageTitle();
        } else {
            title = lookForTitleInTarget(routeTarget).map(PageTitle::value)
                    .orElse("");
        }
        navigationEvent.getUI().getPage().setTitle(title);
    }

    private static Optional<PageTitle> lookForTitleInTarget(
            Component routeTarget) {
        return Optional.ofNullable(
                routeTarget.getClass().getAnnotation(PageTitle.class));
    }

    private static boolean isPreserveOnRefreshTarget(
            Class<? extends Component> routeTargetType,
            List<Class<? extends RouterLayout>> routeLayoutTypes) {
        return routeTargetType.isAnnotationPresent(PreserveOnRefresh.class)
                || routeLayoutTypes.stream().anyMatch(layoutType ->
                layoutType.isAnnotationPresent(PreserveOnRefresh.class)
        );
    }

    private void moveElementsToNewUI(UI prevUi, UI newUi) {
        final List<Element> uiChildren = prevUi.getElement()
                .getChildren()
                .collect(Collectors.toList());
        uiChildren.forEach(element -> {
            element.removeFromTree();
            newUi.getElement().appendChild(element);
        });
    }

    // maps window.name to (location, chain)
    private static class PreservedComponentCache extends
            HashMap<String, Pair<String, ArrayList<HasElement>>> {
    }

    static boolean hasPreservedChain(VaadinSession session) {
        final PreservedComponentCache cache =
                session.getAttribute(PreservedComponentCache.class);
        return cache!=null && !cache.isEmpty();
    }

    static boolean hasPreservedChainOfLocation(
            VaadinSession session, Location location) {
        final PreservedComponentCache cache =
                session.getAttribute(PreservedComponentCache.class);
        return cache != null &&
                cache.values().stream().anyMatch(entry ->
                        entry.getFirst().equals(location.getPath()));
    }

    static Optional<ArrayList<HasElement>> getPreservedChain(
            VaadinSession session, String windowName, Location location) {
        final PreservedComponentCache cache =
                session.getAttribute(PreservedComponentCache.class);
        if (cache!=null && cache.containsKey(windowName) &&
                cache.get(windowName).getFirst().equals(location.getPath())) {
            return Optional.of(cache.get(windowName).getSecond());
        } else {
            return Optional.empty();
        }
    }

    static void setPreservedChain(VaadinSession session, String windowName,
                                  Location location,
                                  ArrayList<HasElement> chain) {
        PreservedComponentCache cache =
                session.getAttribute(PreservedComponentCache.class);
        if (cache == null) {
            cache = new PreservedComponentCache();
        }
        cache.put(windowName, new Pair<>(location.getPath(), chain));
        session.setAttribute(PreservedComponentCache.class, cache);
    }


    private static void clearAllPreservedChains(UI ui) {
        final VaadinSession session = ui.getSession();
        // Note that this check is always false if @PreserveOnRefresh has not
        // been used at all, avoiding the round-trip overhead.
        if (hasPreservedChain(session)) {
            ui.getPage().retrieveExtendedClientDetails(details -> {
                final String windowName = ui.getInternals()
                        .getExtendedClientDetails().getWindowName();
                final PreservedComponentCache cache =
                        session.getAttribute(PreservedComponentCache.class);
                if (cache != null) {
                    cache.remove(windowName);
                }
            });
        }
    }
}
