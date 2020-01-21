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
package com.vaadin.flow.server.communication;

import com.vaadin.flow.component.UI;
import org.jsoup.nodes.Document;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;

import java.util.Optional;

/**
 * This represents the state of a Index HTML response being generated. The Index
 * HTML response contains of the full DOM of the HTML document as well as the
 * HTTP headers that will be included in the corresponding HTTP response.
 *
 */
public class IndexHtmlResponse {

    private final VaadinRequest vaadinRequest;
    private final VaadinResponse vaadinResponse;
    private final Document document;

    /**
     * Create a response object in useDeprecatedV14Bootstrapping with UI.
     *
     * @param vaadinRequest
     *            the vaadin request which is handling
     * @param vaadinResponse
     *            the corresponding vaadin response
     * @param document
     *            the {@link Document} object of the response page
     * @param ui
     *            the UI for the bootstrap
     */
    public IndexHtmlResponse(VaadinRequest vaadinRequest,
                             VaadinResponse vaadinResponse, Document document, UI ui) {
        this.vaadinRequest = vaadinRequest;
        this.vaadinResponse = vaadinResponse;
        this.document = document;
    }

    /**
     * Create a response object in useDeprecatedV14Bootstrapping.
     *
     * @param vaadinRequest
     *            the vaadin request which is handling
     * @param vaadinResponse
     *            the corresponding vaadin response
     * @param document
     *            the {@link Document} object of the response page
     */
    public IndexHtmlResponse(VaadinRequest vaadinRequest,
                             VaadinResponse vaadinResponse, Document document) {
        this.vaadinRequest = vaadinRequest;
        this.vaadinResponse = vaadinResponse;
        this.document = document;
    }

    /**
     * Get the request which triggers the Index HTML response.
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
     * Get the index.html response in form of a {@link Document} instance.
     *
     * @return the index document
     */
    public Document getDocument() {
        return document;
    }

    /**
     * Gets the UI that will be displayed on the generated HTML page.
     *
     * @return the UI
     */
    public Optional<UI> getUI() {
        if(!this.vaadinRequest.getService()
                .getDeploymentConfiguration().isEagerServerLoad()) {
            return Optional.empty();
        } else {
            return Optional.ofNullable(UI.getCurrent());
        }
    }
}
