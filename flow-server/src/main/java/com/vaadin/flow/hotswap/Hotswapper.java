/*
 * Copyright 2000-2024 Vaadin Ltd.
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

package com.vaadin.flow.hotswap;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.internal.BrowserLiveReload;
import com.vaadin.flow.internal.BrowserLiveReloadAccessor;
import com.vaadin.flow.server.ServiceDestroyEvent;
import com.vaadin.flow.server.ServiceDestroyListener;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.SessionDestroyEvent;
import com.vaadin.flow.server.SessionDestroyListener;
import com.vaadin.flow.server.SessionInitEvent;
import com.vaadin.flow.server.SessionInitListener;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

/**
 * Entry point for application classes hot reloads.
 * <p>
 * </p>
 * This class is meant to be used in combination with class live reloading tools
 * like JRebel, Hotswap agent and Spring Boot Developer Tools, to immediately
 * apply changes on components that should be updated when classes have been
 * added or modified. Currently, class deletion is not supported because of
 * issues with several hotswap agents.
 * <p>
 * </p>
 * Hotswap tools should obtain an instance of this class by calling the
 * {@link #register(VaadinService)} method, providing an active
 * {@link VaadinService} instance. For example, an agent can inject the
 * following code at the beginning of the {@link VaadinService#init()} method:
 *
 * <pre>
 * {@code
 * com.vaadin.flow.hotswap.Hotswapper.register(this);
 * }
 * </pre>
 * <p>
 * </p>
 * The component delegates specific hotswap logic to registered implementors of
 * {@link VaadinHotswapper} interface.
 * <p>
 * </p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @see VaadinHotswapper
 * @since 24.5
 */
public class Hotswapper implements ServiceDestroyListener, SessionInitListener,
        SessionDestroyListener {
    private static final Logger LOGGER = LoggerFactory
            .getLogger(Hotswapper.class);
    private final Set<VaadinSession> sessions = ConcurrentHashMap.newKeySet();
    private final VaadinService vaadinService;
    private final BrowserLiveReload liveReload;

    Hotswapper(VaadinService vaadinService) {
        this.vaadinService = Objects.requireNonNull(vaadinService,
                "VaadinService instance is mandatory");
        liveReload = BrowserLiveReloadAccessor
                .getLiveReloadFromService(vaadinService).orElse(null);
    }

    /**
     * Called by hotswap tools when one or more application classes have been
     * updated.
     * <p>
     * </p>
     * This method delegates update operations to registered
     * {@link VaadinHotswapper} implementors. invoking first
     * {@link VaadinHotswapper#onClassLoadEvent(VaadinService, Set, boolean)}
     * and then invoking
     * {@link VaadinHotswapper#onClassLoadEvent(VaadinSession, Set, boolean)}
     * for each active {@link VaadinSession}.
     *
     * @param redefined
     *            {@literal true} if the classes have been redefined by hotswap
     *            mechanism, {@literal false} if they have been loaded for time
     *            by the ClassLoader.
     * @param classes
     *            the set of affected classes.
     * @see VaadinHotswapper#onClassLoadEvent(VaadinService, Set, boolean)
     * @see VaadinHotswapper#onClassLoadEvent(VaadinSession, Set, boolean)
     */
    // Note: 'redefined' parameter is defined as Boolean wrapper class because
    // Hotswap agent will call this method by reflection, and it fails to
    // identify it if it has primitive parameters
    public void onHotswap(String[] classes, Boolean redefined) {
        if (classes == null || classes.length == 0) {
            LOGGER.debug(
                    "Hotswap event ignored because Hotswapper has been called without changes to apply.");
            return;
        }
        onHotswapInternal(
                Arrays.stream(classes).map(Hotswapper::resolveClass)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toCollection(HashSet::new)),
                redefined);
    }

    /**
     * Called by hotswap tools when one or more application resources have been
     * changed.
     * <p>
     * </p>
     *
     * @param createdResources
     *            the list of potentially newly created resources. Never
     *            {@literal null}.
     * @param modifiedResources
     *            the list of potentially modified resources. Never
     *            {@literal null}.
     * @param deletedResources
     *            the list of potentially deleted resource. Never
     *            {@literal null}.
     */
    public void onHotswap(URI[] createdResources, URI[] modifiedResources,
            URI[] deletedResources) {
        // no-op for the moment, just logging for debugging purpose
        // entry point for future implementations, like reloading I18n provider
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(
                    "Created resources: {}, modified resources: {}, deletedResources: {}.",
                    createdResources, modifiedResources, deletedResources);
        }
    }

    private void onHotswapInternal(HashSet<Class<?>> classes,
            Boolean redefined) {
        if (classes == null || classes.isEmpty()) {
            LOGGER.debug(
                    "Hotswap event ignored because Hotswapper has been called without changes to apply.");
            return;
        }

        Lookup lookup = vaadinService.getContext().getAttribute(Lookup.class);
        if (lookup == null) {
            throw new IllegalStateException(
                    "Lookup not found in VaadinContext");
        }

        boolean forceBrowserReload = false;
        Collection<VaadinHotswapper> hotSwappers = lookup
                .lookupAll(VaadinHotswapper.class);
        for (VaadinHotswapper hotSwapper : hotSwappers) {
            try {
                forceBrowserReload |= hotSwapper.onClassLoadEvent(vaadinService,
                        classes, redefined);
            } catch (Exception ex) {
                LOGGER.debug("Global hotswap failed executing {}", hotSwapper,
                        ex);
            }
        }
        Set<VaadinSession> vaadinSessions = Set.copyOf(sessions);
        for (VaadinSession vaadinSession : vaadinSessions) {
            try {
                vaadinSession.getLockInstance().lock();
                for (VaadinHotswapper hotSwapper : hotSwappers) {
                    try {
                        forceBrowserReload |= hotSwapper.onClassLoadEvent(
                                vaadinSession, classes, redefined);
                    } catch (Exception ex) {
                        LOGGER.debug(
                                "Hotswap failed executing {} for Vaadin session {}",
                                hotSwapper, vaadinSession.getSession().getId(),
                                ex);
                    }
                }
            } finally {
                vaadinSession.getLockInstance().unlock();
            }
        }
        EnumMap<UIRefreshStrategy, List<UI>> refreshActions = computeRefreshStrategies(
                vaadinSessions, classes);
        forceBrowserReload |= refreshActions
                .containsKey(UIRefreshStrategy.RELOAD);
        boolean uiTreeNeedsRefresh = refreshActions
                .containsKey(UIRefreshStrategy.REFRESH_CHAIN)
                || refreshActions.containsKey(UIRefreshStrategy.REFRESH_ROUTE);
        if (forceBrowserReload || uiTreeNeedsRefresh) {
            triggerClientUpdate(refreshActions, forceBrowserReload);
        }
    }

    /**
     * Strategy to use to update the client when a class for a component in the
     * UI tree has changed. See {@link UI#refreshCurrentRoute(boolean)}.
     */
    private enum UIRefreshStrategy {
        /**
         * Reload browser page.
         */
        RELOAD,
        /**
         * Refresh only route instance.
         */
        REFRESH_ROUTE,
        /**
         * Refresh all layouts in the route chain.
         */
        REFRESH_CHAIN,
        /**
         * Refresh not needed.
         */
        SKIP
    }

    private EnumMap<UIRefreshStrategy, List<UI>> computeRefreshStrategies(
            Set<VaadinSession> vaadinSessions, Set<Class<?>> changedClasses) {
        EnumMap<UIRefreshStrategy, List<UI>> uisToRefresh = new EnumMap<>(
                UIRefreshStrategy.class);
        for (VaadinSession session : vaadinSessions) {
            session.getLockInstance().lock();
            try {
                session.getUIs().stream().filter(ui -> !ui.isClosing())
                        .forEach(
                                ui -> uisToRefresh
                                        .computeIfAbsent(
                                                computeRefreshStrategy(ui,
                                                        changedClasses),
                                                k -> new ArrayList<>())
                                        .add(ui));
            } finally {
                session.getLockInstance().unlock();
            }
        }
        uisToRefresh.remove(UIRefreshStrategy.SKIP);
        return uisToRefresh;
    }

    private UIRefreshStrategy computeRefreshStrategy(UI ui,
            Set<Class<?>> changedClasses) {
        List<HasElement> targetsChain = new ArrayList<>(
                ui.getActiveRouterTargetsChain());
        HasElement route = targetsChain.get(0);
        boolean pushEnabled = ui.getPushConfiguration().getPushMode()
                .isEnabled();

        // Detect changed classes affects current route or layouts
        List<HasElement> targetChainChangedItems = changedClasses.stream()
                .flatMap(clazz -> targetsChain.stream()
                        .filter(chainItem -> clazz
                                .isAssignableFrom(chainItem.getClass())))
                .distinct().toList();

        UIRefreshStrategy refreshStrategy;
        if (!targetChainChangedItems.isEmpty()) {
            refreshStrategy = targetChainChangedItems.stream()
                    .allMatch(chainItem -> chainItem == route)
                            ? UIRefreshStrategy.REFRESH_ROUTE
                            : UIRefreshStrategy.REFRESH_CHAIN;
        } else {
            // Look into the UI tree to find if any component is instance of
            // a changed class. If so, detect its parent route or layout to
            // determine the refresh strategy.
            refreshStrategy = computeRefreshStrategyForUITree(ui,
                    changedClasses, targetsChain, route);
        }

        // If push is not enabled we can only request a full page reload
        if (refreshStrategy != UIRefreshStrategy.SKIP && !pushEnabled) {
            refreshStrategy = UIRefreshStrategy.RELOAD;
        }
        return refreshStrategy;
    }

    // Visits UI tree to find potential components affected by a class change
    // and computes the UI refresh strategy:
    // - if an affected component is child of current route, request for a route
    // refresh, but continue to check potential layouts
    // - if an affected component is child of a layout immediately stop search
    // and request a target chain refresh
    private static UIRefreshStrategy computeRefreshStrategyForUITree(UI ui,
            Set<Class<?>> changedClasses, List<HasElement> targetsChain,
            HasElement route) {
        UIRefreshStrategy refreshStrategy = UIRefreshStrategy.SKIP;
        LinkedList<Component> stack = new LinkedList<>();
        ui.getChildren().forEach(stack::add);
        while (!stack.isEmpty()) {
            Component child = stack.removeFirst();
            if (changedClasses.stream().anyMatch(
                    clazz -> clazz.isAssignableFrom(child.getClass()))) {
                Component parent = child.getParent().orElse(null);
                while (parent != null) {
                    if (!targetsChain.contains(parent)) {
                        parent = parent.getParent().orElse(null);
                    } else if (parent == route) {
                        refreshStrategy = UIRefreshStrategy.REFRESH_ROUTE;
                        parent = null;
                    } else {
                        refreshStrategy = UIRefreshStrategy.REFRESH_CHAIN;
                        parent = null;
                        stack.clear();
                    }
                }
            } else {
                child.getChildren().forEach(stack::add);
            }
        }
        return refreshStrategy;
    }

    private void triggerClientUpdate(
            EnumMap<UIRefreshStrategy, List<UI>> uisToRefresh,
            boolean forceReload) {

        // If some UI has push not enabled, BrowserLiveReload should be used
        // to trigger a client update. However, BrowserLiveReload broadcasts
        // the reload request to all active client connection, making calls to
        // UI.refreshCurrentRoute() useless.
        if (forceReload) {
            if (liveReload != null) {
                LOGGER.debug(
                        "Triggering browser live reload triggered because of classes changes");
                liveReload.reload();
            } else {
                LOGGER.debug(
                        "A change to one or more classes requires a browser page reload, but BrowserLiveReload is not available. "
                                + "Please reload the browser page manually to make changes effective.");
            }
        } else {
            LOGGER.debug(
                    "Triggering re-navigation to current route for UIs affected by classes changes.");
            for (UIRefreshStrategy action : uisToRefresh.keySet()) {
                uisToRefresh.get(action)
                        .forEach(ui -> ui.access(() -> ui.refreshCurrentRoute(
                                action == UIRefreshStrategy.REFRESH_CHAIN)));
            }
        }
    }

    private static Class<?> resolveClass(String className) {
        try {
            return Class.forName(className, false,
                    Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            LOGGER.debug("Cannot resolve class {}", className, e);
        }
        return null;
    }

    @Override
    public void sessionInit(SessionInitEvent event) throws ServiceException {
        sessions.add(event.getSession());
    }

    @Override
    public void sessionDestroy(SessionDestroyEvent event) {
        sessions.remove(event.getSession());
    }

    @Override
    public void serviceDestroy(ServiceDestroyEvent event) {
        sessions.clear();
    }

    /**
     * Register the hotwsapper entry point for the given {@link VaadinService}.
     * <p>
     * </p>
     * The hotswapper is registered only in development mode.
     *
     * @param vaadinService
     *            the {@link VaadinService} instance for hotswapper
     *            registration.
     * @return the hotswapper instance wrapped into an {@link Optional}, or an
     *         empty Optional if Vaadin is running in production mode.
     */
    public static Optional<Hotswapper> register(VaadinService vaadinService) {
        if (!vaadinService.getDeploymentConfiguration().isProductionMode()) {
            Hotswapper hotswapper = new Hotswapper(vaadinService);
            vaadinService.addSessionInitListener(hotswapper);
            vaadinService.addSessionDestroyListener(hotswapper);
            vaadinService.addServiceDestroyListener(hotswapper);
            return Optional.of(hotswapper);
        }
        return Optional.empty();
    }

}
