/*
 * Copyright 2000-2025 Vaadin Ltd.
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
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import tools.jackson.databind.node.BaseJsonNode;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.ExtendedClientDetails;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.Pair;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.internal.menu.MenuRegistry;
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
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.LocationChangeEvent;
import com.vaadin.flow.router.NavigationEvent;
import com.vaadin.flow.router.NavigationHandler;
import com.vaadin.flow.router.NavigationState;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.internal.RouteTarget;
import com.vaadin.flow.router.internal.RouteUtil;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.menu.AvailableViewInfo;

/**
 * Base class for navigation handlers that target a navigation state.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public abstract class AbstractNavigationStateRenderer
        implements NavigationHandler {

    private final NavigationState navigationState;

    private List<Class<? extends RouterLayout>> routeLayoutTypes;

    private Postpone postponed = null;

    private LocationChangeEvent locationChangeEvent = null;

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
     * By default, always creates new instances.
     *
     * @param <T>
     *            the route target type
     * @param routeTargetType
     *            the class of the route target component
     * @param event
     *            the navigation event that uses the route target
     * @param lastElement
     *            {@code true} when this is the last element in the chain
     * @return an instance of the route target component
     */
    @SuppressWarnings("unchecked")
    // Non-private for testing purposes
    <T extends HasElement> T getRouteTarget(Class<T> routeTargetType,
            NavigationEvent event, boolean lastElement) {
        UI ui = event.getUI();
        Instantiator instantiator = Instantiator.get(ui);
        boolean forceInstantiation = lastElement ? event.isForceInstantiation()
                : (event.isForceInstantiation()
                        && event.isRecreateLayoutChain());
        Optional<HasElement> currentInstance = forceInstantiation
                ? Optional.empty()
                : ui.getInternals().getActiveRouterTargetsChain().stream()
                        .filter(component -> instantiator
                                .getApplicationClass(component)
                                .equals(routeTargetType))
                        .findAny();
        return (T) currentInstance.orElseGet(
                () -> instantiator.createRouteTarget(routeTargetType, event));
    }

    @Override
    public int handle(NavigationEvent event) {
        initializeNavigation(event);

        Optional<Integer> earlyReturn = handleEarlyNavigationChecks(event);
        if (earlyReturn.isPresent()) {
            return earlyReturn.get();
        }

        NavigationContext context = prepareNavigationContext(event);
        if (context.shouldReturnEarly()) {
            return HttpStatusCode.OK.getCode();
        }

        return executeNavigation(context);
    }

    /**
     * Initialize the navigation state and UI for the given event.
     */
    private void initializeNavigation(NavigationEvent event) {
        UI ui = event.getUI();
        ui.getInternals().setLocationForRefresh(event.getLocation());

        final RouteTarget routeTarget = navigationState.getRouteTarget();
        routeLayoutTypes = routeTarget != null
                ? getTargetParentLayouts(routeTarget,
                        event.getSource().getRegistry(),
                        event.getLocation().getPath())
                : getRouterLayoutTypes(navigationState.getNavigationTarget(),
                        ui.getInternals().getRouter());

        assert navigationState.getNavigationTarget() != null;
        assert routeLayoutTypes != null;

        clearContinueNavigationAction(ui);
        checkForDuplicates(navigationState.getNavigationTarget(), routeLayoutTypes);
    }

    /**
     * Handle early navigation checks that may return immediately.
     */
    private Optional<Integer> handleEarlyNavigationChecks(NavigationEvent event) {
        Optional<Integer> result = handleBeforeLeaveEvents(event,
                navigationState.getNavigationTarget(), navigationState.getRouteParameters());
        if (result.isPresent()) {
            return result;
        }

        String route = getFormattedRoute(event);
        if (isClientHandled(route)) {
            return Optional.of(HttpStatusCode.OK.getCode());
        }

        return Optional.empty();
    }

    /**
     * Prepare the navigation context including chain population.
     */
    private NavigationContext prepareNavigationContext(NavigationEvent event) {
        final ArrayList<HasElement> chain = new ArrayList<>();
        final boolean preserveOnRefreshTarget = isPreserveOnRefreshTarget(
                navigationState.getNavigationTarget(), routeLayoutTypes);

        boolean shouldReturnEarly = populateChain(chain, preserveOnRefreshTarget, event);

        // Set navigationTrigger to RELOAD if this is a refresh of a preserve view.
        NavigationEvent finalEvent = adjustEventForPreservedView(event, preserveOnRefreshTarget, chain);

        return new NavigationContext(finalEvent, chain, preserveOnRefreshTarget, shouldReturnEarly);
    }

    /**
     * Adjust the navigation event for preserved views if needed.
     */
    private NavigationEvent adjustEventForPreservedView(NavigationEvent event, 
            boolean preserveOnRefreshTarget, ArrayList<HasElement> chain) {
        if (preserveOnRefreshTarget && !chain.isEmpty()) {
            return new NavigationEvent(event.getSource(), event.getLocation(),
                    event.getUI(), NavigationTrigger.REFRESH);
        }
        return event;
    }

    /**
     * Execute the main navigation logic.
     */
    private int executeNavigation(NavigationContext context) {
        NavigationEvent event = context.getEvent();
        ArrayList<HasElement> chain = context.getChain();
        boolean preserveOnRefreshTarget = context.isPreserveOnRefreshTarget();

        pushHistoryStateIfNeeded(event, event.getUI());

        Optional<Integer> result = handleBeforeNavigationEvents(event, 
                navigationState.getNavigationTarget(),
                navigationState.getRouteParameters(), chain);
        if (result.isPresent()) {
            return result.get();
        }

        return finalizeNavigation(event, chain, preserveOnRefreshTarget);
    }

    /**
     * Finalize the navigation by setting up components and handling post-navigation tasks.
     */
    private int finalizeNavigation(NavigationEvent event, ArrayList<HasElement> chain, 
            boolean preserveOnRefreshTarget) {
        if (chain.isEmpty()) {
            throw new IllegalStateException("Navigation chain cannot be empty");
        }
        
        HasElement firstElement = chain.get(0);
        if (!(firstElement instanceof Component)) {
            throw new IllegalStateException("First element in navigation chain must be a Component, but was: " + firstElement.getClass());
        }
        final Component componentInstance = (Component) firstElement;

        if (preserveOnRefreshTarget) {
            setPreservedChain(chain, event);
        }

        @SuppressWarnings("unchecked")
        List<RouterLayout> routerLayouts = (List<RouterLayout>) (List<?>) chain
                .subList(1, chain.size());

        cleanModalComponents(event);

        event.getUI().getInternals().showRouteTarget(event.getLocation(),
                componentInstance, routerLayouts);

        int statusCode = locationChangeEvent.getStatusCode();
        validateStatusCode(statusCode, navigationState.getNavigationTarget());

        handleAfterNavigationEvents(event.getUI(), navigationState.getRouteParameters());
        updatePageTitle(event, componentInstance, getFormattedRoute(event));

        return statusCode;
    }

    /**
     * Helper class to hold navigation context during processing.
     */
    private static class NavigationContext {
        private final NavigationEvent event;
        private final ArrayList<HasElement> chain;
        private final boolean preserveOnRefreshTarget;
        private final boolean shouldReturnEarly;

        public NavigationContext(NavigationEvent event, ArrayList<HasElement> chain,
                boolean preserveOnRefreshTarget, boolean shouldReturnEarly) {
            this.event = event;
            this.chain = chain;
            this.preserveOnRefreshTarget = preserveOnRefreshTarget;
            this.shouldReturnEarly = shouldReturnEarly;
        }

        public NavigationEvent getEvent() { return event; }
        public ArrayList<HasElement> getChain() { return chain; }
        public boolean isPreserveOnRefreshTarget() { return preserveOnRefreshTarget; }
        public boolean shouldReturnEarly() { return shouldReturnEarly; }
    }

    /**
     * Populate element chain from a preserved chain or give clean chain to be
     * populated.
     *
     * @param chain
     *            chain to populate
     * @param preserveOnRefreshTarget
     *            preserve on refresh boolean
     * @param event
     *            current navigation event
     * @return {@code true} if additional client data requested, else
     *         {@code false}
     */
    private boolean populateChain(ArrayList<HasElement> chain,
            boolean preserveOnRefreshTarget, NavigationEvent event) {
        if (preserveOnRefreshTarget && !event.isForceInstantiation()) {
            return handlePreserveOnRefresh(chain, event);
        } else {
            handleNormalChainCreation(chain, event);
            return false;
        }
    }

    /**
     * Handle chain population for preserve-on-refresh targets.
     */
    private boolean handlePreserveOnRefresh(ArrayList<HasElement> chain, NavigationEvent event) {
        final Optional<ArrayList<HasElement>> maybeChain = getPreservedChain(event);
        if (maybeChain.isEmpty()) {
            // We're returning because the preserved chain is not ready to
            // be used as is, and requires client data requested within
            // `getPreservedChain`. Once the data is retrieved from the
            // client, `handle` method will be invoked with the same
            // `NavigationEvent` argument.
            return true;
        }
        
        chain.addAll(maybeChain.get());

        // Handle partial match if the main chain is still empty
        if (chain.isEmpty() && isPreservePartialTarget(
                navigationState.getNavigationTarget(), routeLayoutTypes)) {
            return handlePartialMatch(event);
        }

        return false;
    }

    /**
     * Handle partial match for preserve-on-refresh targets.
     */
    private boolean handlePartialMatch(NavigationEvent event) {
        UI ui = event.getUI();
        
        if (needsClientDetailsForPartialMatch(ui)) {
            return requestClientDetailsForPartialMatch(ui, event);
        } else {
            handleExistingPartialMatch(ui);
            return false;
        }
    }

    /**
     * Check if client details are needed for partial match.
     */
    private boolean needsClientDetailsForPartialMatch(UI ui) {
        if (ui.getInternals().getExtendedClientDetails() != null) {
            return false;
        }

        PreservedComponentCache cache = ui.getSession()
                .getAttribute(PreservedComponentCache.class);
        return cache != null && !cache.isEmpty();
    }

    /**
     * Request client details for partial match processing.
     */
    private boolean requestClientDetailsForPartialMatch(UI ui, NavigationEvent event) {
        // As there is a cached chain we get the client details
        // to get the window name so we can determine if the
        // cache contains a chain for us to use.
        ui.getPage().retrieveExtendedClientDetails(details -> handle(event));
        return true;
    }

    /**
     * Handle partial match when client details are already available.
     */
    private void handleExistingPartialMatch(UI ui) {
        Optional<List<HasElement>> partialChain = getWindowPreservedChain(
                ui.getSession(),
                ui.getInternals().getExtendedClientDetails().getWindowName());
        
        if (partialChain.isPresent()) {
            processPartialChain(partialChain.get(), ui);
        }
    }

    /**
     * Process the partial chain by disconnecting elements and setting up router layouts.
     */
    private void processPartialChain(List<HasElement> oldChain, UI ui) {
        disconnectElements(oldChain, ui);

        List<RouterLayout> routerLayouts = extractRouterLayouts(oldChain);
        ui.getInternals().setRouterTargetChain(routerLayouts);
    }

    /**
     * Extract router layouts from the old chain.
     */
    private List<RouterLayout> extractRouterLayouts(List<HasElement> oldChain) {
        List<RouterLayout> routerLayouts = new ArrayList<>();

        for (HasElement hasElement : oldChain) {
            if (hasElement instanceof RouterLayout) {
                routerLayouts.add((RouterLayout) hasElement);
            } else {
                // Remove any non element from their parent to
                // not get old or duplicate route content
                hasElement.getElement().removeFromParent();
            }
        }

        return routerLayouts;
    }

    /**
     * Handle normal chain creation (non-preserve-on-refresh).
     */
    private void handleNormalChainCreation(ArrayList<HasElement> chain, NavigationEvent event) {
        // Create an empty chain which gets populated later in
        // `createChainIfEmptyAndExecuteBeforeEnterNavigation`.
        chain.clear();

        // Has any preserved components already been created here? If so,
        // we don't want to navigate back to them ever so clear cache for
        // window.
        clearAllPreservedChains(event.getUI());
    }

    /**
     * Send before leave event to all listeners.
     *
     * @return optional return http status code
     */
    private Optional<Integer> handleBeforeLeaveEvents(NavigationEvent event,
            Class<? extends Component> routeTargetType,
            RouteParameters parameters) {
        BeforeLeaveEvent beforeNavigationDeactivating = new BeforeLeaveEvent(
                event, routeTargetType, parameters, routeLayoutTypes);

        return executeBeforeLeaveNavigation(event,
                beforeNavigationDeactivating);
    }

    /**
     * If a route refresh has been requested, remove all modal components. This
     * is necessary because maintaining the correct modality cardinality and
     * order is not feasible without knowing who opened them and when.
     *
     * @param event
     *            navigation event
     */
    private static void cleanModalComponents(NavigationEvent event) {
        if (event.getUI().hasModalComponent()
                && event.getTrigger() == NavigationTrigger.REFRESH_ROUTE) {
            Component modalComponent;
            while ((modalComponent = event.getUI().getInternals()
                    .getActiveModalComponent()) != null) {
                modalComponent.removeFromParent();
            }
        }
    }

    /**
     * Send before navigation event to all listeners.
     *
     * @return optional return http status code
     */
    private Optional<Integer> handleBeforeNavigationEvents(
            NavigationEvent event, Class<? extends Component> routeTargetType,
            RouteParameters parameters, ArrayList<HasElement> chain) {
        BeforeEnterEvent beforeNavigationActivating = new BeforeEnterEvent(
                event, routeTargetType, parameters, routeLayoutTypes);

        return createChainIfEmptyAndExecuteBeforeEnterNavigation(
                beforeNavigationActivating, event, chain);
    }

    /**
     * Send after navigation event to all listeners.
     *
     * @return optional return http status code
     */
    private void handleAfterNavigationEvents(UI ui,
            RouteParameters parameters) {
        List<AfterNavigationHandler> afterNavigationHandlers = new ArrayList<>(
                ui.getNavigationListeners(AfterNavigationHandler.class));
        afterNavigationHandlers
                .addAll(EventUtil.collectAfterNavigationObservers(ui));

        fireAfterNavigationListeners(
                new AfterNavigationEvent(locationChangeEvent, parameters),
                afterNavigationHandlers);
    }

    /**
     * Check if target route is client handled and Flow should not handle
     * rendering.
     *
     * @param route
     *            formatted route target string
     * @return {@code true} if client handled render for route
     */
    private boolean isClientHandled(String route) {
        // If navigation target is Hilla route, terminate Flow navigation logic
        // here.
        return MenuRegistry.hasClientRoute(route, true)
                && !MenuRegistry.getClientRoutes(true).get(route).flowLayout();
    }

    /**
     * Get the target location as a standardized route string.
     *
     * @param event
     *            navigation event
     * @return route string
     */
    private static String getFormattedRoute(NavigationEvent event) {
        return event.getLocation().getPath().isEmpty()
                ? event.getLocation().getPath()
                : event.getLocation().getPath().startsWith("/")
                        ? event.getLocation().getPath()
                        : "/" + event.getLocation().getPath();
    }

    /**
     * Get the parentLayouts for given routeTarget or use an applicable
     * {@code @Layout} when no parentLayouts defined and target is a Route
     * annotated target with autoLayout enabled and no layout set.
     *
     * @param routeTarget
     *            RouteTarget to get parents for
     * @param registry
     *            Registry in use
     * @param path
     *            request path
     * @return List of parent layouts
     */
    protected List<Class<? extends RouterLayout>> getTargetParentLayouts(
            RouteTarget routeTarget, RouteRegistry registry, String path) {
        if (routeTarget.getParentLayouts().isEmpty()
                && RouteUtil.isAutolayoutEnabled(routeTarget.getTarget(), path)
                && registry.hasLayout(path)) {
            return RouteUtil
                    .collectRouteParentLayouts(registry.getLayout(path));
        }
        return routeTarget.getParentLayouts();
    }

    private void pushHistoryStateIfNeeded(NavigationEvent event, UI ui) {
        if (handleErrorNavigationEvent(event, ui)) {
            return;
        }

        if (shouldProcessNormalNavigation(event, ui) || isReactEnabledNavigation(event, ui)) {
            if (shouldPushHistoryState(event)) {
                pushHistoryState(event);
            }
        }
    }

    /**
     * Handle error navigation events, returning true if processing should stop.
     */
    private boolean handleErrorNavigationEvent(NavigationEvent event, UI ui) {
        if (event instanceof ErrorNavigationEvent errorEvent) {
            if (isRouterLinkNotFoundNavigationError(errorEvent)) {
                // #8544
                event.getState().ifPresent(s -> ui.getPage().executeJs(
                        "this.scrollPositionHandlerAfterServerNavigation($0);",
                        s));
            }
            return true;
        }
        return false;
    }

    /**
     * Check if this is a normal navigation that should be processed.
     */
    private boolean shouldProcessNormalNavigation(NavigationEvent event, UI ui) {
        NavigationTrigger eventTrigger = event.getTrigger();
        Location currentLocation = ui.getInternals().getActiveViewLocation();
        
        return NavigationTrigger.REFRESH != eventTrigger
                && !event.isForwardTo()
                && hasLocationChanged(event.getLocation(), currentLocation);
    }

    /**
     * Check if the location has actually changed.
     */
    private boolean hasLocationChanged(Location newLocation, Location currentLocation) {
        return currentLocation == null 
                || !newLocation.getPathWithQueryParameters()
                        .equals(currentLocation.getPathWithQueryParameters());
    }

    /**
     * Check if this is a React-enabled navigation that should be processed.
     */
    private boolean isReactEnabledNavigation(NavigationEvent event, UI ui) {
        boolean reactEnabled = ui.getInternals().getSession().getService()
                .getDeploymentConfiguration().isReactEnabled();
        return reactEnabled;
    }

    protected void pushHistoryState(NavigationEvent event) {
        // Enable navigating back
        event.getUI().getPage().getHistory().pushState(null,
                event.getLocation());
    }

    protected boolean shouldPushHistoryState(NavigationEvent event) {
        return NavigationTrigger.UI_NAVIGATE.equals(event.getTrigger())
                || NavigationTrigger.REFRESH.equals(event.getTrigger());
    }

    private boolean isRouterLinkNotFoundNavigationError(
            ErrorNavigationEvent event) {
        return NavigationTrigger.ROUTER_LINK.equals(event.getTrigger())
                && event.getErrorParameter() != null
                && event.getErrorParameter()
                        .getCaughtException() instanceof NotFoundException;
    }

    /**
     * Notify the navigation target about the status of the navigation.
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
     * Collect the element types chain for the current navigation state.
     *
     * @return types chain for navigation target
     */
    private List<Class<? extends HasElement>> getTypesChain() {
        final Class<? extends Component> routeTargetType = navigationState
                .getNavigationTarget();

        List<Class<? extends RouterLayout>> layoutTypes = new ArrayList<>(
                this.routeLayoutTypes);
        Collections.reverse(layoutTypes);

        final ArrayList<Class<? extends HasElement>> chain = new ArrayList<>(
                layoutTypes);

        // The last element in the returned list is always a Component class
        chain.add(routeTargetType);
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
     * @return result of observer events
     */
    private Optional<Integer> executeBeforeLeaveNavigation(
            NavigationEvent event, BeforeLeaveEvent beforeNavigation) {

        Deque<BeforeLeaveHandler> leaveHandlers = getBeforeLeaveHandlers(
                beforeNavigation.getUI());

        while (!leaveHandlers.isEmpty()) {
            BeforeLeaveHandler listener = leaveHandlers.remove();
            listener.beforeLeave(beforeNavigation);

            validateBeforeEvent(beforeNavigation);

            Optional<Integer> result = handleTriggeredBeforeEvent(event,
                    beforeNavigation);
            if (result.isPresent()) {
                return result;
            }

            if (beforeNavigation.isPostponed()) {
                postponed = Postpone.withLeaveObservers(leaveHandlers);

                ContinueNavigationAction currentAction = beforeNavigation
                        .getContinueNavigationAction();
                currentAction.setReferences(this, event);
                storeContinueNavigationAction(event.getUI(), currentAction);

                return Optional.of(HttpStatusCode.OK.getCode());
            }
        }

        return Optional.empty();
    }

    private Deque<BeforeLeaveHandler> getBeforeLeaveHandlers(UI ui) {
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
        return leaveHandlers;
    }

    /**
     * Inform any {@link BeforeEnterObserver}s in attaching element chain. The
     * event is sent first to the {@link BeforeEnterHandler}s registered within
     * the {@link UI}, then to any element in the chain and to any of its child
     * components in the hierarchy which implements {@link BeforeEnterHandler}
     *
     * If the <code>chain</code> argument is empty <code>chainClasses</code> is
     * going to be used and populate <code>chain</code> with new created
     * instance.
     *
     * @param beforeNavigation
     *            before enter navigation event sent to observers
     * @param event
     *            original navigation event
     * @param chain
     *            the chain of {@link HasElement} instances which will be
     *            rendered. In case this is empty it'll be populated with
     *            instances according to the navigation event's location.
     * @return result of observer events
     */
    private Optional<Integer> createChainIfEmptyAndExecuteBeforeEnterNavigation(
            BeforeEnterEvent beforeNavigation, NavigationEvent event,
            ArrayList<HasElement> chain) {

        // Always send the beforeNavigation event first to the registered
        // listeners
        List<BeforeEnterHandler> registeredEnterHandlers = new ArrayList<>(
                beforeNavigation.getUI()
                        .getNavigationListeners(BeforeEnterHandler.class));

        Optional<Integer> result = sendBeforeEnterEvent(registeredEnterHandlers,
                event, beforeNavigation, null);
        if (result.isPresent()) {
            return result;
        }

        if (chain.isEmpty()) {
            return sendBeforeEnterEventAndPopulateChain(beforeNavigation, event,
                    chain);
        } else {
            return sendBeforeEnterEventToExistingChain(beforeNavigation, event,
                    chain);
        }
    }

    private Optional<Integer> sendBeforeEnterEventAndPopulateChain(
            BeforeEnterEvent beforeNavigation, NavigationEvent event,
            ArrayList<HasElement> chain) {
        List<HasElement> oldChain = event.getUI().getInternals()
                .getActiveRouterTargetsChain();

        // Create the chain components if missing.
        List<Class<? extends HasElement>> typesChain = getTypesChain();

        try {
            for (int i = 0; i < typesChain.size(); i++) {
                HasElement element = getRouteTarget(typesChain.get(i), event,
                        i == typesChain.size() - 1);

                if (!beforeNavigation.isErrorEvent()) {
                    UsageStatistics.markAsUsed(Constants.STATISTICS_FLOW_ROUTER,
                            null);
                }

                chain.add(element);

                List<BeforeEnterHandler> chainEnterHandlers = new ArrayList<>(
                        EventUtil.collectBeforeEnterObserversFromChainElement(
                                element, oldChain));

                final boolean lastElement = chain.size() == typesChain.size();
                Optional<Integer> result = sendBeforeEnterEvent(
                        chainEnterHandlers, event, beforeNavigation,
                        lastElement ? chain : null);
                if (result.isPresent()) {
                    return result;
                }
            }

            return Optional.empty();

        } finally {

            // Reverse the chain to preserve backwards compatibility.
            // The events were sent starting from parent layout and ending with
            // the navigation target component.
            // The chain ought to be output starting with the navigation target
            // component as the first element.
            Collections.reverse(chain);
        }
    }

    private Optional<Integer> sendBeforeEnterEventToExistingChain(
            BeforeEnterEvent beforeNavigation, NavigationEvent event,
            ArrayList<HasElement> chain) {
        // Reverse the chain so that the target is last.
        List<HasElement> reversedChain = new ArrayList<>(chain);
        Collections.reverse(reversedChain);

        // Used when the chain already exists by being preserved on refresh.
        // See `isPreserveOnRefreshTarget` method implementation and usage.
        List<BeforeEnterHandler> chainEnterHandlers = new ArrayList<>(
                EventUtil.collectBeforeEnterObserversFromChain(reversedChain, event
                        .getUI().getInternals().getActiveRouterTargetsChain()));

        return sendBeforeEnterEvent(chainEnterHandlers, event, beforeNavigation,
                chain);
    }

    /*
     * Target component is expected to be the last in the chain.
     */
    private Optional<Integer> sendBeforeEnterEvent(
            List<BeforeEnterHandler> eventHandlers, NavigationEvent event,
            BeforeEnterEvent beforeNavigation, ArrayList<HasElement> chain) {

        ComponentContext componentContext = prepareComponentContext(event, chain);
        
        return processEventHandlers(eventHandlers, event, beforeNavigation, componentContext);
    }

    /**
     * Prepare the component context for event handling.
     */
    private ComponentContext prepareComponentContext(NavigationEvent event, ArrayList<HasElement> chain) {
        if (chain == null || chain.isEmpty()) {
            return new ComponentContext(null, false);
        }

        // Reverse the chain to the stored ordered, since that is different
        // from the notification order, and also to keep
        // LocationChangeEvent.getRouteTargetChain backward compatible.
        List<HasElement> reversedChain = new ArrayList<>(chain);
        Collections.reverse(reversedChain);

        HasElement firstElement = reversedChain.get(0);
        Component componentInstance = null;
        if (firstElement instanceof Component) {
            componentInstance = (Component) firstElement;
        }

        locationChangeEvent = new LocationChangeEvent(event.getSource(),
                event.getUI(), event.getTrigger(), event.getLocation(),
                reversedChain);

        return new ComponentContext(componentInstance, componentInstance != null);
    }

    /**
     * Process all event handlers and handle navigation target notification.
     */
    private Optional<Integer> processEventHandlers(List<BeforeEnterHandler> eventHandlers,
            NavigationEvent event, BeforeEnterEvent beforeNavigation, ComponentContext componentContext) {
        
        boolean notifyNavigationTarget = componentContext.shouldNotifyTarget();

        for (BeforeEnterHandler eventHandler : eventHandlers) {
            Optional<Integer> result = handleEventHandler(eventHandler, event, beforeNavigation, 
                    componentContext, notifyNavigationTarget);
            if (result.isPresent()) {
                return result;
            }

            // Clear the flag after first matching handler
            if (notifyNavigationTarget && componentContext.componentInstance != null
                    && isComponentElementEqualsOrChild(eventHandler, componentContext.componentInstance)) {
                notifyNavigationTarget = false;
            }
        }

        // Make sure notifyNavigationTarget is executed if it wasn't already
        if (notifyNavigationTarget && componentContext.componentInstance != null) {
            return notifyNavigationTarget(event, beforeNavigation, locationChangeEvent, 
                    componentContext.componentInstance);
        }

        return Optional.empty();
    }

    /**
     * Handle a single event handler.
     */
    private Optional<Integer> handleEventHandler(BeforeEnterHandler eventHandler, NavigationEvent event,
            BeforeEnterEvent beforeNavigation, ComponentContext componentContext, boolean notifyNavigationTarget) {
        
        // Notify the target itself, i.e. with the url parameter, before
        // sending the event to the navigation target or any of its children.
        if (notifyNavigationTarget && componentContext.componentInstance != null
                && isComponentElementEqualsOrChild(eventHandler, componentContext.componentInstance)) {

            Optional<Integer> result = notifyNavigationTarget(event,
                    beforeNavigation, locationChangeEvent, componentContext.componentInstance);
            if (result.isPresent()) {
                return result;
            }
        }

        return sendBeforeEnterEvent(event, beforeNavigation, eventHandler);
    }

    /**
     * Helper class to hold component context information.
     */
    private static class ComponentContext {
        final Component componentInstance;
        final boolean shouldNotifyTarget;

        ComponentContext(Component componentInstance, boolean shouldNotifyTarget) {
            this.componentInstance = componentInstance;
            this.shouldNotifyTarget = shouldNotifyTarget;
        }

        boolean shouldNotifyTarget() { return shouldNotifyTarget; }
    }

    private Optional<Integer> sendBeforeEnterEvent(NavigationEvent event,
            BeforeEnterEvent beforeNavigation,
            BeforeEnterHandler eventHandler) {
        eventHandler.beforeEnter(beforeNavigation);
        validateBeforeEvent(beforeNavigation);
        return handleTriggeredBeforeEvent(event, beforeNavigation);
    }

    private Optional<Integer> notifyNavigationTarget(NavigationEvent event,
            BeforeEnterEvent beforeNavigation,
            LocationChangeEvent locationChangeEvent,
            Component componentInstance) {

        notifyNavigationTarget(componentInstance, event, beforeNavigation,
                locationChangeEvent);

        return handleTriggeredBeforeEvent(event, beforeNavigation);
    }

    /*
     * Check whether the eventHandler is a HasElement and its Element is equals
     * with the component's Element or a child of it.
     */
    private static boolean isComponentElementEqualsOrChild(
            BeforeEnterHandler eventHandler, Component component) {
        if (eventHandler instanceof HasElement hasElement) {

            final Element componentElement = component.getElement();

            Element element = hasElement.getElement();
            while (element != null) {
                if (element.equals(componentElement)) {
                    return true;
                }

                element = element.getParent();
            }
        }

        return false;
    }

    /**
     * Handle a {@link BeforeEvent} after if has been triggered to an observer.
     *
     * @param event
     *            the navigation event being handled.
     * @param beforeEvent
     *            the {@link BeforeLeaveEvent} or {@link BeforeEnterEvent} being
     *            triggered to an observer.
     * @return an HTTP status code wrapped as an {@link Optional}. If the
     *         {@link Optional} is empty, the process will proceed with next
     *         observer or just move forward, otherwise the process will return
     *         immediately with the provided http code.
     * @see HttpStatusCode
     */
    protected Optional<Integer> handleTriggeredBeforeEvent(
            NavigationEvent event, BeforeEvent beforeEvent) {

        if (beforeEvent.hasExternalForwardUrl()) {
            return Optional.of(forwardToExternalUrl(event, beforeEvent));
        }

        boolean queryParameterChanged = hasQueryParameterChanged(beforeEvent, event);

        Optional<Integer> forwardResult = handleForwardIfNeeded(event, beforeEvent, queryParameterChanged);
        if (forwardResult.isPresent()) {
            return forwardResult;
        }

        Optional<Integer> rerouteResult = handleRerouteIfNeeded(event, beforeEvent, queryParameterChanged);
        if (rerouteResult.isPresent()) {
            return rerouteResult;
        }

        return Optional.empty();
    }

    /**
     * Check if query parameters have changed during the navigation.
     */
    private boolean hasQueryParameterChanged(BeforeEvent beforeEvent, NavigationEvent event) {
        return beforeEvent.hasRedirectQueryParameters()
                && !beforeEvent.getRedirectQueryParameters()
                        .equals(event.getLocation().getQueryParameters());
    }

    /**
     * Handle forward target if needed.
     */
    private Optional<Integer> handleForwardIfNeeded(NavigationEvent event, 
            BeforeEvent beforeEvent, boolean queryParameterChanged) {
        if (!beforeEvent.hasForwardTarget()) {
            return Optional.empty();
        }

        boolean shouldForward = shouldExecuteForward(beforeEvent, queryParameterChanged);
        if (shouldForward) {
            return Optional.of(forward(event, beforeEvent));
        }

        return Optional.empty();
    }

    /**
     * Determine if forward should be executed based on navigation state and parameters.
     */
    private boolean shouldExecuteForward(BeforeEvent beforeEvent, boolean queryParameterChanged) {
        boolean differentNavigationState = !isSameNavigationState(
                beforeEvent.getForwardTargetType(),
                beforeEvent.getForwardTargetRouteParameters());
        
        boolean differentResolvedPath = !isMatchingResolvedPath(beforeEvent);
        
        return differentNavigationState || queryParameterChanged || differentResolvedPath;
    }

    /**
     * Check if the resolved path matches the forward URL.
     */
    private boolean isMatchingResolvedPath(BeforeEvent beforeEvent) {
        return navigationState.getResolvedPath() != null
                && navigationState.getResolvedPath().equals(beforeEvent.getForwardUrl());
    }

    /**
     * Handle reroute target if needed.
     */
    private Optional<Integer> handleRerouteIfNeeded(NavigationEvent event, 
            BeforeEvent beforeEvent, boolean queryParameterChanged) {
        if (!beforeEvent.hasRerouteTarget()) {
            return Optional.empty();
        }

        boolean shouldReroute = !isSameNavigationState(beforeEvent.getRerouteTargetType(),
                beforeEvent.getRerouteTargetRouteParameters()) || queryParameterChanged;
        
        if (shouldReroute) {
            return Optional.of(reroute(event, beforeEvent));
        }

        return Optional.empty();
    }

    private boolean isSameNavigationState(Class<? extends Component> targetType,
            RouteParameters targetParameters) {
        final boolean sameTarget = navigationState.getNavigationTarget()
                .equals(targetType);

        final boolean sameParameters = targetParameters
                .equals(navigationState.getRouteParameters());

        return sameTarget && sameParameters;
    }

    private int forwardToExternalUrl(NavigationEvent event,
            BeforeEvent beforeNavigation) {
        event.getUI().getPage()
                .setLocation(beforeNavigation.getExternalForwardUrl());

        return HttpStatusCode.OK.getCode();
    }

    private int forward(NavigationEvent event, BeforeEvent beforeNavigation) {
        NavigationHandler handler = beforeNavigation.getForwardTarget();

        NavigationEvent newNavigationEvent = getNavigationEvent(event,
                beforeNavigation);
        newNavigationEvent.getUI().getPage().getHistory().replaceState(null,
                newNavigationEvent.getLocation(),
                beforeNavigation.isUseForwardCallback());

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
            return createErrorNavigationEvent(event, beforeNavigation);
        }

        String url = getRedirectUrl(beforeNavigation);
        validateRedirectUrl(url, beforeNavigation);

        QueryParameters queryParameters = getQueryParameters(beforeNavigation, event);
        Location location = new Location(url, queryParameters);

        return new NavigationEvent(event.getSource(), location, event.getUI(),
                NavigationTrigger.PROGRAMMATIC, (BaseJsonNode) null, true);
    }

    /**
     * Create an error navigation event.
     */
    private NavigationEvent createErrorNavigationEvent(NavigationEvent event, BeforeEvent beforeNavigation) {
        ErrorParameter<?> errorParameter = beforeNavigation.getErrorParameter();
        return new ErrorNavigationEvent(event.getSource(),
                event.getLocation(), event.getUI(),
                NavigationTrigger.PROGRAMMATIC, errorParameter);
    }

    /**
     * Get the redirect URL from the before navigation event.
     */
    private String getRedirectUrl(BeforeEvent beforeNavigation) {
        boolean isForward = beforeNavigation.hasForwardTarget();
        return isForward ? beforeNavigation.getForwardUrl() : beforeNavigation.getRerouteUrl();
    }

    /**
     * Validate the redirect URL and throw exception if invalid.
     */
    private void validateRedirectUrl(String url, BeforeEvent beforeNavigation) {
        if (url != null) {
            return;
        }

        RedirectInfo redirectInfo = getRedirectInfo(beforeNavigation);
        throw new IllegalStateException(String.format(
                "Attempting to %s to unresolved location target %s with route parameters %s",
                redirectInfo.type, redirectInfo.target, redirectInfo.parameters));
    }

    /**
     * Get redirect information for error reporting.
     */
    private RedirectInfo getRedirectInfo(BeforeEvent beforeNavigation) {
        boolean isForward = beforeNavigation.hasForwardTarget();
        if (isForward) {
            return new RedirectInfo("forward", 
                    beforeNavigation.getForwardTargetType(),
                    beforeNavigation.getForwardTargetRouteParameters());
        } else {
            return new RedirectInfo("reroute",
                    beforeNavigation.getRerouteTargetType(),
                    beforeNavigation.getRerouteTargetRouteParameters());
        }
    }

    /**
     * Get query parameters for the navigation event.
     */
    private QueryParameters getQueryParameters(BeforeEvent beforeNavigation, NavigationEvent event) {
        return beforeNavigation.hasRedirectQueryParameters()
                ? beforeNavigation.getRedirectQueryParameters()
                : event.getLocation().getQueryParameters();
    }

    /**
     * Helper class to hold redirect information.
     */
    private static class RedirectInfo {
        final String type;
        final Class<? extends Component> target;
        final RouteParameters parameters;

        RedirectInfo(String type, Class<? extends Component> target, RouteParameters parameters) {
            this.type = type;
            this.target = target;
            this.parameters = parameters;
        }
    }

    /**
     * Checks if there exists a cached component chain of the route location in
     * the current window.
     * <p>
     * If retrieving the window name requires another round-trip, schedule it
     * and make a new call to the handle {@link #handle(NavigationEvent)} in the
     * callback. In this case, this method returns {@link Optional#empty()}.
     * <p>
     * If the chain is missing and needs to be created this method returns an
     * {@link Optional} wrapping an empty {@link ArrayList}.
     */
    private Optional<ArrayList<HasElement>> getPreservedChain(
            NavigationEvent event) {
        final Location location = event.getLocation();
        final UI ui = event.getUI();
        final VaadinSession session = ui.getSession();

        if (needsClientDetails(ui)) {
            return handleMissingClientDetails(session, location, ui, event);
        }

        return handleExistingClientDetails(ui, session, location);
    }

    /**
     * Check if client details are needed for preserved chain retrieval.
     */
    private boolean needsClientDetails(UI ui) {
        return ui.getInternals().getExtendedClientDetails() == null;
    }

    /**
     * Handle the case where client details are missing.
     */
    private Optional<ArrayList<HasElement>> handleMissingClientDetails(
            VaadinSession session, Location location, UI ui, NavigationEvent event) {
        
        if (hasPreservedChainOfLocation(session, location)) {
            // We may have a cached instance for this location, but we
            // need to retrieve the window name before we can determine
            // this, so execute a client-side request.
            ui.getPage().retrieveExtendedClientDetails(details -> handle(event));
            return Optional.empty();
        }

        return Optional.of(new ArrayList<>(0));
    }

    /**
     * Handle the case where client details are available.
     */
    private Optional<ArrayList<HasElement>> handleExistingClientDetails(
            UI ui, VaadinSession session, Location location) {
        
        final String windowName = ui.getInternals()
                .getExtendedClientDetails().getWindowName();
        final Optional<ArrayList<HasElement>> maybePreserved = getPreservedChain(
                session, windowName, location);
        
        if (maybePreserved.isPresent()) {
            return handleFoundPreservedChain(maybePreserved.get(), ui);
        }

        return Optional.of(new ArrayList<>(0));
    }

    /**
     * Handle a found preserved chain by disconnecting elements and returning it.
     */
    private Optional<ArrayList<HasElement>> handleFoundPreservedChain(
            ArrayList<HasElement> chain, UI ui) {
        // Re-use preserved chain for this route
        disconnectElements(chain, ui);
        return Optional.of(chain);
    }

    private static void disconnectElements(List<HasElement> chain, UI ui) {
        final HasElement root = chain.get(chain.size() - 1);
        final Component component = (Component) chain.get(0);
        final Optional<UI> maybePrevUI = component.getUI();

        if (maybePrevUI.isPresent() && maybePrevUI.get().equals(ui)) {
            return;
        }

        // Remove the top-level component from the tree
        root.getElement().removeFromTree(false);

        // Transfer all remaining UI child elements (typically dialogs
        // and notifications) to the new UI
        maybePrevUI.ifPresent(prevUi -> {
            ui.getInternals().moveElementsFrom(prevUi);
            prevUi.close();
        });
    }

    /**
     * Invoke this method with the chain that needs to be preserved after
     * {@link #handle(NavigationEvent)} method created it.
     */
    private void setPreservedChain(ArrayList<HasElement> chain,
            NavigationEvent event) {

        final Location location = event.getLocation();
        final UI ui = event.getUI();
        final VaadinSession session = ui.getSession();

        final ExtendedClientDetails extendedClientDetails = ui.getInternals()
                .getExtendedClientDetails();

        if (extendedClientDetails == null) {
            // We need first to retrieve the window name in order to cache the
            // component chain for later potential refreshes.
            ui.getPage().retrieveExtendedClientDetails(
                    details -> setPreservedChain(session,
                            details.getWindowName(), location, chain));

        } else {
            final String windowName = extendedClientDetails.getWindowName();
            setPreservedChain(session, windowName, location, chain);
        }
    }

    private static void validateStatusCode(int statusCode,
            Class<? extends Component> targetClass) {
        if (!HttpStatusCode.isValidStatusCode(statusCode)) {
            String msg = String.format(
                    "Error state code must be a valid HttpStatusCode value. Received invalid value of '%s' for '%s'",
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
            Component routeTarget, String route) {
        Instantiator instantiator = navigationEvent.getUI().getSession()
                .getService().getInstantiator();
        Supplier<String> lookForTitleInTarget = () -> lookForTitleInTarget(
                instantiator.getApplicationClass(routeTarget))
                .map(PageTitle::value).orElse("");

        // check for HasDynamicTitle in current router targets chain
        String title = RouteUtil.getDynamicTitle(navigationEvent.getUI())
                .orElseGet(() -> Optional
                        .ofNullable(
                                MenuRegistry.getClientRoutes(true).get(route))
                        .map(AvailableViewInfo::title)
                        .orElseGet(lookForTitleInTarget));

        navigationEvent.getUI().getPage().setTitle(title);
    }

    private static Optional<PageTitle> lookForTitleInTarget(
            Class<?> routeTarget) {
        return Optional.ofNullable(routeTarget.getAnnotation(PageTitle.class));
    }

    private static boolean isPreserveOnRefreshTarget(
            Class<? extends Component> routeTargetType,
            List<Class<? extends RouterLayout>> routeLayoutTypes) {
        return routeTargetType.isAnnotationPresent(PreserveOnRefresh.class)
                || routeLayoutTypes.stream().anyMatch(layoutType -> layoutType
                        .isAnnotationPresent(PreserveOnRefresh.class));
    }

    private static boolean isPreservePartialTarget(
            Class<? extends Component> routeTargetType,
            List<Class<? extends RouterLayout>> routeLayoutTypes) {
        return (routeTargetType.isAnnotationPresent(PreserveOnRefresh.class)
                && routeTargetType.getAnnotation(PreserveOnRefresh.class)
                        .partialMatch())
                || routeLayoutTypes.stream().anyMatch(layoutType -> layoutType
                        .isAnnotationPresent(PreserveOnRefresh.class)
                        && layoutType.getAnnotation(PreserveOnRefresh.class)
                                .partialMatch());
    }

    // maps window.name to (location, chain)
    private static class PreservedComponentCache
            extends HashMap<String, Pair<String, ArrayList<HasElement>>> {
    }

    static boolean hasPreservedChain(VaadinSession session) {
        final PreservedComponentCache cache = session
                .getAttribute(PreservedComponentCache.class);
        return cache != null && !cache.isEmpty();
    }

    static boolean hasPreservedChainOfLocation(VaadinSession session,
            Location location) {
        final PreservedComponentCache cache = session
                .getAttribute(PreservedComponentCache.class);
        return cache != null && cache.values().stream()
                .anyMatch(entry -> entry.getFirst().equals(location.getPath()));
    }

    static Optional<ArrayList<HasElement>> getPreservedChain(
            VaadinSession session, String windowName, Location location) {
        final PreservedComponentCache cache = session
                .getAttribute(PreservedComponentCache.class);
        if (cache != null && cache.containsKey(windowName) && cache
                .get(windowName).getFirst().equals(location.getPath())) {
            return Optional.of(cache.get(windowName).getSecond());
        }
        return Optional.empty();
    }

    /**
     * Get a preserved chain by window name only ignoring location path.
     *
     * @param session
     *            current session
     * @param windowName
     *            window name to get cached view stack for
     * @return view stack cache if available for window name
     */
    static Optional<List<HasElement>> getWindowPreservedChain(
            VaadinSession session, String windowName) {
        final PreservedComponentCache cache = session
                .getAttribute(PreservedComponentCache.class);
        if (cache != null && cache.containsKey(windowName)) {
            return Optional.of(cache.get(windowName).getSecond());
        }
        return Optional.empty();
    }

    static void setPreservedChain(VaadinSession session, String windowName,
            Location location, ArrayList<HasElement> chain) {
        PreservedComponentCache cache = session
                .getAttribute(PreservedComponentCache.class);
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
                final PreservedComponentCache cache = session
                        .getAttribute(PreservedComponentCache.class);
                if (cache != null) {
                    cache.remove(windowName);
                }
            });
        }
    }

    /**
     * Removes preserved component cache for an inactive UI.
     *
     * @param inactiveUI
     *            the inactive UI
     * @throws IllegalStateException
     *             if the UI is not in closing state
     */
    public static void purgeInactiveUIPreservedChainCache(UI inactiveUI) {
        if (!inactiveUI.isClosing()) {
            throw new IllegalStateException(
                    "Cannot purge preserved chain cache for an active UI");
        }
        final VaadinSession session = inactiveUI.getSession();
        final PreservedComponentCache cache = session
                .getAttribute(PreservedComponentCache.class);
        if (cache != null && !cache.isEmpty()) {
            StateNode uiNode = inactiveUI.getElement().getNode();
            Set<String> inactiveWindows = cache.entrySet().stream()
                    .filter(e -> {
                        ArrayList<HasElement> chain = e.getValue().getSecond();
                        // chain is never empty
                        StateNode chainNode = chain.get(0).getElement()
                                .getNode();
                        while (chainNode.getParent() != null) {
                            chainNode = chainNode.getParent();
                        }
                        return uiNode == chainNode;
                    }).map(Map.Entry::getKey).collect(Collectors.toSet());
            if (!inactiveWindows.isEmpty()) {
                LoggerFactory.getLogger(AbstractNavigationStateRenderer.class)
                        .debug("Removing preserved chain cache for inactive UI {} on VaadinSession {} (windows: {})",
                                inactiveUI.getUIId(),
                                session.getSession().getId(), inactiveWindows);
            }
            inactiveWindows.forEach(cache::remove);
        }
    }

}
