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

import java.util.HashMap;
import java.util.Map;
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
    private static final JsArray<Command> callbacks = JsCollections.array();

    // Listener that loads the next when one is completed
    private static final ResourceLoadListener BLOCKING_RESOURCE_LOAD_LISTENER = new ResourceLoadListener() {
        @Override
        public void onLoad(ResourceLoadEvent event) {
            // Call start for next before calling end for current
            endBlockingDependencyLoading();
        }

        @Override
        public void onError(ResourceLoadEvent event) {
            Console.error(event.getResourceUrl() + " could not be loaded.");
            // The show must go on
            onLoad(event);
        }
    };

    private static final ResourceLoadListener NON_BLOCKING_RESOURCE_LOAD_LISTENER = new ResourceLoadListener() {
        @Override
        public void onLoad(ResourceLoadEvent event) {
            // Do nothing on success, simply continue loading
        }

        @Override
        public void onError(ResourceLoadEvent event) {
            Console.error(event.getResourceUrl() + " could not be loaded.");
            // The show must go on
            onLoad(event);
        }
    };

    private static int blockingDependenciesLoading;

    private final URIResolver uriResolver;
    private final ResourceLoader resourceLoader;

    /**
     * Creates a new instance connected to the given registry.
     *
     * @param registry
     *            the global registry
     */
    DependencyLoader(Registry registry) {
        uriResolver = registry.getURIResolver();
        resourceLoader = registry.getResourceLoader();
    }

    /**
     * Loads the given dependency using the given loader and ensures any
     * callbacks registered using
     * {@link #runWhenBlockingDependenciesLoaded(Command)} are run when all
     * blocking dependencies have been loaded.
     *
     * @param dependencyUrl
     *            a dependency URL to load, will be translated using
     *            {@link URIResolver#resolveVaadinUri(String)} before it is
     *            loaded, not {@code null}
     * @param blocking
     *            indicates whether the resource is blocking or not
     * @param loader
     *            function which takes care of loading the dependency URL
     */
    private void loadDependency(final String dependencyUrl, boolean blocking,
            final BiConsumer<String, ResourceLoadListener> loader) {
        assert dependencyUrl != null;
        assert loader != null;

        // Start chain by loading first
        String url = uriResolver.resolveVaadinUri(dependencyUrl);
        if (blocking) {
            startBlockingDependencyLoading();
            loader.accept(url, BLOCKING_RESOURCE_LOAD_LISTENER);
        } else {
            loader.accept(url, NON_BLOCKING_RESOURCE_LOAD_LISTENER);
        }
    }

    /**
     * Adds a command to be run when all blocking dependencies have finished
     * loading.
     * <p>
     * If no blocking dependencies are currently being loaded, runs the command
     * immediately.
     *
     * @see #startBlockingDependencyLoading()
     * @see #endBlockingDependencyLoading()
     * @param command
     *            the command to run when blocking dependencies have been loaded
     */
    public static void runWhenBlockingDependenciesLoaded(Command command) {
        if (blockingDependenciesLoading == 0) {
            command.execute();
        } else {
            callbacks.push(command);
        }
    }

    /**
     * Marks that loading of a dependency has started.
     *
     * @see #runWhenBlockingDependenciesLoaded(Command)
     * @see #endBlockingDependencyLoading()
     */
    private static void startBlockingDependencyLoading() {
        blockingDependenciesLoading++;
    }

    /**
     * Marks that loading of a dependency has ended.
     * <p>
     * If all pending dependencies have been loaded, calls any callback
     * registered using {@link #runWhenBlockingDependenciesLoaded(Command)}.
     */
    private static void endBlockingDependencyLoading() {
        blockingDependenciesLoading--;
        if (blockingDependenciesLoading == 0 && callbacks.length() != 0) {
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

        Map<String, BiConsumer<String, ResourceLoadListener>> nonBlockingDependencies = new HashMap<>();

        for (int i = 0; i < deps.length(); i++) {
            JsonObject dependencyJson = deps.getObject(i);
            String url = dependencyJson.getString(DependencyList.KEY_URL);
            boolean blocking = dependencyJson
                    .getBoolean(DependencyList.KEY_BLOCKING);
            BiConsumer<String, ResourceLoadListener> loader = getResourceLoader(
                    dependencyJson.getString(DependencyList.KEY_TYPE),
                    blocking);

            if (loader != null) {
                if (blocking) {
                    loadDependency(url, true, loader);
                } else {
                    nonBlockingDependencies.put(url, loader);
                }
            }
        }

        runWhenBlockingDependenciesLoaded(() -> nonBlockingDependencies
                .forEach((url, loader) -> loadDependency(url, false, loader)));
    }

    private BiConsumer<String, ResourceLoadListener> getResourceLoader(
            String resourceType, boolean blocking) {
        switch (resourceType) {
        case DependencyList.TYPE_STYLESHEET:
            return resourceLoader::loadStylesheet;
        case DependencyList.TYPE_HTML_IMPORT:
            return resourceLoader::loadHtml;
        case DependencyList.TYPE_JAVASCRIPT:
            return (scriptUrl, resourceLoadListener) -> resourceLoader
                    .loadScript(scriptUrl, resourceLoadListener, false,
                            !blocking);
        default:
            Console.error("Unknown dependency type " + resourceType);
            return null;
        }
    }
}
