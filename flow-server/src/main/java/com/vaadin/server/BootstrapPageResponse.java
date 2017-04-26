/*
 * Copyright 2000-2017 Vaadin Ltd.
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

package com.vaadin.server;

import com.vaadin.external.jsoup.nodes.Document;
import com.vaadin.ui.UI;

/**
 * A representation of a bootstrap page being generated. The bootstrap page
 * contains of the full DOM of the HTML document as well as the HTTP headers
 * that will be included in the corresponding HTTP response.
 *
 * @author Vaadin Ltd
 */
public class BootstrapPageResponse extends BootstrapResponse {

    private final VaadinResponse response;
    private final Document document;

    /**
     * Crate a new bootstrap page response.
     *
     * @param request
     *            the Vaadin request for which the bootstrap page should be
     *            generated.
     * @param session
     *            the service session for which the bootstrap page should be
     *            generated.
     * @param response
     *            the Vaadin response that contains the bootstrap page.
     * @param document
     *            the DOM document making up the HTML page.
     * @param headers
     *            a map into which header data can be added.
     * @param ui
     *            the UI for the bootstrap.
     */
    public BootstrapPageResponse(VaadinRequest request, VaadinSession session,
            VaadinResponse response, Document document, UI ui) {
        super(request, session, ui);
        this.response = response;
        this.document = document;
    }

    /**
     * Gets the HTTP response that serves the bootstrap page.
     * 
     * @return the Vaadin response.
     */
    public VaadinResponse getResponse() {
        return response;
    }

    /**
     * Sets a header value that will be added to the HTTP response. If the
     * header had already been set, the new value overwrites the previous one.
     *
     * @see VaadinResponse#setHeader(String, String)
     *
     * @param name
     *            the name of the header.
     * @param value
     *            the header value.
     */
    public void setHeader(String name, String value) {
        response.setHeader(name, value);
    }

    /**
     * Properly formats a timestamp as a date in a header that will be included
     * in the HTTP response. If the header had already been set, the new value
     * overwrites the previous one.
     *
     * @see #setHeader(String, String)
     * @see VaadinResponse#setDateHeader(String, long)
     *
     * @param name
     *            the name of the header.
     * @param timestamp
     *            the number of milliseconds since epoch.
     */
    public void setDateHeader(String name, long timestamp) {
        response.setDateHeader(name, timestamp);
    }

    /**
     * Gets the document node representing the root of the DOM hierarchy that
     * will be used to generate the HTML page. Changes to the document will be
     * reflected in the HTML.
     *
     * @return the document node.
     */
    public Document getDocument() {
        return document;
    }

}
