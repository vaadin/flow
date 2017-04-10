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
package com.vaadin.client;

import java.util.function.BiConsumer;

import com.vaadin.client.ResourceLoader.ResourceLoadEvent;
import com.vaadin.client.ResourceLoader.ResourceLoadListener;
import com.vaadin.client.flow.collection.JsArray;
import com.vaadin.client.flow.collection.JsCollections;
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
     * Loads the given dependency using the given loader and ensures any
     * callbacks registered using {@link #runWhenDependenciesLoaded(Command)}
     * are run when all dependencies have been loaded.
     *
     * @param dependencyUrl
     *            a dependency URL to load, will be translated using
     *            {@link #translateVaadinUri(String)} before it is loaded, not
     *            {@code null}
     * @param loader
     *            function which takes care of loading the dependency URL
     */
    public void loadDependency(final String dependencyUrl,
            final BiConsumer<String, ResourceLoadListener> loader) {
        assert dependencyUrl != null;
        assert loader != null;

        // Listener that loads the next when one is completed
        ResourceLoadListener resourceLoadListener = new ResourceLoadListener() {
            @Override
            public void onLoad(ResourceLoadEvent event) {
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
        String url = translateVaadinUri(dependencyUrl);
        startDependencyLoading();
        loader.accept(url, resourceLoadListener);
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

        for (int i = 0; i < deps.length(); i++) {
            JsonObject dependencyJson = (JsonObject) deps.get(i);
            String type = dependencyJson.getString(DependencyList.KEY_TYPE);
            String url = dependencyJson.getString(DependencyList.KEY_URL);
            switch (type) {
            case DependencyList.TYPE_STYLESHEET:
                loadDependency(url, resourceLoader::loadStylesheet);
                break;
            case DependencyList.TYPE_HTML_IMPORT:
                loadDependency(url, resourceLoader::loadHtml);
                break;
            case DependencyList.TYPE_JAVASCRIPT:
                loadDependency(url, resourceLoader::loadScript);
                break;
            default:
                Console.error("Unknown dependency type " + type);
            }
        }
    }
}
