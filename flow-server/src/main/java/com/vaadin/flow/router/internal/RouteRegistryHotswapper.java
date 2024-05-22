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

package com.vaadin.flow.router.internal;

import java.util.Set;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.hotswap.VaadinHotswapper;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.SessionRouteRegistry;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
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
 * @since 24.4
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

        if (hasNoComponentClasses(addedClasses)
                && hasNoComponentClasses(modifiedClasses)
                && hasNoComponentClasses(removedClasses)) {
            return false;
        }
        ApplicationRouteRegistry appRegistry = ApplicationRouteRegistry
                .getInstance(vaadinService.getContext());
        RouteUtil.updateRouteRegistry(appRegistry, addedClasses,
                modifiedClasses, removedClasses);
        return false;
    }

    @Override
    public boolean onClassLoadEvent(VaadinSession session,
            Set<Class<?>> classes, boolean redefined) {
        Set<Class<?>> addedClasses = redefined ? Set.of() : classes;
        Set<Class<?>> modifiedClasses = redefined ? classes : Set.of();
        Set<Class<?>> removedClasses = Set.of();
        if (hasNoComponentClasses(addedClasses)
                && hasNoComponentClasses(modifiedClasses)
                && hasNoComponentClasses(removedClasses)) {
            return false;
        }
        if (session.getAttribute(SessionRouteRegistry.class) != null) {
            RouteUtil.updateRouteRegistry(
                    SessionRouteRegistry.getSessionRegistry(session), Set.of(),
                    modifiedClasses, removedClasses);
        }
        return false;
    }

    private boolean hasNoComponentClasses(Set<Class<?>> classes) {
        return classes == null || classes.isEmpty() || classes.stream()
                .noneMatch(Component.class::isAssignableFrom);
    }
}
