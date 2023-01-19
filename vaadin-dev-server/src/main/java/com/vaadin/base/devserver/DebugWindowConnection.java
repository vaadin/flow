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

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import org.atmosphere.cpr.AtmosphereResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.base.devserver.stats.DevModeUsageStatistics;
import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.BrowserLiveReload;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.pro.licensechecker.BuildType;
import com.vaadin.pro.licensechecker.LicenseChecker;
import com.vaadin.pro.licensechecker.LocalProKey;
import com.vaadin.pro.licensechecker.Product;

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

    private final ConcurrentLinkedQueue<WeakReference<AtmosphereResource>> atmosphereResources = new ConcurrentLinkedQueue<>();

    private Backend backend = null;

    private static final EnumMap<Backend, List<String>> IDENTIFIER_CLASSES = new EnumMap<>(
            Backend.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    private IdeIntegration ideIntegration;

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
        this.ideIntegration = new IdeIntegration(
                ApplicationConfiguration.get(context));
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
    public void onConnect(AtmosphereResource resource) {
        resource.suspend(-1);
        atmosphereResources.add(new WeakReference<>(resource));
        resource.getBroadcaster().broadcast("{\"command\": \"hello\"}",
                resource);

        send(resource, "serverInfo", new ServerInfo());
        send(resource, "featureFlags", new FeatureFlagMessage(FeatureFlags
                .get(context).getFeatures().stream()
                .filter(feature -> !feature.equals(FeatureFlags.EXAMPLE))
                .collect(Collectors.toList())));

        if (LocalProKey.get() != null) {
            send(resource, "vaadin-dev-tools-code-ok", null);
        }
    }

    private void send(AtmosphereResource resource, String command,
            Object data) {
        try {
            resource.getBroadcaster().broadcast(objectMapper.writeValueAsString(
                    new DebugWindowMessage(command, data)), resource);
        } catch (Exception e) {
            getLogger().error("Error sending message", e);
        }
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

    @SuppressWarnings("FutureReturnValueIgnored")
    @Override
    public void onMessage(AtmosphereResource resource, String message) {
        if (message.isEmpty()) {
            getLogger().debug("Received live reload heartbeat");
            return;
        }
        JsonObject json = Json.parse(message);
        String command = json.getString("command");
        JsonObject data = json.getObject("data");
        if ("setFeature".equals(command)) {
            FeatureFlags.get(context).setEnabled(data.getString("featureId"),
                    data.getBoolean("enabled"));
        } else if ("reportTelemetry".equals(command)) {
            DevModeUsageStatistics.handleBrowserData(data);
        } else if ("checkLicense".equals(command)) {
            String name = data.getString("name");
            String version = data.getString("version");
            Product product = new Product(name, version);
            boolean ok;
            String errorMessage = "";

            try {
                LicenseChecker.checkLicense(product.getName(),
                        product.getVersion(), BuildType.DEVELOPMENT, keyUrl -> {
                            send(resource, "license-check-nokey",
                                    new ProductAndMessage(product, keyUrl));
                        });
                ok = true;
            } catch (Exception e) {
                ok = false;
                errorMessage = e.getMessage();
            }
            if (ok) {
                send(resource, "license-check-ok", product);
            } else {
                ProductAndMessage pm = new ProductAndMessage(product,
                        errorMessage);
                send(resource, "license-check-failed", pm);
            }
        } else if ("showComponentCreateLocation".equals(command)
                || "showComponentAttachLocation".equals(command)) {
            int nodeId = (int) data.getNumber("nodeId");
            int uiId = (int) data.getNumber("uiId");
            VaadinSession session = VaadinSession.getCurrent();
            session.access(() -> {
                Element element = session.findElement(uiId, nodeId);
                Optional<Component> c = element.getComponent();
                if (c.isPresent()) {
                    if ("showComponentCreateLocation".equals(command)) {
                        ideIntegration.showComponentCreateInIde(c.get());
                    } else {
                        ideIntegration.showComponentAttachInIde(c.get());
                    }
                } else {
                    getLogger().error(
                            "Only component locations are tracked. The given node id refers to an element and not a component");
                }
            });
        } else {
            getLogger().info("Unknown command from the browser: " + command);
        }
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(DebugWindowConnection.class.getName());
    }

}
