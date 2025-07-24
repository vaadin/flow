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
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Optional;

import jakarta.servlet.http.Part;
import org.apache.commons.fileupload2.core.FileItemInput;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;

/**
 * Class containing data on requested client upload to server.
 *
 * @since 24.8
 */
public class UploadEvent {

    private final VaadinRequest request;
    private final VaadinResponse response;
    private final VaadinSession session;

    private final String fileName;
    private final String contentType;

    private final long fileSize;

    private final Element owningElement;

    private final FileItemInput item;
    private final Part part;

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
     * @param contentType
     *            content type string for upload
     * @param contentLength
     *            size of the upload
     * @param owningElement
     *            element owning this upload
     * @param item
     *            multipart stream file item, {@code null} for xhr or parts
     *            multipart stream
     * @param part
     *            multipart part item, {@code null} for xhr or multipart file
     *            stream
     */
    public UploadEvent(VaadinRequest request, VaadinResponse response,
            VaadinSession session, String fileName, long contentLength,
            String contentType, Element owningElement, FileItemInput item,
            Part part) {
        this.request = request;
        this.response = response;
        this.session = session;
        this.fileName = fileName;
        this.fileSize = contentLength;
        this.owningElement = owningElement;
        this.contentType = contentType;
        this.item = item;
        this.part = part;
    }

    /**
     * Returns an input stream from which the request content can be read.
     *
     * @return the input stream from which the contents of the request can be
     *         read
     */
    public InputStream getInputStream() {
        try {
            if (item != null) {
                return item.getInputStream();
            }
            if (part != null) {
                return part.getInputStream();
            }
            return request.getInputStream();
        } catch (IOException e) {
            LoggerFactory.getLogger(UploadEvent.class)
                    .error("Error getting input stream", e);
            throw new UncheckedIOException("Error getting input stream", e);
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
     * Get the set file name for current upload.
     * <p>
     * The file name will only be available for multipart uploads.
     *
     * @return file name
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Get the content type for the data to download.
     * <p>
     * The content type will only be available for multipart uploads.
     *
     * @return set content type
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Get the content type for the data to download.
     *
     * @return set content type
     */
    public long getFileSize() {
        return fileSize;
    }

    /**
     * Get owner {@link Component} for this event.
     *
     * @return owning component or null in none defined
     */
    public Component getOwningComponent() {
        return owningElement.getComponent().orElse(null);
    }

    /**
     * Get the owning element for the upload related to this event.
     *
     * @return owning element
     */
    public Element getOwningElement() {
        return owningElement;
    }

    /**
     * Return the UI for this upload event.
     *
     * @return UI for upload event
     */
    public UI getUI() {
        Optional<Component> component = owningElement.getComponent();
        return component.map(
                value -> value.getUI().orElseGet(() -> getUiFromSession(value)))
                .orElseGet(UI::getCurrent);
    }

    private UI getUiFromSession(Component value) {
        try {
            session.lock();
            return session.getUIById(Integer
                    .parseInt((String) ComponentUtil.getData(value, "uiid")));
        } finally {
            session.unlock();
        }
    }
}
