/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import java.io.File;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.base.devserver.ServerInfo;
import com.vaadin.flow.server.Version;
import com.vaadin.pro.licensechecker.MachineId;

/**
 * Singleton for collecting development time usage metrics
 * <p>
 * All statistics gathering methods in this class are static for easy caller
 * code and they immediately update the stored JSON on disk.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since
 */
public class DevModeUsageStatistics {

    private static DevModeUsageStatistics instance = null;

    private final StatisticsStorage storage;

    private final File projectFolder;

    /**
     * Creates the instance.
     *
     * @param projectFolder
     *            the project root folder
     * @param storage
     *            the storage instance to use
     */
    private DevModeUsageStatistics(File projectFolder,
            StatisticsStorage storage) {
        this.projectFolder = projectFolder;
        this.storage = storage;
    }

    /**
     * Gets the singleton instance.
     *
     * @return the singleton instance
     */
    static DevModeUsageStatistics get() {
        return instance;
    }

    /**
     * Initialize the statistics module.
     * <p>
     * This should only ever be called in development mode.
     *
     * @param projectFolder
     *            the folder of the current project
     * @param storage
     *            the statistics storage to use
     * @param sender
     *            the statistics sender to use
     *
     * @return the created instance or {@code null} if telemetry is not used
     */
    public static DevModeUsageStatistics init(File projectFolder,
            StatisticsStorage storage, StatisticsSender sender) {

        getLogger().debug("Telemetry enabled");

        storage.access(() -> {
            instance = new DevModeUsageStatistics(projectFolder, storage);
            // Make sure we are tracking the right project
            String projectId = ProjectHelpers.generateProjectId(projectFolder);
            storage.setProjectId(projectId);
            instance.trackGlobalData();
            sender.triggerSendIfNeeded(storage.read());
        });

        return instance;
    }

    private void trackGlobalData() {
        storage.update((globalData, projectData) -> {
            // Update the machine / user / source level data
            globalData.setValue(StatisticsConstants.FIELD_OPERATING_SYSTEM,
                    ServerInfo.fetchOperatingSystem());
            globalData.setValue(StatisticsConstants.FIELD_JVM,
                    ServerInfo.fetchJavaVersion());
            globalData.setValue(StatisticsConstants.FIELD_PROKEY,
                    ProjectHelpers.getProKey());
            globalData.setValue(StatisticsConstants.FIELD_USER_KEY,
                    ProjectHelpers.getUserKey());
            try {
                globalData.setValue(StatisticsConstants.FIELD_MACHINE_ID,
                        MachineId.get());
            } catch (Throwable ex) {
                globalData.setValue(StatisticsConstants.FIELD_MACHINE_ID,
                        "ERROR");
                getLogger().debug("Cannot get Machine ID", ex);
            }

            // Update basic project statistics and save
            projectData.setValue(StatisticsConstants.FIELD_FLOW_VERSION,
                    Version.getFullVersion());
            projectData.setValue(StatisticsConstants.FIELD_VAADIN_VERSION,
                    ServerInfo.fetchVaadinVersion());
            projectData.setValue(StatisticsConstants.FIELD_HILLA_VERSION,
                    ServerInfo.fetchHillaVersion());
            projectData.setValue(StatisticsConstants.FIELD_SOURCE_ID,
                    ProjectHelpers.getProjectSource(projectFolder));
            projectData.increment(
                    StatisticsConstants.FIELD_PROJECT_DEVMODE_STARTS);
        });

    }

    /**
     * Stores telemetry data received from the browser.
     *
     * @param data
     *            the data from the browser
     */
    public static void handleBrowserData(JsonNode data) {
        getLogger().debug("Received client usage statistics from the browser");

        if (!isStatisticsEnabled()) {
            return;
        }

        get().storage.update((global, project) -> {
            try {
                String json = data.get("browserData").toString();
                JsonNode clientData = JsonHelpers.getJsonMapper()
                        .readTree(json);
                if (clientData != null && clientData.isObject()) {
                    clientData.fields().forEachRemaining(
                            e -> project.setValue(e.getKey(), e.getValue()));
                }
            } catch (Exception e) {
                getLogger().debug("Failed to update client telemetry data", e);
            }
        });

    }

    /**
     * Checks if usage statistic collection is currently enabled.
     *
     * @return {@code true} if statistics are collected, {@code false}
     *         otherwise.
     */
    static boolean isStatisticsEnabled() {
        return instance != null;
    }

    /**
     * Increments specified event count in the current project data.
     * <p>
     * Good for logging statistics of recurring events.
     *
     * @param name
     *            Name of the event.
     */
    public static void collectEvent(String name) {
        if (!isStatisticsEnabled()) {
            return;
        }

        try {
            get().storage.update((global, project) -> project.increment(name));
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
    public static void collectEvent(String name, double value) {
        if (!isStatisticsEnabled())
            return;

        try {
            get().storage.update(
                    (global, project) -> project.aggregate(name, value));
        } catch (Exception e) {
            getLogger().debug("Failed to collect event '" + name + "'", e);
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
            storage.update((global, project) -> project.setValue(name, value));
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
            storage.update((global, project) -> global.setValue(name, value));
        } catch (Exception e) {
            getLogger().debug("Failed to set global '" + name + "'", e);
        }
    }

    static Logger getLogger() {
        return LoggerFactory.getLogger(DevModeUsageStatistics.class);
    }

}
