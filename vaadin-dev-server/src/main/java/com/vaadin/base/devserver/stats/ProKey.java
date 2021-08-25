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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An internal helper class representing a Vaadin Pro key.
 * <p>
 * This class is also used to load and save the generated user key.
 */
class ProKey {

    public static final String FIELD_NAME = "username";
    public static final String FIELD_KEY = "proKey";
    private final String username;
    private final String key;

    public ProKey(String username, String key) {
        super();
        this.username = username;
        this.key = key;
    }

    public static ProKey fromJson(String jsonData) {
        ProKey proKey = new ProKey(null, null);
        try {
            JsonNode json = JsonHelpers.getJsonMapper().readTree(jsonData);
            proKey = new ProKey(json.get(FIELD_NAME).asText(),
                json.get(FIELD_KEY).asText());
            return proKey;
        } catch (JsonProcessingException | NullPointerException e) {
            getLogger().debug("Failed to parse proKey from json", e);
        }
        return proKey;
    }

    public static ProKey fromFile(File jsonFile) throws IOException {
        ProKey proKey = new ProKey(null, null);
        try {
            JsonNode json = JsonHelpers.getJsonMapper().readTree(jsonFile);
            proKey = new ProKey(json.get(FIELD_NAME).asText(),
                json.get(FIELD_KEY).asText());
            return proKey;
        } catch (JsonProcessingException | NullPointerException e) {
            getLogger().debug("Failed to parse proKey from json file", e);
        }
        return proKey;
    }

    public static ProKey get() {
        ProKey proKey = getSystemProperty();
        if (proKey != null) {
            return proKey;
        }
        proKey = getEnvironmentVariable();
        if (proKey != null) {
            return proKey;
        }
        File proKeyLocation = getFileLocation();
        try {
            proKey = read(proKeyLocation);
            return proKey;
        } catch (IOException e) {
            getLogger().debug("Unable to read proKey", e);
            return null;
        }
    }

    private static ProKey getSystemProperty() {
        String value = System.getProperty("vaadin.proKey");
        if (value == null) {
            return null;
        }
        String[] parts = value.split("/");
        if (parts.length != 2) {
            getLogger().debug(
                "Unable to read pro key from the vaadin.proKey system property. The property must be of type -Dvaadin.proKey=[vaadin.com login email]/[prokey]");
            return null;
        }

        return new ProKey(parts[0], parts[1]);
    }

    private static ProKey getEnvironmentVariable() {
        String value = System.getenv("VAADIN_PRO_KEY");
        if (value == null) {
            return null;
        }
        String[] parts = value.split("/");
        if (parts.length != 2) {
            getLogger().debug(
                "Unable to read pro key from the VAADIN_PRO_KEY environment variable. The value must be of type VAADIN_PRO_KEY=[vaadin.com login email]/[prokey]");
            return null;
        }

        return new ProKey(parts[0], parts[1]);
    }

    public static File getFileLocation() {
        File vaadinHome = ProjectHelpers.resolveVaadinHomeDirectory();
        return new File(vaadinHome, StatisticsConstants.PRO_KEY_FILE_NAME);
    }

    private static ProKey read(File proKeyLocation) throws IOException {
        if (!proKeyLocation.exists()) {
            return null;
        }
        return ProKey.fromFile(proKeyLocation);
    }

    public static void write(ProKey proKey, File proKeyLocation)
        throws IOException {
        File proKeyDirectory = proKeyLocation.getParentFile();
        if (!proKeyDirectory.exists() &&
            !proKeyDirectory.mkdirs()) {
            throw new IOException("Failed to create directory "+
                proKeyDirectory.getAbsolutePath());
        }
        proKey.toFile(proKeyLocation);
    }

    private static Logger getLogger() {
        // Use the same logger that DevModeUsageStatistics uses
        return LoggerFactory.getLogger(DevModeUsageStatistics.class.getName());
    }

    public String getUsername() {
        return username;
    }

    public String getKey() {
        return key;
    }

    public void toFile(File proKeyLocation) throws IOException {
        JsonHelpers.getJsonMapper().writeValue(proKeyLocation, this);
    }

    public String toJson() {
        ObjectNode json = JsonHelpers.getJsonMapper().createObjectNode();
        json.put(FIELD_NAME, username);
        json.put(FIELD_KEY, key);
        try {
            return JsonHelpers.getJsonMapper().writeValueAsString(json);
        } catch (JsonProcessingException e) {
            getLogger().debug("Unable to read proKey", e);
        }
        return null;
    }

}
