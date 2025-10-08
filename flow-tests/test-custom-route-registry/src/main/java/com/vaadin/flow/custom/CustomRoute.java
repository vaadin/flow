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
package com.vaadin.flow.custom;

import java.util.Map;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.internal.AbstractRouteRegistry;
import com.vaadin.flow.server.RouteRegistry;

/**
 * The root view for the test only registered to the CustomRouteRegistry.
 */
public class CustomRoute extends Div {

    public CustomRoute() {
        final RouteRegistry registry = ((CustomServletService) CustomServletService
                .getCurrent()).getRouteRegistry();
        final Span span = new Span(
                "Used " + registry.getClass().getSimpleName());
        span.setId("registry");

        add(span);
        add(new Div(), new Span("--"), new Div());

        final Map<Class<? extends Exception>, Class<? extends Component>> exceptionHandlers = ((AbstractRouteRegistry) registry)
                .getConfiguration().getExceptionHandlers();
        add(new Span(
                "Found " + exceptionHandlers.size() + " exception handlers"));
        exceptionHandlers.forEach((exception, view) -> {
            Span exceptionSpan = new Span(
                    exception.getSimpleName() + " :: " + view.getSimpleName());
            exceptionSpan.setId(exception.getSimpleName());
            add(new Div(), exceptionSpan);
        });

    }
}
