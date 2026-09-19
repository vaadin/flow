/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.testutil;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.devtools.Command;
import org.openqa.selenium.devtools.Connection;
import org.openqa.selenium.devtools.ConverterFunctions;
import org.openqa.selenium.devtools.SeleniumCdpConnection;
import org.openqa.selenium.devtools.idealized.target.model.SessionID;
import org.openqa.selenium.json.Json;
import org.openqa.selenium.remote.http.ClientConfig;

/**
 * Controls browser network conditions through the Chrome DevTools Protocol.
 * <p>
 * The CDP commands are built by hand rather than through the generated
 * {@code org.openqa.selenium.devtools.vNNN} classes or the version-independent
 * {@code Domains} facade in front of them. Selenium picks a generated
 * implementation by matching the browser major version against the CDP
 * implementations it bundles, and it only ever falls back to an implementation
 * older than the browser. A Selenium release bundles the few newest Chrome
 * versions, so a browser older than all of them matches nothing and resolves to
 * a no-op implementation that throws on every call. The commands used here are
 * part of the stable CDP surface and are sent over the raw connection, which
 * makes them work with any Chrome version.
 */
public class DevToolsWrapper {
    private final WebDriver driver;
    private final Duration timeout = Duration.ofSeconds(3);
    private final HashMap<String, SessionID> attachedTargets = new HashMap<>();
    private Connection connection = null;

    public DevToolsWrapper(WebDriver driver) {
        this.driver = driver;
    }

    /**
     * Controls the throttling `Offline` option in DevTools via the
     * corresponding Selenium API.
     *
     * @param isEnabled
     *            whether to enable the offline mode.
     */
    public void setOfflineEnabled(Boolean isEnabled) {
        sendToAllTargets(new Command<>("Network.enable", Map.of()));
        sendToAllTargets(new Command<>("Network.emulateNetworkConditions",
                Map.of("offline", isEnabled, "latency", -1,
                        "downloadThroughput", -1, "uploadThroughput", -1)));
    }

    /**
     * Controls the `Disable cache` option in DevTools via the corresponding
     * Selenium API.
     *
     * @param isDisabled
     *            whether to disable the browser cache.
     */
    public void setCacheDisabled(Boolean isDisabled) {
        sendToAllTargets(new Command<>("Network.enable", Map.of()));
        sendToAllTargets(new Command<>("Network.setCacheDisabled",
                Map.of("cacheDisabled", isDisabled)));
    }

    public void close() {
        if (connection != null) {
            connection.close();
            connection = null;
        }
        attachedTargets.clear();
    }

    /**
     * Creates a custom DevTools CDP connection if there is not one yet.
     * <p>
     * Note, there is already a CDP connection provided by
     * {@link org.openqa.selenium.devtools.DevTools} but it allows sending
     * commands only to the page session whereas we need to also send commands
     * to service workers. Therefore a custom connection is necessary.
     */
    private void createConnectionIfThereIsNotOne() {
        if (connection == null) {
            connection = SeleniumCdpConnection
                    .create(driver, ClientConfig.defaultConfig()).get();
        }
    }

    /**
     * Attaches to all the available targets by creating a session per each.
     * These sessions can be later used for sending commands to the
     * corresponding targets.
     * <p>
     * Every target represents a certain browser page, service worker and etc.
     * <p>
     * Read more about targets and sessions here:
     * https://github.com/aslushnikov/getting-started-with-cdp#targets--sessions
     */
    private void attachToAllTargets() {
        createConnectionIfThereIsNotOne();

        Command<List<Map<String, Object>>> getTargets = new Command<>(
                "Target.getTargets", Map.of(),
                ConverterFunctions.map("targetInfos", Json.LIST_OF_MAPS_TYPE));

        connection.sendAndWait(null, getTargets, timeout).stream()
                .map(target -> String.valueOf(target.get("targetId")))
                .filter(targetId -> !attachedTargets.containsKey(targetId))
                .forEach(targetId -> attachedTargets.put(targetId,
                        attachToTarget(targetId)));
    }

    /**
     * Attaches to a single target and returns the id of the created session.
     * <p>
     * The session is flattened so that commands for it can be sent over the
     * same connection by setting the session id on the message, which is what
     * {@link Connection#sendAndWait(SessionID, Command, Duration)} does.
     */
    private SessionID attachToTarget(String targetId) {
        Command<SessionID> attachToTarget = new Command<>(
                "Target.attachToTarget",
                Map.of("targetId", targetId, "flatten", true),
                ConverterFunctions.map("sessionId", SessionID.class));
        return connection.sendAndWait(null, attachToTarget, timeout);
    }

    /**
     * Sends a DevTools command to all the available targets.
     */
    private <X> void sendToAllTargets(Command<X> command) {
        attachToAllTargets();

        for (SessionID sessionId : attachedTargets.values()) {
            connection.sendAndWait(sessionId, command, timeout);
        }
    }
}
