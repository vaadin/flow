/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import java.util.Optional;

import org.jsoup.nodes.Document;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.ContentSecurityPolicy;
import com.vaadin.flow.server.CspMode;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;

/**
 * This represents the state of a Index HTML response being generated. The Index
 * HTML response contains of the full DOM of the HTML document as well as the
 * HTTP headers that will be included in the corresponding HTTP response.
 *
 * @since 3.0
 */
public class IndexHtmlResponse {

    private final VaadinRequest vaadinRequest;
    private final VaadinResponse vaadinResponse;
    private final Document document;
    private @Nullable String cspNonce;
    private @Nullable ContentSecurityPolicy contentSecurityPolicy;

    /**
     * Create a response object for client side bootstrapping with UI.
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
     * Create a response object for client side bootstrapping.
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
        if (!this.vaadinRequest.getService().getDeploymentConfiguration()
                .isEagerServerLoad()) {
            return Optional.empty();
        } else {
            return Optional.ofNullable(UI.getCurrent());
        }
    }

    /**
     * Gets the nonce of this response, which Vaadin puts on every
     * {@code <script>} element of the document and allows in the
     * {@code script-src} directive of the Content Security Policy.
     * <p>
     * A new nonce is created for every response when the application runs with
     * the {@link CspMode#STRICT} mode. Scripts added by an
     * {@link IndexHtmlRequestListener} get the nonce automatically, but the
     * value can also be used for other content that needs it. The nonce is not
     * put on the scripts in development mode.
     *
     * @return the nonce, or {@code null} if the mode is not
     *         {@link CspMode#STRICT}
     */
    public @Nullable String getCspNonce() {
        return cspNonce;
    }

    /**
     * Gets the Content Security Policy that is sent with this response.
     * <p>
     * The policy is created when the application runs with the
     * {@link CspMode#STRICT} mode. An {@link IndexHtmlRequestListener} can add
     * or override directives for resources of the application, such as
     * {@code connect-src}, {@code img-src}, {@code frame-src} or
     * {@code report-to}. The header is written after all listeners have run. It
     * is not written in development mode, because the development server and
     * the development tools need {@code eval}. In production mode, the request
     * fails with an {@link IllegalStateException} if the response already has a
     * {@code Content-Security-Policy} header, for example from a servlet
     * filter, since the page would otherwise be sent without the policy of the
     * strict mode.
     *
     * @return the mutable policy, or {@code null} if the mode is not
     *         {@link CspMode#STRICT}
     */
    public @Nullable ContentSecurityPolicy getContentSecurityPolicy() {
        return contentSecurityPolicy;
    }

    void setContentSecurityPolicy(String cspNonce,
            ContentSecurityPolicy contentSecurityPolicy) {
        this.cspNonce = cspNonce;
        this.contentSecurityPolicy = contentSecurityPolicy;
    }
}
