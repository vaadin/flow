/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal;

import com.vaadin.flow.server.VaadinContext;

/**
 * Allows to access the web application classloader.
 * <p>
 * The functionality is intended to internal usage only. The implementation of
 * this interface may be set as an attribute in {@link VaadinContext} so that
 * the classloader may be used in other place where {@link VaadinContext} is
 * available.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since
 *
 */
@FunctionalInterface
public interface ApplicationClassLoaderAccess {

    /**
     * Gets the web application classloader.
     *
     * @return the web application classloader.
     */
    ClassLoader getClassloader();
}
