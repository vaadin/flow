/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.base.devserver.stats;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
