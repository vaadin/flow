/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;

/**
 * Provides API to access to the {@link BrowserLiveReload} instance by a
 * {@link VaadinService}.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since
 *
 */
public class BrowserLiveReloadAccess {

    /**
     * Returns a {@link BrowserLiveReload} instance for the given
     * {@code service}.
     * <p>
     * Returns {@code null} if production mode is enabled for the service.
     *
     * @param service
     *            a service
     * @return a BrowserLiveReload instance or null for production mode
     */
    public BrowserLiveReload getLiveReload(VaadinService service) {
        if (service.getDeploymentConfiguration().isProductionMode()) {
            LoggerFactory.getLogger(BrowserLiveReloadAccess.class).debug(
                    "BrowserLiveReloadAccess::getLiveReload is called in production mode.");
            return null;
        }
        if (!service.getDeploymentConfiguration()
                .isDevModeLiveReloadEnabled()) {
            LoggerFactory.getLogger(BrowserLiveReloadAccess.class).debug(
                    "BrowserLiveReloadAccess::getLiveReload is called when live reload is disabled.");
            return null;
        }
        VaadinContext context = service.getContext();
        DebugWindowConnection liveReload;
        synchronized (this) {
            liveReload = context.getAttribute(DebugWindowConnection.class);
            if (liveReload == null) {
                liveReload = new DebugWindowConnection();
                context.setAttribute(DebugWindowConnection.class, liveReload);
            }
        }
        return liveReload;
    }
}
