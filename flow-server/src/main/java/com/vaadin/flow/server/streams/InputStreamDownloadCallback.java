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
