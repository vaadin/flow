/*
 * Copyright 2000-2021 Vaadin Ltd.
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

package com.vaadin.base.devserver.stats;

import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.JsonNode;
import com.vaadin.base.devserver.ServerInfo;
import com.vaadin.flow.server.Version;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import elemental.json.JsonObject;

/**
 * Singleton for collecting development time usage metrics
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since
 */
public class DevModeUsageStatistics {

    private static DevModeUsageStatistics instance = null;

    private final StatisticsStorage storage;

    private final boolean statisticsEnabled;

    private final String projectFolder;

    /**
     * Creates the instance.
     * 
     * @param projectFolder
     *            the project root folder
     * @param storage
     *            the storage instance to use
     * @param statisticsEnabled
     *            {@code true} to enable stats, {@code false} otherwise
     */
    private DevModeUsageStatistics(String projectFolder,
            StatisticsStorage storage, boolean statisticsEnabled) {
        this.projectFolder = projectFolder;
        this.storage = storage;
        this.statisticsEnabled = statisticsEnabled;
    }

    /**
     * Gets the singleton instance.
     */
    public static DevModeUsageStatistics get() {
        return instance;
    }

    /**
     * Initialize statistics module. This should be done on devmode startup.
     * First check if statistics collection is enabled.
     *
     * @param config
     *            application configuration parameters
     * @param projectFolder
     *            the folder of the current project
     * @param storage
     *            the statistics storage to use
     */
    public static DevModeUsageStatistics init(ApplicationConfiguration config,
            String projectFolder, StatisticsStorage storage) {

        boolean enabled = (config != null && !config.isProductionMode()
                && config.isUsageStatisticsEnabled());

        getLogger().debug("Telemetry " + (enabled ? "enabled" : "disabled"));

        synchronized (DevModeUsageStatistics.class) { // Lock data for init
            if (instance != null) {
                getLogger().warn("init should only be called once");
            }

            instance = new DevModeUsageStatistics(projectFolder, storage,
                    enabled);
            if (enabled) {
                instance.trackGlobalData();
                // Send usage statistics asynchronously, if enough time has
                // passed
                if (storage.isIntervalElapsed()) {
                    CompletableFuture.runAsync(instance::sendCurrentStatistics);
                }
            }
            return instance;
        }

    }

    private void trackGlobalData() {
        if (!isStatisticsEnabled()) {
            return;
        }
        // Read the current statistics data
        storage.update(storage -> {
            // Make sure we are tracking the right project
            storage.setProjectId(
                    ProjectHelpers.generateProjectId(projectFolder));

            ServerInfo serverInfo = new ServerInfo();

            // Update the machine / user / source level data
            storage.setGlobalValue(StatisticsConstants.FIELD_OPERATING_SYSTEM,
                    serverInfo.getOsVersion());
            storage.setGlobalValue(StatisticsConstants.FIELD_JVM,
                    serverInfo.getJavaVersion());
            storage.setGlobalValue(StatisticsConstants.FIELD_PROKEY,
                    ProjectHelpers.getProKey());
            storage.setGlobalValue(StatisticsConstants.FIELD_USER_KEY,
                    ProjectHelpers.getUserKey());

            // Update basic project statistics and save
            storage.setValue(StatisticsConstants.FIELD_FLOW_VERSION,
                    Version.getFullVersion());
            storage.setValue(StatisticsConstants.FIELD_SOURCE_ID,
                    ProjectHelpers.getProjectSource(projectFolder));
            storage.increment(StatisticsConstants.FIELD_PROJECT_DEVMODE_STARTS);
        });

    }

    /**
     * Stores telemetry data received from the browser.
     * 
     * @param browserData
     *            the data
     */
    public void handleBrowserData(JsonObject data) {
        getLogger().debug("Received client usage statistics from the browser");

        if (!isStatisticsEnabled()) {
            return;
        }

        try {
            // Update the stored data
            String json = data.get("browserData").toJson();
            JsonNode clientData = JsonHelpers.getJsonMapper().readTree(json);

            storage.update(
                    storage -> storage.updateProjectTelemetryData(clientData));

        } catch (Exception e) {
            getLogger().debug("Error handling telemetry request", e);
        }
    }

    /**
     * Checks if usage statistic collection is currently enabled.
     *
     * @return {@code true} if statistics are collected, {@code false}
     *         otherwise.
     */
    public boolean isStatisticsEnabled() {
        return statisticsEnabled;
    }

    /**
     * Checks if tracking is enabled and increments specified event count for
     * the current project data.
     * <p>
     * Good for logging statistics of recurring events.
     *
     * @param name
     *            Name of the event.
     */
    public void collectEvent(String name) {
        if (!isStatisticsEnabled()) {
            return;
        }

        try {
            storage.update(storage -> storage.increment(name));
        } catch (Exception e) {
            getLogger().debug("Failed to log '" + name + "'", e);
        }
    }

    /**
     * Update a value in usage statistics. Also, automatically aggregates min,
     * max and average of the value.
     * <p>
     * Good for logging statistics about chancing values over time.
     *
     * @param name
     *            Name of the field to update.
     * @param value
     *            The new value to store.
     */
    public void collectEvent(String name, double value) {
        if (!isStatisticsEnabled())
            return;

        try {
            storage.update(storage -> storage.aggregate(name, value));
        } catch (Exception e) {
            getLogger().debug("Failed to collect event '" + name + "'", e);
        }
    }

    /**
     * Send current statistics to given reporting URL.
     * <p>
     * Reads the current data and posts it to given URL. Updates or replaces the
     * local data according to the response.
     * <p>
     * Updates <code>FIELD_LAST_SENT</code> and <code>FIELD_LAST_STATUS</code>
     * and <code>FIELD_SERVER_MESSAGE</code>
     */
    void sendCurrentStatistics() {
        if (!isStatisticsEnabled())
            return;

        try {
            storage.update(storage -> {
                String message = storage.sendCurrentStatistics();

                // Show message on console, if present
                if (message != null && !message.trim().isEmpty()) {
                    getLogger().info(message);
                }

            });
        } catch (Exception e) {
            getLogger().debug("Failed to send statistics", e);
        }

    }

    /**
     * Set value of string value in current project statistics data.
     *
     * @param name
     *            name of the field to set.
     * @param value
     *            the new string value to set.
     */
    public void set(String name, String value) {
        if (!isStatisticsEnabled())
            return;

        try {
            storage.update(storage -> storage.setValue(name, value));
        } catch (Exception e) {
            getLogger().debug("Failed to set  '" + name + "'", e);
        }
    }

    /**
     * Set value of string field in current statistics data.
     *
     * @param name
     *            name of the field to set.
     * @param value
     *            the new string value to set.
     */
    public void setGlobal(String name, String value) {
        if (!isStatisticsEnabled())
            return;

        try {
            storage.update(storage -> storage.setGlobalValue(name, value));
        } catch (Exception e) {
            getLogger().debug("Failed to set global '" + name + "'", e);
        }
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(DevModeUsageStatistics.class);
    }

}
