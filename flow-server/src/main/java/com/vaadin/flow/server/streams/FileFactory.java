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
import java.io.Serializable;

/**
 * File factory interface for generating file to store the uploaded data into.
 *
 * @since 24.8
 */
@FunctionalInterface
public interface FileFactory extends Serializable {

    /**
     * Create a new file for given file name.
     *
     * @param uploadMetadata
     *            metadata for upload that should get a file created
     * @return {@link File} that should be used
     */
    File createFile(UploadMetadata uploadMetadata) throws IOException;
}
