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
package com.vaadin.flow.shared.ui;

import java.io.Serializable;
import java.util.Objects;
import java.util.stream.Stream;

import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.flow.shared.util.SharedUtil;

import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * Represents an html import, stylesheet or JavaScript to include on the page.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class Dependency implements Serializable {

    public static final String KEY_URL = "url";
    public static final String KEY_TYPE = "type";
    public static final String KEY_LOAD_MODE = "mode";
    public static final String KEY_CONTENTS = "contents";

    /**
     * The type of a dependency.
     */
    public enum Type {
        STYLESHEET, JAVASCRIPT, JS_MODULE, HTML_IMPORT, DYNAMIC_IMPORT;

        /**
         * Check if the given value is contained as a enum value.
         *
         * @param value
         *            value to check
         * @return true if there is a matching enum value
         */
        public static boolean contains(String value) {
            return Stream.of(values())
                    .anyMatch(enumValue -> enumValue.toString().equals(value));
        }
    }

    private final Type type;
    private final String url;
    private final LoadMode loadMode;

    /**
     * Creates a new dependency of the given type, to be loaded from the given
     * URL.
     * <p>
     * A relative URL is expanded to use the {@code frontend://} prefix. URLs
     * with a defined protocol and absolute URLs without a protocol are used
     * as-is.
     * <p>
     * The URL is passed through the translation mechanism before loading, so
     * custom protocols, specified at
     * {@link com.vaadin.flow.shared.VaadinUriResolver} can be used.
     *
     * @param type
     *            the type of the dependency, not {@code null}
     * @param url
     *            the URL to load the dependency from, not {@code null}
     * @param loadMode
     *            determines dependency load mode, refer to {@link LoadMode} for
     *            details
     */
    public Dependency(Type type, String url, LoadMode loadMode) {
        if (url == null) {
            throw new IllegalArgumentException("url cannot be null");
        }
        this.type = Objects.requireNonNull(type);

        if (type.equals(Type.JS_MODULE) || type.equals(Type.DYNAMIC_IMPORT)) {
            this.url = url;
        } else {
            this.url = SharedUtil.prefixIfRelative(url,
                    ApplicationConstants.FRONTEND_PROTOCOL_PREFIX);
        }
        this.loadMode = loadMode;
    }

    /**
     * Creates a new dependency of the given type, to be loaded using JS
     * expression which is supposed to return a {@code Promise}.
     * <p>
     * The created instance dependency mode is {@link LoadMode#LAZY}.
     *
     * @param type
     *            the type of the dependency, not {@code null}
     * @param expression
     *            the JS expression to load the dependency, not {@code null}
     */
    public Dependency(Type type, String expression) {
        // It's important that the load mode of the dependency is Lazy because
        // any other mode is not sent to the client at all when it's added at
        // the initial request: it's processed by the bootstrap handler via
        // adding an element into the document head right away (no client side
        // processing is involved).
        this(type, expression, LoadMode.LAZY);
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
     * Gets load mode that will be used for dependency loading. Refer to
     * {@link LoadMode} for details.
     *
     * @return the load mode that will be used during dependency loading
     */
    public LoadMode getLoadMode() {
        return loadMode;
    }

    /**
     * Converts the object into json representation.
     *
     * @return json representation of the object
     */
    public JsonObject toJson() {
        JsonObject jsonObject = Json.createObject();
        jsonObject.put(KEY_URL, url);
        jsonObject.put(KEY_TYPE, type.name());
        jsonObject.put(KEY_LOAD_MODE, loadMode.name());
        return jsonObject;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, url, loadMode);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Dependency that = (Dependency) o;
        return type == that.type && loadMode == that.loadMode
                && Objects.equals(url, that.url);

    }

    @Override
    public String toString() {
        return "Dependency [type=" + type + ", url=" + url + ", loadMode="
                + loadMode + "]";
    }
}
