/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.streams;

/**
 * Metadata for successful upload.
 * <p>
 * The fileName and contentType will only be available for multipart uploads.
 *
 * @since 24.8
 */
public record UploadMetadata(String fileName, String contentType,
        long contentLength) {
}
