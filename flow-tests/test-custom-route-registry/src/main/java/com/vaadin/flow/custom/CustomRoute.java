/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
