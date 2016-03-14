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
package com.vaadin.hummingbird.namespace;

import java.net.URI;
import java.net.URISyntaxException;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.ui.Dependency;
import com.vaadin.ui.Dependency.Type;

import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * List for storing dependencies/files (Javascript, Stylesheets) to be loaded
 * and included on the client side.
 *
 * @author Vaadin
 * @since
 */
public class DependencyListNamespace extends JsonListNamespace {

    public static final String KEY_URL = "url";
    public static final String KEY_TYPE = "type";
    public static final String TYPE_STYLESHEET = "css";
    public static final String TYPE_JAVASCRIPT = "js";

    /**
     * Creates a new namespace for the given node.
     *
     * @param node
     *            the node that the namespace belongs to
     */
    protected DependencyListNamespace(StateNode node) {
        super(node);
    }

    /**
     * Adds the given dependency to be loaded by the client side.
     * <p>
     * Relative URLs are interpreted as relative to the service (servlet) path.
     * You can prefix the URL with {@literal context://} to make it relative to
     * the context path or use an absolute URL to refer to files outside the
     * service (servlet) path.
     *
     * @param dependency
     *            the dependency to include on the page
     */
    public void add(Dependency dependency) {
        JsonObject jsonObject = Json.createObject();
        jsonObject.put(KEY_TYPE, getType(dependency));
        String url = dependency.getUrl();
        try {
            URI uri = new URI(url);
            if (uri.getScheme() == null && !url.startsWith("/")) {
                // Relative URL without protocol ->
                // interpret as relative to context root
                url = "context://" + url;
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Unable to parse the given URL",
                    e);
        }
        jsonObject.put(KEY_URL, dependency.getUrl());

        super.add(jsonObject);
    }

    /**
     * Gets the type string to be sent to the client.
     *
     * @param dependency
     *            the dependency
     * @return the type for the JSON
     */
    private static String getType(Dependency dependency) {
        if (dependency.getType() == Type.JAVASCRIPT) {
            return TYPE_JAVASCRIPT;
        } else if (dependency.getType() == Type.STYLESHEET) {
            return TYPE_STYLESHEET;
        } else {
            throw new IllegalArgumentException(
                    "Unknown dependency type: " + dependency.getType());
        }

    }
}
