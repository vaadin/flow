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
package com.vaadin.flow.testutil;

import java.time.Duration;
import java.util.HashMap;
import java.util.Optional;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.devtools.Command;
import org.openqa.selenium.devtools.Connection;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.HasDevTools;
import org.openqa.selenium.devtools.SeleniumCdpConnection;
import org.openqa.selenium.devtools.idealized.Domains;
import org.openqa.selenium.devtools.idealized.target.model.SessionID;
import org.openqa.selenium.devtools.idealized.target.model.TargetID;
import org.openqa.selenium.devtools.v103.network.Network;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.remote.RemoteWebDriver;

public class DevToolsWrapper {
    private final WebDriver driver;
    private final Duration timeout = Duration.ofSeconds(3);
    private final HashMap<TargetID, SessionID> attachedTargets = new HashMap<TargetID, SessionID>();
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
        sendToAllTargets(Network.enable(Optional.empty(), Optional.empty(),
                Optional.empty()));
        sendToAllTargets(Network.emulateNetworkConditions(isEnabled, -1, -1, -1,
                Optional.empty()));
    }

    /**
     * Controls the `Disable cache` option in DevTools via the corresponding
     * Selenium API.
     *
     * @param isDisabled
     *            whether to disable the browser cache.
     */
    public void setCacheDisabled(Boolean isDisabled) {
        sendToAllTargets(Network.enable(Optional.empty(), Optional.empty(),
                Optional.empty()));
        sendToAllTargets(Network.setCacheDisabled(isDisabled));
    }

    /**
     * Creates a custom DevTools CDP connection if there is not one yet.
     *
     * Note, there is already a CDP connection provided by {@link DevTools} but
     * it allows sending commands only to the page session whereas we need to
     * also send commands to service workers. Therefore a custom connection is
     * necessary.
     */
    private void createConnectionIfThereIsNotOne() {
        if (connection == null) {
            connection = SeleniumCdpConnection.create(driver).get();
        }
    }

    /**
     * Attaches to all the available targets by creating a session per each.
     * These sessions can be later used for sending commands to the
     * corresponding targets.
     *
     * Every target represents a certain browser page, service worker and etc.
     *
     * Read more about targets and sessions here:
     * https://github.com/aslushnikov/getting-started-with-cdp#targets--sessions
     */
    private void attachToAllTargets() {
        createConnectionIfThereIsNotOne();

        connection
                .sendAndWait(null, getDomains().target().getTargets(), timeout)
                .stream()
                .filter((target) -> !attachedTargets
                        .containsKey(target.getTargetId()))
                .forEach((target) -> {
                    TargetID targetId = target.getTargetId();
                    SessionID sessionId = connection.sendAndWait(null,
                            getDomains().target().attachToTarget(targetId),
                            timeout);
                    attachedTargets.put(targetId, sessionId);
                });
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

    private DevTools getDevTools() {
        WebDriver driver = new Augmenter()
                .augment((RemoteWebDriver) this.driver);
        return ((HasDevTools) driver).getDevTools();
    }

    private Domains getDomains() {
        return getDevTools().getDomains();
    }
}
