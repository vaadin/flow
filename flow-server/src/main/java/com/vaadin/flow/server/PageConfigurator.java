/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server;

import java.io.Serializable;

import com.vaadin.flow.component.UI;

/**
 * Configures the initial page contents.
 *
 * @since 1.0
 * @deprecated Deprecated due to multiple issues on feature design, like this
 *             won't work together with the
 *             {@link com.vaadin.flow.router.PreserveOnRefresh} annotation. Will
 *             not be removed until <em>after the next long term support
 *             version</em> (targeted Vaadin 23).
 *             <p>
 *             For Vaadin 14, use {@link BootstrapListener} instead, which
 *             provides API for modifying the bootstrap page and access to the
 *             {@link UI}, which provides further replacement API like
 *             {@link UI#getLoadingIndicatorConfiguration()}.
 *             <p>
 *             For Vaadin 15+, use {@code AppShellConfigurator} instead.
 */
@Deprecated
@FunctionalInterface
public interface PageConfigurator extends Serializable {

    /**
     * Configure the initial page settings when called.
     *
     * @param settings
     *            initial page settings
     */
    void configurePage(InitialPageSettings settings);
}
