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

import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;

/**
 * Provides API to access to the {@link BrowserLiveReload} instance by a
 * {@link VaadinService}.
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
            LoggerFactory.getLogger(BrowserLiveReloadAccess.class)
                    .debug("Live reload getter is called in production mode.");
            return null;
        }
        VaadinContext context = service.getContext();
        BrowserLiveReload liveReload;
        synchronized (this) {
            liveReload = context.getAttribute(BrowserLiveReload.class);
            if (liveReload == null) {
                liveReload = new BrowserLiveReloadImpl(service);
                context.setAttribute(BrowserLiveReload.class, liveReload);
            }
        }
        return liveReload;
    }
}
