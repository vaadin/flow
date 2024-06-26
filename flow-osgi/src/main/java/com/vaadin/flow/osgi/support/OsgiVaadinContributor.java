/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.osgi.support;

import java.util.List;

/**
 * Used to declare multiple OsgiVaadinResources with a single OSGi component.
 *
 * @since 1.2
 */
public interface OsgiVaadinContributor {
    /**
     * Gets the contributions to register.
     *
     * @return a list of resources to register
     */
    List<OsgiVaadinStaticResource> getContributions();
}
