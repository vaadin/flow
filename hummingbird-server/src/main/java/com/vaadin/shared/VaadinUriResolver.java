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
package com.vaadin.shared;

import java.io.Serializable;

import com.vaadin.server.VaadinService;

/**
 * Utility for translating special Vaadin URIs into URLs usable by the browser.
 * This is an abstract class performing the main logic in
 * {@link #resolveVaadinUri(String)} and using abstract methods in the class for
 * accessing information specific to the current environment.
 * <p>
 * Concrete implementations of this class should implement {@link Serializable}
 * in case a reference to an object of this class is stored on the server side.
 *
 * @since 7.4
 * @author Vaadin Ltd
 */
public abstract class VaadinUriResolver {

    /**
     * Translates a Vaadin URI to a URL that can be loaded by the browser. The
     * following URI schemes are supported:
     * <ul>
     * <li><code>{@value ApplicationConstants#SERVICE_PROTOCOL_PREFIX}</code> -
     * resolves to a URL that will be routed to the current
     * {@link VaadinService}.</li>
     * <li><code>{@value ApplicationConstants#CONTEXT_PROTOCOL_PREFIX}</code> -
     * resolves to the application context root</li>
     * </ul>
     * Any other URI protocols, such as <code>http://</code> or
     * <code>https://</code> are passed through this method unmodified.
     *
     * @since 7.4
     * @param vaadinUri
     *            the uri to resolve
     * @return the resolved uri
     */
    public String resolveVaadinUri(String vaadinUri) {
        if (vaadinUri == null) {
            return null;
        }

        if (vaadinUri
                .startsWith(ApplicationConstants.SERVICE_PROTOCOL_PREFIX)) {
            String relativeUrl = vaadinUri.substring(
                    ApplicationConstants.SERVICE_PROTOCOL_PREFIX.length());
            vaadinUri = getServiceUrl() + relativeUrl;
        }
        if (vaadinUri
                .startsWith(ApplicationConstants.CONTEXT_PROTOCOL_PREFIX)) {
            String relativeUrl = vaadinUri.substring(
                    ApplicationConstants.CONTEXT_PROTOCOL_PREFIX.length());
            vaadinUri = getContextRootUrl() + relativeUrl;
        }

        return vaadinUri;
    }

    /**
     * Gets the URL pointing to the context root.
     *
     * @return the context root URL
     */
    protected abstract String getContextRootUrl();

    /**
     * Gets the URL handled by {@link com.vaadin.server.VaadinService
     * VaadinService} to handle application requests.
     *
     * @return the service URL
     */
    protected abstract String getServiceUrl();

}
