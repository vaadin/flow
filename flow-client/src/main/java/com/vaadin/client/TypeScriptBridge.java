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

/**
 * Bridge to TypeScript implementations for GWT code.
 *
 * This class provides a single point of access for all TypeScript functionality
 * that is called from GWT code during the migration from GWT to TypeScript.
 * Each method here represents a piece of functionality that has been migrated
 * to TypeScript but is still being called from GWT code.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class TypeScriptBridge {
    private final Registry registry;

    /**
     * Creates a new TypeScript bridge connected to the given registry.
     *
     * @param registry the global registry
     */
    public TypeScriptBridge(Registry registry) {
        this.registry = registry;
    }

    /**
     * Translates a Vaadin URI to a URL that can be loaded by the browser.
     * Uses the TypeScript URIResolver implementation.
     *
     * @param uri the URI to resolve
     * @return the resolved URI
     */
    public String resolveVaadinUri(String uri) {
        return resolveVaadinUriNative(registry, uri);
    }

    private static native String resolveVaadinUriNative(Registry registry,
            String uri)
    /*-{
        var tsResolver = new $wnd.Vaadin.GWT.URIResolver(registry);
        return tsResolver.resolveVaadinUri(uri);
    }-*/;
}
