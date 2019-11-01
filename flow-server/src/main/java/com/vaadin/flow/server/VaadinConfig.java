package com.vaadin.flow.server;

import java.io.Serializable;
import java.util.Enumeration;

/**
 * Configuration in which {@link VaadinService} is running.
 * This is a wrapper for Config objects for instance <code>ServletConfig</code> and
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
     * Returns the names of the context's initialization parameters as an
     * <code>Enumeration</code> of <code>String</code> objects, or an
     * empty <code>Enumeration</code> if the context has no initialization
     * parameters.
     *
     * @return an <code>Enumeration</code> of <code>String</code>
     * objects containing the names of the context's
     * initialization parameters
     */
    Enumeration<String> getInitParameterNames();

    /**
     * Returns a <code>String</code> containing the value of the named
     * initialization parameter, or <code>null</code> if the
     * parameter does not exist.
     *
     * @param name
     *         a <code>String</code> containing the name of the
     *         parameter whose value is requested
     * @return parameter value as <code>String</code> or <code>null</code> for
     * no parameter
     */
    String getInitParameter(String name);
}
