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

/**
 * This represents the state of a client index bootstrap page being generated.
 * The bootstrap page contains of the full DOM of the HTML document as well as
 * the HTTP headers that will be included in the corresponding HTTP response.
 *
 */
public class ClientIndexResponse {

    private final VaadinRequest vaadinRequest;
    private final VaadinResponse vaadinResponse;
    private final VaadinSession vaadinSession;
    private final Document document;

    /**
     * Create a response object in clientSideMode.
     * 
     * @param vaadinRequest
     *            the vaadin request which is handling
     * @param vaadinResponse
     *            the corresponding vaadin response
     * @param vaadinSession
     *            the current vaadin session
     * @param document
     *            the {@link Document} object of the response page
     */
    public ClientIndexResponse(VaadinRequest vaadinRequest,
            VaadinResponse vaadinResponse, VaadinSession vaadinSession,
            Document document) {
        this.vaadinRequest = vaadinRequest;
        this.vaadinResponse = vaadinResponse;
        this.vaadinSession = vaadinSession;
        this.document = document;
    }

    /**
     * Get the request which triggers client index response.
     * 
     * @return the Vaadin request
     */
    public VaadinRequest getVaadinRequest() {
        return vaadinRequest;
    }

    /**
     * Get the Vaadin response object including all the headers which will be
     * sent to browser.
     * 
     * @return the Vaadin response
     */
    public VaadinResponse getVaadinResponse() {
        return vaadinResponse;
    }

    /**
     * Get the current Vaadin session.
     * 
     * @return the Vaadin session
     */
    public VaadinSession getVaadinSession() {
        return vaadinSession;
    }

    /**
     * Get the index.html response in form of a {@link Document} instance.
     * 
     * @return the index document
     */
    public Document getDocument() {
        return document;
    }
}
