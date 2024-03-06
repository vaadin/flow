/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.startup;

import java.io.Serializable;
import java.util.ServiceLoader;

import com.vaadin.flow.component.Component;

/**
 * A filter that can prevent specific navigation targets from being registered.
 * <p>
 * Listener instances are by discovered and instantiated using
 * {@link ServiceLoader}. This means that all implementations must have a
 * zero-argument constructor and the fully qualified name of the implementation
 * class must be listed on a separate line in a
 * META-INF/services/com.vaadin.flow.server.startup.NavigationTargetFilter file
 * present in the jar file containing the implementation class.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public interface NavigationTargetFilter extends Serializable {
    /**
     * Tests whether the given navigation target class should be included.
     *
     * @param navigationTarget
     *            the navigation target class to test
     * @return <code>true</code> to include the navigation target,
     *         <code>false</code> to discard it
     */
    boolean testNavigationTarget(Class<? extends Component> navigationTarget);

    /**
     * Tests whether the given error navigation target class should be included.
     *
     * @param errorNavigationTarget
     *            the error navigation target class to test
     * @return <code>true</code> to include the error navigation target,
     *         <code>false</code> to discard it
     */
    boolean testErrorNavigationTarget(Class<?> errorNavigationTarget);
}
