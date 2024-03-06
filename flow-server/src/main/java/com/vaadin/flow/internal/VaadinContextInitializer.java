/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal;

import javax.servlet.ServletContextListener;

import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinServlet;

/**
 * Allows to run initialization of {@link VaadinContext} which for some reasons
 * may not be done via {@link ServletContextListener}.
 * <p>
 * The functionality is intended to internal usage only. The implementation of
 * this interface may be available as an attribute in a {@link VaadinContext}.
 * In the latter case {@link VaadinServlet#init()} method will run
 * {@link #initialize(VaadinContext)} method.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since
 *
 */
@FunctionalInterface
public interface VaadinContextInitializer {

    /**
     * Initializes the Vaadin {@code context}.
     *
     * @param context
     *            the Vaadin context instance
     */
    void initialize(VaadinContext context);

}
