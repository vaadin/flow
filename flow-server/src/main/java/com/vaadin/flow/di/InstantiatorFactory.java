/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.di;

import com.vaadin.flow.server.VaadinService;

/**
 * A factory for an {@link Instantiator}.
 *
 * @author Vaadin Ltd
 * @since
 *
 */
public interface InstantiatorFactory {

    /**
     * Create an {@link Instantiator} using the provided {@code service}.
     *
     * @param service
     *            a {@code VaadinService} to create an {@code Instantiator} for
     * @return an instantiator for the service or null if this factory is not
     *         able to create an instantiator for the provided service
     */
    Instantiator createInstantitor(VaadinService service);
}
