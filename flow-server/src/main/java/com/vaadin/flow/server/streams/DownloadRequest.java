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
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;

/**
 * Class containing data on requested client download.
 *
 * @since 24.8
 */
public class DownloadRequest extends TransferRequest {

    /**
     * Create a new download event with required data.
     *
     * @param request
     *            current request
     * @param response
     *            current response to write response data to
     * @param session
     *            current session
     * @param fileName
     *            defined download file name
     */
    public DownloadRequest(VaadinRequest request, VaadinResponse response,
            VaadinSession session, String fileName) {
        super(request, response, session, fileName);
    }

    /**
     * Returns a <code>OutputStream</code> for writing binary data in the
     * response.
     * <p>
     * Either this method or getWriter() may be called to write the response,
     * not both.
     *
     * @return a <code>OutputStream</code> for writing binary data or empty
     *         optional if an error happened
     */
    public OutputStream getOutputStream() {
        try {
            return getResponse().getOutputStream();
        } catch (IOException e) {
            LoggerFactory.getLogger(DownloadRequest.class)
                    .error("Error getting output stream", e);
            throw new UncheckedIOException("Error getting output stream", e);
        }
    }

    /**
     * Returns a <code>PrintWriter</code> object that can send character text to
     * the client. The PrintWriter uses the character encoding defined using
     * setContentType.
     * <p>
     * Either this method or getOutputStream() may be called to write the
     * response, not both.
     *
     * @return a <code>PrintWriter</code> for writing character text or empty
     *         optional if an error happened
     */
    public PrintWriter getWriter() {
        try {
            return getResponse().getWriter();
        } catch (IOException e) {
            LoggerFactory.getLogger(DownloadRequest.class)
                    .error("Error getting print writer");
            throw new UncheckedIOException("Error getting writer", e);
        }
    }
}
