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
package com.vaadin.client;

import com.vaadin.client.ResourceLoader.ResourceLoadEvent;
import com.vaadin.client.ResourceLoader.ResourceLoadListener;
import com.vaadin.client.hummingbird.collection.JsArray;
import com.vaadin.client.hummingbird.collection.JsCollections;
import com.vaadin.ui.DependencyList;

import elemental.json.JsonArray;
import elemental.json.JsonObject;

/**
 * Handles loading of dependencies (stylesheets and scripts) in the application.
 *
 * @author Vaadin Ltd
 */
public class DependencyLoader {

    private static JsArray<Command> callbacks = JsCollections.array();

    private static int dependenciesLoading;

    private Registry registry;

    /**
     * Creates a new instance connected to the given registry.
     *
     * @param registry
     *            the global registry
     */
    public DependencyLoader(Registry registry) {
        this.registry = registry;
    }

    /**
     * Loads the given stylesheets and ensures any callbacks registered using
     * {@link runWhenDependenciesLoaded(Command)} are run when all dependencies
     * have been loaded.
     *
     * @param dependencies
     *            a list of dependency URLs to load, will be translated using
     *            {@link #translateVaadinUri(String)} before they are loaded
     */
    public void loadStyleDependencies(JsArray<String> dependencies) {
        // Assuming no reason to interpret in a defined order
        ResourceLoadListener resourceLoadListener = new ResourceLoadListener() {
            @Override
            public void onLoad(ResourceLoadEvent event) {
                endDependencyLoading();
            }

            @Override
            public void onError(ResourceLoadEvent event) {
                Console.error(event.getResourceUrl()
                        + " could not be loaded, or the load detection failed because the stylesheet is empty.");
                // The show must go on
                onLoad(event);
            }
        };
        ResourceLoader loader = ResourceLoader.get();
        for (int i = 0; i < dependencies.length(); i++) {
            String url = translateVaadinUri(dependencies.get(i));
            startDependencyLoading();
            loader.loadStylesheet(url, resourceLoadListener);
        }
    }

    /**
     * Loads the given scripts and ensures any callbacks registered using
     * {@link runWhenDependenciesLoaded(Command)} are run when all dependencies
     * have been loaded.
     *
     * @param dependencies
     *            a list of dependency URLs to load, will be translated using
     *            {@link #translateVaadinUri(String)} before they are loaded
     */
    public void loadScriptDependencies(final JsArray<String> dependencies) {
        if (dependencies.length() == 0) {
            return;
        }

        // Listener that loads the next when one is completed
        ResourceLoadListener resourceLoadListener = new ResourceLoadListener() {
            @Override
            public void onLoad(ResourceLoadEvent event) {
                if (dependencies.length() != 0) {
                    String url = translateVaadinUri(dependencies.shift());
                    startDependencyLoading();
                    // Load next in chain (hopefully already preloaded)
                    event.getResourceLoader().loadScript(url, this);
                }
                // Call start for next before calling end for current
                endDependencyLoading();
            }

            @Override
            public void onError(ResourceLoadEvent event) {
                Console.error(event.getResourceUrl() + " could not be loaded.");
                // The show must go on
                onLoad(event);
            }
        };

        ResourceLoader loader = ResourceLoader.get();

        // Start chain by loading first
        String url = translateVaadinUri(dependencies.shift());
        startDependencyLoading();
        loader.loadScript(url, resourceLoadListener);

        for (int i = 0; i < dependencies.length(); i++) {
            String preloadUrl = translateVaadinUri(dependencies.get(i));
            loader.loadScript(preloadUrl, null);
        }
    }

    /**
     * Run the URI through all protocol translators.
     *
     * @param uri
     *            the URI to translate
     * @return the translated URI
     */
    private String translateVaadinUri(String uri) {
        return registry.getURIResolver().resolveVaadinUri(uri);
    }

    /**
     * Adds a command to be run when all dependencies have finished loading.
     * <p>
     * If no dependencies are currently being loaded, runs the command
     * immediately.
     *
     * @see #startDependencyLoading()
     * @see #endDependencyLoading()
     * @param command
     *            the command to run when dependencies have been loaded
     */
    public static void runWhenDependenciesLoaded(Command command) {
        if (dependenciesLoading == 0) {
            command.execute();
        } else {
            callbacks.push(command);
        }
    }

    /**
     * Marks that loading of a dependency has started.
     *
     * @see #runWhenDependenciesLoaded(Command)
     * @see #endDependencyLoading()
     */
    public static void startDependencyLoading() {
        dependenciesLoading++;
    }

    /**
     * Marks that loading of a dependency has ended.
     * <p>
     * If all pending dependencies have been loaded, calls any callback
     * registered using {@link #runWhenDependenciesLoaded(Command)}.
     */
    public static void endDependencyLoading() {
        dependenciesLoading--;
        if (dependenciesLoading == 0 && callbacks.length() != 0) {
            for (int i = 0; i < callbacks.length(); i++) {
                Command cmd = callbacks.get(i);
                cmd.execute();
            }
            callbacks.clear();
        }
    }

    /**
     * Triggers loading of the given dependencies.
     *
     * @param deps
     *            the dependencies to load.
     */
    public void loadDependencies(JsonArray deps) {
        JsArray<String> scripts = JsCollections.array();
        JsArray<String> stylesheets = JsCollections.array();

        for (int i = 0; i < deps.length(); i++) {
            JsonObject dependencyJson = (JsonObject) deps.get(i);
            String type = dependencyJson.getString(DependencyList.KEY_TYPE);
            String url = dependencyJson.getString(DependencyList.KEY_URL);
            if (DependencyList.TYPE_STYLESHEET.equals(type)) {
                stylesheets.push(url);
            } else if (DependencyList.TYPE_JAVASCRIPT.equals(type)) {
                scripts.push(url);
            } else {
                Console.error("Unknown dependency type " + type);
            }
        }

        if (!scripts.isEmpty()) {
            loadScriptDependencies(scripts);
        }
        if (!stylesheets.isEmpty()) {
            loadStyleDependencies(stylesheets);
        }

    }

}
