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

import java.util.concurrent.ConcurrentLinkedQueue;

import org.atmosphere.cpr.AtmosphereResource;
import org.slf4j.Logger;
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

    private final ConcurrentLinkedQueue<AtmosphereResource> atmosphereResources = new ConcurrentLinkedQueue<>();

    BrowserLiveReloadImpl(VaadinService service) {
        this.service = service;
    }

    @Override
    public void onConnect(AtmosphereResource resource) {
        resource.suspend(-1);
        atmosphereResources.add(resource);
        resource.getBroadcaster().broadcast("{\"command\": \"hello\"}",
                resource);
    }

    @Override
    public void onDisconnect(AtmosphereResource resource) {
        if (!atmosphereResources.remove(resource)) {
            getLogger().warn(
                    "Push connection {} is not a live-reload connection or already closed",
                    resource.uuid());
        }
    }

    @Override
    public void reload() {
        atmosphereResources.forEach(resource -> {
            resource.getBroadcaster().broadcast("{\"command\": \"reload\"}",
                    resource);
        });
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(BrowserLiveReloadImpl.class.getName());
    }
}
