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
package com.vaadin.flow.shared.ui;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.dom.Node;
import com.vaadin.flow.internal.JacksonCodec;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.JsonDecodingException;

/**
 * Represents an html import, stylesheet or JavaScript to include on the page.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class Dependency implements Serializable {

    public static final String KEY_URL = "url";
    public static final String KEY_TYPE = "type";
    public static final String KEY_LOAD_MODE = "loadMode";
    public static final String KEY_CONTENTS = "contents";
    public static final String KEY_ID = "id";
    public static final String KEY_LAYER = "layer";

    /**
     * The type of a dependency.
     */
    public enum Type {
        STYLESHEET, JAVASCRIPT, JS_MODULE, DYNAMIC_IMPORT;

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
    private final String id;
    private final String layer;

    /**
     * Creates a new dependency of the given type, to be loaded from the given
     * URL.
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
        this(type, url, loadMode, null);
    }

    /**
     * Creates a new dependency of the given type, to be loaded from the given
     * URL with an optional ID for tracking.
     * <p>
     * The URL is passed through the translation mechanism before loading, so
     * custom protocols, specified at
     * {@link com.vaadin.flow.shared.VaadinUriResolver} can be used.
     * <p>
     * For internal use only. May be renamed or removed in a future release.
     *
     * @param type
     *            the type of the dependency, not {@code null}
     * @param url
     *            the URL to load the dependency from, not {@code null}
     * @param loadMode
     *            determines dependency load mode, refer to {@link LoadMode} for
     *            details
     * @param id
     *            optional ID for tracking the dependency
     * @since 25.0
     */
    public Dependency(Type type, String url, LoadMode loadMode, String id) {
        this(type, url, loadMode, id, null);
    }

    /**
     * Creates a new dependency of the given type, to be loaded from the given
     * URL into the given CSS cascade layer, with an optional ID for tracking.
     * <p>
     * The URL is passed through the translation mechanism before loading, so
     * custom protocols, specified at
     * {@link com.vaadin.flow.shared.VaadinUriResolver} can be used.
     * <p>
     * For internal use only. May be renamed or removed in a future release.
     *
     * @param type
     *            the type of the dependency, not {@code null}
     * @param url
     *            the URL to load the dependency from, not {@code null}
     * @param loadMode
     *            determines dependency load mode, refer to {@link LoadMode} for
     *            details
     * @param id
     *            optional ID for tracking the dependency
     * @param layer
     *            the CSS cascade layer to load a style sheet into, or
     *            {@code null} to not use a layer
     * @throws IllegalArgumentException
     *             if a layer is given for a dependency that is not a
     *             {@link Type#STYLESHEET}
     * @since 25.4
     */
    public Dependency(Type type, String url, LoadMode loadMode, String id,
            String layer) {
        if (url == null) {
            throw new IllegalArgumentException("url cannot be null");
        }
        this.type = Objects.requireNonNull(type);
        if (layer != null && type != Type.STYLESHEET) {
            throw new IllegalArgumentException(
                    "Only a style sheet can be loaded into a layer, not "
                            + type);
        }
        this.url = url;
        this.loadMode = loadMode;
        this.id = id;
        this.layer = layer;
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
     * @since 2.1
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
     * Creates a new {@link Type#DYNAMIC_IMPORT} dependency, loaded by calling
     * the given function, which is supposed to return a {@code Promise}.
     * <p>
     * The created instance dependency mode is {@link LoadMode#LAZY}. Its URL is
     * the JSON the function is sent to the browser as, which tells apart
     * dependencies that load different things, and which a copy made from the
     * type and the URL keeps.
     * <p>
     * For internal use only. May be renamed or removed in a future release.
     *
     * @param dynamicImport
     *            the function that loads the dependency, not {@code null}
     * @throws IllegalArgumentException
     *             if the function captures an element or a component, attached
     *             or not, directly or in a collection, a map, an array or a
     *             captured function, or an attached one in a bean property: the
     *             browser loads dependencies before it applies the changes of
     *             the same round trip, and a detached element cannot be
     *             referenced
     */
    public Dependency(JsFunction dynamicImport) {
        // LAZY for the same reason as in Dependency(Type, String)
        this(Type.DYNAMIC_IMPORT, encodeDynamicImport(dynamicImport),
                LoadMode.LAZY);
    }

    private static String encodeDynamicImport(JsFunction dynamicImport) {
        Objects.requireNonNull(dynamicImport, "dynamicImport cannot be null");
        // Checked on the Java values, as JacksonCodec encodes a detached
        // element as null, which the check below cannot see
        if (capturesElement(dynamicImport)) {
            throw elementCaptured();
        }
        JsonNode encoded = JacksonCodec.encodeWithTypeInfo(dynamicImport);
        // How JacksonCodec references an attached element, such as one in a
        // bean property, which capturesElement does not look into
        if (encoded.findValue("@v-node") != null) {
            throw elementCaptured();
        }
        return JacksonUtils.getMapper().writeValueAsString(encoded);
    }

    private static boolean capturesElement(Object value) {
        if (value instanceof Node<?> || value instanceof Component) {
            return true;
        }
        if (value instanceof JsFunction function) {
            return function.getCaptures().stream()
                    .anyMatch(Dependency::capturesElement);
        }
        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                if (capturesElement(item)) {
                    return true;
                }
            }
            return false;
        }
        if (value instanceof Map<?, ?> map) {
            return map.values().stream().anyMatch(Dependency::capturesElement);
        }
        if (value instanceof Object[] array) {
            return Stream.of(array).anyMatch(Dependency::capturesElement);
        }
        return false;
    }

    private static IllegalArgumentException elementCaptured() {
        return new IllegalArgumentException(
                "A dynamic import cannot capture an element or a component,"
                        + " attached or not: the browser loads dependencies"
                        + " before it applies the changes of the same round"
                        + " trip, and a detached element cannot be referenced");
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
     * Gets the ID associated with this dependency.
     * <p>
     * For internal use only. May be renamed or removed in a future release.
     *
     * @return the dependency ID or null if not set
     * @since 25.0
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the CSS cascade layer the style sheet is loaded into.
     * <p>
     * For internal use only. May be renamed or removed in a future release.
     *
     * @return the layer name or null if the dependency does not use a layer
     * @since 25.4
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getLayer() {
        return layer;
    }

    /**
     * Gets the function that loads a {@link Type#DYNAMIC_IMPORT} dependency
     * created with {@link #Dependency(JsFunction)}, as the JSON it is sent to
     * the browser as.
     * <p>
     * For internal use only. May be renamed or removed in a future release.
     *
     * @return the encoded function, or {@code null} for a dependency that is
     *         loaded from its URL or a JavaScript expression
     */
    @JsonIgnore
    public JsonNode getEncodedDynamicImport() {
        // Read from the URL alone, so that a copy made from the type and the
        // URL loads the function too. An expression can start with '{' (a
        // block), so one that is not JSON is an expression. A JSON object
        // with the function key never is one: it does not compile as the
        // body of a function.
        if (type != Type.DYNAMIC_IMPORT || !url.startsWith("{")) {
            return null;
        }
        try {
            JsonNode node = JacksonUtils.readTree(url);
            return node.has(JacksonCodec.FUNCTION_KEY) ? node : null;
        } catch (JsonDecodingException e) {
            LoggerFactory.getLogger(Dependency.class).debug(
                    "Loading dynamic import '{}' as an expression, as it is not JSON",
                    url, e);
            return null;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, url, loadMode, id, layer);
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
                && Objects.equals(url, that.url) && Objects.equals(id, that.id)
                && Objects.equals(layer, that.layer);

    }

    @Override
    public String toString() {
        return "Dependency [type=" + type + ", url=" + url + ", loadMode="
                + loadMode + ", id=" + id + ", layer=" + layer + "]";
    }
}
