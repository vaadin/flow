/*
 * Copyright 2000-2020 Vaadin Ltd.
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

/**
 * Handle file download from given url to target destination.
 * <p>
 * Derived from eirslett/frontend-maven-plugin
 *
 * @since
 */
interface FileDownloader {

    /**
     * Download to destination from url using username and password.
     *
     * @param downloadUrl
     *         url to download
     * @param destination
     *         file target directory
     * @param userName
     *         user name, {@code null} accepted
     * @param password
     *         password, {@code null} accepted
     * @throws DownloadException
     *         exception thrown when download fails
     */
    void download(String downloadUrl, File destination, String userName,
            String password) throws DownloadException;
}

