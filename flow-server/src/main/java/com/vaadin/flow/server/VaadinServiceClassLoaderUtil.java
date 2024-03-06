/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Utility class used by {@link VaadinService#setDefaultClassLoader()}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
class VaadinServiceClassLoaderUtil {

    private static class GetClassLoaderPrivilegedAction
            implements PrivilegedAction<ClassLoader> {
        @Override
        public ClassLoader run() {
            return Thread.currentThread().getContextClassLoader();
        }
    }

    /**
     * Called by {@link VaadinService#setDefaultClassLoader()} to acquire
     * appropriate class loader to load application's classes (e.g. UI). Calls
     * should be guarded by try/catch block to catch SecurityException and log
     * appropriate message. The code for this method is modeled after
     * recommendations laid out by JEE 5 specification sections EE.6.2.4.7 and
     * EE.8.2.5
     *
     * @return Instance of {@link ClassLoader} that should be used by this
     *         instance of {@link VaadinService}
     * @throws SecurityException
     *             if current security policy doesn't allow acquiring current
     *             thread's context class loader
     */
    protected static ClassLoader findDefaultClassLoader()
            throws SecurityException {
        return AccessController.doPrivileged(
                new VaadinServiceClassLoaderUtil.GetClassLoaderPrivilegedAction());
    }

}
