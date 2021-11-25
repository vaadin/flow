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
import java.util.function.Consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
    private ObjectNode json;
    private ObjectNode projectJson;
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
     * Set the project id. All subsequent calls to stores data is stored using
     * this project id.
     *
     * @param projectId
     *            The unique project id.
     */
    void setProjectId(String projectId) {
        this.projectId = projectId;
        // Find the project we are working on
        if (!json.has(StatisticsConstants.FIELD_PROJECTS)) {
            json.set(StatisticsConstants.FIELD_PROJECTS,
                    JsonHelpers.getJsonMapper().createArrayNode());
        }
        this.projectJson = JsonHelpers.getOrCreate(this.projectId,
                this.json.get(StatisticsConstants.FIELD_PROJECTS),
                StatisticsConstants.FIELD_PROJECT_ID, true);
    }

    /*
     * Gets the id for the active project.
     * 
     * All calls to stores data is stored using this project id.
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
                clientData.fields().forEachRemaining(
                        e -> projectJson.set(e.getKey(), e.getValue()));
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
    String sendCurrentStatistics() {

        // Post copy of the current data
        String message = null;
        JsonNode response = postData(getUsageReportingUrl(), json.deepCopy());

        // Update the last sent time
        // If the last send was successful we clear the project data
        if (response.isObject()
                && response.has(StatisticsConstants.FIELD_LAST_STATUS)) {
            json.put(StatisticsConstants.FIELD_LAST_SENT,
                    System.currentTimeMillis());
            json.put(StatisticsConstants.FIELD_LAST_STATUS, response
                    .get(StatisticsConstants.FIELD_LAST_STATUS).asText());

            // Use different interval, if requested in response or default to
            // 24H
            if (response.has(StatisticsConstants.FIELD_SEND_INTERVAL)
                    && response.get(StatisticsConstants.FIELD_SEND_INTERVAL)
                            .isNumber()) {
                json.put(StatisticsConstants.FIELD_SEND_INTERVAL,
                        normalizeInterval(response
                                .get(StatisticsConstants.FIELD_SEND_INTERVAL)
                                .asLong()));
            } else {
                json.put(StatisticsConstants.FIELD_SEND_INTERVAL,
                        StatisticsConstants.TIME_SEC_24H);
            }

            // Update the server message
            if (response.has(StatisticsConstants.FIELD_SERVER_MESSAGE)
                    && response.get(StatisticsConstants.FIELD_SERVER_MESSAGE)
                            .isTextual()) {
                message = response.get(StatisticsConstants.FIELD_SERVER_MESSAGE)
                        .asText();
                json.put(StatisticsConstants.FIELD_SERVER_MESSAGE, message);
            }

            // If data was sent ok, clear the existing project data
            if (response.get(StatisticsConstants.FIELD_LAST_STATUS).asText()
                    .startsWith("200:")) {
                json.set(StatisticsConstants.FIELD_PROJECTS,
                        JsonHelpers.getJsonMapper().createArrayNode());
                projectJson = JsonHelpers.getOrCreate(projectId,
                        json.get(StatisticsConstants.FIELD_PROJECTS),
                        StatisticsConstants.FIELD_PROJECT_ID, true);
            }
        }

        return message;
    }

    /**
     * Store a single string value in project statistics.
     *
     * @param name
     *            Uniques name of the field in project data.
     * @param value
     *            Value to set.
     */
    void setValue(String name, String value) {
        projectJson.put(name, value);
    }

    String getValue(String name) {
        return projectJson.get(name).asText();
    }

    /**
     * Update a single increment number value in current project data.
     * <p>
     * Stores the data to the disk automatically.
     *
     * @param name
     *            N of the field to increment.
     * @see JsonHelpers#incrementJsonValue(ObjectNode, String)
     */
    void increment(String name) {
        JsonHelpers.incrementJsonValue(projectJson, name);
    }

    /**
     * Update a field in current project data and calculate and update the
     * aggregate fields.
     * <p>
     * Updates the following fields:
     * <li></li><code>name</code> Set newValue.</li>
     * <li><code>name_min</code> Minimum value</li>
     * <li><code>name_max</code></li>
     * <li><code>name_count</code> Count of values collected</li> Stores the
     * data to the disk automatically.
     *
     * @param name
     *            Name of the field to update.
     * @param newValue
     *            The new value to store.
     */
    void aggregate(String name, double newValue) {
        // Update count
        JsonHelpers.incrementJsonValue(projectJson, name + "_count");
        double count = projectJson.get(name + "_count").asInt();

        // Update min & max
        double min = newValue;
        if (projectJson.has(name + "_min")
                && projectJson.get(name + "_min").isDouble()) {
            min = projectJson.get(name + "_min").asDouble(newValue);
        }
        projectJson.put(name + "_min", Math.min(newValue, min));

        double max = newValue;
        if (projectJson.has(name + "_max")
                && projectJson.get(name + "_max").isDouble()) {
            max = projectJson.get(name + "_max").asDouble(newValue);
        }
        projectJson.put(name + "_max", Math.max(newValue, max));

        // Update average
        double avg = newValue;
        if (projectJson.has(name + "_avg")
                && projectJson.get(name + "_avg").isDouble()) {
            // Calcalate new incremental average
            avg = projectJson.get(name + "_avg").asDouble(newValue);
            avg += (newValue - avg) / count;
        }
        projectJson.put(name + "_avg", avg);
        projectJson.put(name, newValue);
    }

    /**
     * Get a value of number value in current project data.
     *
     * @see #increment(String) (String)
     * @param name
     *            name of the field to get
     * @return Value if this is integer field, 0 if missing
     */
    int getFieldAsInt(String name) {
        if (projectJson != null && projectJson.has(name)
                && projectJson.get(name).isInt()) {
            return projectJson.get(name).asInt(0);
        }
        return 0;
    }

    /**
     * Get a value of number value in current project data.
     *
     * @param name
     *            name of the field to get
     * @return Value if this is integer field, 0 if missing
     */
    double getFieldAsDouble(String name) {
        if (projectJson != null && projectJson.has(name)
                && projectJson.get(name).isDouble()) {
            return projectJson.get(name).asDouble(0);
        }
        return 0;

    }

    /**
     * Set a global value in storage.
     *
     * @param globalField
     *            name of the field to get
     * @param value
     *            The new value to set
     * @see #increment(String) (String)
     */
    void setGlobalValue(String globalField, String value) {
        json.put(globalField, value);
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
    boolean isIntervalElapsed() {
        long now = System.currentTimeMillis();
        long lastSend = getLastSendTime();
        long interval = getInterval();
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
    long getInterval() {
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
    long getLastSendTime() {
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
    String getLastSendStatus() {
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
     * Updates the store in a safe way.
     * 
     * @param updater
     *            the update logic
     */
    void update(Consumer<StatisticsStorage> updater) {
        // Lock data for update
        synchronized (DevModeUsageStatistics.class) {
            read();
            updater.accept(this);
            write();
        }
    }

    /**
     * Read the data from local project statistics file.
     *
     * @see #getUsageStatisticsStore()
     */
    private void read() {
        File file = getUsageStatisticsStore();
        getLogger().debug("Reading statistics from {}", file.getAbsolutePath());
        try {
            if (file.exists()) {
                // Read full data and make sure we track the right project
                json = (ObjectNode) JsonHelpers.getJsonMapper().readTree(file);
                if (this.projectId != null) {
                    projectJson = JsonHelpers.getOrCreate(this.projectId,
                            json.get(StatisticsConstants.FIELD_PROJECTS),
                            StatisticsConstants.FIELD_PROJECT_ID, true);
                }
                return;
            }
        } catch (JsonProcessingException e) {
            getLogger().debug("Failed to parse json", e);
        } catch (IOException e) {
            getLogger().debug("Failed to read json", e);
        }

        // Empty node if nothing else is found and make sure we
        // track the right project
        json = JsonHelpers.getJsonMapper().createObjectNode();
        json.set(StatisticsConstants.FIELD_PROJECTS,
                JsonHelpers.getJsonMapper().createArrayNode());
        if (this.projectId != null) {
            projectJson = JsonHelpers.getOrCreate(this.projectId,
                    json.get(StatisticsConstants.FIELD_PROJECTS),
                    StatisticsConstants.FIELD_PROJECT_ID, true);
        }
    }

    /**
     * Writes the data to local project statistics json file.
     *
     * @see #getUsageStatisticsStore()
     */
    private void write() {
        try {
            getUsageStatisticsStore().getParentFile().mkdirs();
            JsonHelpers.getJsonMapper().writeValue(getUsageStatisticsStore(),
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
    File getUsageStatisticsStore() {
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
     * Get number of projects.
     *
     * @return Number of projects or zero.
     */
    int getNumberOfProjects() {
        if (json != null && json.has(StatisticsConstants.FIELD_PROJECTS)) {
            return json.get(StatisticsConstants.FIELD_PROJECTS).size();
        }
        return 0;
    }

    /**
     * Get the last server message.
     *
     * @return The message string returned from server in last successful
     *         requests.
     */
    String getLastServerMessage() {
        return json.has(StatisticsConstants.FIELD_SERVER_MESSAGE)
                ? json.get(StatisticsConstants.FIELD_SERVER_MESSAGE).asText()
                : null;
    }

}
