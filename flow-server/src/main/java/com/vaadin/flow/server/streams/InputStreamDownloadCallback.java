/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.streams;

import java.io.IOException;
import java.io.Serializable;

/**
 * A callback for providing an InputStream, other download meta-data and
 * handling logic for {@link InputStreamDownloadHandler}.
 *
 * The method invocation may throw an {@link IOException} to handle cases where
 * reading from InputStream fails.
 *
 * @since 24.8
 */
@FunctionalInterface
public interface InputStreamDownloadCallback extends Serializable {

    /**
     * Applies the given {@link DownloadEvent} to provide a
     * {@link DownloadResponse}.
     *
     * @param downloadEvent
     *            the event containing information about the download request
     * @return a {@link DownloadResponse} containing the InputStream and other
     *         meta-data for the download
     * @throws IOException
     *             if an error occurs while reading from InputStream fails
     */
    DownloadResponse complete(DownloadEvent downloadEvent) throws IOException;
}
