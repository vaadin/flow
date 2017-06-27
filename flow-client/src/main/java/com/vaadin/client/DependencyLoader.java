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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import com.google.gwt.core.client.Scheduler;
import com.vaadin.client.ResourceLoader.ResourceLoadEvent;
import com.vaadin.client.ResourceLoader.ResourceLoadListener;
import com.vaadin.client.flow.collection.JsArray;
import com.vaadin.client.flow.collection.JsCollections;
import com.vaadin.shared.ui.LoadMode;
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
    private static final ResourceLoadListener EAGER_RESOURCE_LOAD_LISTENER = new ResourceLoadListener() {
        @Override
        public void onLoad(ResourceLoadEvent event) {
            // Call start for next before calling end for current
            endEagerDependencyLoading();
        }

        @Override
        public void onError(ResourceLoadEvent event) {
            Console.error(event.getResourceUrl() + " could not be loaded.");
            // The show must go on
            onLoad(event);
        }
    };

    private static final ResourceLoadListener LAZY_RESOURCE_LOAD_LISTENER = new ResourceLoadListener() {
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

    private static int eagerDependenciesLoading;

    private final Registry registry;

    /**
     * Creates a new instance connected to the given registry.
     *
     * @param registry
     *            the global registry
     */
    public DependencyLoader(Registry registry) {
        this.registry = registry;
    }

    private void loadDependency(final String dependencyUrl, boolean eager,
            final BiConsumer<String, ResourceLoadListener> loader) {
        assert dependencyUrl != null;
        assert loader != null;

        // Start chain by loading first
        String url = registry.getURIResolver().resolveVaadinUri(dependencyUrl);
        if (eager) {
            startEagerDependencyLoading();
            loader.accept(url, EAGER_RESOURCE_LOAD_LISTENER);
        } else {
            loader.accept(url, LAZY_RESOURCE_LOAD_LISTENER);
        }
    }

    /**
     * Adds a command to be run when all eager dependencies have finished
     * loading.
     * <p>
     * If no eager dependencies are currently being loaded, runs the command
     * immediately.
     *
     * @see #startEagerDependencyLoading()
     * @see #endEagerDependencyLoading()
     * @param command
     *            the command to run when eager dependencies have been loaded
     */
    public static void runWhenEagerDependenciesLoaded(Command command) {
        if (eagerDependenciesLoading == 0) {
            command.execute();
        } else {
            callbacks.push(command);
        }
    }

    /**
     * Marks that loading of a dependency has started.
     *
     * @see #runWhenEagerDependenciesLoaded(Command)
     * @see #endEagerDependencyLoading()
     */
    private static void startEagerDependencyLoading() {
        eagerDependenciesLoading++;
    }

    /**
     * Marks that loading of a dependency has ended.
     * <p>
     * If all pending dependencies have been loaded, calls any callback
     * registered using {@link #runWhenEagerDependenciesLoaded(Command)}.
     */
    private static void endEagerDependencyLoading() {
        eagerDependenciesLoading--;
        if (eagerDependenciesLoading == 0 && callbacks.length() != 0) {
            try {
                for (int i = 0; i < callbacks.length(); i++) {
                    Command cmd = callbacks.get(i);
                    cmd.execute();
                }
            } finally {
                callbacks.clear();
            }
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

        Map<String, BiConsumer<String, ResourceLoadListener>> lazyDependencies = new LinkedHashMap<>();

        for (int i = 0; i < deps.length(); i++) {
            JsonObject dependencyJson = deps.getObject(i);
            String url = dependencyJson.getString(DependencyList.KEY_URL);
            LoadMode loadMode = LoadMode.valueOf(
                    dependencyJson.getString(DependencyList.KEY_LOAD_MODE));
            BiConsumer<String, ResourceLoadListener> loader = getResourceLoader(
                    dependencyJson.getString(DependencyList.KEY_TYPE));
            if (loadMode == LoadMode.EAGER) {
                loadDependency(url, true, loader);
            } else {
                lazyDependencies.put(url, loader);
            }
        }

        // postpone load dependencies execution after the browser event
        // loop to make possible to execute all other commands that should be
        // run after the eager dependencies so that lazy dependencies
        // don't block those commands
        if (!lazyDependencies.isEmpty()) {
            runWhenEagerDependenciesLoaded(
                    () -> Scheduler.get().scheduleDeferred(() -> {
                        Console.log(
                                "Finished loading eager dependencies, loading lazy.");
                        lazyDependencies.forEach((url,
                                loader) -> loadDependency(url, false, loader));
                    }));
        }
    }

    private BiConsumer<String, ResourceLoadListener> getResourceLoader(
            String resourceType) {
        ResourceLoader resourceLoader = registry.getResourceLoader();
        switch (resourceType) {
        case DependencyList.TYPE_STYLESHEET:
            return resourceLoader::loadStylesheet;
        case DependencyList.TYPE_HTML_IMPORT:
            return (scriptUrl, resourceLoadListener) -> resourceLoader
                    .loadHtml(scriptUrl, resourceLoadListener, false);
        case DependencyList.TYPE_JAVASCRIPT:
            return (scriptUrl, resourceLoadListener) -> resourceLoader
                    .loadScript(scriptUrl, resourceLoadListener, false, true);
        default:
            throw new IllegalArgumentException(
                    "Unknown dependency type " + resourceType);
        }
    }

    /**
     * Prevents eager dependencies from being considered as loaded until
     * <code>HTMLImports.whenReady</code> has been run.
     */
    public void requireHtmlImportsReady() {
        startEagerDependencyLoading();
        registry.getResourceLoader().runWhenHtmlImportsReady(
                DependencyLoader::endEagerDependencyLoading);
    }
}
