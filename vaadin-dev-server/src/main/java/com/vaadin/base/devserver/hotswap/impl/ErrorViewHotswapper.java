/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.base.devserver.hotswap.impl;

import jakarta.annotation.Priority;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.vaadin.base.devserver.hotswap.HotswapClassSessionEvent;
import com.vaadin.base.devserver.hotswap.UIUpdateStrategy;
import com.vaadin.base.devserver.hotswap.VaadinHotswapper;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.AbstractRouteNotFoundError;

/**
 * Re-attempts the navigation when hotswap occurs while an error view is
 * displayed. This ensures that fixing a broken class, or adding a missing
 * route, during development retries the navigation to the original location.
 * <p>
 * The UI is normally refreshed in place. The page is reloaded only when the
 * first routes of the project appear while the route-not-found page is shown:
 * in development mode, the page shown for a project without routes hands the
 * document over to Copilot, which replaces the page body, so an in-place
 * refresh would render the new view into detached DOM. A reload is not used
 * more widely because it is sent to every open browser tab, not only to the
 * affected one.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 25.1
 */
@Priority(100)
public class ErrorViewHotswapper implements VaadinHotswapper {

    /**
     * Creates the hotswapper. It is found through the service loader.
     */
    public ErrorViewHotswapper() {
        // Default constructor
    }

    @Override
    public void onClassesChange(HotswapClassSessionEvent event) {
        boolean redefined = event.isRedefined();
        Set<Class<?>> changedClasses = event.getChangedClasses();
        for (UI ui : event.getVaadinSession().getUIs()) {
            if (ui.isClosing() || !ui.getInternals().isShowingErrorView()) {
                continue;
            }
            Set<Class<?>> routeTargets = getRouteTargets(ui);
            // A class loaded for the first time cannot affect the error view
            // unless it is a new route, which the global registry update has
            // already registered by now
            if (redefined || changedClasses.stream()
                    .anyMatch(routeTargets::contains)) {
                event.triggerUpdate(ui,
                        isShowingNoRoutesPage(ui, routeTargets, changedClasses)
                                ? UIUpdateStrategy.RELOAD
                                : UIUpdateStrategy.REFRESH);
            }
        }
    }

    private static Set<Class<?>> getRouteTargets(UI ui) {
        Set<Class<?>> routeTargets = new HashSet<>();
        ui.getInternals().getRouter().getRegistry().getRegisteredRoutes()
                .forEach(
                        route -> routeTargets.add(route.getNavigationTarget()));
        return routeTargets;
    }

    /**
     * Checks whether the UI shows the route-not-found page of a project that
     * had no routes before the given classes changed, i.e. the page that
     * Copilot takes over.
     */
    private static boolean isShowingNoRoutesPage(UI ui,
            Set<Class<?>> routeTargets, Set<Class<?>> changedClasses) {
        List<HasElement> chain = ui.getActiveRouterTargetsChain();
        return !chain.isEmpty()
                && chain.get(0) instanceof AbstractRouteNotFoundError
                && !routeTargets.isEmpty()
                && changedClasses.containsAll(routeTargets);
    }
}
