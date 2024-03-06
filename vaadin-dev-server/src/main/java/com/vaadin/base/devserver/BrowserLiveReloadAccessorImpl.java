/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.base.devserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.vaadin.flow.internal.BrowserLiveReload;
import com.vaadin.flow.internal.BrowserLiveReloadAccessor;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;

/**
 * Default implementation for {@link BrowserLiveReloadAccessor} that stores the
 * instance in the Vaadin context.
 */
public class BrowserLiveReloadAccessorImpl
        implements BrowserLiveReloadAccessor {

    @Override
    public BrowserLiveReload getLiveReload(VaadinContext context) {
        return context.getAttribute(BrowserLiveReload.class,
                () -> new DebugWindowConnection(context));
    }

    @Override
    public BrowserLiveReload getLiveReload(VaadinService service) {
        if (service.getDeploymentConfiguration().isProductionMode()) {
            getLogger().debug(
                    "BrowserLiveReloadAccessImpl::getLiveReload is called in production mode.");
            return null;
        }
        return getLiveReload(service.getContext());
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(BrowserLiveReloadAccessor.class);
    }
}
