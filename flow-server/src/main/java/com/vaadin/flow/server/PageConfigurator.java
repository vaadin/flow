/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server;

import java.io.Serializable;

import com.vaadin.flow.component.page.AppShellConfigurator;

/**
 * Configures the initial page contents.
 *
 * @since 1.0
 * @deprecated since 3.0 use {@link AppShellConfigurator}
 */
@FunctionalInterface
@Deprecated
public interface PageConfigurator extends Serializable {

    /**
     * Configure the initial page settings when called.
     *
     * @param settings
     *            initial page settings
     * @deprecated Since 3.0, use
     *             {@link AppShellConfigurator#configurePage(AppShellSettings)}
     *             instead
     */
    @Deprecated
    void configurePage(InitialPageSettings settings);
}
