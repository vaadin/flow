/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.ccdm;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.FallibleCommand;

/**
 * This class generates token-signing-key (for vaadin-connect) and writes it
 * back to the application properties file.
 */
public class TaskGenerateTokenSigningKey implements FallibleCommand {
    private static final String VAADIN_CONNECT_AUTH_TOKEN_SIGNING_KEY = "vaadin.connect.auth.token-signing-key";
    private static final int TOKEN_SIGNING_KEY_LENGTH = 6;
    public static final String TOKEN_COMMENT = "\n# The token signing key is generated automatically by vaadin-maven-plugin."
            + "\n# It's highly recommended to use your own key in production.";
    private final File propertiesFile;

    /**
     * Create a new task that generates Token Signing Key in vaadin-connect
     * integrated project.
     * 
     * @param propertiesFile
     *            the application.properties file.
     */
    public TaskGenerateTokenSigningKey(File propertiesFile) {
        this.propertiesFile = propertiesFile;
    }

    @Override
    public void execute() throws ExecutionFailedException {
        if (propertiesFile.exists() && !propertiesFile.isFile()) {
            getLogger().info(
                    "The given properties file '{}' is not a file. Skipping generating the 'Token Signing "
                            + "Key'.",
                    propertiesFile.getPath());
            return;
        }

        if (!propertiesFile.exists()) {
            try {
                propertiesFile.createNewFile();
            } catch (IOException e) {
                getLogger().error("Unable to create application properties "
                        + "file at '{}'", propertiesFile.getPath(), e);
                return;
            }
        }

        try {
            List<String> readLines = FileUtils.readLines(propertiesFile,
                    StandardCharsets.UTF_8.name());
            boolean shouldAppendNewKey = true;
            for (int i = 0; i < readLines.size(); i++) {
                String currentLine = readLines.get(i);
                if (isTokenPropertyLine(currentLine)
                        && shouldUpdateKey(currentLine)) {
                    readLines.set(i, generateKey());
                    shouldAppendNewKey = false;
                }
            }
            if (shouldAppendNewKey) {
                readLines.add(TOKEN_COMMENT);
                readLines.add(VAADIN_CONNECT_AUTH_TOKEN_SIGNING_KEY + "=" + generateKey());
            }
            FileUtils.writeLines(propertiesFile, readLines);
            getLogger().info("Generated a new 'Token Signing Key' in {}",
                    propertiesFile.getPath());
        } catch (IOException e) {
            getLogger().error("Unable to generate 'Token Signing Key'.", e);
        }
    }

    protected String generateKey() {
        String randomUUID = UUID.randomUUID().toString();
        String plainKey = randomUUID.replace("-", "");
        return plainKey.substring(0,
                Math.min(TOKEN_SIGNING_KEY_LENGTH + 1, plainKey.length()));
    }

    private boolean isTokenPropertyLine(String currentLine) {
        return currentLine.contains(VAADIN_CONNECT_AUTH_TOKEN_SIGNING_KEY)
                && !currentLine.startsWith("#");
    }

    private boolean shouldUpdateKey(String currentLine) {
        String[] split = currentLine.split("=");
        if (split.length != 2) {
            getLogger().info("Unable to update 'Token Signing Key'. The "
                    + "property entry is invalid.");
            return false;
        }
        if (!split[1].isEmpty()) {
            getLogger().info("Skip updating 'Token Signing Key'.");
            return false;
        }
        return true;
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(TaskGenerateTokenSigningKey.class);
    }
}
