/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server;

/**
 * A factory to create a {@link StaticFileHandler}.
 *
 * @author Vaadin Ltd
 * @since
 *
 */
public interface StaticFileHandlerFactory {

    /**
     * Creates a new instance of {@link StaticFileHandler} for the given
     * {@code service}.
     *
     * @param service
     *            a {@link VaadinServletService} instance
     * @return a new {@link StaticFileHandler} instance for the {@code service}
     */
    StaticFileHandler createHandler(VaadinService service);
}
