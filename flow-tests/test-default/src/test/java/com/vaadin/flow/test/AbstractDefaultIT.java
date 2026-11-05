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
package com.vaadin.flow.test;

import java.util.Collections;
import java.util.Enumeration;
import java.util.function.Supplier;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.internal.RouteUtil;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.testutil.junit5.AbstractBrowserIT;

/**
 * Base class for the integration tests in the default configuration module.
 * <p>
 * Adds to the shared {@link AbstractBrowserIT} the view-based navigation
 * ({@link #open()}, {@link #getTestURL()}, {@link #getTestPath()}) that
 * resolves the test path from the {@link TestFor} annotation. Tests annotate
 * methods with {@link com.vaadin.testbench.BrowserTest @BrowserTest}.
 * <p>
 * The tests call the JUnit assertions unqualified, through a static import, as
 * the conventions ask. {@code assertEquals} is the exception: TestBench's base
 * class declares {@code assertEquals(WebElement, WebElement)}, and an inherited
 * method shadows every static import of the same name, so those calls stay
 * written as {@code Assertions.assertEquals(...)}.
 */
public abstract class AbstractDefaultIT extends AbstractBrowserIT {

    /**
     * Gets the absolute path to the test view, starting with a "/".
     * <p>
     * The default implementation derives the path from the {@link TestFor}
     * annotation on the test class, resolving the {@link Route @Route} of the
     * referenced view through {@link RouteUtil#getRoutePath} (which handles
     * naming-convention defaults, parent {@code @RoutePrefix} layouts and
     * absolute routes). Tests without a single corresponding view can override
     * this method instead of using {@link TestFor}.
     *
     * @return the path appended to {@link #getRootURL()} for the full test URL
     */
    protected String getTestPath() {
        TestFor testFor = getClass().getAnnotation(TestFor.class);
        if (testFor == null) {
            throw new IllegalStateException(getClass().getName()
                    + " must either be annotated with @TestFor(view) or override getTestPath()");
        }
        Class<? extends Component> viewType = testFor.value();
        if (!viewType.isAnnotationPresent(Route.class)) {
            throw new IllegalStateException(viewType.getName()
                    + " must declare a @Route to be used with @TestFor");
        }
        return "/" + RouteUtil.getRoutePath(ROUTE_RESOLUTION_CONTEXT, viewType);
    }

    /**
     * Minimal context for {@link RouteUtil#getRoutePath}. It carries no
     * {@code Lookup}, so route resolution falls back to the default naming
     * provider, which is all that is needed to compute a path from annotations.
     */
    private static final VaadinContext ROUTE_RESOLUTION_CONTEXT = new VaadinContext() {
        @Override
        public <T> T getAttribute(Class<T> type,
                Supplier<T> defaultValueSupplier) {
            return defaultValueSupplier != null ? defaultValueSupplier.get()
                    : null;
        }

        @Override
        public <T> void setAttribute(Class<T> clazz, T value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeAttribute(Class<?> clazz) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Enumeration<String> getContextParameterNames() {
            return Collections.emptyEnumeration();
        }

        @Override
        public String getContextParameter(String name) {
            return null;
        }
    };

    /**
     * Opens the view returned by {@link #getTestPath()} and waits for the dev
     * server to be ready.
     */
    protected void open() {
        open(getTestPath());
    }

    /**
     * Returns the full URL to the test view.
     *
     * @return the URL for the test
     */
    protected String getTestURL() {
        return getRootURL() + getTestPath();
    }
}
