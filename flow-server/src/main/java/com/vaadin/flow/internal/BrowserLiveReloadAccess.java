/*
 * Copyright 2000-2022 Vaadin Ltd.
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
