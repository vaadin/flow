/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server;

import javax.servlet.ServletContext;

import org.mockito.Mockito;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RoutePathProvider;

public class MockVaadinContext extends VaadinServletContext {

    private Lookup lookup = Mockito.mock(Lookup.class);

    public static class RoutePathProviderImpl implements RoutePathProvider {

        @Override
        public String getRoutePath(Class<?> navigationTarget) {
            Route route = navigationTarget.getAnnotation(Route.class);
            return route.value();
        }

    }

    public MockVaadinContext() {
        this(new MockServletContext(), new RoutePathProviderImpl());
    }

    public MockVaadinContext(RoutePathProvider provider) {
        this(new MockServletContext(), provider);
    }

    public MockVaadinContext(ServletContext context) {
        this(context, new RoutePathProviderImpl());
    }

    public MockVaadinContext(ServletContext context,
            RoutePathProvider provider) {
        super(context);

        Mockito.when(lookup.lookup(RoutePathProvider.class)).thenReturn(null);

        Mockito.when(lookup.lookup(RoutePathProvider.class))
                .thenReturn(provider);

        setAttribute(lookup);
    }

    @Override
    public <T> T getAttribute(Class<T> type) {
        if (type.equals(Lookup.class)) {
            return type.cast(lookup);
        }
        return super.getAttribute(type);
    }

}
