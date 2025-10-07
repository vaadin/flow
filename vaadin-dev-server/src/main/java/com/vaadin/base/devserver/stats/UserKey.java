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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * An internal helper class representing a user key.
 */
class UserKey {

    private static final String FIELD_KEY = "key";
    private final String key;

    UserKey(String key) {
        this.key = key;
    }

    UserKey(File keyFile) {
        String keyFromFile = null;
        try {
            JsonNode value = JsonHelpers.getJsonMapper().readTree(keyFile);
            keyFromFile = value.get(FIELD_KEY).asText();
        } catch (Exception e) {
            getLogger().debug("Unable to read UserKey", e);
        }
        this.key = keyFromFile;
    }

    public String getKey() {
        return key;
    }

    /**
     * Writes the user key to the given file.
     */
    void toFile(File fileLocation) throws IOException {
        ObjectMapper jsonMapper = JsonHelpers.getJsonMapper();

        ObjectNode value = jsonMapper.createObjectNode();
        value.put(FIELD_KEY, this.key);
        fileLocation.getParentFile().mkdirs();
        jsonMapper.writeValue(fileLocation, value);
    }

    private static Logger getLogger() {
        // Use the same logger that DevModeUsageStatistics uses
        return LoggerFactory.getLogger(DevModeUsageStatistics.class.getName());
    }
}
