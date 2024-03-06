/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
