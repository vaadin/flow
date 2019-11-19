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
package com.vaadin.flow.component.page;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Function;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.component.internal.UIInternals.JavaScriptInvocation;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.shared.ui.Dependency;
import com.vaadin.flow.shared.ui.Dependency.Type;
import com.vaadin.flow.shared.ui.LoadMode;

import elemental.json.JsonObject;
import elemental.json.JsonType;
import elemental.json.JsonValue;

/**
 * Represents the web page open in the browser, containing the UI it is
 * connected to.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class Page implements Serializable {

    @Tag(Tag.DIV)
    private static class ResizeEventReceiver extends Component {

        private int windowResizeListenersSize;

        @ClientCallable
        private void windowResized(int width, int height) {
            if (windowResizeListenersSize != 0) {
                fireEvent(new ResizeEvent(this, width, height));
            }
        }

        private Registration addListener(BrowserWindowResizeListener listener) {
            windowResizeListenersSize++;
            Registration registration = addListener(ResizeEvent.class,
                    event -> listener
                            .browserWindowResized(event.getApiEvent()));
            return new ResizeRegistration(this, registration);
        }

        private void listenerIsUnregistered() {
            windowResizeListenersSize--;
            if (windowResizeListenersSize == 0) {
                // remove JS listener
                getUI().get().getPage().executeJs("$0.resizeRemove()", this);
            }
        }
    }

    private static class ResizeEvent
            extends ComponentEvent<ResizeEventReceiver> {

        private final BrowserWindowResizeEvent apiEvent;

        private ResizeEvent(ResizeEventReceiver source, int width, int height) {
            super(source, true);
            apiEvent = new BrowserWindowResizeEvent(
                    source.getUI().get().getPage(), width, height);
        }

        private BrowserWindowResizeEvent getApiEvent() {
            return apiEvent;
        }
    }

    private static class ResizeRegistration implements Registration {
        private boolean isInvoked;

        private final Registration origin;
        private final ResizeEventReceiver receiver;

        private ResizeRegistration(ResizeEventReceiver receiver,
                Registration origin) {
            this.origin = origin;
            this.receiver = receiver;
        }

        @Override
        public void remove() {
            if (isInvoked) {
                return;
            }
            origin.remove();
            receiver.listenerIsUnregistered();

            isInvoked = true;
        }

    }

    private ResizeEventReceiver resizeReceiver;

    /**
     * Callback method for canceling executable javascript set with
     * {@link Page#executeJs(String, Serializable...)}.
     *
     * @deprecated superseded by {@link PendingJavaScriptResult}
     */
    @FunctionalInterface
    @Deprecated
    public interface ExecutionCanceler extends Serializable {
        /**
         * Cancel the javascript execution, if it was not yet sent to the
         * browser for execution.
         *
         * @return <code>true</code> if the execution was canceled,
         *         <code>false</code> if not
         */
        boolean cancelExecution();
    }

    private final UI ui;
    private final History history;

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
     * Relative URLs are interpreted as relative to the configured
     * {@code frontend} directory location. You can prefix the URL with
     * {@code context://} to make it relative to the context path or use an
     * absolute URL to refer to files outside the frontend directory.
     * <p>
     * For component related style sheet dependencies, you should use the
     * {@link StyleSheet @StyleSheet} annotation.
     * <p>
     * Is is guaranteed that style sheet will be loaded before the first page
     * load. For more options, refer to {@link #addStyleSheet(String, LoadMode)}
     *
     * @param url
     *            the URL to load the style sheet from, not <code>null</code>
     */
    public void addStyleSheet(String url) {
        addStyleSheet(url, LoadMode.EAGER);
    }

    /**
     * Adds the given style sheet to the page and ensures that it is loaded
     * successfully.
     * <p>
     * Relative URLs are interpreted as relative to the configured
     * {@code frontend} directory location. You can prefix the URL with
     * {@code context://} to make it relative to the context path or use an
     * absolute URL to refer to files outside the frontend directory.
     * <p>
     * For component related style sheet dependencies, you should use the
     * {@link StyleSheet @StyleSheet} annotation.
     *
     * @param url
     *            the URL to load the style sheet from, not <code>null</code>
     * @param loadMode
     *            determines dependency load mode, refer to {@link LoadMode} for
     *            details
     */
    public void addStyleSheet(String url, LoadMode loadMode) {
        addDependency(new Dependency(Type.STYLESHEET, url, loadMode));
    }

    /**
     * Adds the given JavaScript to the page and ensures that it is loaded
     * successfully.
     * <p>
     * Relative URLs are interpreted as relative to the configured
     * {@code frontend} directory location. You can prefix the URL with
     * {@code context://} to make it relative to the context path or use an
     * absolute URL to refer to files outside the frontend directory.
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
     * Relative URLs are interpreted as relative to the configured
     * {@code frontend} directory location. You can prefix the URL with
     * {@code context://} to make it relative to the context path or use an
     * absolute URL to refer to files outside the frontend directory.
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
     * If the JavaScript modules are local or do not need to be added
     * dynamically, you should use the {@link JsModule @JsModule} annotation
     * instead.
     *
     * @param url
     *            the URL to load the JavaScript module from, not
     *            <code>null</code>
     */
    public void addJsModule(String url) {
        addJsModule(url, LoadMode.EAGER);
    }

    /**
     * Adds the given external JavaScript module to the page and ensures that it
     * is loaded successfully.
     * <p>
     * If the JavaScript modules are local or do not need to be added
     * dynamically, you should use the {@link JsModule @JsModule} annotation
     * instead.
     *
     * @param url
     *            the URL to load the JavaScript module from, not
     *            <code>null</code>
     * @param loadMode
     *            determines dependency load mode, refer to {@link LoadMode} for
     *            details
     * @deprecated {@code LoadMode} is not functional with external JavaScript
     *             modules, as those are loaded as deferred due to
     *             {@code type=module} in {@code scrip} tag. Use
     *             {@link #addJsModule(String)} instead.
     */
    @Deprecated
    public void addJsModule(String url, LoadMode loadMode) {
        addDependency(new Dependency(Type.JS_MODULE, url, loadMode));
    }

    /**
     * In compatibility mode (or Flow 1.x), adds the given HTML import to the
     * page and ensures that it is loaded successfully. In normal mode (Flow 2.x
     * with npm support), throws an {@code UnsupportedOperationException}.
     * <p>
     * Relative URLs are interpreted as relative to the configured
     * {@code frontend} directory location. You can prefix the URL with
     * {@code context://} to make it relative to the context path or use an
     * absolute URL to refer to files outside the frontend directory.
     * <p>
     * Is is guaranteed that html import will be loaded before the first page
     * load. For more options, refer to {@link #addHtmlImport(String, LoadMode)}
     *
     * @param url
     *            the URL to load the HTML import from, not <code>null</code>
     * @throws java.lang.UnsupportedOperationException
     *             if called outside of compatibility mode.
     */
    public void addHtmlImport(String url) {
        addHtmlImport(url, LoadMode.EAGER);
    }

    /**
     * In compatibility mode (or Flow 1.x), adds the given HTML import to the
     * page and ensures that it is loaded successfully. In normal mode (Flow 2.x
     * with npm support), throws an {@code UnsupportedOperationException}.
     * <p>
     * Relative URLs are interpreted as relative to the configured
     * {@code frontend} directory location. You can prefix the URL with
     * {@code context://} to make it relative to the context path or use an
     * absolute URL to refer to files outside the frontend directory.
     *
     * @param url
     *            the URL to load the HTML import from, not <code>null</code>
     * @param loadMode
     *            determines dependency load mode, refer to {@link LoadMode} for
     *            details
     * @throws java.lang.UnsupportedOperationException
     *             if called outside of compatibility mode.
     */
    public void addHtmlImport(String url, LoadMode loadMode) {
        if (ui.getSession().getConfiguration().isCompatibilityMode()) {
            addDependency(new Dependency(Type.HTML_IMPORT, url, loadMode));
        } else {
            throw new UnsupportedOperationException("Adding html imports is "
                    + "only supported in compatibility mode. Either run the "
                    + "application in compatibility mode or add the "
                    + "dependency via annotation (@NpmPackage and @JsModule).");
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
     * @see #addHtmlImport(String)
     * @param expression
     *            the JavaScript expression which return a Promise
     */
    public void addDynamicImport(String expression) {
        addDependency(new Dependency(Type.DYNAMIC_IMPORT, expression));
    }

    // When updating JavaDocs here, keep in sync with Element.executeJavaScript
    /**
     * Asynchronously runs the given JavaScript expression in the browser. The
     * given parameters will be available to the expression as variables named
     * <code>$0</code>, <code>$1</code>, and so on. Supported parameter types
     * are:
     * <ul>
     * <li>{@link String}
     * <li>{@link Integer}
     * <li>{@link Double}
     * <li>{@link Boolean}
     * <li>{@link JsonValue}
     * <li>{@link Element} (will be sent as <code>null</code> if the server-side
     * element instance is not attached when the invocation is sent to the
     * client)
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
     * @return a callback for canceling the execution if not yet sent to browser
     * @deprecated Use {@link #executeJs(String,Serializable...)} instead since
     *             it also allows getting return value back.
     */
    @Deprecated
    public ExecutionCanceler executeJavaScript(String expression,
            Serializable... parameters) {
        return executeJs(expression, parameters);
    }

    // When updating JavaDocs here, keep in sync with Element.executeJavaScript
    /**
     * Asynchronously runs the given JavaScript expression in the browser.
     * <p>
     * The returned <code>PendingJavaScriptResult</code> can be used to retrieve
     * any <code>return</code> value from the JavaScript expression. If no
     * return value handler is registered, the return value will be ignored.
     * <p>
     * The given parameters will be available to the expression as variables
     * named <code>$0</code>, <code>$1</code>, and so on. Supported parameter
     * types are:
     * <ul>
     * <li>{@link String}
     * <li>{@link Integer}
     * <li>{@link Double}
     * <li>{@link Boolean}
     * <li>{@link JsonValue}
     * <li>{@link Element} (will be sent as <code>null</code> if the server-side
     * element instance is not attached when the invocation is sent to the
     * client)
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
            // lazy creation which is done only one time since there is no way
            // to remove virtual children
            resizeReceiver = new ResizeEventReceiver();
            ui.getElement().appendVirtualChild(resizeReceiver.getElement());
        }
        if (resizeReceiver.windowResizeListenersSize == 0) {
            // JS resize listener may be completely disabled if there are not
            // listeners
            executeJs(LazyJsLoader.WINDOW_LISTENER_JS, resizeReceiver);
        }
        return resizeReceiver.addListener(resizeListener);
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
        executeJavaScript("window.open($0, $1)", url, windowName);
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

    private static class LazyJsLoader implements Serializable {

        private static final String JS_FILE_NAME = "windowResizeListener.js";

        private static final String WINDOW_LISTENER_JS = readJS();

        private static String readJS() {
            try (InputStream stream = Page.class
                    .getResourceAsStream(JS_FILE_NAME);
                    BufferedReader bf = new BufferedReader(
                            new InputStreamReader(stream,
                                    StandardCharsets.UTF_8))) {
                StringBuilder builder = new StringBuilder();
                bf.lines().forEach(builder::append);
                return builder.toString();
            } catch (IOException e) {
                throw new RuntimeException(
                        "Couldn't read window resize listener JavaScript file "
                                + JS_FILE_NAME + ". The package is broken",
                        e);
            }
        }
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
        final SerializableConsumer<JsonValue> resultHandler = json -> {
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

    private void handleExtendedClientDetailsResponse(JsonValue json) {
        if (!(json instanceof JsonObject)) {
            throw new RuntimeException("Expected a JSON object");
        }
        final JsonObject jsonObj = (JsonObject) json;

        // Note that JSON returned is a plain string -> string map, the actual
        // parsing of the fields happens in ExtendedClient's constructor. If a
        // field is missing or the wrong type, pass on null for default.
        final Function<String, String> getStringElseNull = key -> {
            final JsonValue jsValue = jsonObj.get(key);
            if (jsValue != null && JsonType.STRING.equals(jsValue.getType())) {
                return jsValue.asString();
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
                        getStringElseNull.apply("v-wn")));
    }

}
