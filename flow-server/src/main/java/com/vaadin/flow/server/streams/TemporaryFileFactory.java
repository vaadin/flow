/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
    public File createFile(UploadMetadata uploadMetadata) throws IOException {

        Path tempDirPath;
        try {
            tempDirPath = Files.createTempDirectory("temp_dir");
        } catch (IOException e) {
            throw new IOException("Failed to create temp directory", e);
        }

        return Files
                .createTempFile(tempDirPath, uploadMetadata.fileName(), ".tmp")
                .toFile();
    }
}
