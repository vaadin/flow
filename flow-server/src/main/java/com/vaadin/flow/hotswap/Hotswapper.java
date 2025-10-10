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
package com.vaadin.flow.hotswap;

import java.io.File;
import java.io.Serializable;
import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.internal.BrowserLiveReload;
import com.vaadin.flow.internal.BrowserLiveReloadAccessor;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.router.internal.RouteTarget;
import com.vaadin.flow.router.internal.RouteUtil;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.ServiceDestroyEvent;
import com.vaadin.flow.server.ServiceDestroyListener;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.SessionDestroyEvent;
import com.vaadin.flow.server.SessionDestroyListener;
import com.vaadin.flow.server.SessionInitEvent;
import com.vaadin.flow.server.SessionInitListener;
import com.vaadin.flow.server.UIInitEvent;
import com.vaadin.flow.server.UIInitListener;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.frontend.FrontendUtils;

/**
 * Entry point for application classes hot reloads.
 * <p>
 * This class is meant to be used in combination with class live reloading tools
 * like JRebel, Hotswap agent and Spring Boot Developer Tools, to immediately
 * apply changes on components that should be updated when classes have been
 * added or modified. Currently, class deletion is not supported because of
 * issues with several hotswap agents.
 * <p>
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
 * The component delegates specific hotswap logic to registered implementors of
 * {@link VaadinHotswapper} interface.
 * <p>
 * By default, Hotswapper determines the best browser page refresh strategy, but
 * a full page reload can be forced by setting the
 * {@code vaadin.hotswap.forcePageReload} system property. Hotswap tools can
 * alter the behavior at runtime by calling
 * {@link #forcePageReload(VaadinService, boolean)}
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @see VaadinHotswapper
 * @since 24.5
 */
public class Hotswapper implements ServiceDestroyListener, SessionInitListener,
        SessionDestroyListener, UIInitListener {

    /**
     * Configuration name for the system parameter that determines whether
     * Hotswapper should always trigger a full page reload instead of computing
     * an appropriate UI refresh strategy.
     */
    public static final String FORCE_RELOAD_PROPERTY = "vaadin.hotswap.forcePageReload";

    private static final Logger LOGGER = LoggerFactory
            .getLogger(Hotswapper.class);
    private final Set<VaadinSession> sessions = ConcurrentHashMap.newKeySet();
    private final VaadinService vaadinService;
    private final BrowserLiveReload liveReload;
    private volatile boolean serviceDestroyed = false;

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
     *
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
        if (serviceDestroyed) {
            LOGGER.debug(
                    "Hotswap classes change event ignored because VaadinService has been destroyed.");
            return;
        }
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
        if (serviceDestroyed) {
            LOGGER.debug(
                    "Hotswap resources change event ignored because VaadinService has been destroyed.");
            return;
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(
                    "Created resources: {}, modified resources: {}, deletedResources: {}.",
                    createdResources, modifiedResources, deletedResources);
        }

        if (anyMatches(".*\\.css", createdResources, modifiedResources,
                deletedResources)) {
            if (liveReload == null) {
                LOGGER.debug(
                        "A change to one or more CSS resources requires a browser page refresh, but BrowserLiveReload is not available. "
                                + "Please reload the browser page manually to make changes effective.");
            } else {
                LOGGER.debug(
                        "Triggering browser live reload because of CSS resources changes");

                String buildFolder = vaadinService.getDeploymentConfiguration()
                        .getBuildFolder();
                File projectFolder = vaadinService.getDeploymentConfiguration()
                        .getProjectFolder();

                String pathToClasses = FrontendUtils
                        .getUnixPath(projectFolder.toPath().resolve(
                                Paths.get(buildFolder, "classes").normalize()));

                List<File> publicStaticResourcesFolders = Stream
                        .of("META-INF/resources", "resources", "static",
                                "public")
                        .map(path -> new File(pathToClasses, path))
                        .filter(File::exists).toList();

                Arrays.stream(modifiedResources).forEach(resource -> {
                    String resourcePath = resource.getPath();
                    for (File staticResourcesFolder : publicStaticResourcesFolders) {
                        String staticResourcesPath = FrontendUtils
                                .getUnixPath(staticResourcesFolder.toPath());
                        if (resourcePath.startsWith(staticResourcesPath)) {
                            String path = resourcePath
                                    .replace(staticResourcesPath, "");
                            if (path.startsWith("/")) {
                                path = path.substring(1);
                            }
                            liveReload.update(path, null);
                        }
                    }
                });
            }
        }

        if (anyMatches(".*/vaadin-i18n/.*\\.properties", createdResources,
                modifiedResources, deletedResources)) {
            // Clear resource bundle cache so that translations (and other
            // resources) are reloaded
            ResourceBundle.clearCache();

            // Trigger any potential Hilla translation updates
            liveReload.sendHmrEvent("translations-update",
                    JacksonUtils.createObjectNode());

            // Trigger any potential Flow translation updates
            EnumMap<UIRefreshStrategy, List<UI>> refreshActions = new EnumMap<>(
                    UIRefreshStrategy.class);
            forEachActiveUI(ui -> {
                UIRefreshStrategy strategy = ui.getPushConfiguration()
                        .getPushMode().isEnabled()
                                ? UIRefreshStrategy.PUSH_REFRESH_CHAIN
                                : UIRefreshStrategy.REFRESH;
                refreshActions.computeIfAbsent(strategy, k -> new ArrayList<>())
                        .add(ui);
            });
            triggerClientUpdate(refreshActions, false);
        }

    }

    private boolean anyMatches(String regexp, URI[]... resources) {
        for (URI[] uris : resources) {
            for (URI uri : uris) {
                if (uri.toString().matches(regexp)) {
                    return true;
                }
            }
        }
        return false;
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
        forceBrowserReload = forceBrowserReload
                || (getForceReloadHolder(vaadinService).shouldReloadPage()
                        && redefined);

        boolean uiTreeNeedsRefresh = false;
        EnumMap<UIRefreshStrategy, List<UI>> refreshActions = null;

        // When a full page reload is requested it does not make sense to
        // compute refresh strategy
        if (!forceBrowserReload) {
            refreshActions = computeRefreshStrategies(classes, redefined);
            uiTreeNeedsRefresh = !refreshActions.isEmpty();
        }
        if (forceBrowserReload || uiTreeNeedsRefresh) {
            triggerClientUpdate(refreshActions, forceBrowserReload);
        }

        HotswapCompleteEvent event = new HotswapCompleteEvent(vaadinService,
                classes, redefined);
        for (VaadinHotswapper hotSwapper : hotSwappers) {
            try {
                hotSwapper.onHotswapComplete(event);
            } catch (Exception ex) {
                LOGGER.debug("Hotswap complete event handling failed for {}",
                        hotSwapper, ex);
            }
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
         * Refresh UI without a page reload.
         */
        REFRESH,
        /**
         * Refresh only route instance via UI PUSH connection.
         */
        PUSH_REFRESH_ROUTE,
        /**
         * Refresh all layouts in the route chain via UI PUSH connection.
         */
        PUSH_REFRESH_CHAIN,
        /**
         * Refresh not needed.
         */
        SKIP
    }

    private EnumMap<UIRefreshStrategy, List<UI>> computeRefreshStrategies(
            Set<Class<?>> changedClasses, boolean redefined) {
        EnumMap<UIRefreshStrategy, List<UI>> uisToRefresh = new EnumMap<>(
                UIRefreshStrategy.class);
        forEachActiveUI(ui -> uisToRefresh.computeIfAbsent(
                computeRefreshStrategy(ui, changedClasses, redefined),
                k -> new ArrayList<>()).add(ui));

        uisToRefresh.remove(UIRefreshStrategy.SKIP);
        return uisToRefresh;
    }

    private void forEachActiveUI(Consumer<UI> consumer) {
        for (VaadinSession session : Set.copyOf(sessions)) {
            session.getLockInstance().lock();
            try {
                session.getUIs().stream().filter(ui -> !ui.isClosing())
                        .forEach(consumer);
            } finally {
                session.getLockInstance().unlock();
            }
        }
    }

    private UIRefreshStrategy computeRefreshStrategy(UI ui,
            Set<Class<?>> changedClasses, boolean redefined) {
        List<HasElement> targetsChain = new ArrayList<>(
                ui.getActiveRouterTargetsChain());
        if (targetsChain.isEmpty()) {
            // Navigation to a view has not yet happened
            return UIRefreshStrategy.SKIP;
        }
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
        if (redefined) {
            // A full chain refresh should be triggered if there are modal
            // components, since they could be attached to UI or parent layouts
            if (ui.hasModalComponent()) {
                refreshStrategy = UIRefreshStrategy.PUSH_REFRESH_CHAIN;
            } else if (!targetChainChangedItems.isEmpty()) {
                refreshStrategy = targetChainChangedItems.stream()
                        .allMatch(chainItem -> chainItem == route)
                                ? UIRefreshStrategy.PUSH_REFRESH_ROUTE
                                : UIRefreshStrategy.PUSH_REFRESH_CHAIN;
            } else {
                // Look into the UI tree to find if any component is instance of
                // a changed class. If so, detect its parent route or layout to
                // determine the refresh strategy.
                refreshStrategy = computeRefreshStrategyForUITree(ui,
                        changedClasses, targetsChain, route);
            }
        } else {
            // prevent refresh for classes loaded for the first time since
            // it shouldn't impact current view, unless they are newly defined
            // auto layouts.
            // For example, it prevents refresh caused by Dialog related classes
            // loaded for the first time when the dialog is opened
            refreshStrategy = UIRefreshStrategy.SKIP;
        }

        // A different layout might have been applied after hotswap
        if (refreshStrategy == UIRefreshStrategy.SKIP) {
            RouteRegistry registry = ui.getInternals().getRouter()
                    .getRegistry();
            String currentPath = ui.getActiveViewLocation().getPath();
            RouteTarget routeTarget = registry
                    .getNavigationRouteTarget(currentPath).getRouteTarget();
            if (routeTarget != null) {
                // parent layout changed
                if ((redefined && routeTarget.getParentLayouts().stream()
                        .anyMatch(changedClasses::contains)) ||
                // applied auto layout changed or added
                        RouteUtil.isAutolayoutEnabled(routeTarget.getTarget(),
                                currentPath)
                                && registry.hasLayout(currentPath)
                                && RouteUtil
                                        .collectRouteParentLayouts(
                                                registry.getLayout(currentPath))
                                        .stream()
                                        .anyMatch(changedClasses::contains)) {
                    refreshStrategy = UIRefreshStrategy.PUSH_REFRESH_CHAIN;
                }
            }
        }

        // If push is not enabled we can only request a full page refresh
        if (refreshStrategy != UIRefreshStrategy.SKIP && !pushEnabled) {
            refreshStrategy = UIRefreshStrategy.REFRESH;
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
                        refreshStrategy = UIRefreshStrategy.PUSH_REFRESH_ROUTE;
                        parent = null;
                    } else {
                        refreshStrategy = UIRefreshStrategy.PUSH_REFRESH_CHAIN;
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

        boolean refreshRequested = !forceReload
                && uisToRefresh.containsKey(UIRefreshStrategy.REFRESH);

        // If some UI has push not enabled, BrowserLiveReload should be used to
        // trigger a client update. However, BrowserLiveReload broadcasts the
        // reload/refresh request to all active client connection, making calls
        // to UI.refreshCurrentRoute() useless.
        if (forceReload || refreshRequested) {
            if (liveReload == null) {
                LOGGER.debug(
                        "A change to one or more classes requires a browser page reload, but BrowserLiveReload is not available. "
                                + "Please reload the browser page manually to make changes effective.");
            } else if (forceReload) {
                LOGGER.debug(
                        "Triggering browser live reload because of classes changes");
                liveReload.reload();
            } else {
                LOGGER.debug(
                        "Triggering browser live refresh because of classes changes");
                liveReload.refresh(true);
            }
        } else {
            LOGGER.debug(
                    "Triggering re-navigation to current route for UIs affected by classes changes.");
            for (UIRefreshStrategy action : uisToRefresh.keySet()) {
                String triggerEventJS = String.format(
                        "window.dispatchEvent(new CustomEvent(\"vaadin-refresh-ui\", { detail: { fullRefresh: %s }}));",
                        action == UIRefreshStrategy.PUSH_REFRESH_CHAIN);
                uisToRefresh.get(action).forEach(ui -> ui
                        .access(() -> ui.getPage().executeJs(triggerEventJS)));
            }
        }
    }

    private static Class<?> resolveClass(String className) {
        try {
            return Class.forName(className, false,
                    Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
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
        serviceDestroyed = true;
        sessions.clear();
    }

    @Override
    public void uiInit(UIInitEvent event) {
        UI ui = event.getUI();
        sessions.add(ui.getSession());
        ui.getPage().executeJs(
                """
                        const $wnd = window;
                        window.addEventListener('vaadin-refresh-ui', (ev) => {
                            const senderFn = $wnd.Vaadin?.Flow?.clients[$0]?.sendEventMessage;
                            if (senderFn) {
                                senderFn(1, "ui-refresh", ev.detail);
                            }
                        });
                        """,
                ui.getInternals().getAppId());
    }

    /**
     * Register the hotwsapper entry point for the given {@link VaadinService}.
     * <p>
     *
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
            vaadinService.addUIInitListener(hotswapper);
            vaadinService.addSessionInitListener(hotswapper);
            vaadinService.addSessionDestroyListener(hotswapper);
            vaadinService.addServiceDestroyListener(hotswapper);
            return Optional.of(hotswapper);
        }
        return Optional.empty();
    }

    /**
     * Instructs the {@link Hotswapper} if a full page reload should always be
     * triggered instead of detecting the best UI refresh strategy.
     *
     * @param vaadinService
     *            the {@link VaadinService} instance.
     * @param forceReload
     *            {@literal true} to always force a page reload,
     *            {@literal false} to let the {@link Hotswapper} decide the
     *            refresh strategy.
     */
    public static void forcePageReload(VaadinService vaadinService,
            boolean forceReload) {
        Objects.requireNonNull(vaadinService, "VaadinService cannot be null");
        getForceReloadHolder(vaadinService).activate(forceReload);
    }

    /**
     * Gets whether a forced full page reload is triggered on class changes.
     *
     * @param vaadinService
     *            the {@link VaadinService} instance.
     * @return {@literal true} if full page reload if forced, otherwise
     *         {@literal false}.
     */
    public static boolean isForcedPageReload(VaadinService vaadinService) {
        return getForceReloadHolder(vaadinService).isActive();
    }

    private static ForcePageReloadHolder getForceReloadHolder(
            VaadinService vaadinService) {
        return vaadinService.getContext().getAttribute(
                ForcePageReloadHolder.class, ForcePageReloadHolder::new);
    }

    private static class ForcePageReloadHolder implements Serializable {

        private final AtomicBoolean forceReload = new AtomicBoolean(false);

        void activate(boolean active) {
            forceReload.set(active);
        }

        boolean isActive() {
            return forceReload.get();
        }

        boolean shouldReloadPage() {
            return forceReload.get()
                    || Boolean.getBoolean(FORCE_RELOAD_PROPERTY);
        }
    }

}
