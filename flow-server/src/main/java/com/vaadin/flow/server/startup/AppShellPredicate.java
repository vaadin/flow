/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.startup;

import com.vaadin.flow.component.page.AppShellConfigurator;

/**
 * Checks whether the provided class is an {@link AppShellConfigurator} eligible
 * for the web application.
 *
 * @author Vaadin Ltd
 * @since
 *
 */
@FunctionalInterface
public interface AppShellPredicate {

    /**
     * Checks whether the class is an {@link AppShellConfigurator} class inside
     * the web application.
     *
     * @param clz
     *            the class to check , not {@code null}
     * @return true if the class extends {@link AppShellConfigurator}.
     */
    boolean isShell(Class<?> clz);
}
