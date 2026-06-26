/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.router.internal;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.hotswap.VaadinHotswapper;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.SessionRouteRegistry;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.frontend.Options;
import com.vaadin.flow.server.frontend.TaskGenerateReactFiles;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;

/**
 * A component that reacts on class changes to update route registries.
 * <p>
 * </p>
 * This class is meant to be used in combination wit Flow
 * {@link com.vaadin.flow.hotswap.Hotswapper} to immediately update routes
 * registries when classes have been added or modified.
 * <p>
 * </p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 24.5
 */
public class RouteRegistryHotswapper implements VaadinHotswapper {

    /**
     * Updates both application registry, to reflect provided class changes.
     * <p>
     * </p>
     * For modified route classes, the following changes are taken into account:
     * <ul>
     * <li>{@link Route} annotation removed: the previous route is removed from
     * the registry</li>
     * <li>{@link Route#value()} modified</li>
     * </ul>
     *
     */
    @Override
    public boolean onClassLoadEvent(VaadinService vaadinService,
            Set<Class<?>> classes, boolean redefined) {
        Set<Class<?>> addedClasses = redefined ? Set.of() : classes;
        Set<Class<?>> modifiedClasses = redefined ? classes : Set.of();
        Set<Class<?>> removedClasses = Set.of();

        if (hasComponentClasses(addedClasses, modifiedClasses,
                removedClasses)) {
            ApplicationRouteRegistry appRegistry = ApplicationRouteRegistry
                    .getInstance(vaadinService.getContext());
            // Collect layouts before and after changes to trigger layouts.json
            // regeneration if something changed
            Set<Class<?>> layouts = new HashSet<>(
                    ((AbstractRouteRegistry) appRegistry).getLayouts());

            RouteUtil.updateRouteRegistry(appRegistry, addedClasses,
                    modifiedClasses, removedClasses);

            layouts.addAll(((AbstractRouteRegistry) appRegistry).getLayouts());

            DeploymentConfiguration configuration = vaadinService
                    .getDeploymentConfiguration();
            if (configuration.isReactEnabled()
                    && Stream.of(addedClasses, modifiedClasses, removedClasses)
                            .flatMap(Set::stream).anyMatch(layouts::contains)) {

                Options options = new Options(
                        vaadinService.getContext().getAttribute(Lookup.class),
                        null, configuration.getProjectFolder())
                        .withFrontendDirectory(
                                configuration.getFrontendFolder());
                TaskGenerateReactFiles.writeLayouts(options,
                        ((AbstractRouteRegistry) appRegistry).getLayouts());
            }
        }
        return false;
    }

    @Override
    public boolean onClassLoadEvent(VaadinSession session,
            Set<Class<?>> classes, boolean redefined) {
        Set<Class<?>> addedClasses = redefined ? Set.of() : classes;
        Set<Class<?>> modifiedClasses = redefined ? classes : Set.of();
        Set<Class<?>> removedClasses = Set.of();
        if (session.getAttribute(SessionRouteRegistry.class) != null
                && hasComponentClasses(addedClasses, modifiedClasses,
                        removedClasses)) {
            RouteUtil.updateRouteRegistry(
                    SessionRouteRegistry.getSessionRegistry(session), Set.of(),
                    modifiedClasses, removedClasses);
        }
        return false;
    }

    @SafeVarargs
    private boolean hasComponentClasses(Set<Class<?>>... classes) {
        return Stream.of(classes)
                .filter(classSet -> classSet != null && !classSet.isEmpty())
                .flatMap(Set::stream)
                .anyMatch(Component.class::isAssignableFrom);
    }

}
