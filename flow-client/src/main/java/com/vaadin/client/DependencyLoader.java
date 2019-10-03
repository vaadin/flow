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
package com.vaadin.client;

import java.util.function.BiConsumer;

import com.google.gwt.core.client.Scheduler;

import com.vaadin.client.ResourceLoader.ResourceLoadEvent;
import com.vaadin.client.ResourceLoader.ResourceLoadListener;
import com.vaadin.client.flow.collection.JsArray;
import com.vaadin.client.flow.collection.JsCollections;
import com.vaadin.client.flow.collection.JsMap;
import com.vaadin.flow.shared.ui.Dependency;
import com.vaadin.flow.shared.ui.LoadMode;

import elemental.json.JsonArray;
import elemental.json.JsonObject;

/**
 * Handles loading of dependencies (stylesheets and scripts) in the application.
 *
 * @author Vaadin Ltd
 * @since 1.0
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
            Console.error(
                    "'" + event.getResourceData() + "' could not be loaded.");
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
            Console.error(event.getResourceData() + " could not be loaded.");
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

    private void loadLazyDependency(String dependencyUrl,
            final BiConsumer<String, ResourceLoadListener> loader) {
        loader.accept(dependencyUrl, LAZY_RESOURCE_LOAD_LISTENER);
    }

    private void loadDependencyEagerly(String data,
            final BiConsumer<String, ResourceLoadListener> loader) {
        startEagerDependencyLoading();
        loader.accept(data, EAGER_RESOURCE_LOAD_LISTENER);
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
        if (eagerDependenciesLoading == 0 && !callbacks.isEmpty()) {
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
     * @param clientDependencies
     *            the map of the dependencies to load, divided into groups by
     *            load mode, not {@code null}.
     */
    public void loadDependencies(
            JsMap<LoadMode, JsonArray> clientDependencies) {
        assert clientDependencies != null;

        JsMap<String, BiConsumer<String, ResourceLoadListener>> lazyDependencies = JsCollections
                .map();

        clientDependencies.forEach((dependencies, mode) -> {
            extractLazyDependenciesAndLoadOthers(mode, dependencies).forEach(
                    (loader, url) -> lazyDependencies.set(url, loader));
        });

        // postpone load dependencies execution after the browser event
        // loop to make possible to execute all other commands that should be
        // run after the eager dependencies so that lazy dependencies
        // don't block those commands
        if (!lazyDependencies.isEmpty()) {
            runWhenEagerDependenciesLoaded(
                    () -> Scheduler.get().scheduleDeferred(() -> {
                        Console.log(
                                "Finished loading eager dependencies, loading lazy.");
                        lazyDependencies.forEach((loader,
                                url) -> loadLazyDependency(url, loader));
                    }));
        }
    }

    private JsMap<String, BiConsumer<String, ResourceLoadListener>> extractLazyDependenciesAndLoadOthers(
            LoadMode loadMode, JsonArray dependencies) {
        JsMap<String, BiConsumer<String, ResourceLoadListener>> lazyDependencies = JsCollections
                .map();
        for (int i = 0; i < dependencies.length(); i++) {
            JsonObject dependencyJson = dependencies.getObject(i);
            Dependency.Type type = Dependency.Type
                    .valueOf(dependencyJson.getString(Dependency.KEY_TYPE));
            BiConsumer<String, ResourceLoadListener> resourceLoader = getResourceLoader(
                    type, loadMode);

            if (type == Dependency.Type.DYNAMIC_IMPORT) {
                loadDependencyEagerly(
                        dependencyJson.getString(Dependency.KEY_URL),
                        resourceLoader);
            } else {
                switch (loadMode) {
                case EAGER:
                    loadDependencyEagerly(getDependencyUrl(dependencyJson),
                            resourceLoader);
                    break;
                case LAZY:
                    lazyDependencies.set(getDependencyUrl(dependencyJson),
                            resourceLoader);
                    break;
                case INLINE:
                    loadDependencyEagerly(
                            dependencyJson.getString(Dependency.KEY_CONTENTS),
                            resourceLoader);
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Unknown load mode = " + loadMode);
                }
            }
        }
        return lazyDependencies;
    }

    private String getDependencyUrl(JsonObject dependencyJson) {
        return registry.getURIResolver()
                .resolveVaadinUri(dependencyJson.getString(Dependency.KEY_URL));
    }

    private BiConsumer<String, ResourceLoadListener> getResourceLoader(
            Dependency.Type resourceType, LoadMode loadMode) {
        ResourceLoader resourceLoader = registry.getResourceLoader();
        boolean inline = loadMode == LoadMode.INLINE;

        switch (resourceType) {
        case STYLESHEET:
            if (inline) {
                return resourceLoader::inlineStyleSheet;
            }
            return resourceLoader::loadStylesheet;
        case HTML_IMPORT:
            if (inline) {
                return resourceLoader::inlineHtml;
            }
            return (scriptUrl, resourceLoadListener) -> resourceLoader
                    .loadHtml(scriptUrl, resourceLoadListener, false);
        case JAVASCRIPT:
            if (inline) {
                return resourceLoader::inlineScript;
            }
            return (scriptUrl, resourceLoadListener) -> resourceLoader
                    .loadScript(scriptUrl, resourceLoadListener, false, true);
        case JS_MODULE:
            if (inline) {
                throw new IllegalArgumentException(
                        "Inline load mode is not supported for JsModule.");
            }
            return (scriptUrl, resourceLoadListener) -> resourceLoader
                    .loadJsModule(scriptUrl, resourceLoadListener, false, true);
        case DYNAMIC_IMPORT:
            return resourceLoader::loadDynamicImport;
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
