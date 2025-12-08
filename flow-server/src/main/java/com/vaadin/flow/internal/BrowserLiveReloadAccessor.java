/*
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.internal;

import java.util.Optional;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

/**
 * Provides API to access to the {@link BrowserLiveReload} instance by a
 * {@link VaadinService}.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
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
        return getLiveReloadFromContext(context);
    }

    /**
     * Create a {@link BrowserLiveReload} if factory available.
     *
     * @param context
     *            a Vaadin Context
     * @return an {@link Optional} containing a {@link BrowserLiveReload}
     *         instance or <code>EMPTY</code> if disabled or in production mode
     */
    static Optional<BrowserLiveReload> getLiveReloadFromContext(
            VaadinContext context) {
        if (ApplicationConfiguration.get(context).isProductionMode()) {
            return Optional.empty();
        }
        return Optional.ofNullable(context)
                .map(ctx -> ctx.getAttribute(Lookup.class))
                .map(lu -> lu.lookup(BrowserLiveReloadAccessor.class))
                .flatMap(blra -> Optional
                        .ofNullable(blra.getLiveReload(context)));
    }
}
