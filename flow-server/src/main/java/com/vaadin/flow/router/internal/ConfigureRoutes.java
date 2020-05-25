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

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.RouterLayout;

/**
 * Configuration class for editing routes. After editing the class should always
 * be set as a {@link ConfiguredRoutes} read only value object.
 * <p>
 * {@link ConfigureRoutes} is always mutable where as {@link ConfiguredRoutes}
 * is always immutable.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 1.3
 */
public class ConfigureRoutes extends ConfiguredRoutes implements Serializable {

    // Stores targets accessed by urls with parameters.
    private final RouteModel routeModel;

    private final Map<String, RouteTarget> routeMap;
    private final Map<Class<? extends Component>, String> targetRouteMap;
    private final Map<Class<? extends Component>, RouteModel> targetRouteModelMap;
    private final Map<Class<? extends Exception>, Class<? extends Component>> exceptionTargetMap;

    /**
     * Create an immutable RouteConfiguration.
     */
    public ConfigureRoutes() {
        routeModel = RouteModel.create(true);
        routeMap = new HashMap<>();
        targetRouteMap = new HashMap<>();
        targetRouteModelMap = new HashMap<>();
        exceptionTargetMap = new HashMap<>();
    }

    /**
     * Create a mutable or immutable configuration with original configuration
     * information.
     *
     * @param original
     *            original configuration to get data from
     */
    public ConfigureRoutes(ConfiguredRoutes original) {
        Map<String, RouteTarget> originalRouteMap = new HashMap<>();
        Map<Class<? extends Component>, String> originalTargetRouteMap = new HashMap<>();
        Map<Class<? extends Exception>, Class<? extends Component>> originalExceptionTargetsMap = new HashMap<>();

        for (Map.Entry<String, RouteTarget> route : original.getRoutesMap()
                .entrySet()) {
            originalRouteMap.put(route.getKey(), route.getValue());
        }
        originalTargetRouteMap.putAll(original.getTargetRoutes());
        originalExceptionTargetsMap.putAll(original.getExceptionHandlers());

        Map<Class<? extends Component>, RouteModel> originalTargetRouteModelMap = original
                .copyTargetRouteModels(true);

        this.routeModel = RouteModel.copy(original.getRouteModel(), true);

        this.routeMap = originalRouteMap;
        this.targetRouteMap = originalTargetRouteMap;
        this.targetRouteModelMap = originalTargetRouteModelMap;
        this.exceptionTargetMap = originalExceptionTargetsMap;
    }

    /**
     * Override so that the getters use the correct routes map for data.
     *
     * @return editable map of routes
     */
    @Override
    protected Map<String, RouteTarget> getRoutesMap() {
        return routeMap;
    }

    @Override
    RouteModel getRouteModel() {
        return routeModel;
    }

    /**
     * Override so that the getters use the correct target routes map for data.
     *
     * @return editable map of targetRoutes
     */
    @Override
    public Map<Class<? extends Component>, String> getTargetRoutes() {
        return targetRouteMap;
    }

    /**
     * Override so that the getters use the correct exception targets map for
     * data.
     *
     * @return editable map of exception targets
     */
    @Override
    public Map<Class<? extends Exception>, Class<? extends Component>> getExceptionHandlers() {
        return exceptionTargetMap;
    }

    /*-----------------------------------*/
    /* Mutation functions */
    /*-----------------------------------*/

    /**
     * Clear all maps from this configuration.
     */
    public void clear() {
        getRoutesMap().clear();
        getTargetRoutes().clear();
    }

    /**
     * Set a new {@link RouteTarget} for the given template.
     * <p>
     * Note! this will override any previous value.
     *
     * @param template
     *            template for which to set route target for
     * @param navigationTarget
     *            navigation target to add
     */
    public void setRoute(String template,
            Class<? extends Component> navigationTarget) {
        setRoute(template, navigationTarget, null);
    }

    /**
     * Set a new {@link RouteTarget} for the given template.
     * <p>
     * Note! this will override any previous value.
     *
     * @param template
     *            template for which to set route target for
     * @param navigationTarget
     *            navigation target to add
     * @param parentChain
     *            chain of parent layouts that should be used with this target
     */
    public void setRoute(String template,
            Class<? extends Component> navigationTarget,
            List<Class<? extends RouterLayout>> parentChain) {

        template = PathUtil.trimPath(template);

        final RouteTarget target = new RouteTarget(navigationTarget,
                parentChain);

        getRouteModel().addRoute(template, target);

        if (!hasRouteTarget(navigationTarget)) {
            setTargetRoute(navigationTarget, template);
        }

        getTargetRouteModelMap().computeIfAbsent(navigationTarget,
                aClass -> RouteModel.create(true));
        getTargetRouteModelMap().get(navigationTarget).addRoute(template,
                target);

        getRoutesMap().put(template, target);
    }

    /**
     * Put a new target route for Class-to-path mapping.
     * <p>
     * This is a reverse mapping to RouteTarget, which also handles any HasUrl
     * parameters, for the main route of this navigation target.
     *
     * @param navigationTarget
     *            navigation target to map
     * @param path
     *            path for given navigation target
     */
    public void setTargetRoute(Class<? extends Component> navigationTarget,
            String path) {
        getTargetRoutes().put(navigationTarget, path);
    }

    /**
     * Set a error route to the configuration.
     * <p>
     * Any exception handler set for a existing error will override the old
     * exception handler.
     *
     * @param exception
     *            exception handled by error route
     * @param errorTarget
     *            error navigation target
     */
    public void setErrorRoute(Class<? extends Exception> exception,
            Class<? extends Component> errorTarget) {
        getExceptionHandlers().put(exception, errorTarget);
    }

    /**
     * Remove the target completely from the configuration.
     *
     * @param target
     *            target registered route to remove
     */
    public void removeRoute(Class<? extends Component> target) {
        if (!hasRouteTarget(target)) {
            return;
        }

        // Remove target route from class-to-string map
        getTargetRoutes().remove(target);

        getTargetRouteModelMap().remove(target).getRoutes().keySet()
                .forEach(template -> {
                    getRouteModel().removeRoute(template);
                    getRoutesMap().remove(template);
                });
    }

    /**
     * Remove route for given template. This will remove all targets registered
     * for given template.
     * <p>
     * In case there exists another template mapping for any of the removed
     * route targets the main class-to-string mapping will be updated to the
     * first found.
     *
     * @param template
     *            template from which to remove routes from
     */
    public void removeRoute(String template) {
        if (!hasTemplate(template)) {
            return;
        }

        RouteTarget removedRoute = getRoutesMap().remove(template);
        if (removedRoute != null) {
            final Class<? extends Component> target = removedRoute.getTarget();

            final RouteModel targetRouteModel = getTargetRouteModelMap()
                    .get(target);
            targetRouteModel.removeRoute(template);

            if (targetRouteModel.isEmpty()) {
                getTargetRouteModelMap().remove(target);
            }

            final String mainTemplate = getTargetRoutes().get(target);
            if (Objects.equals(template, mainTemplate)) {
                if (targetRouteModel.isEmpty()) {
                    getTargetRoutes().remove(target);
                } else {
                    getTargetRoutes().put(target, targetRouteModel.getRoutes()
                            .keySet().iterator().next());
                }
            }
        }

        getRouteModel().removeRoute(template);
    }

    /**
     * Remove navigation target for given template.
     *
     * @param template
     *            template to remove target from.
     * @param targetRoute
     *            target route to remove from template.
     */
    public void removeRoute(String template,
            Class<? extends Component> targetRoute) {
        if (!hasTemplate(template)
                || !getRoutesMap().get(template).containsTarget(targetRoute)) {
            return;
        }

        removeRoute(template);
    }

    @Override
    Map<Class<? extends Component>, RouteModel> getTargetRouteModelMap() {
        return targetRouteModelMap;
    }

}
