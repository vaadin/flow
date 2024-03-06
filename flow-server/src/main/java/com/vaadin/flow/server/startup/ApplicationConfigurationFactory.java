/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.startup;

import com.vaadin.flow.server.VaadinContext;

/**
 * A factory for {@link ApplicationConfiguration}.
 *
 * @author Vaadin Ltd
 * @since
 *
 */
public interface ApplicationConfigurationFactory {

    /**
     * Creates a new instance of {@link ApplicationConfiguration} for the given
     * {@code context}.
     *
     * @param context
     *            the context to create a configuration for
     * @return the configuration created based on the {@code context}
     */
    ApplicationConfiguration create(VaadinContext context);
}
