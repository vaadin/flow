/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal;

import java.util.Optional;

import com.vaadin.flow.di.Lookup;
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
public interface BrowserLiveReloadAccessor {

    /**
     * Returns a {@link BrowserLiveReload} instance for the given
     * {@code context}.
     *
     * @param context
     *            a Vaadin context
     * @return a <code>BrowserLiveReload</code> instance or null if disabled
     */
    BrowserLiveReload getLiveReload(VaadinContext context);

    /**
     * Returns a {@link BrowserLiveReload} instance for the given
     * {@code service}.
     *
     * @param service
     *            a Vaadin service
     * @return a <code>BrowserLiveReload</code> instance or null if disabled
     */
    default BrowserLiveReload getLiveReload(VaadinService service) {
        return getLiveReload(service.getContext());
    }

    /**
     * Create a {@link BrowserLiveReload} if factory available.
     *
     * @param service
     *            a Vaadin service
     * @return an {@link Optional} containing a {@link BrowserLiveReload}
     *         instance or <code>EMPTY</code> if disabled
     */
    static Optional<BrowserLiveReload> getLiveReloadFromService(
            VaadinService service) {
        VaadinContext context = service.getContext();
        return Optional.ofNullable(context)
                .map(ctx -> ctx.getAttribute(Lookup.class))
                .map(lu -> lu.lookup(BrowserLiveReloadAccessor.class))
                .flatMap(blra -> Optional
                        .ofNullable(blra.getLiveReload(service)));
    }
}
