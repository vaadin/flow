/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public interface BrowserLiveReloadAccess {

    /**
     * Returns a {@link BrowserLiveReload} instance for the given
     * {@code service}.
     *
     * @param service
     *            a live reload service
     * @return a <code>BrowserLiveReload</code> instance or null if disabled
     */
    BrowserLiveReload getLiveReload(VaadinService service);

    /**
     * Create a {@link BrowserLiveReload} is factory available.
     *
     * @param service
     *            a service
     * @return a <code>DevModeHandler</code> instance or null if disabled
     */
    static BrowserLiveReload getLiveReloadIfAvailable(VaadinService service) {
        VaadinContext context = service.getContext();
        return Optional.ofNullable(context)
                .map(ctx -> ctx.getAttribute(Lookup.class))
                .map(lu -> lu.lookup(BrowserLiveReloadAccess.class))
                .map(blra -> blra.getLiveReload(service)).orElse(null);
    }
}
