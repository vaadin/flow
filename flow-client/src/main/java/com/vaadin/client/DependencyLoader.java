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
package com.vaadin.client;

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsType;

import com.vaadin.client.flow.collection.JsMap;
import com.vaadin.flow.shared.ui.LoadMode;

import elemental.json.JsonArray;

/**
 * Handles loading of dependencies (stylesheets and scripts) in the application.
 *
 * <p>
 * Pure {@code @JsType(isNative=true)} binding to the TypeScript implementation
 * at {@code src/main/frontend/internal/client/DependencyLoader.ts}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client", name = "DependencyLoader")
public class DependencyLoader {

    /** JS-friendly no-arg runnable used at the TS boundary. */
    @FunctionalInterface
    @JsFunction
    @SuppressWarnings("unusable-by-js")
    public interface JsCommand {
        void execute();
    }

    /**
     * Creates a new DependencyLoader wired to the URI resolver and resource
     * loader it needs. Taking these directly (instead of going through the Java
     * {@code Registry} facade) sidesteps GWT-OBF method-name mangling for TS
     * code reaching back into Java instance methods.
     */
    public DependencyLoader(URIResolver uriResolver,
            ResourceLoader resourceLoader) {
        // Defined by the TS class constructor.
    }

    /**
     * Prevents eager dependencies from being considered as loaded until
     * {@code HTMLImports.whenReady} has been run.
     */
    public native void requireHtmlImportsReady();

    // ---------------- @JsOverlay convenience methods ----------------

    /**
     * Triggers loading of the given dependencies, divided into groups by load
     * mode. {@link LoadMode} enum keys are converted to their {@code name()}
     * strings before crossing into TS, so the TS implementation only sees plain
     * string keys.
     */
    @JsOverlay
    public final void loadDependencies(
            JsMap<LoadMode, JsonArray> clientDependencies) {
        JsMap<String, JsonArray> stringKeyed = stringKeyMap();
        clientDependencies
                .forEach((value, key) -> stringKeyed.set(key.name(), value));
        loadDependenciesJs(stringKeyed);
    }

    /**
     * Calls the TS {@code loadDependencies} entry point. Renamed on the Java
     * side so the {@link LoadMode}-keyed {@link JsOverlay} can be the only
     * Java-visible overload; the {@code name} hint preserves the JS export name
     * the TS class exposes.
     */
    @jsinterop.annotations.JsMethod(name = "loadDependencies")
    private native void loadDependenciesJs(
            JsMap<String, JsonArray> clientDependencies);

    @JsOverlay
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static JsMap<String, JsonArray> stringKeyMap() {
        return new JsMap();
    }

    /**
     * Adds a command to be run when all eager dependencies have finished
     * loading. Wraps the reference into a {@link JsCommand} on the Java side so
     * TS receives a plain function and never has to dispatch through a Java
     * functional interface (which would be OBF-mangled).
     */
    @JsOverlay
    public static void runWhenEagerDependenciesLoaded(Command command) {
        runWhenEagerDependenciesLoadedJs((JsCommand) command::execute);
    }

    /**
     * Calls the TS {@code runWhenEagerDependenciesLoaded} entry point. Renamed
     * on the Java side so the {@link Command}-taking {@link JsOverlay} can be
     * the only Java-visible overload; the {@code name} hint preserves the
     * actual JS export name the TS class exposes.
     */
    @jsinterop.annotations.JsMethod(name = "runWhenEagerDependenciesLoaded")
    private static native void runWhenEagerDependenciesLoadedJs(
            JsCommand command);
}
