/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import java.util.concurrent.atomic.AtomicReference;

import org.atmosphere.cpr.AtmosphereResource;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.VaadinService;

/**
 * {@link BrowserLiveReload} implementation class.
 *
 * @author Vaadin Ltd
 *
 */
class BrowserLiveReloadImpl implements BrowserLiveReload {

    private final VaadinService service;

    private final AtomicReference<AtmosphereResource> resourceRef = new AtomicReference<>();

    BrowserLiveReloadImpl(VaadinService service) {
        this.service = service;
    }

    @Override
    public void onConnect(AtmosphereResource resource) {
        resource.suspend(-1);
        resourceRef.set(resource);
        resource.getBroadcaster().broadcast("{\"command\": \"hello\"}");
    }

    @Override
    public void reload() {
        AtmosphereResource resource = resourceRef.get();
        if (resource == null) {
            // There is no yet any connection: nothing to reload
            LoggerFactory.getLogger(BrowserLiveReloadImpl.class).debug(
                    "Reload request is received but there is no yet WS connection");
        } else {
            resource.getBroadcaster().broadcast("{\"command\": \"reload\"}",
                    resource);
        }
    }

}
