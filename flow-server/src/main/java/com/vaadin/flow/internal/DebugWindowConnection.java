/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
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

import com.vaadin.flow.server.DevModeHandler;

import elemental.json.Json;
import elemental.json.JsonObject;

import com.vaadin.pro.licensechecker.BuildType;
import com.vaadin.pro.licensechecker.LicenseChecker;
import com.vaadin.pro.licensechecker.Product;

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

    private final ConcurrentLinkedQueue<WeakReference<AtmosphereResource>> atmosphereResources = new ConcurrentLinkedQueue<>();

    private Backend backend = null;

    private static final EnumMap<Backend, List<String>> IDENTIFIER_CLASSES = new EnumMap<>(
            Backend.class);

    static {
        IDENTIFIER_CLASSES.put(Backend.JREBEL, Collections.singletonList(
                "org.zeroturnaround.jrebel.vaadin.JRebelClassEventListener"));
        IDENTIFIER_CLASSES.put(Backend.HOTSWAP_AGENT, Collections.singletonList(
                "org.hotswap.agent.plugin.vaadin.VaadinIntegration"));
        IDENTIFIER_CLASSES.put(Backend.SPRING_BOOT_DEVTOOLS, Arrays.asList(
                "com.vaadin.flow.spring.SpringServlet",
                "org.springframework.boot.devtools.livereload.LiveReloadServer"));
    }

    DebugWindowConnection() {
        this(DebugWindowConnection.class.getClassLoader());
    }

    DebugWindowConnection(ClassLoader classLoader) {
        this.classLoader = classLoader;

        DevModeHandler devModeHandler = DevModeHandler.getDevModeHandler();
        if (devModeHandler != null) {
            devModeHandler.setLiveReload(this);
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

    @Override
    public void onConnect(AtmosphereResource resource) {
        resource.suspend(-1);
        atmosphereResources.add(new WeakReference<>(resource));
        resource.getBroadcaster().broadcast("{\"command\": \"hello\"}",
                resource);

        send(resource, "serverInfo", new ServerInfo());
    }

    private void send(AtmosphereResource resource, String command,
            DebugWindowData data) {
        try {
            DebugWindowMessage message = new DebugWindowMessage(command, data);
            resource.getBroadcaster().broadcast(message.toJson(), resource);
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

    private static Logger getLogger() {
        return LoggerFactory.getLogger(DebugWindowConnection.class.getName());
    }

    @Override
    public void onMessage(AtmosphereResource resource, String message) {
        if (message.isEmpty()) {
            getLogger().debug("Received live reload heartbeat");
            return;
        }
        JsonObject json = Json.parse(message);
        String command = json.getString("command");
        if ("checkLicense".equals(command)) {
            JsonObject data = json.getObject("data");
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
                send(resource, "license-check-ok", getProductJson(product));
            } else {
                ProductAndMessage pm = new ProductAndMessage(product,
                        errorMessage);
                send(resource, "license-check-failed", pm);
            }
        } else {
            getLogger().info("Unknown command from the browser: " + command);
        }
    }

    private DebugWindowData getProductJson(Product product) {
        return () -> String.format("{\"name\": \"%s\", \"version\": \"%s\"}",
                product.getName(), product.getVersion());
    }
}
