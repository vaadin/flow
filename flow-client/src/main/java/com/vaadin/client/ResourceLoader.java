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

/**
 * ResourceLoader lets you dynamically include external scripts and styles on
 * the page and lets you know when the resource has been loaded.
 *
 * <p>
 * Pure {@code @JsType(isNative=true)} binding to the TypeScript implementation
 * at {@code src/main/frontend/internal/client/ResourceLoader.ts}.
 *
 * <p>
 * The original Java {@link ResourceLoadListener} interface stays so existing
 * Java callers can pass anonymous implementations. {@link JsOverlay} methods
 * unwrap the listener into two {@link OnLoad} / {@link OnError}
 * {@link JsFunction} callbacks before crossing into TS, sidestepping the
 * GWT-OBF method name mangling that affects TS calling methods on Java
 * anonymous interface implementations.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client", name = "ResourceLoader")
public class ResourceLoader {

    /**
     * Event fired when a resource has been loaded. Plain TS class accessible
     * from Java via the {@link JsType} binding.
     */
    @JsType(isNative = true, namespace = "Vaadin.Flow.internal.client", name = "ResourceLoadEvent")
    public static class ResourceLoadEvent {

        public ResourceLoadEvent(ResourceLoader loader, String resourceData) {
            // Defined by the TS class constructor.
        }

        /** Gets the resource loader that has fired this event. */
        public native ResourceLoader getResourceLoader();

        /**
         * Gets the absolute url or content of the loaded resource or the JS
         * expression that imports the resource.
         */
        public native String getResourceData();
    }

    /**
     * Event listener that gets notified when a resource has been loaded.
     * Java-only interface; the TS implementation accepts two callbacks
     * directly. The {@link JsOverlay} methods on {@link ResourceLoader} split a
     * listener into the {@link OnLoad} / {@link OnError} {@link JsFunction}
     * callbacks before crossing into TS.
     */
    public interface ResourceLoadListener {
        /**
         * Notifies this ResourceLoadListener that a resource has been loaded.
         * Some browsers do not support any way of detecting load errors. In
         * these cases, onLoad will be called regardless of the status.
         */
        void onLoad(ResourceLoadEvent event);

        /**
         * Notifies this ResourceLoadListener that a resource could not be
         * loaded, e.g. because the file could not be found or because the
         * server did not respond.
         */
        void onError(ResourceLoadEvent event);
    }

    /** JS callback shape used at the TS boundary for the load event. */
    @FunctionalInterface
    @JsFunction
    @SuppressWarnings("unusable-by-js")
    public interface OnLoad {
        void onLoad(ResourceLoadEvent event);
    }

    /** JS callback shape used at the TS boundary for the error event. */
    @FunctionalInterface
    @JsFunction
    @SuppressWarnings("unusable-by-js")
    public interface OnError {
        void onError(ResourceLoadEvent event);
    }

    /** JS-friendly no-arg runnable used at the TS boundary. */
    @FunctionalInterface
    @JsFunction
    @SuppressWarnings("unusable-by-js")
    public interface JsTask {
        void run();
    }

    /**
     * JS-callable error sink used at the TS boundary. Java callers wrap
     * {@code systemErrorHandler::handleError} into this shape.
     */
    @FunctionalInterface
    @JsFunction
    @SuppressWarnings("unusable-by-js")
    public interface ErrorHandler {
        void accept(String message);
    }

    /**
     * Creates a new resource loader.
     *
     * @param errorHandler
     *            error sink invoked with a message when a resource fails to
     *            load (typically wired to
     *            {@code SystemErrorHandler::handleError})
     * @param initFromDom
     *            {@code true} if currently loaded resources should be marked as
     *            loaded
     */
    public ResourceLoader(ErrorHandler errorHandler, boolean initFromDom) {
        // Defined by the TS class constructor.
    }

    /** Clears a resource from the loaded resources set by its dependency ID. */
    public native void clearLoadedResourceById(String dependencyId);

    // ---------------- TS-native methods (taking two callbacks)
    // ----------------

    /** Loads a script. Native binding to the TS implementation. */
    public native void loadScript(String scriptUrl, OnLoad onLoad,
            OnError onError);

    /** Loads a script with explicit async / defer values. */
    public native void loadScriptAsyncDefer(String scriptUrl, OnLoad onLoad,
            OnError onError, boolean async, boolean defer);

    /** Loads a script with {@code type="module"}. */
    public native void loadJsModule(String scriptUrl, OnLoad onLoad,
            OnError onError, boolean async, boolean defer);

    /** Inlines a script. */
    public native void inlineScript(String scriptContents, OnLoad onLoad,
            OnError onError);

    /** Loads a stylesheet, optionally tagged with a dependency ID. */
    public native void loadStylesheet(String stylesheetUrl, OnLoad onLoad,
            OnError onError, String dependencyId);

    /** Inlines a stylesheet, optionally tagged with a dependency ID. */
    public native void inlineStyleSheet(String styleSheetContents,
            OnLoad onLoad, OnError onError, String dependencyId);

    /** Loads a dynamic import via the provided JS expression. */
    public native void loadDynamicImport(String expression, OnLoad onLoad,
            OnError onError);

    /**
     * Sets the provided task to be run by {@code HTMLImports.whenReady}. Runs
     * immediately if {@code HTMLImports.whenReady} is not supported.
     */
    public native void runWhenHtmlImportsReady(JsTask task);

    // ---------------- @JsOverlay convenience methods matching the original
    // Java API ----------------

    /**
     * Loads a script with default {@code async=false, defer=false}.
     */
    @JsOverlay
    public final void loadScript(String scriptUrl,
            ResourceLoadListener listener) {
        loadScript(scriptUrl, listener::onLoad, listener::onError);
    }

    /**
     * Loads a script with explicit {@code async} / {@code defer} values.
     */
    @JsOverlay
    public final void loadScript(String scriptUrl,
            ResourceLoadListener listener, boolean async, boolean defer) {
        loadScriptAsyncDefer(scriptUrl, listener::onLoad, listener::onError,
                async, defer);
    }

    /**
     * Loads a script with {@code type="module"}.
     */
    @JsOverlay
    public final void loadJsModule(String scriptUrl,
            ResourceLoadListener listener, boolean async, boolean defer) {
        loadJsModule(scriptUrl, listener::onLoad, listener::onError, async,
                defer);
    }

    /**
     * Inlines a script and notifies a listener.
     */
    @JsOverlay
    public final void inlineScript(String scriptContents,
            ResourceLoadListener listener) {
        inlineScript(scriptContents, listener::onLoad, listener::onError);
    }

    /**
     * Loads a stylesheet and notifies a listener.
     */
    @JsOverlay
    public final void loadStylesheet(String stylesheetUrl,
            ResourceLoadListener listener) {
        loadStylesheet(stylesheetUrl, listener, null);
    }

    /**
     * Loads a stylesheet with a specific dependency ID for tracking.
     */
    @JsOverlay
    public final void loadStylesheet(String stylesheetUrl,
            ResourceLoadListener listener, String dependencyId) {
        loadStylesheet(stylesheetUrl, listener::onLoad, listener::onError,
                dependencyId);
    }

    /**
     * Inlines a stylesheet and notifies a listener.
     */
    @JsOverlay
    public final void inlineStyleSheet(String styleSheetContents,
            ResourceLoadListener listener) {
        inlineStyleSheet(styleSheetContents, listener, null);
    }

    /**
     * Inlines a stylesheet with a specific dependency ID for tracking.
     */
    @JsOverlay
    public final void inlineStyleSheet(String styleSheetContents,
            ResourceLoadListener listener, String dependencyId) {
        inlineStyleSheet(styleSheetContents, listener::onLoad,
                listener::onError, dependencyId);
    }

    /**
     * Loads a dynamic import via the provided JS expression and reports the
     * result via the listener.
     */
    @JsOverlay
    public final void loadDynamicImport(String expression,
            ResourceLoadListener listener) {
        loadDynamicImport(expression, listener::onLoad, listener::onError);
    }

    /**
     * Sets the provided task to be run by {@code HTMLImports.whenReady}. Java
     * overload accepting a {@link Runnable}.
     */
    @JsOverlay
    public final void runWhenHtmlImportsReady(Runnable task) {
        runWhenHtmlImportsReady((JsTask) task::run);
    }
}
