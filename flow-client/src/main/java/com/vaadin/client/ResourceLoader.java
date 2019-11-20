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

import java.util.function.Supplier;

import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.user.client.Timer;

import com.vaadin.client.flow.collection.JsArray;
import com.vaadin.client.flow.collection.JsCollections;
import com.vaadin.client.flow.collection.JsMap;
import com.vaadin.client.flow.collection.JsSet;
import com.vaadin.client.flow.util.NativeFunction;

import elemental.client.Browser;
import elemental.dom.Document;
import elemental.dom.Element;
import elemental.dom.NodeList;
import elemental.html.HeadElement;
import elemental.html.LinkElement;
import elemental.html.ScriptElement;
import elemental.html.SpanElement;
import elemental.html.StyleElement;

/**
 * ResourceLoader lets you dynamically include external scripts and styles on
 * the page and lets you know when the resource has been loaded.
 *
 * You can also preload resources, allowing them to get cached by the browser
 * without being evaluated. This enables downloading multiple resources at once
 * while still controlling in which order e.g. scripts are executed.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ResourceLoader {
    private class StyleSheetLoadListener implements ResourceLoadListener {
        private final String url;

        private StyleSheetLoadListener(String url) {
            this.url = url;
        }

        @Override
        public void onLoad(ResourceLoadEvent event) {
            // Chrome, IE, Edge all fire load for errors, must check
            // stylesheet data
            if (BrowserInfo.get().isChrome() || BrowserInfo.get().isIE()
                    || BrowserInfo.get().isEdge()) {
                int styleSheetLength = getStyleSheetLength(url);
                // Error if there's an empty stylesheet
                if (styleSheetLength == 0) {
                    fireError(event);
                    return;
                }
            }

            fireLoad(event);
        }

        @Override
        public void onError(ResourceLoadEvent event) {
            fireError(event);
        }
    }

    private class HtmlLoadListener implements ResourceLoadListener, Runnable {
        private final ResourceLoadEvent event;

        private boolean errorFired;

        private HtmlLoadListener(ResourceLoadEvent event) {
            this.event = event;
        }

        @Override
        public void run() {
            // Invoked through HTMLImports.whenReady
            if (!errorFired) {
                fireLoad(event);
            }
        }

        @Override
        public void onLoad(ResourceLoadEvent event) {
            if (!supportsHtmlWhenReady) {
                assert !errorFired;
                fireLoad(event);
            }
        }

        @Override
        public void onError(ResourceLoadEvent event) {
            assert !errorFired;
            errorFired = true;
            fireError(event);
        }
    }

    private class SimpleLoadListener implements ResourceLoadListener {
        @Override
        public void onLoad(ResourceLoadEvent event) {
            fireLoad(event);
        }

        @Override
        public void onError(ResourceLoadEvent event) {
            fireError(event);
        }
    }

    /**
     * Event fired when a resource has been loaded.
     */
    public static class ResourceLoadEvent {
        private final ResourceLoader loader;
        private final String resourceData;

        /**
         * Creates a new event.
         *
         * @param loader
         *            the resource loader that has loaded the resource
         * @param resourceData
         *            the url or content of the loaded resource or the JS
         *            expression that imports the resource
         */
        public ResourceLoadEvent(ResourceLoader loader, String resourceData) {
            this.loader = loader;
            this.resourceData = resourceData;
        }

        /**
         * Gets the resource loader that has fired this event.
         *
         * @return the resource loader
         */
        public ResourceLoader getResourceLoader() {
            return loader;
        }

        /**
         * Gets the absolute url or content of the loaded resource or the JS
         * expression that imports the resource.
         *
         * @return the absolute url or content of the loaded resource or the JS
         *         expression that imports the resource
         */
        public String getResourceData() {
            return resourceData;
        }

    }

    /**
     * Event listener that gets notified when a resource has been loaded.
     */
    public interface ResourceLoadListener {
        /**
         * Notifies this ResourceLoadListener that a resource has been loaded.
         * Some browsers do not support any way of detecting load errors. In
         * these cases, onLoad will be called regardless of the status.
         *
         * @see ResourceLoadEvent
         *
         * @param event
         *            a resource load event with information about the loaded
         *            resource
         */
        void onLoad(ResourceLoadEvent event);

        /**
         * Notifies this ResourceLoadListener that a resource could not be
         * loaded, e.g. because the file could not be found or because the
         * server did not respond. Some browsers do not support any way of
         * detecting load errors. In these cases, onLoad will be called
         * regardless of the status.
         *
         * @see ResourceLoadEvent
         *
         * @param event
         *            a resource load event with information about the resource
         *            that could not be loaded.
         */
        void onError(ResourceLoadEvent event);
    }

    private final JsSet<String> loadedResources = JsCollections.set();

    private final JsMap<String, JsArray<ResourceLoadListener>> loadListeners = JsCollections
            .map();

    private Registry registry;

    private final boolean supportsHtmlWhenReady = GWT.isClient()
            && supportsHtmlWhenReady();

    /**
     * Creates a new resource loader. You should not create you own resource
     * loader, but instead use {@link Registry#getResourceLoader()} to get an
     * instance.
     *
     * @param registry
     *            the global registry
     * @param initFromDom
     *            <code>true</code> if currently loaded resources should be
     *            marked as loaded, <code>false</code> to ignore currently
     *            loaded resources
     */
    public ResourceLoader(Registry registry, boolean initFromDom) {
        this.registry = registry;
        if (initFromDom) {
            initLoadedResourcesFromDom();
        }
    }

    /**
     * Populates the resource loader with the scripts currently added to the
     * page.
     */
    private void initLoadedResourcesFromDom() {
        Document document = Browser.getDocument();

        // detect already loaded scripts and stylesheets
        NodeList scripts = document.getElementsByTagName("script");
        for (int i = 0; i < scripts.getLength(); i++) {
            ScriptElement element = (ScriptElement) scripts.item(i);
            String src = element.getSrc();
            if (src != null && src.length() != 0) {
                loadedResources.add(src);
            }
        }

        NodeList links = document.getElementsByTagName("link");
        for (int i = 0; i < links.getLength(); i++) {
            LinkElement linkElement = (LinkElement) links.item(i);
            String rel = linkElement.getRel();
            String href = linkElement.getHref();
            if (("stylesheet".equalsIgnoreCase(rel)
                    || "import".equalsIgnoreCase(rel)) && href != null
                    && href.length() != 0) {
                loadedResources.add(href);
            }
        }
    }

    /**
     * Load a script and notify a listener when the script is loaded. Calling
     * this method when the script is currently loading or already loaded
     * doesn't cause the script to be loaded again, but the listener will still
     * be notified when appropriate.
     * <p>
     * Loads all dependencies with {@code async = false} and
     * {@code defer = false} attribute values, see
     * {@link #loadScript(String, ResourceLoadListener, boolean, boolean)}.
     *
     * @param scriptUrl
     *            the url of the script to load
     * @param resourceLoadListener
     *            the listener that will get notified when the script is loaded
     */
    public void loadScript(final String scriptUrl,
            final ResourceLoadListener resourceLoadListener) {
        loadScript(scriptUrl, resourceLoadListener, false, false);
    }

    /**
     * Load a script and notify a listener when the script is loaded. Calling
     * this method when the script is currently loading or already loaded
     * doesn't cause the script to be loaded again, but the listener will still
     * be notified when appropriate.
     *
     *
     * @param scriptUrl
     *            url of script to load
     * @param resourceLoadListener
     *            listener to notify when script is loaded
     * @param async
     *            What mode the script.async attribute should be set to
     * @param defer
     *            What mode the script.defer attribute should be set to
     */
    public void loadScript(final String scriptUrl,
            final ResourceLoadListener resourceLoadListener, boolean async,
            boolean defer) {
        loadScript(scriptUrl, resourceLoadListener, async, defer,
                "text/javascript");
    }

    /**
     * Load a script with type module and notify a listener when the script is
     * loaded. Calling this method when the script is currently loading or
     * already loaded doesn't cause the script to be loaded again, but the
     * listener will still be notified when appropriate.
     *
     *
     * @param scriptUrl
     *            url of script to load. It should be an external URL.
     * @param resourceLoadListener
     *            listener to notify when script is loaded
     * @param async
     *            What mode the script.async attribute should be set to
     * @param defer
     *            What mode the script.defer attribute should be set to
     */
    public void loadJsModule(final String scriptUrl,
            final ResourceLoadListener resourceLoadListener, boolean async,
            boolean defer) {
        loadScript(scriptUrl, resourceLoadListener, async, defer, "module");
    }

    private void loadScript(String scriptUrl,
            ResourceLoadListener resourceLoadListener, boolean async,
            boolean defer, String type) {
        final String url = WidgetUtil.getAbsoluteUrl(scriptUrl);
        ResourceLoadEvent event = new ResourceLoadEvent(this, url);
        if (loadedResources.has(url)) {
            if (resourceLoadListener != null) {
                resourceLoadListener.onLoad(event);
            }
            return;
        }

        if (addListener(url, resourceLoadListener, loadListeners)) {
            ScriptElement scriptTag = Browser.getDocument()
                    .createScriptElement();
            scriptTag.setSrc(url);
            scriptTag.setType(type);
            scriptTag.setAsync(async);
            scriptTag.setDefer(defer);

            addOnloadHandler(scriptTag, new SimpleLoadListener(), event);
            getHead().appendChild(scriptTag);
        }
    }

    /**
     * Inlines a script and notify a listener when the script is loaded. Calling
     * this method when the script is currently loading or already loaded
     * doesn't cause the script to be loaded again, but the listener will still
     * be notified when appropriate.
     *
     * @param scriptContents
     *            the script contents to inline
     * @param resourceLoadListener
     *            listener to notify when script is loaded
     */
    public void inlineScript(String scriptContents,
            final ResourceLoadListener resourceLoadListener) {
        ResourceLoadEvent event = new ResourceLoadEvent(this, scriptContents);
        if (loadedResources.has(scriptContents)) {
            if (resourceLoadListener != null) {
                resourceLoadListener.onLoad(event);
            }
            return;
        }

        if (addListener(scriptContents, resourceLoadListener, loadListeners)) {
            ScriptElement scriptElement = getDocument().createScriptElement();
            scriptElement.setTextContent(scriptContents);
            scriptElement.setType("text/javascript");

            addOnloadHandler(scriptElement, new SimpleLoadListener(), event);
            getHead().appendChild(scriptElement);
        }
    }

    private static Document getDocument() {
        return Browser.getDocument();
    }

    private static HeadElement getHead() {
        return getDocument().getHead();
    }

    /**
     * Loads an HTML import and notify a listener when the HTML import is
     * loaded. Calling this method when the HTML import is currently loading or
     * already loaded doesn't cause the HTML import to be loaded again, but the
     * listener will still be notified when appropriate.
     *
     * @param htmlUrl
     *            url of HTML import to load
     * @param resourceLoadListener
     *            listener to notify when the HTML import is loaded
     * @param async
     *            loads the import asynchronously, if {@code true},
     *            synchronously otherwise
     */
    public void loadHtml(final String htmlUrl,
            final ResourceLoadListener resourceLoadListener, boolean async) {
        final String url = WidgetUtil.getAbsoluteUrl(htmlUrl);
        ResourceLoadEvent event = new ResourceLoadEvent(this, url);
        if (loadedResources.has(url)) {
            if (resourceLoadListener != null) {
                resourceLoadListener.onLoad(event);
            }
            return;
        }

        if (addListener(url, resourceLoadListener, loadListeners)) {
            LinkElement linkTag = getDocument().createLinkElement();
            linkTag.setAttribute("rel", "import");
            linkTag.setAttribute("href", url);
            if (async) {
                linkTag.setAttribute("async", "true");
            }

            HtmlLoadListener listener = new HtmlLoadListener(event);

            addOnloadHandler(linkTag, listener, event);
            getHead().appendChild(linkTag);

            if (supportsHtmlWhenReady) {
                addHtmlImportsReadyHandler(listener);
            }
        }
    }

    /**
     * Inlines an HTML import and notify a listener when the HTML import is
     * loaded. Calling this method when the HTML import is currently loading or
     * already loaded doesn't cause the HTML import to be loaded again, but the
     * listener will still be notified when appropriate.
     *
     * @param htmlContents
     *            the html contents to inline
     * @param resourceLoadListener
     *            listener to notify when the HTML import is loaded
     */
    public void inlineHtml(String htmlContents,
            final ResourceLoadListener resourceLoadListener) {
        ResourceLoadEvent event = new ResourceLoadEvent(this, htmlContents);
        if (loadedResources.has(htmlContents)) {
            if (resourceLoadListener != null) {
                resourceLoadListener.onLoad(event);
            }
            return;
        }

        if (addListener(htmlContents, resourceLoadListener, loadListeners)) {
            SpanElement spanElement = getDocument().createSpanElement();
            spanElement.setInnerHTML(htmlContents);
            spanElement.setAttribute("hidden", "true");

            HtmlLoadListener listener = new HtmlLoadListener(event);
            getDocument().appendChild(spanElement);
            addOnloadHandler(spanElement, listener, event);

            if (supportsHtmlWhenReady) {
                addHtmlImportsReadyHandler(listener);
            }
        }
    }

    /**
     * Sets the provided task to be run by <code>HTMLImports.whenReady</code>.
     * The task is run immediately if <code>HTMLImports.whenReady</code> is not
     * supported.
     *
     * @param task
     *            the task to run, not <code>null</code>
     */
    public void runWhenHtmlImportsReady(Runnable task) {
        assert task != null;
        if (supportsHtmlWhenReady) {
            addHtmlImportsReadyHandler(task);
        } else {
            task.run();
        }
    }

    private static native boolean supportsHtmlWhenReady()
    /*-{
        return !!($wnd.HTMLImports && $wnd.HTMLImports.whenReady);
    }-*/;

    private static native void addHtmlImportsReadyHandler(Runnable handler)
    /*-{
        $wnd.HTMLImports.whenReady($entry(function() {
            handler.@Runnable::run()();
        }));
    }-*/;

    /**
     * Adds an onload listener to the given element, which should be a link or a
     * script tag. The listener is called whenever loading is complete or an
     * error occurred.
     *
     * @param element
     *            the element to attach a listener to
     * @param listener
     *            the listener to call
     * @param event
     *            the event passed to the listener
     */
    public static native void addOnloadHandler(Element element,
            ResourceLoadListener listener, ResourceLoadEvent event)
    /*-{
        element.onload = $entry(function() {
            element.onload = null;
            element.onerror = null;
            element.onreadystatechange = null;
            listener.@com.vaadin.client.ResourceLoader.ResourceLoadListener::onLoad(Lcom/vaadin/client/ResourceLoader$ResourceLoadEvent;)(event);
        });
        element.onerror = $entry(function() {
            element.onload = null;
            element.onerror = null;
            element.onreadystatechange = null;
            listener.@com.vaadin.client.ResourceLoader.ResourceLoadListener::onError(Lcom/vaadin/client/ResourceLoader$ResourceLoadEvent;)(event);
        });
        element.onreadystatechange = function() {
            if ("loaded" === element.readyState || "complete" === element.readyState ) {
                element.onload(arguments[0]);
            }
        };
    }-*/;

    /**
     * Load a stylesheet and notify a listener when the stylesheet is loaded.
     * Calling this method when the stylesheet is currently loading or already
     * loaded doesn't cause the stylesheet to be loaded again, but the listener
     * will still be notified when appropriate.
     *
     * @param stylesheetUrl
     *            the url of the stylesheet to load
     * @param resourceLoadListener
     *            the listener that will get notified when the stylesheet is
     *            loaded
     */
    public void loadStylesheet(final String stylesheetUrl,
            final ResourceLoadListener resourceLoadListener) {
        final String url = WidgetUtil.getAbsoluteUrl(stylesheetUrl);
        final ResourceLoadEvent event = new ResourceLoadEvent(this, url);
        if (loadedResources.has(url)) {
            if (resourceLoadListener != null) {
                resourceLoadListener.onLoad(event);
            }
            return;
        }

        if (addListener(url, resourceLoadListener, loadListeners)) {
            LinkElement linkElement = getDocument().createLinkElement();
            linkElement.setRel("stylesheet");
            linkElement.setType("text/css");
            linkElement.setHref(url);

            if (BrowserInfo.get().isSafariOrIOS()) {
                // Safari doesn't fire any events for link elements
                // See http://www.phpied.com/when-is-a-stylesheet-really-loaded/
                Scheduler.get().scheduleFixedPeriod(new RepeatingCommand() {
                    private final Duration duration = new Duration();

                    @Override
                    public boolean execute() {
                        int styleSheetLength = getStyleSheetLength(url);
                        if (getStyleSheetLength(url) > 0) {
                            fireLoad(event);
                            return false; // Stop repeating
                        } else if (styleSheetLength == 0) {
                            // "Loaded" empty sheet -> most likely 404 error
                            fireError(event);
                            return true;
                        } else if (duration.elapsedMillis() > 60 * 1000) {
                            fireError(event);
                            return false;
                        } else {
                            return true; // Continue repeating
                        }
                    }
                }, 10);
            } else {
                addOnloadHandler(linkElement, new StyleSheetLoadListener(url),
                        event);
                if (BrowserInfo.get().isOpera()) {
                    // Opera onerror never fired, assume error if no onload in x
                    // seconds
                    new Timer() {
                        @Override
                        public void run() {
                            if (!loadedResources.has(url)) {
                                fireError(event);
                            }
                        }
                    }.schedule(5 * 1000);
                }
            }

            getHead().appendChild(linkElement);
        }
    }

    /**
     * Inlines a stylesheet and notify a listener when the stylesheet is loaded.
     * Calling this method when the stylesheet is currently loading or already
     * loaded doesn't cause the stylesheet to be loaded again, but the listener
     * will still be notified when appropriate.
     *
     * @param styleSheetContents
     *            the contents to inline
     * @param resourceLoadListener
     *            the listener that will get notified when the stylesheet is
     *            loaded
     */
    public void inlineStyleSheet(String styleSheetContents,
            final ResourceLoadListener resourceLoadListener) {
        final ResourceLoadEvent event = new ResourceLoadEvent(this,
                styleSheetContents);
        if (loadedResources.has(styleSheetContents)) {
            if (resourceLoadListener != null) {
                resourceLoadListener.onLoad(event);
            }
            return;
        }

        if (addListener(styleSheetContents, resourceLoadListener,
                loadListeners)) {
            StyleElement styleSheetElement = getDocument().createStyleElement();
            styleSheetElement.setTextContent(styleSheetContents);
            styleSheetElement.setType("text/css");

            addCssLoadHandler(styleSheetContents, event, styleSheetElement);

            getHead().appendChild(styleSheetElement);
        }
    }

    /**
     * Loads a dynamic import via the provided JS {@code expression} and reports
     * the result via the {@code resourceLoadListener}.
     *
     * @param expression
     *            the JS expression which returns a Promise
     * @param resourceLoadListener
     *            a listener to report the Promise result exection
     */
    public void loadDynamicImport(String expression,
            ResourceLoadListener resourceLoadListener) {

        ResourceLoadEvent event = new ResourceLoadEvent(this, expression);
        NativeFunction function = new NativeFunction(expression);
        runPromiseExpression(expression, () -> function.call(null),
                () -> resourceLoadListener.onLoad(event),
                () -> resourceLoadListener.onError(event));
    }

    private void addCssLoadHandler(String styleSheetContents,
            ResourceLoadEvent event, StyleElement styleSheetElement) {
        if (BrowserInfo.get().isSafariOrIOS() || BrowserInfo.get().isOpera()) {
            // Safari and Opera don't fire any events for link elements
            // See http://www.phpied.com/when-is-a-stylesheet-really-loaded/
            new Timer() {
                @Override
                public void run() {
                    if (loadedResources.has(styleSheetContents)) {
                        fireLoad(event);
                    } else {
                        fireError(event);
                    }
                }
            }.schedule(5 * 1000);
        } else {
            addOnloadHandler(styleSheetElement, new ResourceLoadListener() {
                @Override
                public void onLoad(ResourceLoadEvent event) {
                    fireLoad(event);
                }

                @Override
                public void onError(ResourceLoadEvent event) {
                    fireError(event);
                }
            }, event);
        }
    }

    private static native int getStyleSheetLength(String url)
    /*-{
        for(var i = 0; i < $doc.styleSheets.length; i++) {
            if ($doc.styleSheets[i].href === url) {
                var sheet = $doc.styleSheets[i];
                try {
                    var rules = sheet.cssRules;
                    if (rules === undefined) {
                        rules = sheet.rules;
                    }
                    if (rules === null) {
                        // Style sheet loaded, but can't access length because of XSS -> assume there's something there
                        return 1;
                    }
                    // Return length so we can distinguish 0 (probably 404 error) from normal case.
                    return rules.length;
                } catch (err) {
                    return 1;
                }
            }
        }
        // No matching stylesheet found -> not yet loaded
        return -1;
    }-*/;

    private static boolean addListener(String resourceId,
            ResourceLoadListener listener,
            JsMap<String, JsArray<ResourceLoadListener>> listenerMap) {
        JsArray<ResourceLoadListener> listeners = listenerMap.get(resourceId);
        if (listeners == null) {
            listeners = JsCollections.array();
            listeners.push(listener);
            listenerMap.set(resourceId, listeners);
            return true;
        } else {
            listeners.push(listener);
            return false;
        }
    }

    private void fireError(ResourceLoadEvent event) {
        registry.getSystemErrorHandler()
                .handleError("Error loading " + event.getResourceData());
        String resource = event.getResourceData();

        JsArray<ResourceLoadListener> listeners = loadListeners.get(resource);
        loadListeners.delete(resource);
        if (listeners != null && !listeners.isEmpty()) {
            for (int i = 0; i < listeners.length(); i++) {
                ResourceLoadListener listener = listeners.get(i);
                if (listener != null) {
                    listener.onError(event);
                }
            }
        }
    }

    private void fireLoad(ResourceLoadEvent event) {
        Console.log("Loaded " + event.getResourceData());
        String resource = event.getResourceData();
        JsArray<ResourceLoadListener> listeners = loadListeners.get(resource);
        loadedResources.add(resource);
        loadListeners.delete(resource);
        if (listeners != null && !listeners.isEmpty()) {
            for (int i = 0; i < listeners.length(); i++) {
                ResourceLoadListener listener = listeners.get(i);
                if (listener != null) {
                    listener.onLoad(event);
                }
            }
        }
    }

    private static native void runPromiseExpression(String expression,
            Supplier<Object> promiseSupplier, Runnable onSuccess,
            Runnable onError)
    /*-{
          try {
            var promise = promiseSupplier.@java.util.function.Supplier::get(*)();
            if ( !(promise instanceof $wnd.Promise )){
                throw new Error('The expression "'+expression+'" result is not a Promise.');
            }
            promise.then( function(result) { onSuccess.@java.lang.Runnable::run(*)(); } ,
                          function(error) { console.error(error); onError.@java.lang.Runnable::run(*)(); } );
          }
          catch(error) {
               console.error(error);
               onError.@java.lang.Runnable::run(*)();
          }
    }-*/;

}
