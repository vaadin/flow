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

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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

    private final ClassLoader classLoader;

    private final ConcurrentLinkedQueue<WeakReference<AtmosphereResource>> atmosphereResources = new ConcurrentLinkedQueue<>();

    private Backend backend = null;

    private static final EnumMap<Backend, List<String>> IDENTIFIER_CLASSES = new EnumMap<>(Backend.class);

    static {
        IDENTIFIER_CLASSES.put(Backend.JREBEL, Collections.singletonList(
                "com.vaadin.flow.server.jrebel.JRebelInitializer"));
        IDENTIFIER_CLASSES.put(Backend.HOTSWAP_AGENT, Collections.singletonList(
                "org.hotswap.agent.plugin.vaadin.VaadinIntegration"));
        IDENTIFIER_CLASSES.put(Backend.SPRING_BOOT_DEVTOOLS, Arrays.asList(
                "com.vaadin.flow.spring.SpringServlet",
                "org.springframework.boot.devtools.livereload.LiveReloadServer"));
    }

    BrowserLiveReloadImpl(VaadinService service) {
        this(service, BrowserLiveReloadImpl.class.getClassLoader());
    }

    BrowserLiveReloadImpl(VaadinService service, ClassLoader classLoader) {
        this.service = service;
        this.classLoader = classLoader;
    }

    @Override
    public Backend getBackend() {
        if (backend == null) {
            for (Map.Entry<Backend, List<String>> entry : IDENTIFIER_CLASSES
                    .entrySet()) {
                Backend backendCandidate = entry.getKey();
                boolean found = true;
                for (String clazz : entry.getValue()) {
                    try {
                        classLoader.loadClass(clazz);
                    } catch (ClassNotFoundException e) { // NOSONAR
                        getLogger().debug("Class {} not found, excluding {}",
                                clazz, backendCandidate);
                        found = false;
                        break;
                    }
                }
                if (found) {
                    backend = backendCandidate;
                    break;
                }
            }
        }
        return backend;
    }

    @Override
    public void setBackend(Backend backend) {
        assert (backend != null);
        this.backend = backend;
    }


    @Override
    public void onConnect(AtmosphereResource resource) {
        resource.suspend(-1);
        atmosphereResources.add(new WeakReference<>(resource));
        resource.getBroadcaster().broadcast("{\"command\": \"hello\"}",
                resource);
    }

    @Override
    public void onDisconnect(AtmosphereResource resource) {
        if (!atmosphereResources
                .removeIf(resourceRef -> resource.equals(resourceRef.get()))) {
            String uuid = resource.uuid();
            getLogger().warn(
                    "Push connection {} is not a live-reload connection or already closed",
                    uuid);
        }
    }

    @Override
    public boolean isLiveReload(AtmosphereResource resource) {
        return atmosphereResources.stream()
                .anyMatch(resourceRef -> resource.equals(resourceRef.get()));
    }

    @Override
    public void reload() {
        atmosphereResources.forEach(resourceRef -> {
            AtmosphereResource resource = resourceRef.get();
            if (resource != null) {
                resource.getBroadcaster().broadcast("{\"command\": \"reload\"}",
                        resource);
            }
        });
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(BrowserLiveReloadImpl.class.getName());
    }
}
