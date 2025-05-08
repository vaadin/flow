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

package com.vaadin.flow.server.streams;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * File factory to generate a temporary file for given file name
 *
 * @since 24.8
 */
public class TemporaryFileFactory implements FileFactory {

    /**
     * Create a new temporary file for filename. Adds the suffix {@code .tmp}
     */
    @Override
    public File createFile(String fileName) throws IOException {

        Path tempDirPath;
        try {
            tempDirPath = Files.createTempDirectory("temp_dir");
        } catch (IOException e) {
            throw new IOException("Failed to create temp directory", e);
        }

        return Files.createTempFile(tempDirPath, fileName, ".tmp").toFile();
    }
}