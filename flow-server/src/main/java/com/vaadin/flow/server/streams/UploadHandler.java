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

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.fileupload2.jakarta.JakartaServletFileUpload;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.server.ElementRequestHandler;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.UploadException;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;

import static com.vaadin.flow.server.communication.StreamRequestHandler.DYN_RES_PREFIX;

/**
 * Interface for handling download of data from the server to the client.
 *
 * @since 24.8
 */
@FunctionalInterface
public interface UploadHandler extends ElementRequestHandler {

    /**
     * Method that is called when the client wants to upload data to the url
     * stored for this specific handler registration.
     *
     * @param event
     *            upload event containing the necessary data for getting the
     *            request
     */
    void handleUploadRequest(UploadEvent event);

    default void handleRequest(VaadinRequest request, VaadinResponse response,
            VaadinSession session, Element owner) {
        boolean isMultipartUpload = request instanceof HttpServletRequest
                && JakartaServletFileUpload
                        .isMultipartContent((HttpServletRequest) request);

        StateNode node = owner.getNode();

        session.lock();
        try {
            if (node == null) {
                session.getErrorHandler()
                        .error(new ErrorEvent(new UploadException(
                                "File upload ignored because the node for the stream variable was not found")));
                return;
            }
            if (!node.isAttached()) {
                session.getErrorHandler().error(new ErrorEvent(
                        new UploadException("Warning: file upload ignored for "
                                + node.getId()
                                + " because the component was disabled")));
                return;
            }
        } finally {
            session.unlock();
        }

        String fileName = getUrlPostfix() == null ? "" : getUrlPostfix();
        if (isMultipartUpload) {
            // To be implemented later
        } else {
            // These are unknown in filexhr ATM
            fileName = "unknown";
            String contentType = "unknown";

            UploadEvent event = new UploadEvent(request, response, session,
                    fileName, request.getContentLengthLong(), contentType,
                    owner);

            handleUploadRequest(event);
        }
    }

}
