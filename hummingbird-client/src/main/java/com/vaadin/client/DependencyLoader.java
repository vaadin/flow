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

import java.util.function.BiConsumer;

import com.vaadin.client.ResourceLoader.ResourceLoadEvent;
import com.vaadin.client.ResourceLoader.ResourceLoadListener;
import com.vaadin.client.hummingbird.collection.JsArray;
import com.vaadin.client.hummingbird.collection.JsCollections;
import com.vaadin.client.hummingbird.collection.JsMap;
import com.vaadin.client.hummingbird.collection.JsSet;
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
     * Loads the given dependencies in the given order using the given loaders
     * and ensures any callbacks registered using
     * {@link runWhenDependenciesLoaded(Command)} are run when all dependencies
     * have been loaded.
     *
     * @param dependencies
     *            a list of dependency URLs to load, will be translated using
     *            {@link #translateVaadinUri(String)} before they are loaded,
     *            not <code>null</code>
     * @param resourceLoaders
     *            functions which takes care of loading the dependency URL
     * @param loadOneByOne
     *            load dependencies one by one if {@code true} (do not load next
     *            one until current one is loaded), otherwise start to load all
     *            resources simultaneously
     *
     */
    public void loadDependencies(final JsArray<String> dependencies,
            JsArray<BiConsumer<String, ResourceLoadListener>> resourceLoaders,
            boolean loadOneByOne) {
        assert dependencies != null;
        assert resourceLoaders != null;
        assert dependencies.length() == resourceLoaders.length();

        if (dependencies.length() == 0) {
            return;
        }
        // Listener that loads the next when one is completed
        ResourceLoadListener resourceLoadListener = new ResourceLoadListener() {
            @Override
            public void onLoad(ResourceLoadEvent event) {
                loadNextDependency(dependencies, resourceLoaders, this);
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

        // Start chain by loading first
        loadNextDependency(dependencies, resourceLoaders, resourceLoadListener);

        if (!loadOneByOne) {
            for (int i = 0; i < dependencies.length(); i++) {
                String url = translateVaadinUri(dependencies.get(i));
                resourceLoaders.get(i).accept(url, null);
            }
        }
    }

    private void loadNextDependency(final JsArray<String> dependencies,
            JsArray<BiConsumer<String, ResourceLoadListener>> resourceLoaders,
            ResourceLoadListener resourceLoadListener) {
        if (dependencies.length() != 0) {
            String url = translateVaadinUri(dependencies.shift());
            startDependencyLoading();
            resourceLoaders.shift().accept(url, resourceLoadListener);
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
     *            the dependencies to load, not <code>null</code>.
     */
    public void loadDependencies(JsonArray deps) {
        assert deps != null;

        ResourceLoader resourceLoader = registry.getResourceLoader();
        JsMap<String, BiConsumer<String, ResourceLoadListener>> styleSheets = JsCollections
                .map();
        styleSheets.set(DependencyList.TYPE_STYLESHEET,
                resourceLoader::loadStylesheet);
        loadDependencies(deps, styleSheets);

        JsMap<String, BiConsumer<String, ResourceLoadListener>> scriptsAndImports = JsCollections
                .map();
        scriptsAndImports.set(DependencyList.TYPE_JAVASCRIPT,
                resourceLoader::loadScript);
        scriptsAndImports.set(DependencyList.TYPE_HTML_IMPORT,
                resourceLoader::loadHtml);
        loadDependencies(deps, scriptsAndImports);

        for (int i = 0; i < deps.length(); i++) {
            Console.error(
                    "Unknown dependency type " + ((JsonObject) deps.get(i))
                            .getString(DependencyList.KEY_TYPE));
        }
    }

    @SuppressWarnings("unchecked")
    private void loadDependencies(JsonArray deps,
            JsMap<String, BiConsumer<String, ResourceLoadListener>> typesToLoad) {
        JsArray<String> toLoad = JsCollections.array();
        JsArray<BiConsumer<String, ResourceLoadListener>> loaders = JsCollections
                .array();

        JsSet<String> presentTypes = JsCollections.set();
        for (int i = 0; i < deps.length(); i++) {
            JsonObject dependencyJson = (JsonObject) deps.get(i);
            String type = dependencyJson.getString(DependencyList.KEY_TYPE);
            if (typesToLoad.has(type)) {
                presentTypes.add(type);
                String url = dependencyJson.getString(DependencyList.KEY_URL);
                toLoad.push(url);
                BiConsumer<String, ResourceLoadListener> loader = typesToLoad
                        .get(type);
                loaders.push(loader);
                deps.remove(i);
                i--;
            }
        }

        if (!toLoad.isEmpty()) {
            loadDependencies(toLoad, loaders, presentTypes.size() > 1);
        }
    }
}
