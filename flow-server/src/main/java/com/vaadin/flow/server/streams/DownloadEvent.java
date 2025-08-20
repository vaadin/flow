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
import java.util.Optional;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;

/**
 * Class containing meta-data for handling the requested client download.
 * <p>
 * It gives direct access to the underlying request, response and session as
 * well as various helpers specifically for handling downloads.
 *
 *
 * @since 24.8
 */
public class DownloadEvent {

    private VaadinRequest request;
    private VaadinResponse response;
    private VaadinSession session;
    private Element owningElement;
    private String fileName;
    private String contentType;
    private long contentLength = -1;
    private Exception exception;

    public DownloadEvent(VaadinRequest request, VaadinResponse response,
            VaadinSession session, Element owningElement) {
        this.request = request;
        this.response = response;
        this.session = session;
        this.owningElement = owningElement;
    }

    /**
     * Returns a <code>OutputStream</code> for writing binary data in the
     * response.
     * <p>
     * Either this method or getWriter() may be called to write the response,
     * not both.
     *
     * @return a <code>OutputStream</code> for writing binary data
     * @throws UncheckedIOException
     *             if an I/O error occurred while getting the output stream
     */
    public OutputStream getOutputStream() {
        try {
            return response.getOutputStream();
        } catch (IOException e) {
            LoggerFactory.getLogger(DownloadEvent.class)
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
     * @return a <code>PrintWriter</code> for writing character text
     * @throws UncheckedIOException
     *             if an I/O error occurred while getting the writer
     */
    public PrintWriter getWriter() {
        try {
            return response.getWriter();
        } catch (IOException e) {
            LoggerFactory.getLogger(DownloadEvent.class)
                    .error("Error getting print writer");
            throw new UncheckedIOException("Error getting writer", e);
        }
    }

    /**
     * Get {@link VaadinRequest} for download event.
     *
     * @return vaadin request
     */
    public VaadinRequest getRequest() {
        return request;
    }

    /**
     * Get {@link VaadinResponse} for download event.
     *
     * @return vaadin response
     */
    public VaadinResponse getResponse() {
        return response;
    }

    /**
     * Get {@link VaadinSession} for download event.
     *
     * @return vaadin session
     */
    public VaadinSession getSession() {
        return session;
    }

    /**
     * Sets the name of the file to be downloaded. This method utilizes the HTTP
     * Content-Disposition header to specify the name of the file to be
     * downloaded.
     * <p>
     * To be called before the response is committed.
     * <p>
     * If the <code>fileName</code> is <code>null</code>, the
     * Content-Disposition header won't be set.
     *
     * @param fileName
     *            the name to be assigned to the file
     */
    public void setFileName(String fileName) {
        if (fileName == null) {
            return;
        }
        if (fileName.isEmpty()) {
            response.setHeader("Content-Disposition", "attachment");
        } else {
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + fileName + "\"");
        }
        this.fileName = fileName;
    }

    /**
     * Sets the content type for the current download. These methods utilize the
     * HTTP Content-Type header to specify the type of content being sent to the
     * client.
     * <p>
     * To be called before the response is committed.
     *
     * @param contentType
     *            the MIME type to set as the content type
     */
    public void setContentType(String contentType) {
        response.setContentType(contentType);
        this.contentType = contentType;
    }

    /**
     * Sets the length of the content body in the response if the length is not
     * <code>-1</code>. This method utilizes the HTTP Content-Length header to
     * specify the length of the content being sent to the client.
     * <p>
     * To be called before the response is committed.
     *
     * @param contentLength
     *            the length of the response content in bytes
     */
    public void setContentLength(long contentLength) {
        if (contentLength != -1) {
            response.setContentLengthLong(contentLength);
        }
        this.contentLength = contentLength;
    }

    /**
     * Get owner {@link Component} for this event.
     * <p>
     * The download handler may change the component's state during download,
     * e.g. disable or hide it during download or get the component's own data
     * like id.
     *
     * @return owning component or null in none defined
     */
    public Component getOwningComponent() {
        return owningElement.getComponent().orElse(null);
    }

    /**
     * Get the owning element for the download related to this event.
     * <p>
     * The download handler may use element's attributes or properties to define
     * what to download or change the element, e.g. element's id or data id to
     * fetch a row from a database or disable element once the download is
     * started.
     *
     * @return owning element
     */
    public Element getOwningElement() {
        return owningElement;
    }

    /**
     * Get the current UI instance for this request that can be used to make
     * asynchronous UI updates with
     * {@link UI#access(com.vaadin.flow.server.Command)}.
     *
     * @return Current UI instance
     */
    public UI getUI() {
        Optional<Component> component = owningElement.getComponent();
        return component.map(value -> value.getUI().orElseGet(UI::getCurrent))
                .orElseGet(UI::getCurrent);
    }

    String getFileName() {
        return fileName;
    }

    String getContentType() {
        return contentType;
    }

    long getContentLength() {
        return contentLength;
    }

    Exception getException() {
        return exception;
    }

    void setException(Exception exception) {
        this.exception = exception;
    }
}
