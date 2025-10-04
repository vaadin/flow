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
package com.vaadin.client;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Bridge to TypeScript URIResolver implementation.
 *
 * This class delegates all calls to the TypeScript implementation at
 * window.Vaadin.GWT.URIResolver.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class URIResolver {
    private final JavaScriptObject tsInstance;

    /**
     * Creates a new instance connected to the given registry.
     *
     * @param registry
     *            the global registry
     */
    public URIResolver(Registry registry) {
        this.tsInstance = createTypeScriptInstance(registry);
    }

    private static native JavaScriptObject createTypeScriptInstance(
            Registry registry)
    /*-{
        if (!$wnd.Vaadin || !$wnd.Vaadin.GWT || !$wnd.Vaadin.GWT.URIResolver) {
            throw new Error("TypeScript URIResolver not loaded. Make sure Flow.ts imports core/URIResolver before GWT.");
        }
        return new $wnd.Vaadin.GWT.URIResolver(registry);
    }-*/;

    /**
     * Translates a Vaadin URI to a URL that can be loaded by the browser.
     *
     * @param uri
     *            the URI to resolve
     * @return the resolved URI
     */
    public String resolveVaadinUri(String uri) {
        return resolveVaadinUriNative(tsInstance, uri);
    }

    private static native String resolveVaadinUriNative(
            JavaScriptObject tsInstance, String uri)
    /*-{
        return tsInstance.resolveVaadinUri(uri);
    }-*/;

    /**
     * Returns the current document location as relative to the base uri of the
     * document.
     *
     * @return the document current location as relative to the document base
     *         uri
     */
    public static String getCurrentLocationRelativeToBaseUri() {
        return getCurrentLocationRelativeToBaseUriNative();
    }

    private static native String getCurrentLocationRelativeToBaseUriNative()
    /*-{
        return $wnd.Vaadin.GWT.URIResolver.getCurrentLocationRelativeToBaseUri();
    }-*/;

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
    public static String getBaseRelativeUri(String baseURI, String uri) {
        return getBaseRelativeUriNative(baseURI, uri);
    }

    private static native String getBaseRelativeUriNative(String baseURI,
            String uri)
    /*-{
        return $wnd.Vaadin.GWT.URIResolver.getBaseRelativeUri(baseURI, uri);
    }-*/;
}
