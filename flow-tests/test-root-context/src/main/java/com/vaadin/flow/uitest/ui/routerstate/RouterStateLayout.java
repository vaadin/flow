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
package com.vaadin.flow.uitest.ui.routerstate;

import java.util.stream.Collectors;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RouterState;
import com.vaadin.flow.signals.Signal;

/**
 * Shared layout that displays the active {@link RouterState} via the read-only
 * signal exposed by {@link UI#routerStateSignal()}. Used by the
 * {@code RouterStateIT} integration test to verify that layout content driven
 * by the router signal updates correctly across navigations and parameter
 * changes.
 */
public class RouterStateLayout extends Div implements RouterLayout {

    public static final String PATH_ID = "router-state-path";
    public static final String TARGET_ID = "router-state-target";
    public static final String PARAMS_ID = "router-state-params";
    public static final String UPDATES_ID = "router-state-updates";

    private final Div path = new Div();
    private final Div target = new Div();
    private final Div params = new Div();
    private final Div updates = new Div();

    private int updateCount;

    public RouterStateLayout() {
        path.setId(PATH_ID);
        target.setId(TARGET_ID);
        params.setId(PARAMS_ID);
        updates.setId(UPDATES_ID);

        Div header = new Div(path, target, params, updates);
        getElement().appendChild(header.getElement());

        Signal.effect(this, () -> {
            RouterState state = UI.getCurrent().routerStateSignal().get();
            updateCount++;

            path.setText(state.location().getPathWithQueryParameters());
            target.setText(state.navigationTarget() == null ? ""
                    : state.navigationTarget().getSimpleName());
            params.setText(formatParameters(state));
            updates.setText(String.valueOf(updateCount));
        });
    }

    private static String formatParameters(RouterState state) {
        return state.routeParameters().getParameterNames().stream().sorted()
                .map(n -> n + "=" + state.routeParameters().get(n).orElse(""))
                .collect(Collectors.joining(","));
    }
}
