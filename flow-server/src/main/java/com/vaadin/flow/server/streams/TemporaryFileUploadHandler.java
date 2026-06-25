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

import com.vaadin.flow.function.SerializableBiConsumer;

/**
 * Upload handler that stores the data into a temporary file. Stored temporary
 * file is returned in the successHandler for further use.
 *
 * @since 24.8
 */
public class TemporaryFileUploadHandler
        extends AbstractFileUploadHandler<TemporaryFileUploadHandler> {

    public TemporaryFileUploadHandler(FileUploadCallback successCallback) {
        super(successCallback, new TemporaryFileFactory());
    }
}
