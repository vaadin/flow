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
package com.vaadin.flow.server.frontend.installer;

import java.io.File;
import java.net.URI;

/**
 * Handle file download from given url to target destination.
 * <p>
 * Derived from eirslett/frontend-maven-plugin
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 */
public interface FileDownloader {

    public interface ProgressListener {

        /**
         * Called periodically during a download.
         *
         * @param bytesTransferred
         *            the bytes transferred so far
         * @param totalBytes
         *            the total bytes if known, otherwise -1
         * @param progress
         *            the progress (0.0 - 1.0) if the total bytes is known,
         *            otherwise -1
         */
        void onProgress(long bytesTransferred, long totalBytes,
                double progress);
    }

    /**
     * Download to destination from url using username and password.
     *
     * @param downloadTarget
     *            uri string from where to download
     * @param destination
     *            file target directory
     * @param userName
     *            user name, {@code null} accepted
     * @param password
     *            password, {@code null} accepted
     * @param progressListener
     *            a progres listener or {@code null} if no progress listener is
     *            needed
     * @throws DownloadException
     *             exception thrown when download fails
     */
    void download(URI downloadTarget, File destination, String userName,
            String password, ProgressListener progressListener)
            throws DownloadException;
}
