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

import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.user.client.Timer;
import com.vaadin.client.hummingbird.collection.JsArray;
import com.vaadin.client.hummingbird.collection.JsCollections;
import com.vaadin.client.hummingbird.collection.JsMap;
import com.vaadin.client.hummingbird.collection.JsSet;

import elemental.client.Browser;
import elemental.dom.Document;
import elemental.dom.Element;
import elemental.dom.NodeList;
import elemental.html.HeadElement;
import elemental.html.LinkElement;
import elemental.html.ScriptElement;

/**
 * ResourceLoader lets you dynamically include external scripts and styles on
 * the page and lets you know when the resource has been loaded.
 *
 * You can also preload resources, allowing them to get cached by the browser
 * without being evaluated. This enables downloading multiple resources at once
 * while still controlling in which order e.g. scripts are executed.
 *
 * @author Vaadin Ltd
 * @since 7.0.0
 */
public class ResourceLoader {
    /**
     * Event fired when a resource has been loaded.
     */
    public static class ResourceLoadEvent {
        private final ResourceLoader loader;
        private final String resourceUrl;

        /**
         * Creates a new event.
         *
         * @param loader
         *            the resource loader that has loaded the resource
         * @param resourceUrl
         *            the url of the loaded resource
         */
        public ResourceLoadEvent(ResourceLoader loader, String resourceUrl) {
            this.loader = loader;
            this.resourceUrl = resourceUrl;
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
         * Gets the absolute url of the loaded resource.
         *
         * @return the absolute url of the loaded resource
         */
        public String getResourceUrl() {
            return resourceUrl;
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
     *
     *
     * @param scriptUrl
     *            the url of the script to load
     * @param resourceLoadListener
     *            the listener that will get notified when the script is loaded
     */
    public void loadScript(final String scriptUrl,
            final ResourceLoadListener resourceLoadListener) {
        loadScript(scriptUrl, resourceLoadListener, false);
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
     * @since 7.2.4
     */
    public void loadScript(final String scriptUrl,
            final ResourceLoadListener resourceLoadListener, boolean async) {
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
            scriptTag.setType("text/javascript");
            scriptTag.setAsync(async);

            addOnloadHandler(scriptTag, new ResourceLoadListener() {
                @Override
                public void onLoad(ResourceLoadEvent event) {
                    fireLoad(event);
                }

                @Override
                public void onError(ResourceLoadEvent event) {
                    fireError(event);
                }
            }, event);
            getHead().appendChild(scriptTag);
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
     *
     * @param htmlUrl
     *            url of HTML import to load
     * @param resourceLoadListener
     *            listener to notify when the HTML import is loaded
     */
    public void loadHtml(final String htmlUrl,
            final ResourceLoadListener resourceLoadListener) {
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

            class LoadAndReadyListener
                    implements ResourceLoadListener, Runnable {
                private boolean errorFired = false;

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
            LoadAndReadyListener listener = new LoadAndReadyListener();

            addOnloadHandler(linkTag, listener, event);
            getHead().appendChild(linkTag);

            if (supportsHtmlWhenReady) {
                addHtmlImportsReadyHandler(listener);
            }
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
     * @since 7.3
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

            if (BrowserInfo.get().isSafari()) {
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
                addOnloadHandler(linkElement, new ResourceLoadListener() {
                    @Override
                    public void onLoad(ResourceLoadEvent event) {
                        // Chrome, IE, Edge all fire load for errors, must check
                        // stylesheet data
                        if (BrowserInfo.get().isChrome()
                                || BrowserInfo.get().isIE()
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
                }, event);
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

    private static native int getStyleSheetLength(String url)
    /*-{
        for(var i = 0; i < $doc.styleSheets.length; i++) {
            if ($doc.styleSheets[i].href === url) {
                var sheet = $doc.styleSheets[i];
                try {
                    var rules = sheet.cssRules
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

    private static boolean addListener(String url,
            ResourceLoadListener listener,
            JsMap<String, JsArray<ResourceLoadListener>> listenerMap) {
        JsArray<ResourceLoadListener> listeners = listenerMap.get(url);
        if (listeners == null) {
            listeners = JsCollections.array();
            listeners.push(listener);
            listenerMap.set(url, listeners);
            return true;
        } else {
            listeners.push(listener);
            return false;
        }
    }

    private void fireError(ResourceLoadEvent event) {
        registry.getSystemErrorHandler()
                .handleError("Error loading " + event.getResourceUrl());
        String resource = event.getResourceUrl();

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
        Console.log("Loaded " + event.getResourceUrl());
        String resource = event.getResourceUrl();
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

}
