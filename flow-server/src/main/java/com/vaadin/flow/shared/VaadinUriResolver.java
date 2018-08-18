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
package com.vaadin.flow.shared;

import java.io.Serializable;

/**
 * Utility for translating special Vaadin URIs into URLs usable by the browser.
 * This is an abstract class performing the main logic in
 * {@link #resolveVaadinUri(String, String, String)}.
 * <p>
 * Concrete implementations of this class should implement {@link Serializable}
 * in case a reference to an object of this class is stored on the server side.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public abstract class VaadinUriResolver implements Serializable {

    /**
     * Translates a Vaadin URI to a URL that can be loaded by the browser. The
     * following URI schemes are supported:
     * <ul>
     * <li><code>{@value com.vaadin.flow.shared.ApplicationConstants#CONTEXT_PROTOCOL_PREFIX}</code>
     * resolves to the application context root</li>
     * <li><code>{@value com.vaadin.flow.shared.ApplicationConstants#FRONTEND_PROTOCOL_PREFIX}</code>
     * - resolves to the build path where web components were compiled. Browsers
     * supporting ES6 can receive different, more optimized files than browsers
     * that only support ES5.</li>
     * <li><code>{@value com.vaadin.flow.shared.ApplicationConstants#BASE_PROTOCOL_PREFIX}</code>
     * - resolves to the base URI of the page</li>
     * </ul>
     * Any other URI protocols, such as <code>http://</code> or
     * <code>https://</code> are passed through this method unmodified.
     *
     * @param uri
     *            the URI to resolve
     * @param frontendUrl
     *            the URL pointing to the path where the frontend files can be
     *            found. It is expected that different browsers receive
     *            different files depending on their capabilities. Can use the
     *            other protocols.
     * @param servletToContextRoot
     *            the relative path from the servlet path (used as base path in
     *            the client) to the context root
     * @return the resolved URI
     */
    protected String resolveVaadinUri(String uri, String frontendUrl,
            String servletToContextRoot) {
        if (uri == null) {
            return null;
        }

        String processedUri = processProtocol(
                ApplicationConstants.FRONTEND_PROTOCOL_PREFIX, frontendUrl,
                uri);
        processedUri = processProtocol(
                ApplicationConstants.CONTEXT_PROTOCOL_PREFIX,
                servletToContextRoot, processedUri);
        processedUri = processProtocol(
                ApplicationConstants.BASE_PROTOCOL_PREFIX, "", processedUri);

        return processedUri;
    }

    private String processProtocol(String protocol, String replacement,
            String vaadinUri) {
        if (vaadinUri.startsWith(protocol)) {
            vaadinUri = replacement + vaadinUri.substring(protocol.length());
        }
        return vaadinUri;
    }

}
