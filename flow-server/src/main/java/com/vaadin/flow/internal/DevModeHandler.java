/*
 * Copyright 2000-2021 Vaadin Ltd.
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.HttpURLConnection;

import com.vaadin.flow.server.RequestHandler;

public interface DevModeHandler extends RequestHandler {

    /**
     * Return webpack console output when a compilation error happened.
     *
     * @return console output if error or null otherwise.
     */
    String getFailedOutput();

    /**
     * Prepare a HTTP connection against webpack-dev-server.
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
     * Returns true if it's a request that should be handled by webpack.
     *
     * @param request
     *            the servlet request
     * @return true if the request should be forwarded to webpack
     */
    boolean isDevModeRequest(HttpServletRequest request);

    /**
     * Serve a file by proxying to webpack.
     *
     * @param request
     *            the servlet request
     * @param response
     *            the servlet response
     * @return false if webpack returned a not found, true otherwise
     * @throws IOException
     *             in the case something went wrong like connection refused
     */
    boolean serveDevModeRequest(HttpServletRequest request,
            HttpServletResponse response) throws IOException;

    /**
     * Stop the dev-server.
     */
    void stop();
}
