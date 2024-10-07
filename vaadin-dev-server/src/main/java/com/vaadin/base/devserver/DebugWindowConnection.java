/*
 * Copyright 2000-2024 Vaadin Ltd.
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

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.atmosphere.cpr.AtmosphereResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.base.devserver.stats.DevModeUsageStatistics;
import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.BrowserLiveReload;
import com.vaadin.flow.server.DevToolsToken;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.communication.AtmospherePushConnection.FragmentedMessage;
import com.vaadin.pro.licensechecker.BuildType;
import com.vaadin.pro.licensechecker.LicenseChecker;
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

    private final ConcurrentHashMap<WeakReference<AtmosphereResource>, FragmentedMessage> resources = new ConcurrentHashMap<>();
    private Backend backend = null;

    private static final EnumMap<Backend, List<String>> IDENTIFIER_CLASSES = new EnumMap<>(
            Backend.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    private List<DevToolsMessageHandler> plugins;

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

        findPlugins();
    }

    private void findPlugins() {
        ServiceLoader<DevToolsMessageHandler> loader = ServiceLoader
                .load(DevToolsMessageHandler.class, classLoader);
        this.plugins = new ArrayList<>();
        for (DevToolsMessageHandler s : loader) {
            this.plugins.add(s);
        }
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

    /** Implementation of the development tools interface. */
    public static class DevToolsInterfaceImpl implements DevToolsInterface {
        private DebugWindowConnection debugWindowConnection;
        private AtmosphereResource resource;

        private DevToolsInterfaceImpl(
                DebugWindowConnection debugWindowConnection,
                AtmosphereResource resource) {
            this.debugWindowConnection = debugWindowConnection;
            this.resource = resource;
        }

        @Override
        public void send(String command, JsonObject data) {
            JsonObject msg = Json.createObject();
            msg.put("command", command);
            if (data != null) {
                msg.put("data", data);
            }

            debugWindowConnection.send(resource, msg.toJson());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            DevToolsInterfaceImpl that = (DevToolsInterfaceImpl) o;
            return Objects.equals(debugWindowConnection,
                    that.debugWindowConnection)
                    && Objects.equals(resource, that.resource);
        }

        @Override
        public int hashCode() {
            return Objects.hash(debugWindowConnection, resource);
        }
    }

    protected DevToolsInterface getDevToolsInterface(
            AtmosphereResource resource) {
        return new DevToolsInterfaceImpl(this, resource);
    }

    @Override
    public void onConnect(AtmosphereResource resource) {
        if (DevToolsToken.getToken()
                .equals(resource.getRequest().getParameter("token"))) {
            handleConnect(resource);
        } else {
            getLogger().debug(
                    "Connection denied because of a missing or invalid token. Either the host is not on the 'vaadin.devmode.hosts-allowed' list or it is using an outdated token");
            try {
                resource.close();
            } catch (IOException e) {
                getLogger().debug(
                        "Error closing the denied websocket connection", e);
            }
        }
    }

    private void handleConnect(AtmosphereResource resource) {
        resource.suspend(-1);
        resources.put(new WeakReference<>(resource), new FragmentedMessage());
        resource.getBroadcaster().broadcast("{\"command\": \"hello\"}",
                resource);

        for (DevToolsMessageHandler plugin : plugins) {
            plugin.handleConnect(getDevToolsInterface(resource));
        }

        send(resource, "serverInfo", new ServerInfo());
        send(resource, "featureFlags", new FeatureFlagMessage(FeatureFlags
                .get(context).getFeatures().stream()
                .filter(feature -> !feature.equals(FeatureFlags.EXAMPLE))
                .collect(Collectors.toList())));

    }

    private void send(AtmosphereResource resource, String command,
            Object data) {
        try {
            send(resource, objectMapper
                    .writeValueAsString(new DebugWindowMessage(command, data)));
        } catch (Exception e) {
            getLogger().error("Error sending message", e);
        }

    }

    private void send(AtmosphereResource resource, String json) {
        resource.getBroadcaster().broadcast(json, resource);
    }

    @Override
    public void onDisconnect(AtmosphereResource resource) {
        for (DevToolsMessageHandler plugin : plugins) {
            plugin.handleDisconnect(getDevToolsInterface(resource));
        }
        if (!resources.keySet()
                .removeIf(resourceRef -> resource.equals(resourceRef.get()))) {
            String uuid = resource.uuid();
            getLogger().warn(
                    "Push connection {} is not a live-reload connection or already closed",
                    uuid);
        }
    }

    @Override
    public boolean isLiveReload(AtmosphereResource resource) {
        return getRef(resource) != null;
    }

    /**
     * Broadcasts the given message to all connected clients.
     *
     * @param msg
     *            the message to broadcast
     */
    public void broadcast(JsonObject msg) {
        resources.keySet().forEach(resourceRef -> {
            AtmosphereResource resource = resourceRef.get();
            if (resource != null) {
                resource.getBroadcaster().broadcast(msg.toJson(), resource);
            }
        });

    }

    @Override
    public void reload() {
        JsonObject msg = Json.createObject();
        msg.put("command", "reload");
        broadcast(msg);
    }

    @Override
    public void refresh(boolean refreshLayouts) {
        JsonObject msg = Json.createObject();
        msg.put("command", "reload");
        msg.put("strategy", refreshLayouts ? "full-refresh" : "refresh");
        broadcast(msg);
    }

    @Override
    public void update(String path, String content) {
        JsonObject msg = Json.createObject();
        msg.put("command", "update");
        msg.put("path", path);
        msg.put("content", content);
        broadcast(msg);
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
        } else {
            boolean handled = false;
            for (DevToolsMessageHandler plugin : plugins) {
                handled = plugin.handleMessage(command, data,
                        getDevToolsInterface(resource));
                if (handled) {
                    break;
                }
            }
            if (!handled && command != null
                    && !command.startsWith("copilot-")) {
                getLogger()
                        .info("Unknown command from the browser: " + command);
            }
        }
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(DebugWindowConnection.class.getName());
    }

    @Override
    public FragmentedMessage getOrCreateFragmentedMessage(
            AtmosphereResource resource) {
        WeakReference<AtmosphereResource> ref = getRef(resource);
        if (ref == null) {
            throw new IllegalStateException(
                    "Tried to create a fragmented message for a non-existing resource");
        }
        return resources.get(ref);
    }

    private WeakReference<AtmosphereResource> getRef(
            AtmosphereResource resource) {
        return resources.keySet().stream()
                .filter(resourceRef -> resource.equals(resourceRef.get()))
                .findFirst().orElse(null);
    }

    @Override
    public void clearFragmentedMessage(AtmosphereResource resource) {
        WeakReference<AtmosphereResource> ref = getRef(resource);
        if (ref == null) {
            getLogger().debug(
                    "Tried to clear the fragmented message for a non-existing resource: {}",
                    resource);
            return;
        }
        resources.put(ref, new FragmentedMessage());
    }

    @Override
    public void sendHmrEvent(String event, JsonObject eventData) {
        JsonObject msg = Json.createObject();
        msg.put("command", "hmr");
        JsonObject data = Json.createObject();
        msg.put("data", data);
        data.put("event", event);
        data.put("eventData", eventData);
        broadcast(msg);
    }

}
