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
import java.util.Enumeration;

/**
 * Configuration in which {@link VaadinService} is running. This is a wrapper
 * for Config objects for instance <code>ServletConfig</code> and
 * <code>PortletConfig</code>.
 *
 * @since
 */
public interface VaadinConfig extends Serializable {

    /**
     * Get the VaadinContext for this configuration.
     *
     * @return VaadinContext object for this VaadinConfiguration
     */
    VaadinContext getVaadinContext();

    /**
     * Returns the names of the initialization parameters as an
     * <code>Enumeration</code>, or an empty <code>Enumeration</code> if there
     * are o initialization parameters.
     *
     * @return initialization parameters as a <code>Enumeration</code>
     */
    Enumeration<String> getConfigParameterNames();

    /**
     * Returns the value for the requested parameter, or <code>null</code> if
     * the parameter does not exist.
     *
     * @param name
     *            name of the parameter whose value is requested
     * @return parameter value as <code>String</code> or <code>null</code> for
     *         no parameter
     */
    String getConfigParameter(String name);
}
