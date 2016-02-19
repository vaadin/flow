/*
 * Copyright 2000-2016 Vaadin Ltd.
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

import org.jsoup.nodes.Document;

import com.vaadin.ui.UI;

/**
 * A representation of a bootstrap page being generated. The bootstrap page
 * contains the full DOM of the HTML document.
 *
 * @author Vaadin Ltd
 * @since 7.0.0
 */
public class BootstrapPageResponse extends BootstrapResponse {

    private final Document document;

    /**
     * Crate a new bootstrap page response.
     *
     * @see BootstrapResponse#BootstrapResponse(VaadinRequest, VaadinSession,
     *      UI)
     *
     * @param request
     *            the Vaadin request for which the bootstrap page should be
     *            generated
     * @param session
     *            the service session for which the bootstrap page should be
     *            generated
     * @param ui
     *            the UI that will be displayed on the page
     * @param document
     *            the DOM document making up the HTML page
     */
    public BootstrapPageResponse(VaadinRequest request, VaadinSession session,
            UI ui, Document document) {
        super(request, session, ui);
        this.document = document;
    }

    /**
     * Gets the document node representing the root of the DOM hierarchy that
     * will be used to generate the HTML page. Changes to the document will be
     * reflected in the HTML.
     *
     * @return the document node
     */
    public Document getDocument() {
        return document;
    }

}
