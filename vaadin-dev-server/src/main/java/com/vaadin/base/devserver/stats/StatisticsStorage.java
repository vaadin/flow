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

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vaadin.flow.server.Command;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Development more usage statistic storage and methods for loading, saving and
 * sending the data.
 */
public class StatisticsStorage {

    private static final String FAILED_TO_READ = "Failed to read ";
    private String projectId;
    private String usageReportingUrl;
    private File usageStatisticsFile;

    /**
     * Creates an instance.
     */
    public StatisticsStorage() {
        // Intentionally empty
    }

    /**
     * Get interval that is between {@link StatisticsConstants#TIME_SEC_12H} and
     * {@link StatisticsConstants#TIME_SEC_30D}
     *
     * @param intervalSec
     *            Interval to normalize
     * @return <code>interval</code> if inside valid range.
     */
    private static long normalizeInterval(long intervalSec) {
        if (intervalSec < StatisticsConstants.TIME_SEC_12H) {
            return StatisticsConstants.TIME_SEC_12H;
        }
        return Math.min(intervalSec, StatisticsConstants.TIME_SEC_30D);
    }

    /**
     * Posts given Json data to a URL.
     * <p>
     * Updates <code>FIELD_LAST_STATUS</code>.
     *
     * @param postUrl
     *            URL to post data to.
     * @param data
     *            Json data to send
     * @return Response or <code>data</code> if the data was not successfully
     *         sent.
     */
    private static ObjectNode postData(String postUrl, JsonNode data) {
        ObjectNode result;
        try {
            HttpPost post = new HttpPost(postUrl);
            post.addHeader("Content-Type", "application/json");
            post.setEntity(new StringEntity(
                    JsonHelpers.getJsonMapper().writeValueAsString(data)));

            HttpClient client = HttpClientBuilder.create().build();
            HttpResponse response = client.execute(post);
            String responseStatus = response.getStatusLine().getStatusCode()
                    + ": " + response.getStatusLine().getReasonPhrase();
            JsonNode jsonResponse = null;
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                String responseString = EntityUtils
                        .toString(response.getEntity());
                jsonResponse = JsonHelpers.getJsonMapper()
                        .readTree(responseString);
            }

            if (jsonResponse != null && jsonResponse.isObject()) {
                result = (ObjectNode) jsonResponse;
            } else {
                // Default response in case of any problems
                result = JsonHelpers.getJsonMapper().createObjectNode();
            }
            // Update the status and return the results
            result.put(StatisticsConstants.FIELD_LAST_STATUS, responseStatus);
            return result;

        } catch (IOException e) {
            getLogger().debug("Failed to send statistics.", e);
        }

        // Fallback
        result = JsonHelpers.getJsonMapper().createObjectNode();
        result.put(StatisticsConstants.FIELD_LAST_STATUS,
                StatisticsConstants.INVALID_SERVER_RESPONSE);
        return result;
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(StatisticsStorage.class);
    }

    /**
     * Sets the active project id.
     * <p>
     * The project id should be unique enough to avoid collisions and data
     * overwrites.
     * <p>
     * Used in {@link #update(BiConsumer)}.
     *
     * @param projectId
     *            The unique project id.
     */
    void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    /*
     * Gets the id for the active project.
     */
    String getProjectId() {
        return projectId;
    }

    /**
     * Get the remote reporting URL.
     *
     * @return Returns {@link StatisticsConstants#USAGE_REPORT_URL} by default.
     */
    String getUsageReportingUrl() {
        return usageReportingUrl == null ? StatisticsConstants.USAGE_REPORT_URL
                : usageReportingUrl;
    }

    /**
     * Set the remote reporting URL.
     *
     * If not set, <code>StatisticsConstants.USAGE_REPORT_URL</code> is used.
     *
     * @param reportingUrl
     *            Set the reporting URL.
     */
    void setUsageReportingUrl(String reportingUrl) {
        this.usageReportingUrl = reportingUrl;
    }

    /**
     * Helper to update client data in current project.
     *
     * @param clientData
     *            Json data received from client.
     */
    void updateProjectTelemetryData(JsonNode clientData) {
        try {
            if (clientData != null && clientData.isObject()) {
                update((global, project) -> {
                    clientData.fields().forEachRemaining(
                            e -> project.setValue(e.getKey(), e.getValue()));
                });
            }
        } catch (Exception e) {
            getLogger().debug("Failed to update client telemetry data", e);
        }
    }

    /**
     * Send current statistics to given reporting URL.
     * <p>
     * Reads the current data and posts it to given URL. Updates or replaces the
     * local data according to the response.
     *
     * @see #postData(String, JsonNode)
     */
    String sendCurrentStatistics(ObjectNode json) {
        // Post copy of the current data
        AtomicReference<String> message = new AtomicReference<>(null);
        JsonNode response = postData(getUsageReportingUrl(), json.deepCopy());

        // Update the last sent time
        // If the last send was successful we clear the project data
        if (response.isObject()
                && response.has(StatisticsConstants.FIELD_LAST_STATUS)) {
            update((global, project) -> {

                global.setValue(StatisticsConstants.FIELD_LAST_SENT,
                        System.currentTimeMillis());
                global.setValue(StatisticsConstants.FIELD_LAST_STATUS, response
                        .get(StatisticsConstants.FIELD_LAST_STATUS).asText());

                // Use different interval, if requested in response or default
                // to 24H
                if (response.has(StatisticsConstants.FIELD_SEND_INTERVAL)
                        && response.get(StatisticsConstants.FIELD_SEND_INTERVAL)
                                .isNumber()) {
                    global.setValue(StatisticsConstants.FIELD_SEND_INTERVAL,
                            normalizeInterval(response.get(
                                    StatisticsConstants.FIELD_SEND_INTERVAL)
                                    .asLong()));
                } else {
                    global.setValue(StatisticsConstants.FIELD_SEND_INTERVAL,
                            StatisticsConstants.TIME_SEC_24H);
                }

                // Update the server message
                if (response.has(StatisticsConstants.FIELD_SERVER_MESSAGE)
                        && response
                                .get(StatisticsConstants.FIELD_SERVER_MESSAGE)
                                .isTextual()) {
                    String msg = response
                            .get(StatisticsConstants.FIELD_SERVER_MESSAGE)
                            .asText();
                    global.setValue(StatisticsConstants.FIELD_SERVER_MESSAGE,
                            msg);
                    message.set(msg);
                }

                // If data was sent ok, clear the existing project data
                if (response.get(StatisticsConstants.FIELD_LAST_STATUS).asText()
                        .startsWith("200:")) {
                    global.setValue(StatisticsConstants.FIELD_PROJECTS,
                            JsonHelpers.getJsonMapper().createArrayNode());
                }
            });
        }

        return message.get();
    }

    /**
     * Check the Interval has elapsed.
     * <p>
     * Uses <code>System.currentTimeMillis</code> as time source.
     *
     * @return true if enough time has passed since the last send attempt.
     * @see #getLastSendTime()
     * @see #getInterval()
     */
    boolean isIntervalElapsed(ObjectNode json) {
        long now = System.currentTimeMillis();
        long lastSend = getLastSendTime(json);
        long interval = getInterval(json);
        return lastSend + interval * 1000 < now;
    }

    /**
     * Reads the statistics update interval.
     *
     * @return Time interval in seconds.
     *         {@link StatisticsConstants#TIME_SEC_24H} in minumun and
     *         {@link StatisticsConstants#TIME_SEC_30D} as maximum.
     * @see StatisticsConstants#FIELD_SEND_INTERVAL
     */
    long getInterval(ObjectNode json) {
        try {
            long interval = json.get(StatisticsConstants.FIELD_SEND_INTERVAL)
                    .asLong();
            return normalizeInterval(interval);
        } catch (Exception e) {
            // Just return the default value
            getLogger().debug(
                    FAILED_TO_READ + StatisticsConstants.FIELD_SEND_INTERVAL,
                    e);
        }
        return StatisticsConstants.TIME_SEC_24H;
    }

    /**
     * Gets the last time the data was collected according to the statistics
     * file.
     *
     * @return Unix timestamp or -1 if not present
     * @see StatisticsConstants#FIELD_LAST_SENT
     */
    long getLastSendTime(ObjectNode json) {
        try {
            return json.get(StatisticsConstants.FIELD_LAST_SENT).asLong();
        } catch (Exception e) {
            // Use default value in case of any problems
            getLogger().debug(
                    FAILED_TO_READ + StatisticsConstants.FIELD_LAST_SENT, e);
        }
        return -1; //
    }

    /**
     * Gets the last time the data was collected according to the statistics
     * file.
     *
     * @return Unix timestamp or -1 if not present
     * @see StatisticsConstants#FIELD_LAST_STATUS
     */
    String getLastSendStatus(ObjectNode json) {
        try {
            return json.get(StatisticsConstants.FIELD_LAST_STATUS).asText();
        } catch (Exception e) {
            // Use default value in case of any problems
            getLogger().debug(
                    FAILED_TO_READ + StatisticsConstants.FIELD_LAST_STATUS, e);
        }
        return null; //
    }

    /**
     * Runs the given command with the store locked.
     * 
     * @param whenLocked
     *            the command to run
     */
    public void access(Command whenLocked) {
        synchronized (DevModeUsageStatistics.class) { // Lock data for init
            whenLocked.execute();
        }
    }

    /**
     * Updates the store in a safe way.
     * 
     * @param updater
     *            the update logic
     */
    void update(BiConsumer<StatisticsContainer, StatisticsContainer> updater) {
        access(() -> {
            ObjectNode fullJson = internalRead();
            ObjectNode projectJson = getProjectData(fullJson, projectId);
            updater.accept(new StatisticsContainer(fullJson),
                    new StatisticsContainer(projectJson));
            // TODO Is fullJson updated automatically from projectJson or not?
            internalWrite(fullJson);
        });
    }

    private static ObjectNode getProjectData(ObjectNode fullJson,
            String projectId) {
        if (projectId == null) {
            return null;
        }
        return JsonHelpers.getOrCreate(projectId,
                fullJson.get(StatisticsConstants.FIELD_PROJECTS),
                StatisticsConstants.FIELD_PROJECT_ID, true);
    }

    /**
     * Reads all data from the statistics file.
     * 
     * @return
     *
     * @see #getUsageStatisticsFile()
     */
    ObjectNode read() {
        AtomicReference<ObjectNode> data = new AtomicReference<>(null);
        access(() -> data.set(internalRead()));
        return data.get();
    }

    /**
     * Reads the active project data from the statistics file.
     * 
     * @return
     *
     * @see #getUsageStatisticsFile()
     */
    ObjectNode readProject() {
        ObjectNode data = read();
        return getProjectData(data, projectId);
    }

    /**
     * Read the data from local project statistics file.
     * 
     * @return
     *
     * @see #getUsageStatisticsFile()
     */
    ObjectNode internalRead() {
        File file = getUsageStatisticsFile();
        getLogger().debug("Reading statistics from {}", file.getAbsolutePath());
        try {
            if (file.exists()) {
                // Read full data and make sure we track the right project
                return (ObjectNode) JsonHelpers.getJsonMapper().readTree(file);
            }
        } catch (JsonProcessingException e) {
            getLogger().debug("Failed to parse json", e);
        } catch (IOException e) {
            getLogger().debug("Failed to read json", e);
        }

        // Empty node if nothing else is found and make sure we
        // track the right project
        ObjectNode json = JsonHelpers.getJsonMapper().createObjectNode();
        json.set(StatisticsConstants.FIELD_PROJECTS,
                JsonHelpers.getJsonMapper().createArrayNode());
        return json;
    }

    /**
     * Writes the data to local project statistics json file.
     *
     * @see #getUsageStatisticsFile()
     */
    private void internalWrite(ObjectNode json) {
        try {
            getUsageStatisticsFile().getParentFile().mkdirs();
            JsonHelpers.getJsonMapper().writeValue(getUsageStatisticsFile(),
                    json);
        } catch (IOException e) {
            getLogger().debug("Failed to write json", e);
        }
    }

    /**
     * Get usage statistics json file location.
     *
     * @return the location of statistics storage file.
     * @see ProjectHelpers#resolveStatisticsStore()
     */
    File getUsageStatisticsFile() {
        if (this.usageStatisticsFile == null) {
            this.usageStatisticsFile = ProjectHelpers.resolveStatisticsStore();
        }
        return this.usageStatisticsFile;
    }

    /**
     * Set custom usage statistics json file location.
     *
     * If not set <code>ProjectHelpers.resolveStatisticsStore()</code> is used.
     *
     * @see ProjectHelpers#resolveStatisticsStore
     *
     */
    void setUsageStatisticsStore(File usageStatistics) {
        this.usageStatisticsFile = usageStatistics;
    }

    /**
     * Get the last server message.
     *
     * @return The message string returned from server in last successful
     *         requests.
     */
    String getLastServerMessage(ObjectNode json) {
        return json.has(StatisticsConstants.FIELD_SERVER_MESSAGE)
                ? json.get(StatisticsConstants.FIELD_SERVER_MESSAGE).asText()
                : null;
    }

}
