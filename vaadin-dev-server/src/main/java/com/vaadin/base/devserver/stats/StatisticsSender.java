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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Handles sending of telemetry data.
 */
public class StatisticsSender {

    private static final String FAILED_TO_READ = "Failed to read ";
    private StatisticsStorage storage;

    /**
     * Creates a new instance connected to the given storage.
     *
     * @param storage
     *            the storage to use
     */
    public StatisticsSender(StatisticsStorage storage) {
        this.storage = storage;
    }

    /**
     * Get the remote reporting URL.
     *
     * @return Returns {@link StatisticsConstants#USAGE_REPORT_URL} by default.
     */
    public String getReportingUrl() {
        return StatisticsConstants.USAGE_REPORT_URL;
    }

    /**
     * Get the last server message.
     *
     * @param json
     *            The json returned by {@link StatisticsStorage#read()}
     * @return The message string returned from server in last successful
     *         requests.
     */
    String getLastServerMessage(ObjectNode json) {
        return json.has(StatisticsConstants.FIELD_SERVER_MESSAGE)
                ? json.get(StatisticsConstants.FIELD_SERVER_MESSAGE).asText()
                : null;
    }

    /**
     * Check the Interval has elapsed.
     * <p>
     * Uses <code>System.currentTimeMillis</code> as time source.
     *
     * @param json
     *            The json returned by {@link StatisticsStorage#read()}
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
     * @param json
     *            The json returned by {@link StatisticsStorage#read()}
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
     * Gets the last time the data was collected according to the statistics
     * file.
     *
     * @param json
     *            The json returned by {@link StatisticsStorage#read()}
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
     * @param json
     *            The json returned by {@link StatisticsStorage#read()}
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
     * Send data in the background if needed.
     *
     * @param json
     *            The json returned by {@link StatisticsStorage#read()}
     */
    public void triggerSendIfNeeded(ObjectNode json) {
        // Send usage statistics asynchronously, if enough time has
        // passed
        if (isIntervalElapsed(json)) {
            CompletableFuture.runAsync(() -> {
                String message = sendStatistics(json);

                // Show message on console, if present
                if (message != null && !message.trim().isEmpty()) {
                    DevModeUsageStatistics.getLogger().info(message);
                }
            });
        }
    }

    /**
     * Send current statistics to given reporting URL.
     * <p>
     * Reads the current data and posts it to given URL. Updates or replaces the
     * local data according to the response.
     *
     * @param json
     *            The json returned by {@link StatisticsStorage#read()}
     *
     * @see #postData(String, JsonNode)
     */
    String sendStatistics(ObjectNode json) {

        // Post copy of the current data
        AtomicReference<String> message = new AtomicReference<>(null);
        String stringData;
        try {
            stringData = JsonHelpers.getJsonMapper().writeValueAsString(json);
        } catch (JacksonException e) {
            getLogger().debug("Error converting statistics to a string", e);
            return null;
        }
        JsonNode response = postData(getReportingUrl(), stringData);

        // Update the last sent time
        // If the last send was successful we clear the project data
        if (response.isObject()
                && response.has(StatisticsConstants.FIELD_LAST_STATUS)) {

            storage.update((global, project) -> {
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

            });

            // If data was sent ok, clear the existing project data
            if (response.get(StatisticsConstants.FIELD_LAST_STATUS).asText()
                    .startsWith("200:")) {
                storage.clearAllProjectData();
            }
        }

        return message.get();
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
    private static ObjectNode postData(String postUrl, String data) {
        ObjectNode result;
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(postUrl))
                    .POST(HttpRequest.BodyPublishers.ofString(data))
                    .header("Content-Type", "application/json").build();

            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            int statusCode = response.statusCode();

            JsonNode jsonResponse = null;
            if (statusCode == 200) {
                jsonResponse = JsonHelpers.getJsonMapper()
                        .readTree(response.body());
            }

            if (jsonResponse != null && jsonResponse.isObject()) {
                result = (ObjectNode) jsonResponse;
            } else {
                // Default response in case of any problems
                result = JsonHelpers.getJsonMapper().createObjectNode();
            }
            // Update the status and return the results
            result.put(StatisticsConstants.FIELD_LAST_STATUS,
                    statusCode + ": "); // TODO is reason phrase needed here
            return result;

        } catch (IOException ex) {
            getLogger().debug("Failed to send statistics.", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            getLogger().debug("Failed to send statistics.", ex);
        }

        // Fallback
        result = JsonHelpers.getJsonMapper().createObjectNode();
        result.put(StatisticsConstants.FIELD_LAST_STATUS,
                StatisticsConstants.INVALID_SERVER_RESPONSE);
        return result;
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(StatisticsSender.class);
    }

}
