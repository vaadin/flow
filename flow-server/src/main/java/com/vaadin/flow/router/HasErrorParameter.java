/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.router;

import java.io.Serializable;

/**
 * Defines a view that handles the exceptions for the set Exception type T.
 *
 * @param <T>
 *            type Exception type handled
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@FunctionalInterface
public interface HasErrorParameter<T extends Exception> extends Serializable {

    /**
     * Callback executed before rendering the exception view.
     * <p>
     * Note! returned int should be a valid
     * {@link com.vaadin.flow.server.HttpStatusCode} code
     *
     * @param event
     *            the before navigation event for this request
     * @param parameter
     *            error parameter containing custom exception and caught
     *            exception
     * @return a valid {@link com.vaadin.flow.server.HttpStatusCode} code
     */
    int setErrorParameter(BeforeEnterEvent event, ErrorParameter<T> parameter);
}
