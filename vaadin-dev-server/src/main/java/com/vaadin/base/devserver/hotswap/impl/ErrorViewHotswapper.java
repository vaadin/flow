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

import java.util.Set;

import com.vaadin.base.devserver.hotswap.HotswapClassSessionEvent;
import com.vaadin.base.devserver.hotswap.UIUpdateStrategy;
import com.vaadin.base.devserver.hotswap.VaadinHotswapper;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.AbstractRouteNotFoundError;

/**
 * Triggers UI refresh when hotswap occurs while an error view is displayed.
 * This ensures that fixing a broken class during development will refresh the
 * error page and attempt to re-navigate to the original location.
 * <p>
 * A route-not-found page is reloaded instead of refreshed in place, both when a
 * class is redefined and when a newly loaded class adds a route. In development
 * mode, the page shown for a project without routes hands the document over to
 * Copilot, which replaces the page body, so an in-place refresh would render
 * the new view into detached DOM.
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
            // A class loaded for the first time cannot affect the error view
            // unless it is a new route, which the global registry update has
            // already registered by now
            if (!redefined && !isRegisteredRoute(ui, changedClasses)) {
                continue;
            }
            event.triggerUpdate(ui,
                    ui.getActiveRouterTargetsChain()
                            .get(0) instanceof AbstractRouteNotFoundError
                                    ? UIUpdateStrategy.RELOAD
                                    : UIUpdateStrategy.REFRESH);
        }
    }

    private static boolean isRegisteredRoute(UI ui, Set<Class<?>> classes) {
        return ui.getInternals().getRouter().getRegistry().getRegisteredRoutes()
                .stream().anyMatch(
                        route -> classes.contains(route.getNavigationTarget()));
    }
}
