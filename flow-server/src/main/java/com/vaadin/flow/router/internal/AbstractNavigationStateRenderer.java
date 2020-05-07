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

import javax.servlet.http.HttpServletResponse;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.ExtendedClientDetails;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.DeploymentConfiguration;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for navigation handlers that target a navigation state.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public abstract class AbstractNavigationStateRenderer
        implements NavigationHandler {

    private static List<Integer> statusCodes = ReflectTools
            .getConstantIntValues(HttpServletResponse.class);

    private final NavigationState navigationState;

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
        Optional<Integer> result = executeBeforeLeaveNavigation(event,
                beforeNavigationDeactivating, leaveHandlers);
        if (result.isPresent()) {
            return result.get();
        }

        final ArrayList<HasElement> chain;

        final boolean preserveOnRefreshTarget = isPreserveOnRefreshTarget(
                routeTargetType, routeLayoutTypes);

        if (preserveOnRefreshTarget) {
            final Optional<ArrayList<HasElement>> maybeChain = getPreservedChain(
                    event);
            if (!maybeChain.isPresent()) {
                // We're returning because the preserved chain is not ready to
                // be used as is, and requires client data requested within
                // `getPreservedChain`. Once the data is retrieved from the
                // client, `handle` method will be invoked with the same
                // `NavigationEvent` argument.
                return HttpServletResponse.SC_OK;
            } else {
                chain = maybeChain.get();
            }
        } else {

            // Create an empty chain which gets populated later in
            // `createChainIfEmptyAndExecuteBeforeEnterNavigation`.
            chain = new ArrayList<>();

            // Has any preserved components already been created here? If so,
            // we don't want to navigate back to them ever so clear cache for
            // window.
            clearAllPreservedChains(ui);
        }

        BeforeEnterEvent beforeNavigationActivating = new BeforeEnterEvent(
                event, routeTargetType, routeLayoutTypes);

        result = createChainIfEmptyAndExecuteBeforeEnterNavigation(
                beforeNavigationActivating, event, chain);
        if (result.isPresent()) {
            return result.get();
        }

        final Component componentInstance = (Component) chain.get(0);

        // Preserve the navigation chain if all went well and it's being shown
        // on the UI.
        if (preserveOnRefreshTarget) {
            setPreservedChain(chain, event);
            warnAboutPreserveOnRefreshAndLiveReloadCombo(ui);
        }

        @SuppressWarnings("unchecked")
        List<RouterLayout> routerLayouts = (List<RouterLayout>) (List<?>) chain
                .subList(1, chain.size());

        // Change the UI according to the navigation Component chain.
        ui.getInternals().showRouteTarget(event.getLocation(),
                navigationState.getResolvedPath(), componentInstance,
                routerLayouts);

        updatePageTitle(event, componentInstance);

        int statusCode = locationChangeEvent.getStatusCode();
        validateStatusCode(statusCode, routeTargetType);

        // After navigation event
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

    // The last element in the returned list is always a Component class
    private List<Class<? extends HasElement>> createTypesChain(
            NavigationEvent event) {
        final Class<? extends Component> routeTargetType = navigationState
                .getNavigationTarget();

        List<Class<? extends RouterLayout>> routeLayoutTypes = new ArrayList<>(
                getRouterLayoutTypes(routeTargetType,
                        event.getUI().getRouter()));
        Collections.reverse(routeLayoutTypes);

        final ArrayList<Class<? extends HasElement>> chain = new ArrayList<>();
        for (Class<? extends RouterLayout> parentType : routeLayoutTypes) {
            chain.add(parentType);
        }
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
     * @param leaveHandlers
     *            handlers for before leave event
     * @return result of observer events
     */
    private Optional<Integer> executeBeforeLeaveNavigation(NavigationEvent event,
            BeforeLeaveEvent beforeNavigation,
            Deque<BeforeLeaveHandler> leaveHandlers) {
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

                return Optional.of(HttpServletResponse.SC_OK);
            }
        }

        return Optional.empty();
    }

    /**
     * Inform any {@link BeforeEnterObserver}s in attaching element chain. The
     * event is sent first to the {@link BeforeEnterHandler}s registered within
     * the {@link UI}, then to any element in the chain and to any of it's child
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
     *            instances according with the navigation event's location.
     * @return result of observer events
     */
    private Optional<Integer> createChainIfEmptyAndExecuteBeforeEnterNavigation(
            BeforeEnterEvent beforeNavigation, NavigationEvent event,
            List<HasElement> chain) {

        // Always send the beforeNavigation event first to the registered
        // listeners
        List<BeforeEnterHandler> registeredEnterHandlers = new ArrayList<>(
                beforeNavigation.getUI()
                        .getNavigationListeners(BeforeEnterHandler.class));

        Optional<Integer> result = sendBeforeEnterEvent(
                registeredEnterHandlers, event, beforeNavigation, null);
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
            List<HasElement> chain) {
        List<HasElement> oldChain = event.getUI().getInternals()
                .getActiveRouterTargetsChain();

        // Create the chain components if missing.
        List<Class<? extends HasElement>> typesChain = createTypesChain(event);

        try {
            for (Class<? extends HasElement> elementType : typesChain) {
                HasElement element = getRouteTarget(elementType, event);

                chain.add(element);

                List<BeforeEnterHandler> chainEnterHandlers = new ArrayList<>(
                        EventUtil.collectBeforeEnterObserversFromChainElement(
                                element, oldChain));

                final boolean lastElement = chain.size() == typesChain.size();
                Optional<Integer> result = sendBeforeEnterEvent(chainEnterHandlers,
                        event, beforeNavigation, lastElement ? chain : null);
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
            List<HasElement> chain) {
        // Reverse the chain so that the target is last.
        chain = new ArrayList<>(chain);
        Collections.reverse(chain);

        // Used when the chain already exists by being preserved on refresh.
        // See `isPreserveOnRefreshTarget` method implementation and usage.
        List<BeforeEnterHandler> chainEnterHandlers = new ArrayList<>(
                EventUtil.collectBeforeEnterObserversFromChain(chain, event
                        .getUI().getInternals().getActiveRouterTargetsChain()));

        Optional<Integer> result = sendBeforeEnterEvent(chainEnterHandlers,
                event, beforeNavigation, chain);

        if (result.isPresent()) {
            return result;
        }

        return Optional.empty();
    }

    /*
     * Target component is expected to be the last in the chain.
     */
    private Optional<Integer> sendBeforeEnterEvent(
            List<BeforeEnterHandler> eventHandlers, NavigationEvent event,
            BeforeEnterEvent beforeNavigation, List<HasElement> chain) {

        Component componentInstance = null;
        boolean notifyNavigationTarget = false;

        if (chain != null) {
            // Reverse the chain to the stored ordered, since that is different
            // than the notification order, and also to keep
            // LocationChangeEvent.getRouteTargetChain backward compatible.
            chain = new ArrayList<>(chain);
            Collections.reverse(chain);

            componentInstance = (Component) chain.get(0);

            locationChangeEvent = new LocationChangeEvent(event.getSource(),
                    event.getUI(), event.getTrigger(), event.getLocation(),
                    chain);

            notifyNavigationTarget = true;
        }

        for (BeforeEnterHandler eventHandler : eventHandlers) {

            // Notify the target itself, i.e. with the url parameter, before
            // sending the event to the navigation target or any of its
            // children.
            if (notifyNavigationTarget
                    && (isComponentElementEqualsOrChild(eventHandler,
                    componentInstance))) {

                Optional<Integer> result = notifyNavigationTarget(
                        event, beforeNavigation, locationChangeEvent,
                        componentInstance);
                if (result.isPresent()) {
                    return result;
                }

                notifyNavigationTarget = false;
            }

            Optional<Integer> result = sendBeforeEnterEvent(event,
                    beforeNavigation, eventHandler);
            if (result.isPresent()) {
                return result;
            }
        }

        // Make sure notifyNavigationTarget is executed.
        if (notifyNavigationTarget) {

            Optional<Integer> result = notifyNavigationTarget(
                    event, beforeNavigation, locationChangeEvent,
                    componentInstance);
            if (result.isPresent()) {
                return result;
            }
        }

        return Optional.empty();
    }

    private Optional<Integer> sendBeforeEnterEvent(NavigationEvent event,
            BeforeEnterEvent beforeNavigation,
            BeforeEnterHandler eventHandler) {
        eventHandler.beforeEnter(beforeNavigation);
        validateBeforeEvent(beforeNavigation);
        return handleTriggeredBeforeEvent(event, beforeNavigation);
    }

    private Optional<Integer> notifyNavigationTarget(
            NavigationEvent event, BeforeEnterEvent beforeNavigation,
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
        if (eventHandler instanceof HasElement) {
            HasElement hasElement = (HasElement) eventHandler;

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
     * @return a HTTP status code wrapped as an {@link Optional}. If the
     *         {@link Optional} is empty, the process will proceed with next
     *         observer or just move forward, otherwise the process will return
     *         immediately with the provided http code.
     * @see HttpServletResponse
     */
    protected Optional<Integer> handleTriggeredBeforeEvent(
            NavigationEvent event, BeforeEvent beforeEvent) {

        if (beforeEvent.hasForwardTarget()
                && !isSameNavigationState(beforeEvent.getForwardTargetType(),
                beforeEvent.getForwardTargetParameters())) {
            return Optional.of(forward(event, beforeEvent));
        }

        if (beforeEvent.hasRerouteTarget()
                && !isSameNavigationState(beforeEvent.getRerouteTargetType(),
                beforeEvent.getRerouteTargetParameters())) {
            return Optional.of(reroute(event, beforeEvent));
        }
        
        return Optional.empty();
    }

    private boolean isSameNavigationState(Class<? extends Component> targetType,
                                          List<String> targetParameters) {
        final boolean sameTarget = navigationState.getNavigationTarget()
                .equals(targetType);

        final boolean sameParameters = targetParameters.equals(navigationState
                .getUrlParameters().orElse(Collections.emptyList()));

        return sameTarget && sameParameters;
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
            targetType = beforeNavigation.getRerouteTargetType();
        }

        Location location = new Location(RouteConfiguration
                .forRegistry(event.getSource().getRegistry())
                .getUrlBase(targetType)
                .orElseThrow(() -> new IllegalStateException(String.format(
                        "The target component '%s' has no registered route",
                        targetType))),
                event.getLocation().getQueryParameters());

        if (beforeNavigation.hasForwardTarget()) {
            List<String> segments = new ArrayList<>(location.getSegments());
            segments.addAll(beforeNavigation.getForwardTargetParameters());
            location = new Location(segments);
        }

        return new NavigationEvent(event.getSource(), location, event.getUI(),
                NavigationTrigger.PROGRAMMATIC);
    }

    /**
     * Checks if there exists a cached component chain of the route location in
     * the current window.
     *
     * If retrieving the window name requires another round-trip, schedule it
     * and make a new call to the handle {@link #handle(NavigationEvent)} in the
     * callback. In this case, this method returns {@link Optional#empty()}.
     *
     * If the chain is missing and needs to be created this method returns an
     * {@link Optional} wrapping an empty {@link ArrayList}.
     */
    private Optional<ArrayList<HasElement>> getPreservedChain(
            NavigationEvent event) {
        final Location location = event.getLocation();
        final UI ui = event.getUI();
        final VaadinSession session = ui.getSession();

        if (ui.getInternals().getExtendedClientDetails() == null) {
            if (hasPreservedChainOfLocation(session, location)) {
                // We may have a cached instance for this location, but we
                // need to retrieve the window name before we can determine
                // this, so execute a client-side request.
                ui.getPage().retrieveExtendedClientDetails(
                        details -> handle(event));
                return Optional.empty();
            }
        } else {
            final String windowName = ui.getInternals()
                    .getExtendedClientDetails().getWindowName();
            final Optional<ArrayList<HasElement>> maybePreserved = getPreservedChain(
                    session, windowName, event.getLocation());
            if (maybePreserved.isPresent()) {
                // Re-use preserved chain for this route
                ArrayList<HasElement> chain = maybePreserved.get();
                final HasElement root = chain.get(chain.size() - 1);
                final Component component = (Component) chain.get(0);
                final Optional<UI> maybePrevUI = component.getUI();

                // Remove the top-level component from the tree
                root.getElement().removeFromTree();

                // Transfer all remaining UI child elements (typically dialogs
                // and notifications) to the new UI
                maybePrevUI.ifPresent(
                        prevUi -> ui.getInternals().moveElementsFrom(prevUi));

                return Optional.of(chain);
            }
        }

        return Optional.of(new ArrayList<>(0));
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
                || routeLayoutTypes.stream().anyMatch(layoutType -> layoutType
                .isAnnotationPresent(PreserveOnRefresh.class));
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
        } else {
            return Optional.empty();
        }
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

    private static void warnAboutPreserveOnRefreshAndLiveReloadCombo(UI ui) {
        // Show a warning that live-reload may work counter-intuitively
        DeploymentConfiguration configuration = ui.getSession()
                .getConfiguration();
        if (!configuration.isProductionMode()
                && configuration.isDevModeLiveReloadEnabled()) {
            ui.getPage().executeJs(
                    "Vaadin.Flow.devModeGizmo.showNotification('warning', '@PreserveOnRefresh enabled', 'When refreshing the page in the browser, the server-side Java view instance is reused rather than being recreated.', null, 'preserveOnRefreshWarning')");
        }
    }
}
