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
package com.vaadin.client;

import jsinterop.annotations.JsType;

/**
 * Client side URL resolver for Vaadin protocols. Pure
 * {@code @JsType(isNative=true)} binding to the TypeScript implementation at
 * {@code src/main/frontend/internal/client/URIResolver.ts}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client", name = "URIResolver")
public class URIResolver {

    public URIResolver(Registry registry) {
        // Defined by the TS class constructor.
    }

    /**
     * Translates a Vaadin URI to a URL that can be loaded by the browser. The
     * following URI schemes are supported:
     * <ul>
     * <li>{@code context://} - resolves to the application context root</li>
     * <li>{@code base://} - resolves to the base URI of the page</li>
     * </ul>
     * Any other URI protocols, such as {@code http://} or {@code https://}, are
     * passed through this method unmodified.
     *
     * @param uri
     *            the URI to resolve
     * @return the resolved URI
     */
    public native String resolveVaadinUri(String uri);

    /**
     * Returns the current document location as relative to the base uri of the
     * document.
     *
     * @return the document current location as relative to the document base
     *         uri
     */
    public static native String getCurrentLocationRelativeToBaseUri();

    /**
     * Returns the given uri as relative to the given base uri.
     *
     * @param baseURI
     *            the base uri of the document
     * @param uri
     *            an absolute uri to transform
     * @return the uri as relative to the document base uri, or the given uri
     *         unmodified if it is for different context.
     */
    public static native String getBaseRelativeUri(String baseURI, String uri);
}
