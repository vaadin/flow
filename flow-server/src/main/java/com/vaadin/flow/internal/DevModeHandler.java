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
package com.vaadin.flow.internal;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;

import com.vaadin.flow.server.RequestHandler;

public interface DevModeHandler extends RequestHandler {

    /**
     * Return the dev server console output when a compilation error happened.
     *
     * @return console output if error or null otherwise.
     */
    String getFailedOutput();

    /**
     * Prepare a HTTP connection against the dev server.
     *
     * @param path
     *            the file to request, needs to be safe
     * @param method
     *            the http method to use
     * @return the connection
     * @throws IOException
     *             on connection error
     */
    HttpURLConnection prepareConnection(String path, String method)
            throws IOException;

    /**
     * Serve a file by proxying to the dev server.
     *
     * @param request
     *            the servlet request
     * @param response
     *            the servlet response
     * @return false if the dev server returned a not found, true otherwise
     * @throws IOException
     *             in the case something went wrong like connection refused
     */
    boolean serveDevModeRequest(HttpServletRequest request,
            HttpServletResponse response) throws IOException;

    /**
     * Stop the dev-server.
     */
    void stop();

    /**
     * Gets the project root folder.
     *
     * @return the project root folder
     */
    File getProjectRoot();

    /**
     * Get the listening port of the dev server.
     *
     * @return the listening port
     */
    int getPort();
}
