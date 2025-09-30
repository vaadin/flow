/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.component.page;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeType;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.Direction;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.internal.DependencyList;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.component.internal.UIInternals.JavaScriptInvocation;
import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.internal.UrlUtil;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.shared.ui.Dependency;
import com.vaadin.flow.shared.ui.Dependency.Type;
import com.vaadin.flow.shared.ui.LoadMode;

/**
 * Represents the web page open in the browser, containing the UI it is
 * connected to.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class Page implements Serializable {

    private final UI ui;
    private final History history;
    private DomListenerRegistration resizeReceiver;
    private ArrayList<BrowserWindowResizeListener> resizeListeners;

    /**
     * Creates a page instance for the given UI.
     *
     * @param ui
     *            the UI that this page instance is connected to
     */
    public Page(UI ui) {
        this.ui = ui;
        history = new History(ui);
    }

    /**
     * Sets the page title. The title is displayed by the browser e.g. as the
     * title of the browser window or tab.
     * <p>
     * To clear the page title, use an empty string.
     *
     * @param title
     *            the page title to set, not <code>null</code>
     */
    public void setTitle(String title) {
        if (title == null) {
            throw new IllegalArgumentException("Cannot set a null page title.");
        }

        ui.getInternals().setTitle(title);
    }

    /**
     * Adds the given style sheet to the page and ensures that it is loaded
     * successfully.
     * <p>
     * Relative URLs are interpreted as relative to the static web resources
     * directory. So if the {@code url} value is {@code "some.js"} and
     * {@code src/main/webapp} is used as a location for static web resources
     * (which is the default location) then the file system path for the
     * resource should be {@code src/main/webapp/some.js}.
     * <p>
     * You can prefix the URL with {@code context://} to make it relative to the
     * context path or use an absolute URL to refer to files outside the
     * frontend directory.
     * <p>
     * For component related style sheet dependencies, you should use the
     * {@link StyleSheet @StyleSheet} annotation.
     * <p>
     * Is is guaranteed that style sheet will be loaded before the first page
     * load. For more options, refer to {@link #addStyleSheet(String, LoadMode)}
     *
     * @param url
     *            the URL to load the style sheet from, not <code>null</code>
     * @return a registration object that can be used to remove the style sheet
     */
    public Registration addStyleSheet(String url) {
        return addStyleSheet(url, LoadMode.EAGER);
    }

    /**
     * Adds the given style sheet to the page and ensures that it is loaded
     * successfully.
     * <p>
     * Relative URLs are interpreted as relative to the static web resources
     * directory. So if the {@code url} value is {@code "some.js"} and
     * {@code src/main/webapp} is used as a location for static web resources
     * (which is the default location) then the file system path for the
     * resource should be {@code src/main/webapp/some.js}.
     * <p>
     * You can prefix the URL with {@code context://} to make it relative to the
     * context path or use an absolute URL to refer to files outside the
     * frontend directory.
     * <p>
     * For component related style sheet dependencies, you should use the
     * {@link StyleSheet @StyleSheet} annotation.
     *
     * @param url
     *            the URL to load the style sheet from, not <code>null</code>
     * @param loadMode
     *            determines dependency load mode, refer to {@link LoadMode} for
     *            details
     * @return a registration object that can be used to remove the style sheet
     */
    public Registration addStyleSheet(String url, LoadMode loadMode) {
        DependencyList dependencyList = ui.getInternals().getDependencyList();

        // Check if dependency already exists with this URL
        Dependency existing = dependencyList.getDependencyByUrl(url,
                Type.STYLESHEET);
        String dependencyId;

        if (existing != null && existing.getId() != null) {
            // Reuse the existing dependency's ID for duplicates
            dependencyId = existing.getId();
        } else {
            // Create new ID for new dependencies
            dependencyId = UUID.randomUUID().toString();
        }

        Dependency dependency = new Dependency(Type.STYLESHEET, url, loadMode,
                dependencyId);
        dependencyList.add(dependency);

        // Return Registration for removal
        return () -> ui.getInternals().removeStyleSheet(dependencyId);
    }

    /**
     * Adds the given JavaScript to the page and ensures that it is loaded
     * successfully.
     * <p>
     * Relative URLs are interpreted as relative to the static web resources
     * directory. So if the {@code url} value is {@code "some.js"} and
     * {@code src/main/webapp} is used as a location for static web resources
     * (which is the default location) then the file system path for the
     * resource should be {@code src/main/webapp/some.js}.
     * <p>
     * You can prefix the URL with {@code context://} to make it relative to the
     * context path or use an absolute URL to refer to files outside the
     * frontend directory.
     * <p>
     * For component related JavaScript dependencies, you should use the
     * {@link JavaScript @JavaScript} annotation.
     * <p>
     * Is is guaranteed that script will be loaded before the first page load.
     * For more options, refer to {@link #addJavaScript(String, LoadMode)}
     *
     * @param url
     *            the URL to load the JavaScript from, not <code>null</code>
     */
    public void addJavaScript(String url) {
        addJavaScript(url, LoadMode.EAGER);
    }

    /**
     * Adds the given JavaScript to the page and ensures that it is loaded
     * successfully.
     * <p>
     * Relative URLs are interpreted as relative to the static web resources
     * directory. So if the {@code url} value is {@code "some.js"} and
     * {@code src/main/webapp} is used as a location for static web resources
     * (which is the default location) then the file system path for the
     * resource should be {@code src/main/webapp/some.js}.
     * <p>
     * You can prefix the URL with {@code context://} to make it relative to the
     * context path or use an absolute URL to refer to files outside the
     * frontend directory.
     * <p>
     * For component related JavaScript dependencies, you should use the
     * {@link JavaScript @JavaScript} annotation.
     *
     * @param url
     *            the URL to load the JavaScript from, not <code>null</code>
     * @param loadMode
     *            determines dependency load mode, refer to {@link LoadMode} for
     *            details
     */
    public void addJavaScript(String url, LoadMode loadMode) {
        addDependency(new Dependency(Type.JAVASCRIPT, url, loadMode));
    }

    /**
     * Adds the given external JavaScript module to the page and ensures that it
     * is loaded successfully.
     * <p>
     * If the JavaScript modules do not need to be added dynamically, you should
     * use the {@link JsModule @JsModule} annotation instead.
     *
     * @param url
     *            the URL to load the JavaScript module from, not
     *            <code>null</code>
     */
    public void addJsModule(String url) {
        if (UrlUtil.isExternal(url) || url.startsWith("/")) {
            addDependency(new Dependency(Type.JS_MODULE, url, LoadMode.EAGER));
        } else {
            throw new IllegalArgumentException(
                    "url argument must contains either a protocol (eg. starts with \"http://\" or \"//\"), or starts with \"/\".");
        }
    }

    /**
     * Adds a dynamic import using a JavaScript {@code expression} which is
     * supposed to return a JavaScript {@code Promise}.
     * <p>
     * No change will be applied on the client side until resulting
     * {@code Promise} of the {@code expression} is completed. It behaves like
     * other dependencies ({@link #addJavaScript(String)},
     * {@link #addJsModule(String)}, etc.)
     *
     *
     * @param expression
     *            the JavaScript expression which return a Promise
     */
    public void addDynamicImport(String expression) {
        addDependency(new Dependency(Type.DYNAMIC_IMPORT, expression));
    }

    // When updating JavaDocs here, keep in sync with Element.executeJavaScript
    /**
     * Asynchronously runs the given JavaScript expression in the browser.
     * <p>
     * The expression is executed in an <code>async</code> JavaScript method, so
     * you can utilize <code>await</code> syntax when consuming JavaScript API
     * returning a <code>Promise</code>. The returned
     * <code>PendingJavaScriptResult</code> can be used to retrieve the
     * <code>return</code> value from the JavaScript expression. If a
     * <code>Promise</code> is returned in the JavaScript expression,
     * <code>PendingJavaScriptResult</code> will report the resolved value once
     * it becomes available. If no return value handler is registered, the
     * return value will be ignored.
     * <p>
     * The given parameters will be available to the expression as variables
     * named <code>$0</code>, <code>$1</code>, and so on. Supported parameter
     * types are:
     * <ul>
     * <li>{@link String}
     * <li>{@link Integer}
     * <li>{@link Double}
     * <li>{@link Boolean}
     * <li>{@link tools.jackson.databind.node.BaseJsonNode}
     * <li>{@link Element} (will be sent as <code>null</code> if the server-side
     * element instance is not attached when the invocation is sent to the
     * client)
     * <li>{@link Component} (will be sent as the root {@link Element})
     * <li>{@link java.util.List} of supported types (will be sent as a JavaScript
     * array)
     * <li>{@link java.util.Set} of supported types (will be sent as a JavaScript
     * array)
     * <li>{@link java.util.Map} with {@link String} keys and supported value types
     * (will be sent as a JavaScript object). Note that only String keys are
     * allowed.
     * <li>Java Beans with properties of supported types. Beans will be sent as
     * JavaScript objects with bean properties as object properties. Getters will
     * be ignored, only fields are included.
     * </ul>
     * Note that the parameter variables can only be used in contexts where a
     * JavaScript variable can be used. You should for instance do
     * <code>'prefix' + $0</code> instead of <code>'prefix$0'</code> and
     * <code>value[$0]</code> instead of <code>value.$0</code> since JavaScript
     * variables aren't evaluated inside strings or property names.
     *
     * @param expression
     *            the JavaScript expression to invoke
     * @param parameters
     *            parameters to pass to the expression
     * @return a pending result that can be used to get a value returned from
     *         the expression
     */
    public PendingJavaScriptResult executeJs(String expression,
            Serializable... parameters) {
        JavaScriptInvocation invocation = new JavaScriptInvocation(expression,
                parameters);

        PendingJavaScriptInvocation execution = new PendingJavaScriptInvocation(
                ui.getInternals().getStateTree().getRootNode(), invocation);

        ui.getInternals().addJavaScriptInvocation(execution);

        return execution;
    }

    /**
     * Gets a representation of <code>window.history</code> for this page.
     *
     * @return the history representation
     */
    public History getHistory() {
        return history;
    }

    /**
     * Reloads the page in the browser.
     */
    public void reload() {
        executeJs("window.location.reload();");
    }

    /**
     * Adds a new {@link BrowserWindowResizeListener} to this UI. The listener
     * will be notified whenever the browser window within which this UI resides
     * is resized.
     *
     * @param resizeListener
     *            the listener to add, not {@code null}
     * @return a registration object for removing the listener
     *
     * @see BrowserWindowResizeListener#browserWindowResized(BrowserWindowResizeEvent)
     * @see Registration
     */
    public Registration addBrowserWindowResizeListener(
            BrowserWindowResizeListener resizeListener) {
        Objects.requireNonNull(resizeListener);
        if (resizeReceiver == null) {
            // "republish" on the UI element, so can be listened with core APIs
            ui.getElement().executeJs("""
                        const el = this;
                        window.addEventListener('resize', evt => {
                            const event = new Event("window-resize");
                            event.w = document.documentElement.clientWidth;
                            event.h = document.documentElement.clientHeight;
                            el.dispatchEvent(event);
                        });
                    """);
            resizeReceiver = ui.getElement()
                    .addEventListener("window-resize", e -> {
                        var evt = new BrowserWindowResizeEvent(this,
                                e.getEventData().get("event.w").intValue(),
                                e.getEventData().get("event.h").intValue());
                        // Clone list to avoid issues if listener unregisters
                        // itself
                        new ArrayList<>(resizeListeners)
                                .forEach(l -> l.browserWindowResized(evt));
                    }).addEventData("event.w").addEventData("event.h")
                    .debounce(300).allowInert();
        }
        if (resizeListeners == null) {
            resizeListeners = new ArrayList<>(1);
        }
        resizeListeners.add(resizeListener);
        return () -> resizeListeners.remove(resizeListener);
    }

    /**
     * Opens the given url in a new tab.
     *
     * @param url
     *            the URL to open.
     */
    public void open(String url) {
        open(url, "_blank");
    }

    /**
     * Opens the given URL in a window with the given name.
     * <p>
     * The supplied {@code windowName} is used as the target name in a
     * window.open call in the client. This means that special values such as
     * "_blank", "_self", "_top", "_parent" have special meaning. An empty or
     * <code>null</code> window name is also a special case.
     * </p>
     * <p>
     * "", null and "_self" as {@code windowName} all causes the URL to be
     * opened in the current window, replacing any old contents. For
     * downloadable content you should avoid "_self" as "_self" causes the
     * client to skip rendering of any other changes as it considers them
     * irrelevant (the page will be replaced by the response from the URL). This
     * can speed up the opening of a URL, but it might also put the client side
     * into an inconsistent state if the window content is not completely
     * replaced e.g., if the URL is downloaded instead of displayed in the
     * browser.
     * </p>
     * <p>
     * "_blank" as {@code windowName} causes the URL to always be opened in a
     * new window or tab (depends on the browser and browser settings).
     * </p>
     * <p>
     * "_top" and "_parent" as {@code windowName} works as specified by the HTML
     * standard.
     * </p>
     * <p>
     * Any other {@code windowName} will open the URL in a window with that
     * name, either by opening a new window/tab in the browser or by replacing
     * the contents of an existing window with that name.
     * </p>
     *
     * @param url
     *            the URL to open.
     * @param windowName
     *            the name of the window.
     */
    public void open(String url, String windowName) {
        executeJs(
                "if ($1 == '_self') this.stopApplication(); window.open($0, $1)",
                url, windowName);
    }

    /**
     * Navigates this page to the given URI. The contents of this page in the
     * browser is replaced with whatever is returned for the given URI.
     *
     * @param uri
     *            the URI to show
     */
    public void setLocation(String uri) {
        open(uri, "_self");
    }

    /**
     * Navigates this page to the given URI. The contents of this page in the
     * browser is replaced with whatever is returned for the given URI.
     *
     * @param uri
     *            the URI to show
     */
    public void setLocation(URI uri) {
        setLocation(uri.toString());
    }

    private void addDependency(Dependency dependency) {
        assert dependency != null;
        ui.getInternals().getDependencyList().add(dependency);
    }

    /**
     * Callback for receiving extended client-side details.
     */
    @FunctionalInterface
    public interface ExtendedClientDetailsReceiver extends Serializable {

        /**
         * Invoked when the client-side details are available.
         *
         * @param extendedClientDetails
         *            object containing extended client details
         */
        void receiveDetails(ExtendedClientDetails extendedClientDetails);
    }

    /**
     * Obtain extended client side details, such as time screen and time zone
     * information, via callback. If already obtained, the callback is called
     * directly. Otherwise, a client-side roundtrip will be carried out.
     *
     * @param receiver
     *            the callback to which the details are provided
     */
    public void retrieveExtendedClientDetails(
            ExtendedClientDetailsReceiver receiver) {
        final ExtendedClientDetails cachedDetails = ui.getInternals()
                .getExtendedClientDetails();
        if (cachedDetails != null) {
            receiver.receiveDetails(cachedDetails);
            return;
        }
        final String js = "return Vaadin.Flow.getBrowserDetailsParameters();";
        final SerializableConsumer<JsonNode> resultHandler = json -> {
            handleExtendedClientDetailsResponse(json);
            receiver.receiveDetails(
                    ui.getInternals().getExtendedClientDetails());
        };
        final SerializableConsumer<String> errorHandler = err -> {
            throw new RuntimeException("Unable to retrieve extended "
                    + "client details. JS error is '" + err + "'");
        };
        executeJs(js).then(resultHandler, errorHandler);
    }

    private void handleExtendedClientDetailsResponse(JsonNode json) {
        ExtendedClientDetails cachedDetails = ui.getInternals()
                .getExtendedClientDetails();
        if (cachedDetails != null) {
            return;
        }
        if (!(json instanceof ObjectNode)) {
            throw new RuntimeException("Expected a JSON object");
        }
        final ObjectNode jsonObj = (ObjectNode) json;

        // Note that JSON returned is a plain string -> string map, the actual
        // parsing of the fields happens in ExtendedClient's constructor. If a
        // field is missing or the wrong type, pass on null for default.
        final Function<String, String> getStringElseNull = key -> {
            final JsonNode jsValue = jsonObj.get(key);
            if (jsValue != null
                    && JsonNodeType.STRING.equals(jsValue.getNodeType())) {
                return jsValue.asText();
            } else {
                return null;
            }
        };
        ui.getInternals()
                .setExtendedClientDetails(new ExtendedClientDetails(
                        getStringElseNull.apply("v-sw"),
                        getStringElseNull.apply("v-sh"),
                        getStringElseNull.apply("v-ww"),
                        getStringElseNull.apply("v-wh"),
                        getStringElseNull.apply("v-bw"),
                        getStringElseNull.apply("v-bh"),
                        getStringElseNull.apply("v-tzo"),
                        getStringElseNull.apply("v-rtzo"),
                        getStringElseNull.apply("v-dstd"),
                        getStringElseNull.apply("v-dston"),
                        getStringElseNull.apply("v-tzid"),
                        getStringElseNull.apply("v-curdate"),
                        getStringElseNull.apply("v-td"),
                        getStringElseNull.apply("v-pr"),
                        getStringElseNull.apply("v-wn"),
                        getStringElseNull.apply("v-np")));
    }

    /**
     * Retrieves the current url from the browser. The URL is fetched from the
     * browser in another request asynchronously and passed to the callback. The
     * URL is the full URL that the user sees in the browser (including hash #)
     * and works even when client side routing is used or there is a reverse
     * proxy between the client and the server.
     * <p>
     * In case you need more control over the execution you can use
     * {@link #executeJs(String, Serializable...)} by passing
     * {@code return window.location.href}.
     * <p>
     * <em>NOTE: </em> the URL is not escaped, use {@link URL#toURI()} to escape
     * it.
     *
     * @param callback
     *            to be notified when the url is resolved.
     */
    public void fetchCurrentURL(SerializableConsumer<URL> callback) {
        Objects.requireNonNull(callback,
                "Url consumer callback should not be null.");
        final String js = "return window.location.href";
        executeJs(js).then(String.class, urlString -> {
            try {
                callback.accept(new URL(urlString));
            } catch (MalformedURLException e) {
                throw new IllegalStateException(
                        "Error while encoding the URL from client", e);
            }
        });
    }

    /**
     * Retrieves {@code document.dir} of the current UI from the browser and
     * passes it to the {@code callback} parameter. If the {@code document.dir}
     * has not been set explicitly, then {@code Direction.LEFT_TO_RIGHT} will be
     * the fallback value.
     * <p>
     * Note that direction is fetched from the browser in an asynchronous
     * request and passed to the callback.
     * <p>
     * In case you need more control over the execution you can use
     * {@link #executeJs(String, Serializable...)} by passing
     * {@code return document.dir}.
     *
     * @param callback
     *            to be notified when the direction is resolved.
     */
    public void fetchPageDirection(SerializableConsumer<Direction> callback) {
        executeJs("return document.dir").then(String.class, dir -> {
            Direction direction = getDirectionByClientName(dir);
            callback.accept(direction);
        });
    }

    private Direction getDirectionByClientName(String directionClientName) {
        return Arrays.stream(Direction.values())
                .filter(direction -> direction.getClientName()
                        .equals(directionClientName))
                .findFirst().orElse(Direction.LEFT_TO_RIGHT);
    }
}
