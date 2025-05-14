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
package com.vaadin.flow.server;

import jakarta.servlet.ServletContext;

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
