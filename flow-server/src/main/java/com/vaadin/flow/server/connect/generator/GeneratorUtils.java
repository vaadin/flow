/*
 * Copyright 2000-2019 Vaadin Ltd.
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
package com.vaadin.flow.server.connect.generator;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A set of utils for generator functionality.
 */
final class GeneratorUtils {
    static final String ENDPOINT = "vaadin.connect.endpoint";
    static final String DEFAULT_ENDPOINT = "/connect";

    private GeneratorUtils() {
    }

    /**
     * Write to the output path a string content.
     *
     * @param outputPath
     *            output path
     * @param content
     *            content to write
     */
    static void writeToFile(Path outputPath, String content) {
        try {
            Path parentFolder = outputPath.getParent();
            if (parentFolder != null && !parentFolder.toFile().exists()) {
                Files.createDirectories(parentFolder);
            }
            if (!outputPath.toFile().exists()) {
                Files.createFile(outputPath);
            }
            try (BufferedWriter bufferedWriter = Files
                    .newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
                bufferedWriter.write(content);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
