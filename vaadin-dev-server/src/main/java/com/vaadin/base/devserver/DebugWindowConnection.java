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
package com.vaadin.base.devserver;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.base.devserver.stats.DevModeUsageStatistics;
import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.internal.BrowserLiveReload;
import com.vaadin.flow.server.VaadinContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * {@link BrowserLiveReload} implementation class.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 *
 */
public class DebugWindowConnection implements BrowserLiveReload {

    private final ClassLoader classLoader;
    private VaadinContext context;

    private Backend backend = null;

    private static final EnumMap<Backend, List<String>> IDENTIFIER_CLASSES = new EnumMap<>(
            Backend.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    static {
        IDENTIFIER_CLASSES.put(Backend.JREBEL, Collections.singletonList(
                "org.zeroturnaround.jrebel.vaadin.JRebelClassEventListener"));
        IDENTIFIER_CLASSES.put(Backend.HOTSWAP_AGENT, Collections.singletonList(
                "org.hotswap.agent.plugin.vaadin.VaadinIntegration"));
        IDENTIFIER_CLASSES.put(Backend.SPRING_BOOT_DEVTOOLS, Arrays.asList(
                "com.vaadin.flow.spring.SpringServlet",
                "org.springframework.boot.devtools.livereload.LiveReloadServer"));
    }

    DebugWindowConnection(VaadinContext context) {
        this(DebugWindowConnection.class.getClassLoader(), context);
    }

    DebugWindowConnection(ClassLoader classLoader, VaadinContext context) {
        this.classLoader = classLoader;
        this.context = context;
    }

    @Override
    public Backend getBackend() {
        if (backend != null) {
            return backend;
        }
        for (Map.Entry<Backend, List<String>> entry : IDENTIFIER_CLASSES
                .entrySet()) {
            Backend backendCandidate = entry.getKey();
            boolean found = true;
            for (String clazz : entry.getValue()) {
                try {
                    classLoader.loadClass(clazz);
                } catch (ClassNotFoundException e) { // NOSONAR
                    getLogger().debug("Class {} not found, excluding {}", clazz,
                            backendCandidate);
                    found = false;
                    break;
                }
            }
            if (found) {
                backend = backendCandidate;
                break;
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
    public void reload() {
    }

    @Override
    public void onMessage(String message) {
        if (message.isEmpty()) {
            getLogger().debug("Received live reload heartbeat");
            return;
        }
        JsonObject json = Json.parse(message);
        String command = json.getString("command");
        if ("setFeature".equals(command)) {
            JsonObject data = json.getObject("data");
            FeatureFlags.get(context).setEnabled(data.getString("featureId"),
                    data.getBoolean("enabled"));
        } else if ("reportTelemetry".equals(command)) {
            JsonObject data = json.getObject("data");
            DevModeUsageStatistics.handleBrowserData(data);
        }
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(DebugWindowConnection.class.getName());
    }

}
