/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.di;

import javax.servlet.ServletContainerInitializer;

/**
 * The presence of the service implementing this interface with
 * {@link #runOnce()} returning {@code true} means that
 * {@link ServletContainerInitializer}s are executed only once and the
 * implementation doesn't have to care about cleaning up data collected based on
 * previous call.
 * <p>
 * In some cases (e.g. OSGi) the
 * {@link ServletContainerInitializer#onStartup(java.util.Set, javax.servlet.ServletContext)}
 * method may be called several times for the application (with different
 * classes provided). In this case the initializer logic should reset the data
 * passed on the previous call and set the new data. To be able to reset the
 * data correctly the {@link ServletContainerInitializer} implementation may
 * need to store additional data between calls which is excessive if the
 * {@link ServletContainerInitializer#onStartup(java.util.Set, javax.servlet.ServletContext)}
 * is executed only once.
 * 
 * @author Vaadin Ltd
 * @since
 *
 */
@FunctionalInterface
public interface OneTimeInitializerPredicate {

    /**
     * Checks whether the {@link ServletContainerInitializer}s requires reset to
     * the previous state on
     * {@link ServletContainerInitializer#onStartup(java.util.Set, javax.servlet.ServletContext)}
     * call.
     * 
     * @return {@code true} if {@link ServletContainerInitializer}s are executed
     *         only once, {@code false} otherwise
     */
    boolean runOnce();
}
