/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal;

import com.vaadin.flow.server.VaadinService;

/**
 * Creates a live reload instance by delegating to
 * {@link BrowserLiveReloadAccessor#getLiveReload(VaadinService)}
 * <p>
 * Class exists only for backwards compatibility with JRebel and HotswapAgent
 * plugins.
 *
 * @deprecated Use {@link BrowserLiveReloadAccessor} instead
 */
@Deprecated
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
        return BrowserLiveReloadAccessor.getLiveReloadFromService(service)
                .orElse(null);
    }
}
