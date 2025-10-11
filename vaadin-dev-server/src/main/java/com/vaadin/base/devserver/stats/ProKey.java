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
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * An internal helper class representing a Vaadin Pro key.
 */
class ProKey {

    private static final String FIELD_NAME = "username";
    private static final String FIELD_KEY = "proKey";
    private final String username;
    private final String key;

    ProKey(String username, String key) {
        super();
        this.username = username;
        this.key = key;
    }

    public String getUsername() {
        return username;
    }

    public String getKey() {
        return key;
    }

    String toJson() {
        ObjectNode json = JsonHelpers.getJsonMapper().createObjectNode();
        json.put(FIELD_NAME, username);
        json.put(FIELD_KEY, key);
        try {
            return JsonHelpers.getJsonMapper().writeValueAsString(json);
        } catch (JacksonException e) {
            getLogger().debug("Unable to read proKey", e);
        }
        return null;
    }

    static ProKey get() {
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
            proKey = fromFile(proKeyLocation);
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

    private static File getFileLocation() {
        File vaadinHome = ProjectHelpers.resolveVaadinHomeDirectory();
        return new File(vaadinHome, StatisticsConstants.PRO_KEY_FILE_NAME);
    }

    static ProKey fromFile(File jsonFile) throws IOException {
        if (!jsonFile.exists()) {
            return null;
        }

        ProKey proKey = new ProKey(null, null);
        try {
            JsonNode json = JsonHelpers.getJsonMapper().readTree(jsonFile);
            proKey = new ProKey(json.get(FIELD_NAME).asString(),
                    json.get(FIELD_KEY).asString());
            return proKey;
        } catch (JacksonException | NullPointerException e) {
            getLogger().debug("Failed to parse proKey from json file", e);
        }
        return proKey;
    }

    private static Logger getLogger() {
        // Use the same logger that DevModeUsageStatistics uses
        return LoggerFactory.getLogger(DevModeUsageStatistics.class.getName());
    }
}
