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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vaadin.flow.server.Version;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Singleton for collecting development time usage metrics
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since
 */
public class DevModeUsageStatistics {

    /**
     * Initialize statistics module. This should be done on devmode startup.
     * First check if statistics collection is enabled.
     *
     *
     * @param config Application configuration parameters.
     * @param projectFolder Folder of the working project.
     * @return VaadinUsageStatistics instance in case everything is ok, null otherwise
     */
    public static void init(ApplicationConfiguration config, String projectFolder) {

        final StatisticsStorage stats = StatisticsStorage.get();

        synchronized (stats) { // Lock data for init
            stats.setStatisticsEnabled(
                    config != null && !config.isProductionMode() && config.isUsageStatisticsEnabled());
            if (stats.isStatisticsEnabled()) {
                getLogger().debug("VaadinUsageStatistics enabled");
            } else {
                getLogger().debug("VaadinUsageStatistics disabled");
                return; // Do not go any further
            }

            // Read the current statistics data
            stats.read();

            // Make sure we are tracking the right project
            stats.setProjectId(ProjectHelpers.generateProjectId(projectFolder));

            // Update the machine / user / source level data
            stats.setGlobalValue(StatisticsConstants.FIELD_OPEARATING_SYSTEM,
                    ProjectHelpers.getOperatingSystem());
            stats.setGlobalValue(StatisticsConstants.FIELD_JVM, ProjectHelpers.getJVMVersion());
            stats.setGlobalValue(StatisticsConstants.FIELD_PROKEY,
                    ProjectHelpers.getProKey());
            stats.setGlobalValue(StatisticsConstants.FIELD_USER_KEY,
                    ProjectHelpers.getUserKey());

            // Update basic project statistics and save
            stats.setValue(StatisticsConstants.FIELD_FLOW_VERSION, Version.getFullVersion());
            stats.setValue(StatisticsConstants.FIELD_SOURCE_ID, ProjectHelpers.getProjectSource(projectFolder));
            stats.increment(StatisticsConstants.FIELD_PROJECT_DEVMODE_STARTS);

            //Store the data immediately
            stats.write();

            // Send usage statistics asynchronously, if enough time has passed
            if (stats.isIntervalElapsed()) {
                CompletableFuture.runAsync(() -> {
                    stats.sendCurrentStatistics();
                });
            }
        }
    }

    /**
     *  Send current statistics to given reporting URL.
     *
     * Reads the current data and posts it to given URL. Updates or replaces
     * the local data according to the response.
     *
     * Updates <code>FIELD_LAST_SENT</code> and <code>FIELD_LAST_STATUS</code>
     * and <code>FIELD_SERVER_MESSAGE</code>
     *
     */
    static void sendCurrentStatistics() {
        final StatisticsStorage stats = StatisticsStorage.get();
        synchronized (stats) { // Lock data for send
            stats.read();
            String message = stats.sendCurrentStatistics();
            stats.write();

            // Show message on console, if present
            if (message != null && !message.trim().isEmpty()) {
                getLogger().info(message);
            }

        }
    }


    /** Handles a client-side request to receive component telemetry data.
    *
    * @return <code>true</code> if request was handled, <code>false</code> otherwise.
    */
    public static boolean handleClientUsageData(HttpServletRequest request, HttpServletResponse response) {

        if (!StatisticsStorage.get().isStatisticsEnabled()) {
            return false;
        }

        if (request.getParameter(StatisticsConstants.CLIENT_USAGE_DATA) != null && request.getMethod().equals("POST")) {
            getLogger().debug("Received client usage statistics POST from browser");
            try {
                if (request.getContentType() == null || !request.getContentType().equals("application/json")) {
                    // Content type should be correct
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    return true;
                }
                if (request.getContentLength() > StatisticsConstants.MAX_TELEMETRY_LENGTH) {
                    // Do not store meaningless amount of client usage data
                    getLogger().debug("Received too much data. Not storing "+request.getContentLength());
                    ObjectNode clientData = JsonHelpers.getJsonMapper().createObjectNode();
                    clientData.set("elements", clientData);
                    StatisticsStorage stats = StatisticsStorage.get();
                    synchronized (stats) { // Lock data for update
                        stats.read();
                        stats.updateProjectTelemetryData(clientData);
                        stats.write();
                    }
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    return true;
                } else {
                    // Backward compatible parsing: The request contains an explanation,
                    // and the json starts with the first "{"
                    String data = IOUtils.toString(request.getReader());
                    if (!data.contains("{")) {
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                        return true;
                    }
                    String json = data.substring(data.indexOf("{"));

                    // Update the stored data
                    JsonNode clientData = JsonHelpers.getJsonMapper().readTree(json);
                    StatisticsStorage stats = StatisticsStorage.get();
                    synchronized (stats) { // Lock data for update
                        stats.read();
                        stats.updateProjectTelemetryData(clientData);
                        stats.write();
                    }
                }

            } catch (Exception e) {
                getLogger().debug("Failed to handle client update request", e);
            } finally {
                try {
                    response.getWriter().write("Thank you");
                } catch (IOException e) {
                    getLogger().debug("Failed to write client response", e);
                }
            }
            return true;
        }
        return false;
    }

    /** Is usage statistic collection currently enabled.
     *
     * @see StatisticsStorage#isStatisticsEnabled()
     * @return
     */
    public static boolean isStatisticsEnabled() {
        return StatisticsStorage.get().isStatisticsEnabled();
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(DevModeUsageStatistics.class.getName());
    }

}
