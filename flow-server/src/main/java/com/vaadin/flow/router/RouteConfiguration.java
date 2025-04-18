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
package com.vaadin.flow.router;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.router.internal.AbstractRouteRegistry;
import com.vaadin.flow.router.internal.BeforeEnterHandler;
import com.vaadin.flow.router.internal.HasUrlParameterFormat;
import com.vaadin.flow.router.internal.PathUtil;
import com.vaadin.flow.router.internal.RouteUtil;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.InvalidRouteConfigurationException;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.SessionRouteRegistry;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;
import com.vaadin.flow.shared.Registration;

/**
 * Route configuration helper class for adding, removing and reading routes from
 * the different registries.
 *
 * @since 1.3
 */
public class RouteConfiguration implements Serializable {

    private RouteRegistry handledRegistry;

    private RouteConfiguration(RouteRegistry registry) {
        handledRegistry = registry;
    }

    /**
     * Get a {@link RouteConfiguration} that edits the session scope routes.
     * This requires that {@link VaadinSession#getCurrent()} is populated.
     * <p>
     * Note! Session scoped registry sees also the application scope routes.
     *
     * @return configurator for session scope routes
     */
    public static RouteConfiguration forSessionScope() {
        return new RouteConfiguration(getSessionRegistry());
    }

    /**
     * Get a {@link RouteConfiguration} that edits the application scope routes.
     * This requires that {@link VaadinServlet#getCurrent()} is populated.
     *
     * @return configurator for application scope routes
     */
    public static RouteConfiguration forApplicationScope() {
        return new RouteConfiguration(getApplicationRegistry());
    }

    /**
     * Get a {@link RouteConfiguration} for editing the given RouteRegistry
     * implementation. This enables editing of registry when the required
     * {@link CurrentInstance} is not yet populated.
     *
     * @param registry
     *            registry to edit through the controller
     * @return configurator for editing given registry
     */
    public static RouteConfiguration forRegistry(RouteRegistry registry) {
        return new RouteConfiguration(registry);
    }

    /* Static getters for getting information on registered routes */

    /**
     * Get the {@link RouteData} for all registered navigation targets.
     * <p>
     * Note! This would be best to request for session scope registry as it will
     * then contain the actual currently visible routes from both the session
     * and application scopes.
     * <p>
     * Note! Size of the list is only main routes as RouteData will contain a
     * list of alias route registrations.
     *
     * @return list of all routes available
     */
    public List<RouteData> getAvailableRoutes() {
        return handledRegistry.getRegisteredRoutes();
    }

    /**
     * Check if the given path is available.
     *
     * @param path
     *            path to check for availability
     * @return true if there exists a route for the given path
     */
    public boolean isPathAvailable(String path) {
        if (handledRegistry instanceof AbstractRouteRegistry) {
            return ((AbstractRouteRegistry) handledRegistry).getConfiguration()
                    .hasTemplate(path);
        }
        return getAvailableRoutes().stream()
                .anyMatch(routeData -> routeData.getTemplate().equals(path)
                        || routeData.getRouteAliases().stream()
                                .anyMatch(routeAliasData -> routeAliasData
                                        .getTemplate().equals(path)));
    }

    /**
     * Check if the route is available as a registered target.
     *
     * @param route
     *            target class to check for registration
     * @return true if class is registered
     */
    public boolean isRouteRegistered(Class<? extends Component> route) {
        return handledRegistry.getTemplate(route).isPresent();
    }

    /**
     * Gets the registered route class for a given path. Returns an empty
     * optional if no navigation target corresponds to the given path.
     *
     * @param path
     *            path to get route for
     * @return optional containing the path component or empty if not found
     */
    public Optional<Class<? extends Component>> getRoute(String path) {
        return getRoute(path, Collections.emptyList());
    }

    /**
     * Gets the optional navigation target class for a given Location matching
     * with path segments.
     *
     * @param path
     *            path to get navigation target for, not {@code null}
     * @param segments
     *            segments given for path
     * @return optional navigation target corresponding to the given path and
     *         segments
     */
    public Optional<Class<? extends Component>> getRoute(String path,
            List<String> segments) {
        return handledRegistry
                .getNavigationTarget(PathUtil.getPath(path, segments));
    }

    /**
     * Add a listener that is notified when routes change for the registry.
     *
     * @param listener
     *            listener to add
     * @return registration for removing the listener
     */
    public Registration addRoutesChangeListener(
            RoutesChangedListener listener) {
        return handledRegistry.addRoutesChangeListener(listener);
    }

    /* edit methods */

    /**
     * Block updates to the registry configuration from other threads until
     * update command has completed.
     * <p>
     * Using this method makes the registry changes made inside the command
     * atomic for the registry as no one else can change the state during the
     * duration of the command.
     * <p>
     * Any other configuration thread for the same registry will be blocked
     * until all the update locks have been released.
     * <p>
     * Note! During an update other threads will get the pre-update state of the
     * registry until the update has fully completed.
     *
     * @param command
     *            command to execute for the update
     */
    public void update(Command command) {
        handledRegistry.update(command);
    }

    /**
     * Giving a navigation target here will handle the {@link Route} annotation
     * to get the path and also register any {@link RouteAlias} that may be on
     * the class.
     *
     * @param navigationTarget
     *            navigation target to register
     * @throws InvalidRouteConfigurationException
     *             thrown if exact route already defined in this scope
     */
    public void setAnnotatedRoute(Class<? extends Component> navigationTarget) {
        if (!navigationTarget.isAnnotationPresent(Route.class)) {
            String message = String.format(
                    "Given navigationTarget %s is missing the '@Route' annotation.",
                    navigationTarget.getName());
            throw new InvalidRouteConfigurationException(message);
        }
        String route = RouteUtil.getRoutePath(handledRegistry.getContext(),
                navigationTarget);
        handledRegistry.setRoute(route, navigationTarget, RouteUtil
                .getParentLayouts(handledRegistry, navigationTarget, route));

        for (RouteAlias alias : navigationTarget
                .getAnnotationsByType(RouteAlias.class)) {
            String path = RouteUtil.getRouteAliasPath(navigationTarget, alias);
            handledRegistry.setRoute(path, navigationTarget, RouteUtil
                    .getParentLayouts(handledRegistry, navigationTarget, path));
        }
    }

    /**
     * Register a navigation target on the specified path. Any
     * {@link ParentLayout} annotation on class will be used to populate layout
     * chain, but {@link Route} and {@link RouteAlias} will not be taken into
     * consideration.
     *
     * @param path
     *            path to register navigation target to
     * @param navigationTarget
     *            navigation target to register
     * @throws InvalidRouteConfigurationException
     *             thrown if exact route already defined in this scope
     */
    public void setParentAnnotatedRoute(String path,
            Class<? extends Component> navigationTarget) {
        handledRegistry.setRoute(path, navigationTarget,
                RouteUtil.getParentLayoutsForNonRouteTarget(navigationTarget));
    }

    /**
     * Register a navigation target with specified path and with no parent
     * layouts.
     * <p>
     * Note! Any {@link ParentLayout}, {@link Route} or {@link RouteAlias} will
     * be ignored in route handling.
     *
     * @param path
     *            path to register navigation target to
     * @param navigationTarget
     *            navigation target to register
     * @throws InvalidRouteConfigurationException
     *             thrown if exact route already defined in this scope
     */
    public void setRoute(String path,
            Class<? extends Component> navigationTarget) {
        setRoute(path, navigationTarget, Collections.emptyList());
    }

    /**
     * Register a navigation target with specified path and given parent layout
     * chain.
     * <p>
     * Note! Any {@link ParentLayout}, {@link Route} or {@link RouteAlias} will
     * be ignored in route handling.
     *
     * @param path
     *            path to register navigation target to
     * @param navigationTarget
     *            navigation target to register
     * @param parentChain
     *            chain of parent layouts that should be used with this target
     * @throws InvalidRouteConfigurationException
     *             thrown if exact route already defined in this scope
     */
    public void setRoute(String path,
            Class<? extends Component> navigationTarget,
            List<Class<? extends RouterLayout>> parentChain) {
        handledRegistry.setRoute(path, navigationTarget, parentChain);
    }

    /**
     * Register a navigation target with specified path and given parent layout
     * chain.
     * <p>
     * Note! Any {@link ParentLayout}, {@link Route} or {@link RouteAlias} will
     * be ignored in route handling.
     *
     * @param path
     *            path to register navigation target to
     * @param navigationTarget
     *            navigation target to register
     * @param parentChain
     *            chain of parent layouts that should be used with this target
     * @throws InvalidRouteConfigurationException
     *             thrown if exact route already defined in this scope
     */
    public void setRoute(String path,
            Class<? extends Component> navigationTarget,
            Class<? extends RouterLayout>... parentChain) {
        // This method is unchecked for the type due to varargs
        handledRegistry.setRoute(path, navigationTarget,
                Arrays.asList(parentChain));
    }

    /**
     * Remove the given navigation target route registration. Path where the
     * navigation target was may still be usable, e.g. we remove target with url
     * param and there is left a non param target, but will not return the
     * removed target.
     * <p>
     * Note! this will remove target route and if possible any
     * {@link RouteAlias} route that can be found for the class.
     *
     * @param navigationTarget
     *            navigation target class to remove
     */
    public void removeRoute(Class<? extends Component> navigationTarget) {
        handledRegistry.removeRoute(navigationTarget);
    }

    /**
     * Remove all registrations for given path. This means that any navigation
     * target registered on the given path will be removed. But if a removed
     * navigationTarget for the path exists it is then stored with a new main
     * path so it can still get a resolved url.
     * <p>
     * E.g. path "home" contains HomeView and DetailsView[String path param]
     * both will be removed.
     * <p>
     * Note! The restored path will be the first found match for all paths that
     * are registered.
     * <p>
     * In case navigationTarget is a
     * {@link com.vaadin.flow.router.HasUrlParameter}, path argument needs to
     * include the parameter placeholder which is added automatically.
     * Otherwise, using {@link #removeRoute(String, Class)} is preferred in such
     * a case.
     *
     * @param path
     *            path for which to remove all navigation targets
     */
    public void removeRoute(String path) {
        handledRegistry.removeRoute(path);
    }

    /**
     * Remove only the specified navigationTarget from the path and not other
     * targets if they exist for the same path.
     * <p>
     * Note! If another path exists for the removed navigation target it will
     * get a new main path so it can still get a resolved url. The restored path
     * will be the first found match for all paths that are registered.
     *
     * @param path
     *            path to remove from registry
     * @param navigationTarget
     *            path navigation target to remove
     */
    public void removeRoute(String path,
            Class<? extends Component> navigationTarget) {
        handledRegistry.removeRoute(path, navigationTarget);
    }

    /**
     * Get the registry that this configuration is working with.
     *
     * @return handled RouteRegistry
     */
    public RouteRegistry getHandledRegistry() {
        return handledRegistry;
    }

    /**
     * Get the registered url string for given navigation target.
     * <p>
     * Note! If the navigation target has a url parameter that is required then
     * this method will throw an IllegalArgumentException.
     *
     * @param navigationTarget
     *            navigation target to get url for
     * @return url for the navigation target
     * @throws IllegalArgumentException
     *             if the navigation target requires a parameter
     */
    public String getUrl(Class<? extends Component> navigationTarget) {
        return getUrl(navigationTarget, RouteParameters.empty());
    }

    /**
     * Return the url base without any route parameters.
     *
     * @param navigationTarget
     *            navigation target to get url for
     * @return optional url base without route parameters or empty if there is
     *         no registered route for {@code navigationTarget}, not
     *         {@code null}
     */
    public Optional<String> getUrlBase(
            Class<? extends Component> navigationTarget) {
        final Optional<String> template = getTemplate(navigationTarget);
        if (template.isPresent()) {
            return Optional
                    .of(HasUrlParameterFormat.getUrlBase(template.get()));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Gets the route template for the given target.
     *
     * @param navigationTarget
     *            target class.
     * @return main template for the given target.
     */
    public Optional<String> getTemplate(
            Class<? extends Component> navigationTarget) {
        return handledRegistry.getTemplate(navigationTarget);
    }

    /**
     * Get the url string for given navigation target with the parameter in the
     * url.
     * <p>
     * Note! Given parameter is checked for correct class type. This means that
     * if the navigation target defined parameter is of type {@code Boolean}
     * then calling getUrl with a {@code String} will fail.
     *
     * @param navigationTarget
     *            navigation target to get url for
     * @param parameter
     *            parameter to embed into the generated url
     * @param <T>
     *            url parameter type
     * @param <C>
     *            navigation target type
     * @return url for the navigation target with parameter
     */
    public <T, C extends Component & HasUrlParameter<T>> String getUrl(
            Class<? extends C> navigationTarget, T parameter) {
        return getUrl(navigationTarget,
                HasUrlParameterFormat.getParameters(parameter));
    }

    /**
     * Get the url string for given navigation target with the parameters in the
     * url.
     * <p>
     * Note! Given parameters are checked for correct class type. This means
     * that if the navigation target defined parameter is of type
     * {@code Boolean} then calling getUrl with a {@code String} will fail.
     *
     * @param navigationTarget
     *            navigation target to get url for
     * @param parameters
     *            parameters to embed into the generated url, not null
     * @param <T>
     *            url parameter type
     * @param <C>
     *            navigation target type
     * @return url for the navigation target with parameter
     */
    public <T, C extends Component & HasUrlParameter<T>> String getUrl(
            Class<? extends C> navigationTarget, List<T> parameters) {
        return getUrl(navigationTarget,
                HasUrlParameterFormat.getParameters(parameters));
    }

    /**
     * Gets the url which navigates to given navigationTarget using given
     * parameters.
     *
     * @param navigationTarget
     *            navigation target.
     * @param parameters
     *            route parameters.
     * @return the url which navigates to given navigationTarget using given
     *         parameters.
     * @throws NotFoundException
     *             in case the navigationTarget is not registered with a url
     *             template matching the given parameters.
     */
    public String getUrl(Class<? extends Component> navigationTarget,
            RouteParameters parameters) {

        Optional<String> targetUrl = handledRegistry
                .getTargetUrl(navigationTarget, parameters);
        if (!targetUrl.isPresent()) {
            throw new NotFoundException(String.format(
                    "No route found for the given navigation target '%s' and parameters '%s'",
                    navigationTarget.getName(), parameters.toString()));
        }
        return targetUrl.get();
    }

    /* Private methods */

    private static RouteRegistry getApplicationRegistry() {
        return ApplicationRouteRegistry
                .getInstance(VaadinService.getCurrent().getContext());
    }

    private static RouteRegistry getSessionRegistry() {
        return SessionRouteRegistry
                .getSessionRegistry(VaadinSession.getCurrent());
    }

    @SafeVarargs
    private final boolean isAnnotatedParameter(
            Class<? extends Component> navigationTarget,
            Class<? extends Annotation>... parameterAnnotations) {
        for (Class<? extends Annotation> annotation : parameterAnnotations) {
            if (ParameterDeserializer.isAnnotatedParameter(navigationTarget,
                    annotation)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the {@link RouteData} for all accessible registered navigation
     * targets with a menu information. Access checking depends on the active
     * {@link VaadinService} and {@link VaadinRequest}.
     * <p>
     * Automatically adds access controls from UI if available.
     *
     * @return list of accessible menu routes available for handled registry
     */
    public List<RouteData> getRegisteredAccessibleMenuRoutes() {
        UI ui = UI.getCurrent();
        if (ui != null) {
            List<BeforeEnterListener> accessControls = ui.getInternals()
                    .getListeners(BeforeEnterHandler.class).stream()
                    .filter(BeforeEnterListener.class::isInstance)
                    .map(BeforeEnterListener.class::cast).toList();
            return getRegisteredAccessibleMenuRoutes(accessControls);
        }

        return getRegisteredAccessibleMenuRoutes(Collections.emptyList());
    }

    /**
     * Get the {@link RouteData} for all accessible registered navigation
     * targets with a menu information. Access checking depends on the active
     * {@link VaadinService} and {@link VaadinRequest} and the given collection
     * of access controls.
     *
     * @param accessControls
     *            the access controls to use for checking access
     * @return list of accessible menu routes available for handled registry
     */
    public List<RouteData> getRegisteredAccessibleMenuRoutes(
            Collection<BeforeEnterListener> accessControls) {
        return getHandledRegistry().getRegisteredAccessibleMenuRoutes(
                VaadinRequest.getCurrent(), accessControls);
    }
}
