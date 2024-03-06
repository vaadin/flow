/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
 * @since
 */
interface FileDownloader {

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
     * @throws DownloadException
     *             exception thrown when download fails
     */
    void download(URI downloadTarget, File destination, String userName,
            String password) throws DownloadException;
}
