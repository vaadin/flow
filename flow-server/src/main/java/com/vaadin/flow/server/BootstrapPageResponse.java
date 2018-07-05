/*
 * Copyright 2000-2018 Vaadin Ltd.
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

package com.vaadin.flow.server;

import org.jsoup.nodes.Document;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.shared.VaadinUriResolver;

/**
 * This represents the state of a bootstrap page being generated. The bootstrap
 * page contains of the full DOM of the HTML document as well as the HTTP
 * headers that will be included in the corresponding HTTP response.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class BootstrapPageResponse {

    private final VaadinRequest request;
    private final VaadinResponse response;
    private final VaadinSession session;
    private final UI ui;
    private final Document document;
    private final VaadinUriResolver uriResolver;

    /**
     * Create a new bootstrap page response.
     *
     * @param request
     *            the Vaadin request for which the bootstrap page should be
     *            generated.
     * @param session
     *            the service session for which the bootstrap page should be
     *            generated.
     * @param response
     *            the Vaadin response that serves the bootstrap page.
     * @param document
     *            the DOM document making up the HTML page.
     * @param uriResolver
     *            the uri resolver utility
     *
     * @param ui
     *            the UI for the bootstrap.
     */
    public BootstrapPageResponse(VaadinRequest request, VaadinSession session,
            VaadinResponse response, Document document, UI ui,
            VaadinUriResolver uriResolver) {
        this.request = request;
        this.session = session;
        this.ui = ui;
        this.response = response;
        this.document = document;
        this.uriResolver = uriResolver;
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

    /**
     * Gets the request for which the generated bootstrap HTML will be the
     * response. This can be used to read request headers and other additional
     * information.
     *
     * @return the Vaadin request that is being handled
     */
    public VaadinRequest getRequest() {
        return request;
    }

    /**
     * Gets the service session to which the rendered view belongs.
     *
     * @return the Vaadin service session
     */
    public VaadinSession getSession() {
        return session;
    }

    /**
     * Gets the UI that will be displayed on the generated bootstrap page.
     *
     * @return the UI
     */
    public UI getUI() {
        return ui;
    }

    /**
     * Gets the URI resolver utility.
     *
     * @return the URI resolver utility
     */
    public VaadinUriResolver getUriResolver() {
        return uriResolver;
    }
}
