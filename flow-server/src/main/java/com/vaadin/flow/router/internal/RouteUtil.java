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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.internal.menu.MenuRegistry;
import com.vaadin.flow.router.DefaultRoutePathProvider;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.RouteBaseData;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RouteData;
import com.vaadin.flow.router.RoutePathProvider;
import com.vaadin.flow.router.RoutePrefix;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.AbstractConfiguration;
import com.vaadin.flow.server.AmbiguousRouteConfigurationException;
import com.vaadin.flow.server.InvalidRouteConfigurationException;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.SessionRouteRegistry;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.menu.AvailableViewInfo;

/**
 * Utility class with methods for route handling.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 1.3
 */
public class RouteUtil {

    private static final String ROUTE_CONFLICT_PREFIX = "Navigation target paths (considering @Route, @RouteAlias and @RoutePrefix values) must be unique, found navigation targets '%s' and '%s' ";
    public static final String ROUTE_CONFLICT = ROUTE_CONFLICT_PREFIX
            + "having the same route.";
    public static final String ROUTE_CONFLICT_WITH_PARAMS = ROUTE_CONFLICT_PREFIX
            + "with parameter having the same route.";

    protected RouteUtil() {
    }

    /**
     * Get parent layouts for navigation target according to the {@link Route}
     * or {@link RouteAlias} annotation.
     *
     * @param context
     *            a Vaadin context
     * @param component
     *            navigation target to get parents for
     * @param path
     *            path used to get navigation target so we know which annotation
     *            to handle
     * @return parent layouts for target
     */
    public static List<Class<? extends RouterLayout>> getParentLayouts(
            VaadinContext context, Class<?> component, String path) {
        final List<Class<? extends RouterLayout>> list = new ArrayList<>();

        Optional<Route> route = AnnotationReader.getAnnotationFor(component,
                Route.class);

        if (route.isPresent() && path.equals(getRoutePath(context, component))
                && !route.get().layout().equals(UI.class)) {
            list.addAll(collectRouteParentLayouts(route.get().layout()));
        } else {
            List<RouteAlias> routeAliases = AnnotationReader
                    .getAnnotationsFor(component, RouteAlias.class);

            Optional<RouteAlias> matchingRoute = getMatchingRouteAlias(
                    component, path, routeAliases);
            if (matchingRoute.isPresent()) {
                list.addAll(collectRouteParentLayouts(
                        matchingRoute.get().layout()));
            }
        }

        return list;
    }

    /**
     * Get parent layouts for navigation target according to the {@link Route}
     * or {@link RouteAlias} annotation or automatically link RouterLayout
     * annotated with the {@link Layout} annotation matching route path.
     *
     * @param handledRegistry
     *            current routeRegistry
     * @param component
     *            navigation target to get parents for
     * @param path
     *            path used to get navigation target so we know which annotation
     *            to handle
     * @return parent layouts for target
     */
    public static List<Class<? extends RouterLayout>> getParentLayouts(
            RouteRegistry handledRegistry, Class<?> component, String path) {
        final List<Class<? extends RouterLayout>> list = new ArrayList<>();

        Optional<Route> route = AnnotationReader.getAnnotationFor(component,
                Route.class);

        boolean hasRouteAndPathMatches = route.isPresent() && path
                .equals(getRoutePath(handledRegistry.getContext(), component));

        if (hasRouteAndPathMatches && !route.get().layout().equals(UI.class)) {
            list.addAll(collectRouteParentLayouts(route.get().layout()));
        } else {
            List<RouteAlias> routeAliases = AnnotationReader
                    .getAnnotationsFor(component, RouteAlias.class);

            Optional<RouteAlias> matchingRoute = getMatchingRouteAlias(
                    component, path, routeAliases);
            if (matchingRoute.isPresent()) {
                list.addAll(collectRouteParentLayouts(
                        matchingRoute.get().layout()));
            }
        }

        return list;
    }

    /**
     * Get the actual route path including all parent layout
     * {@link RoutePrefix}.
     *
     * @param context
     *            a Vaadin context
     * @param component
     *            navigation target component to get route path for
     * @return actual path for given route target
     */
    public static String getRoutePath(VaadinContext context,
            Class<?> component) {
        Route route = component.getAnnotation(Route.class);
        String routePath = resolve(context, component);
        if (route.absolute()) {
            return routePath;
        }
        List<String> parentRoutePrefixes = getRoutePrefixes(component,
                route.layout(), routePath);
        return parentRoutePrefixes.stream().collect(Collectors.joining("/"));
    }

    /**
     * Get the actual route path including all parent layout
     * {@link RoutePrefix}.
     *
     * @param component
     *            navigation target component to get route path for
     * @param alias
     *            route alias annotation to check
     * @return actual path for given route alias target
     */
    public static String getRouteAliasPath(Class<?> component,
            RouteAlias alias) {
        if (alias.absolute()) {
            return alias.value();
        }
        List<String> parentRoutePrefixes = getRoutePrefixes(component,
                alias.layout(), alias.value());
        return parentRoutePrefixes.stream().collect(Collectors.joining("/"));
    }

    private static List<String> getRoutePrefixes(Class<?> component,
            final Class<? extends RouterLayout> layout, final String value) {
        List<String> parentRoutePrefixes = getParentRoutePrefixes(component,
                () -> layout);
        Collections.reverse(parentRoutePrefixes);
        if (value != null && !value.isEmpty()) {
            parentRoutePrefixes.add(value);
        }

        return parentRoutePrefixes;
    }

    private static List<String> getParentRoutePrefixes(Class<?> component,
            Supplier<Class<? extends RouterLayout>> routerLayoutSupplier) {
        List<String> list = new ArrayList<>();

        Optional<ParentLayout> parentLayout = AnnotationReader
                .getAnnotationFor(component, ParentLayout.class);
        Optional<RoutePrefix> routePrefix = AnnotationReader
                .getAnnotationFor(component, RoutePrefix.class);

        routePrefix.ifPresent(prefix -> list.add(prefix.value()));

        // break chain on an absolute RoutePrefix or Route
        if (routePrefix.isPresent() && routePrefix.get().absolute()) {
            return list;
        }

        Class<? extends RouterLayout> routerLayout = routerLayoutSupplier.get();
        if (routerLayout != null && !routerLayout.equals(UI.class)) {
            list.addAll(getParentRoutePrefixes(routerLayout, () -> null));
        } else if (parentLayout.isPresent()) {
            list.addAll(getParentRoutePrefixes(parentLayout.get().value(),
                    () -> null));
        }

        return list;
    }

    static Optional<RouteAlias> getMatchingRouteAlias(Class<?> component,
            String path, List<RouteAlias> routeAliases) {
        return routeAliases.stream().filter(
                alias -> path.equals(getRouteAliasPath(component, alias))
                        && !alias.layout().equals(UI.class))
                .findFirst();
    }

    /**
     * Collects all parent layouts for a given route layout class.
     *
     * @param layout
     *            the layout class for which the parent layouts are collected.
     * @return a list of all parent layout classes starting from the given
     *         layout and including all ancestors in the hierarchy.
     */
    public static List<Class<? extends RouterLayout>> collectRouteParentLayouts(
            Class<? extends RouterLayout> layout) {
        List<Class<? extends RouterLayout>> layouts = new ArrayList<>();
        layouts.add(layout);

        Optional<ParentLayout> parentLayout = AnnotationReader
                .getAnnotationFor(layout, ParentLayout.class);
        if (parentLayout.isPresent()) {
            layouts.addAll(
                    collectRouteParentLayouts(parentLayout.get().value()));
        }
        return layouts;
    }

    /**
     * Collect possible route parent layouts for a navigation target that is not
     * annotated with {@link Route} nor {@link RouteAlias}, but may still
     * contain {@link ParentLayout}. Mainly error navigation targets.
     *
     * @param navigationTarget
     *            route to check parent layouts for
     * @return list of parent layouts
     */
    public static List<Class<? extends RouterLayout>> getParentLayoutsForNonRouteTarget(
            Class<?> navigationTarget) {
        List<Class<? extends RouterLayout>> layouts = new ArrayList<>();

        Optional<ParentLayout> parentLayout = AnnotationReader
                .getAnnotationFor(navigationTarget, ParentLayout.class);
        if (parentLayout.isPresent()) {
            layouts.addAll(
                    collectRouteParentLayouts(parentLayout.get().value()));
        }
        return layouts;
    }

    /**
     * Get the top most parent layout for navigation target according to the
     * {@link Route} or {@link RouteAlias} annotation. Also handles non route
     * targets with {@link ParentLayout}.
     *
     * @param context
     *            a Vaadin context
     * @param component
     *            navigation target to get top most parent for
     * @param path
     *            path used to get navigation target so we know which annotation
     *            to handle or null for error views.
     * @return top parent layout for target or null if none found
     */
    public static Class<? extends RouterLayout> getTopParentLayout(
            VaadinContext context, final Class<?> component,
            final String path) {
        if (path == null) {
            Optional<ParentLayout> parentLayout = AnnotationReader
                    .getAnnotationFor(component, ParentLayout.class);
            if (parentLayout.isPresent()) {
                return recurseToTopLayout(parentLayout.get().value());
            }
            // No need to check for Route or RouteAlias as the path is null
            return null;
        }

        Optional<Route> route = AnnotationReader.getAnnotationFor(component,
                Route.class);
        List<RouteAlias> routeAliases = AnnotationReader
                .getAnnotationsFor(component, RouteAlias.class);
        if (route.isPresent() && path.equals(getRoutePath(context, component))
                && !route.get().layout().equals(UI.class)) {
            return recurseToTopLayout(route.get().layout());
        } else {
            Optional<RouteAlias> matchingRoute = getMatchingRouteAlias(
                    component, path, routeAliases);
            if (matchingRoute.isPresent()) {
                return recurseToTopLayout(matchingRoute.get().layout());
            }
        }

        return null;
    }

    private static Class<? extends RouterLayout> recurseToTopLayout(
            Class<? extends RouterLayout> layout) {
        Optional<ParentLayout> parentLayout = AnnotationReader
                .getAnnotationFor(layout, ParentLayout.class);

        if (parentLayout.isPresent()) {
            return recurseToTopLayout(parentLayout.get().value());
        }
        return layout;
    }

    /**
     * Gets the effective route path value of the annotated class.
     *
     * @param context
     *            a Vaadin context
     * @param component
     *            the component where the route points to
     * @return The value of the annotation or naming convention based value if
     *         no explicit value is given.
     */
    public static String resolve(VaadinContext context, Class<?> component) {
        RoutePathProvider provider = null;
        Lookup lookup = context.getAttribute(Lookup.class);
        if (lookup != null) {
            provider = lookup.lookup(RoutePathProvider.class);
            assert provider != null;
        }
        if (provider == null) {
            // This is needed especially in unit tests when no Lookup instance
            // is available
            provider = new DefaultRoutePathProvider();
        }
        return provider.getRoutePath(component);
    }

    /**
     * Updates route registry as necessary when classes have been added /
     * modified / deleted.
     * <p>
     *
     * Registry Update rules:
     * <ul>
     * <li>a route is preserved if the class does not have a {@link Route}
     * annotation and did not have it at registration time</li>
     * <li>a route is preserved if the class is annotated with {@link Route} and
     * {@code registerAtStartup=false} and the the flag has not changed</li>
     * <li>new classes are not automatically added to session registries</li>
     * <li>existing routes in session registries are not removed in case of
     * class modification</li>
     * </ul>
     *
     * @param registry
     *            route registry
     * @param addedClasses
     *            added classes
     * @param modifiedClasses
     *            modified classes
     * @param deletedClasses
     *            deleted classes
     */
    public static void updateRouteRegistry(RouteRegistry registry,
            Set<Class<?>> addedClasses, Set<Class<?>> modifiedClasses,
            Set<Class<?>> deletedClasses) {

        if ((addedClasses == null || addedClasses.isEmpty())
                && (modifiedClasses == null || modifiedClasses.isEmpty())
                && (deletedClasses == null || deletedClasses.isEmpty())) {
            // No changes to apply
            return;
        }
        Logger logger = LoggerFactory.getLogger(RouteUtil.class);

        // safe copy to prevent concurrent modification or operation failures on
        // immutable sets
        modifiedClasses = modifiedClasses != null
                ? new HashSet<>(modifiedClasses)
                : new HashSet<>();
        addedClasses = addedClasses != null ? new HashSet<>(addedClasses)
                : new HashSet<>();
        deletedClasses = deletedClasses != null ? new HashSet<>(deletedClasses)
                : new HashSet<>();

        // the same class may be present on more than one input sets depending
        // on how IDE and agent collect file change events.
        // A modified call takes over added and deleted.
        if (!modifiedClasses.isEmpty()) {
            addedClasses.removeIf(modifiedClasses::contains);
            deletedClasses.removeIf(modifiedClasses::contains);
        }

        RouteConfiguration routeConf = RouteConfiguration.forRegistry(registry);

        // collect classes for that are no more Flow components and should be
        // removed from the registry
        // NOTE: not all agents/JVMs support reload on class hierarchy change
        Set<Class<?>> nonFlowComponentsToRemove = new HashSet<>();
        deletedClasses.stream()
                .filter(clazz -> !Component.class.isAssignableFrom(clazz))
                .forEach(nonFlowComponentsToRemove::add);
        modifiedClasses.stream()
                .filter(clazz -> !Component.class.isAssignableFrom(clazz))
                .forEach(nonFlowComponentsToRemove::add);
        Set<Class<? extends RouterLayout>> layouts = new HashSet<>();

        boolean isSessionRegistry = registry instanceof SessionRouteRegistry;
        Predicate<Class<? extends Component>> modifiedClassesRouteRemovalFilter = clazz -> !isSessionRegistry;

        if (registry instanceof AbstractRouteRegistry abstractRouteRegistry) {

            // update layouts
            filterLayoutClasses(deletedClasses).forEach(layouts::add);
            filterLayoutClasses(modifiedClasses).forEach(layouts::add);
            filterLayoutClasses(addedClasses).forEach(layouts::add);
            layouts.forEach(abstractRouteRegistry::updateLayout);
            if (!layouts.isEmpty()) {
                // Gather routes that don't have a layout or reference a layout
                // that has been changed.
                // Mark these routes as modified so they can be re-registered
                // with the correct layouts applied.
                registry.getRegisteredRoutes().stream()
                        .filter(rd -> rd.getParentLayouts().isEmpty()
                                || rd.getParentLayouts().stream()
                                        .anyMatch(layouts::contains))
                        .map(RouteBaseData::getNavigationTarget)
                        .forEach(modifiedClasses::add);
            }

            Map<String, RouteTarget> routesMap = abstractRouteRegistry
                    .getConfiguration().getRoutesMap();
            Map<? extends Class<? extends Component>, RouteTarget> routeTargets = registry
                    .getRegisteredRoutes().stream()
                    .map(routeData -> routesMap.get(routeData.getTemplate()))
                    .filter(Objects::nonNull).collect(Collectors.toMap(
                            RouteTarget::getTarget, Function.identity()));

            modifiedClassesRouteRemovalFilter = modifiedClassesRouteRemovalFilter
                    .and(clazz -> {
                        RouteTarget routeTarget = routeTargets.get(clazz);
                        if (routeTarget == null) {
                            return true;
                        }
                        boolean wasAnnotatedRoute = routeTarget
                                .isAnnotatedRoute();
                        boolean wasRegisteredAtStartup = routeTarget
                                .isRegisteredAtStartup();
                        boolean isAnnotatedRoute = clazz
                                .isAnnotationPresent(Route.class);
                        boolean isRegisteredAtStartup = isAnnotatedRoute
                                && clazz.getAnnotation(Route.class)
                                        .registerAtStartup();
                        if (!isAnnotatedRoute && !wasAnnotatedRoute) {
                            // route was previously registered manually, do not
                            // remove it
                            return false;
                        }
                        if (isAnnotatedRoute && wasAnnotatedRoute
                                && !isRegisteredAtStartup
                                && !wasRegisteredAtStartup) {
                            // a lazy annotated route has changed, but it was
                            // previously registered manually, do not remove it
                            return false;
                        }
                        return !isAnnotatedRoute || !isRegisteredAtStartup;
                    });
        }
        Stream<Class<? extends Component>> toRemove = Stream
                .concat(filterComponentClasses(deletedClasses),
                        filterComponentClasses(modifiedClasses)
                                .filter(modifiedClassesRouteRemovalFilter))
                .distinct();

        Stream<Class<? extends Component>> toAdd;
        if (isSessionRegistry) {
            // routes on session registry are initialized programmatically so
            // new classes should never be added automatically
            toAdd = Stream.empty();
        } else {
            // New classes should be added to the registry only if they have a
            // @Route annotation with registerAtStartup=true
            toAdd = Stream
                    .concat(filterComponentClasses(addedClasses),
                            filterComponentClasses(modifiedClasses))
                    .filter(clazz -> clazz.isAnnotationPresent(Route.class)
                            && clazz.getAnnotation(Route.class)
                                    .registerAtStartup())
                    .distinct();
        }

        registry.update(() -> {
            // remove potential routes for classes that are not Flow
            // components anymore
            nonFlowComponentsToRemove
                    .forEach(clazz -> routeConf.removeRoute((Class) clazz));
            // remove deleted classes and classes that lost the annotation from
            // registry
            toRemove.forEach(componentClass -> {
                logger.debug("Removing route to {}", componentClass);
                routeConf.removeRoute(componentClass);
            });
            // add new routes to registry
            toAdd.forEach(componentClass -> {
                logger.debug("Updating route {} to {}",
                        componentClass.getAnnotation(Route.class).value(),
                        componentClass);
                routeConf.removeRoute(componentClass);
                routeConf.setAnnotatedRoute(componentClass);
            });
        });
    }

    @SuppressWarnings("unchecked")
    private static Stream<Class<? extends RouterLayout>> filterLayoutClasses(
            Set<Class<?>> classes) {
        return filterComponentClasses(classes)
                .filter(RouterLayout.class::isAssignableFrom)
                .map(clazz -> (Class<? extends RouterLayout>) clazz);
    }

    @SuppressWarnings("unchecked")
    private static Stream<Class<? extends Component>> filterComponentClasses(
            Set<Class<?>> classes) {
        return classes.stream().filter(Component.class::isAssignableFrom)
                .map(clazz -> (Class<? extends Component>) clazz);
    }

    /**
     * Check if given route can get the automatic layout. Automatic layout can
     * be used if it is a {@link Route} with no {@link Route#layout()} set and
     * {@link Route#autoLayout()} as true.
     *
     * @param target
     *            target to check for accessibility
     * @param path
     *            path to determine if we are targeting a {@link RouteAlias}
     *            instead of {@link Route}
     * @return {@code true} if auto layout can be used
     */
    public static boolean isAutolayoutEnabled(Class<?> target, String path) {
        if (target.isAnnotationPresent(RouteAlias.class)
                || target.isAnnotationPresent(RouteAlias.Container.class)) {
            for (RouteAlias alias : target
                    .getAnnotationsByType(RouteAlias.class)) {
                String aliasPath = RouteUtil.getRouteAliasPath(target, alias);
                String trimmedTemplate = PathUtil
                        .trimPath(HasUrlParameterFormat.getTemplate(aliasPath,
                                (Class<? extends Component>) target));
                RouteModel routeModel = RouteModel.create(true);
                routeModel.addRoute(trimmedTemplate,
                        new RouteTarget((Class<? extends Component>) target));
                NavigationRouteTarget navigationRouteTarget = routeModel
                        .getNavigationRouteTarget(path);
                if (navigationRouteTarget.hasTarget()) {
                    return alias.autoLayout()
                            && alias.layout().equals(UI.class);
                }
            }

        }
        return target.isAnnotationPresent(Route.class)
                && target.getAnnotation(Route.class).autoLayout()
                && target.getAnnotation(Route.class).layout().equals(UI.class);

    }

    /**
     * Checks the given list of Flow routes for potential collisions with Hilla
     * routes.
     *
     * Note: Routes will only be checked in development mode, when Hilla is in
     * use.
     *
     * @param service
     *            VaadinService instance
     * @param flowRoutes
     *            Flow routes to check against
     * @throws InvalidRouteConfigurationException
     *             if a collision is detected
     */
    public static void checkForClientRouteCollisions(VaadinService service,
            List<RouteData> flowRoutes)
            throws InvalidRouteConfigurationException {
        checkForClientRouteCollisions(service, flowRoutes.stream()
                .map(RouteData::getTemplate).toArray(String[]::new));
    }

    /**
     * Checks the given array of Flow route templates for potential collisions
     * with Hilla routes.
     *
     * Note: Routes will only be checked in development mode, when Hilla is in
     * use.
     *
     * @param service
     *            VaadinService instance
     * @param flowRouteTemplates
     *            Flow routes to check against
     * @throws InvalidRouteConfigurationException
     *             if a collision is detected
     */
    public static void checkForClientRouteCollisions(VaadinService service,
            String... flowRouteTemplates)
            throws InvalidRouteConfigurationException {
        if (service == null
                || service.getDeploymentConfiguration().isProductionMode()
                || !FrontendUtils.isHillaUsed(service
                        .getDeploymentConfiguration().getFrontendFolder())) {
            return;
        }

        List<String> collisions = MenuRegistry
                .collectClientMenuItems(false,
                        service.getDeploymentConfiguration())
                .entrySet().stream()
                .filter(entry -> entry.getValue().children() == null)
                .map(Map.Entry::getKey).map(PathUtil::trimPath)
                .filter(clientRoute -> Arrays.stream(flowRouteTemplates)
                        .map(PathUtil::trimPath).anyMatch(clientRoute::equals))
                .toList();
        if (!collisions.isEmpty()) {
            String msg = String.format(
                    "Invalid route configuration. The following Hilla "
                            + "route(s) conflict with configured Flow routes: '%s'. "
                            + "Please double-check the Hilla and Flow routes path and resolve the conflicts. "
                            + "If the error persists try to delete the %s folder.",
                    String.join("', '", collisions),
                    FrontendUtils.getFrontendGeneratedFolder(service
                            .getDeploymentConfiguration().getFrontendFolder()));
            throw new InvalidRouteConfigurationException(msg);
        }
    }

    /**
     * Check if the given registry has any auto layouts added
     * with @{@link Layout} annotation.
     *
     * @param registry
     *            the registry to check
     * @return {@code true} if the registry has any auto layouts
     */
    public static boolean hasAutoLayout(AbstractRouteRegistry registry) {
        return !registry.getLayouts().isEmpty();
    }

    /**
     * Check if currently registered client routes use auto layout based on
     * {@link AvailableViewInfo#flowLayout()}.
     *
     * @param configuration
     *            deployment configuration
     * @return {@code true} if any client route has auto layout
     */
    public static boolean hasClientRouteWithAutoLayout(
            AbstractConfiguration configuration) {
        return MenuRegistry.collectClientMenuItems(false, configuration)
                .values().stream().anyMatch(AvailableViewInfo::flowLayout);
    }

    /**
     * Check if the given registry has any routes using auto layout.
     *
     * @param registry
     *            the registry to check
     * @return {@code true} if the registry has any auto layouts
     */
    public static boolean hasServerRouteWithAutoLayout(
            AbstractRouteRegistry registry) {
        Collection<?> layouts = registry.getLayouts();
        return registry.getRegisteredRoutes().stream().anyMatch(routeData -> {
            String path;
            if (routeData.getNavigationTarget()
                    .getAnnotation(Route.class) != null) {
                path = getRoutePath(registry.getContext(),
                        routeData.getNavigationTarget());
            } else {
                path = resolve(registry.getContext(),
                        routeData.getNavigationTarget());
                List<String> parentRoutePrefixes = getRoutePrefixes(
                        routeData.getNavigationTarget(), null, path);
                path = String.join("/", parentRoutePrefixes);
            }
            return RouteUtil
                    .isAutolayoutEnabled(routeData.getNavigationTarget(), path)
                    && registry.hasLayout(path)
                    && collectRouteParentLayouts(registry.getLayout(path))
                            .stream().anyMatch(layouts::contains);
        });
    }

    /**
     * Get optional dynamic page title from the active router targets chain of a
     * given UI instance.
     *
     * @param ui
     *            instance of UI, not {@code null}
     * @return dynamic page title found in the routes chain, or empty optional
     *         if no implementor of {@link HasDynamicTitle} was found
     */
    public static Optional<String> getDynamicTitle(UI ui) {
        return Objects.requireNonNull(ui).getInternals()
                .getActiveRouterTargetsChain().stream()
                .filter(HasDynamicTitle.class::isInstance)
                .map(element -> ((HasDynamicTitle) element).getPageTitle())
                .filter(Objects::nonNull).findFirst();
    }

    /**
     * Search for a client route using given navigation url and return target
     * template.
     *
     * @param url
     *            the navigation url used to search a route target.
     *
     * @return a {@link Optional} containing the template of the client route
     *         target or an empty {@link Optional}.
     */
    public static Optional<String> getClientNavigationRouteTargetTemplate(
            String url) {
        if (url == null) {
            return Optional.empty();
        }
        RouteModel routeModel = RouteModel.create(true);
        MenuRegistry.getClientRoutes(false).forEach((key, value) -> {
            try {
                routeModel.addRoute(key, new ClientTarget(key));
            } catch (AmbiguousRouteConfigurationException tolerate) {
                // tolerate ambiguous routes. First added route wins and
                // declares returned template.
            }
        });
        url = url.isEmpty() ? url : url.startsWith("/") ? url : "/" + url;
        return Optional.ofNullable(routeModel.getNavigationRouteTarget(url))
                .map(NavigationRouteTarget::getRouteTarget)
                .map(ClientTarget.class::cast).map(ClientTarget::getTemplate);
    }
}
