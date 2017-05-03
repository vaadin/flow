/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.ui;

import java.io.Serializable;

/**
 * Represents an html import, stylesheet or JavaScript to include on the page.
 *
 * @author Vaadin Ltd
 */
public class Dependency implements Serializable {

    /**
     * The type of a dependency.
     */
    public enum Type {
        STYLESHEET, JAVASCRIPT, HTML_IMPORT
    }

    private final Type type;
    private final String url;
    private final boolean blocking;

    /**
     * Creates a new dependency of the given type, to be loaded from the given
     * URL.
     * <p>
     * The URL is passed through the translation mechanism before loading, so
     * custom protocols, specified at
     * {@link com.vaadin.shared.VaadinUriResolver} can be used.
     *
     * @param type
     *            the type of the dependency, not {@code null}
     * @param url
     *            the URL to load the dependency from, not {@code null}
     * @param blocking
     *            {@code true} forces the dependency to be loaded before the
     *            initial page load, {@code false} allows the dependency to be
     *            loaded independent of initial page load
     */
    public Dependency(Type type, String url, boolean blocking) {
        if (url == null) {
            throw new IllegalArgumentException("url cannot be null");
        }
        assert type != null;

        this.type = type;
        this.url = url;
        this.blocking = blocking;
    }

    /**
     * Gets the untranslated URL for the dependency.
     *
     * @return the URL for the dependency
     */
    public String getUrl() {
        return url;
    }

    /**
     * Gets the type of the dependency.
     *
     * @return the type of the dependency
     */
    public Type getType() {
        return type;
    }

    /**
     * Gets the blocking property of the dependency.
     *
     * @return {@code true} if dependency should be loaded before whole page is
     *         loaded, {@code false} otherwise.
     */
    public boolean isBlocking() {
        return blocking;
    }
}
