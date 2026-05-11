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
package com.vaadin.flow.component.page;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Direction;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.internal.DependencyList;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.component.internal.UIInternals.JavaScriptInvocation;
import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.internal.UrlUtil;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.shared.ui.Dependency;
import com.vaadin.flow.shared.ui.Dependency.Type;
import com.vaadin.flow.shared.ui.LoadMode;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * Represents the web page open in the browser, containing the UI it is
 * connected to.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class Page implements Serializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(Page.class);

    private final UI ui;
    private final History history;
    private DomListenerRegistration resizeReceiver;
    private ArrayList<BrowserWindowResizeListener> resizeListeners;
    private ValueSignal<WindowSize> windowSizeSignal;
    private final ValueSignal<PageVisibility> pageVisibilitySignal = new ValueSignal<>(
            PageVisibility.UNKNOWN);
    private final Signal<PageVisibility> pageVisibilityReadOnly = pageVisibilitySignal
            .asReadonly();
    private final ValueSignal<FullscreenState> fullscreenSignal = new ValueSignal<>(
            FullscreenState.UNKNOWN);
    private final Signal<FullscreenState> fullscreenSignalReadOnly = fullscreenSignal
            .asReadonly();
    private @Nullable FullscreenSession currentFullscreenSession;
    private boolean expectProgrammaticFullscreenExit;

    /**
     * Creates a page instance for the given UI.
     *
     * @param ui
     *            the UI that this page instance is connected to
     */
    public Page(UI ui) {
        this.ui = ui;
        history = new History(ui);
        ui.getElement()
                .addEventListener("vaadin-page-visibility-change",
                        e -> setPageVisibility(e.getEventDetail(String.class)))
                .addEventDetail().debounce(100).allowInert();
        ui.getElement()
                .addEventListener("vaadin-fullscreen-change",
                        e -> setFullscreenState(e.getEventDetail(String.class)))
                .addEventDetail().allowInert();
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
     * Sets the color scheme for the page.
     * <p>
     * The color scheme is applied via both a theme attribute and the
     * color-scheme CSS property on the html element. The theme attribute allows
     * CSS to target different color schemes (e.g.,
     * {@code html[theme~="dark"]}), while the color-scheme property ensures
     * browser UI adaptation works even for custom themes that don't define
     * their own color-scheme CSS rules.
     *
     * @param colorScheme
     *            the color scheme to set (e.g., ColorScheme.Value.DARK,
     *            ColorScheme.Value.LIGHT), or {@code null} to reset to NORMAL
     */
    public void setColorScheme(ColorScheme.Value colorScheme) {
        if (colorScheme == null || colorScheme == ColorScheme.Value.NORMAL) {
            executeJs("""
                    document.documentElement.removeAttribute('theme');
                    document.documentElement.style.colorScheme = '';
                    """);
            getExtendedClientDetails().setColorScheme(ColorScheme.Value.NORMAL);
        } else {
            executeJs("""
                    document.documentElement.setAttribute('theme', $0);
                    document.documentElement.style.colorScheme = $1;
                    """, colorScheme.getThemeValue(), colorScheme.getValue());
            getExtendedClientDetails().setColorScheme(colorScheme);
        }
    }

    /**
     * Gets the color scheme for the page.
     * <p>
     * Note that this method returns the server-side cached value and will not
     * detect color scheme changes made directly via JavaScript or browser
     * developer tools.
     *
     * @return the color scheme value, never {@code null}
     */
    public ColorScheme.Value getColorScheme() {
        return getExtendedClientDetails().getColorScheme();
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
     * Return values from JavaScript can be automatically deserialized into Java
     * objects. All types supported by Jackson for JSON deserialization are
     * supported as return values, including custom bean classes.
     * <p>
     * The given parameters will be available to the expression as variables
     * named <code>$0</code>, <code>$1</code>, and so on. All types supported by
     * Jackson for JSON serialization are supported as parameters. Special
     * cases:
     * <ul>
     * <li>{@link Element} (will be sent as a DOM element reference to the
     * browser if the server-side element instance is attached when the
     * invocation is sent to the client, or as <code>null</code> if not
     * attached)
     * <li>{@link tools.jackson.databind.node.BaseJsonNode} (sent as-is without
     * additional wrapping)
     * <li>{@link JsFunction} (manifested as a callable JavaScript function with
     * its captured parameters pre-bound)
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
            Object... parameters) {
        JavaScriptInvocation invocation = new JavaScriptInvocation(expression,
                parameters);

        PendingJavaScriptInvocation execution = new PendingJavaScriptInvocation(
                ui.getInternals().getStateTree().getRootNode(), invocation);

        ui.getInternals().addJavaScriptInvocation(execution);

        return execution;
    }

    /**
     * Executes the given JavaScript expression in the browser.
     *
     * @deprecated Use {@link #executeJs(String, Object...)} instead. This
     *             method exists only for binary compatibility.
     * @param expression
     *            the JavaScript expression to execute
     * @param parameters
     *            parameters to pass to the expression
     * @return a pending result that can be used to get a value returned from
     *         the expression
     */
    @Deprecated
    public PendingJavaScriptResult executeJs(String expression,
            Serializable[] parameters) {
        return executeJs(expression, (Object[]) parameters);
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
     * Returns a signal that tracks the current browser window size.
     * <p>
     * The signal is lazily initialized on first access and automatically
     * updates when the browser window is resized. The initial value uses
     * dimensions from {@link ExtendedClientDetails} if available, otherwise
     * defaults to 0x0.
     * <p>
     * The returned signal is read-only.
     *
     * @return a read-only signal with the current window size
     */
    public Signal<WindowSize> windowSizeSignal() {
        ensureWindowSizeSignal();
        return windowSizeSignal.asReadonly();
    }

    /**
     * Sets the window size in the signal. Used by {@link ExtendedClientDetails}
     * to feed bootstrap data into the signal.
     *
     * @param width
     *            the window inner width
     * @param height
     *            the window inner height
     */
    void setWindowSize(int width, int height) {
        ensureWindowSizeSignal();
        windowSizeSignal
                .set(new WindowSize(Math.max(width, 0), Math.max(height, 0)));
    }

    private void ensureWindowSizeSignal() {
        if (windowSizeSignal == null) {
            windowSizeSignal = new ValueSignal<>(new WindowSize(0, 0));
        }
        ensureResizeListener();
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
        ensureResizeListener();
        if (resizeListeners == null) {
            resizeListeners = new ArrayList<>(1);
        }
        resizeListeners.add(resizeListener);
        return () -> resizeListeners.remove(resizeListener);
    }

    private void ensureResizeListener() {
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
                        int w = e.getEventData().get("event.w").intValue();
                        int h = e.getEventData().get("event.h").intValue();
                        if (windowSizeSignal != null) {
                            windowSizeSignal.set(new WindowSize(w, h));
                        }
                        if (resizeListeners != null) {
                            var evt = new BrowserWindowResizeEvent(this, w, h);
                            new ArrayList<>(resizeListeners)
                                    .forEach(l -> l.browserWindowResized(evt));
                        }
                    }).addEventData("event.w").addEventData("event.h")
                    .debounce(300).allowInert();
        }
    }

    /**
     * Returns a read-only signal that tracks the browser tab's visibility and
     * focus state.
     * <p>
     * The signal distinguishes between {@link PageVisibility#VISIBLE VISIBLE}
     * (tab shown and focused), {@link PageVisibility#VISIBLE_NOT_FOCUSED
     * VISIBLE_NOT_FOCUSED} (tab shown but behind another window or another
     * application has focus), {@link PageVisibility#HIDDEN HIDDEN} (tab in
     * background, minimized, or on a different virtual desktop), and
     * {@link PageVisibility#UNKNOWN UNKNOWN} (the initial value, replaced with
     * a real one before any user code observes the signal).
     * <p>
     * The signal value is seeded from the initial client bootstrap, so user
     * code always sees a real value. Subscribe with
     * {@code Signal.effect(owner, ...)} to react to changes; call
     * {@code pageVisibilitySignal().peek()} for a snapshot outside a reactive
     * context, and {@code .get()} inside one.
     * <p>
     * <b>Reliability caveats.</b> The value is best-effort:
     * <ul>
     * <li>Firefox defers the {@code visibilitychange} event while the window is
     * blurred, so transitions from {@link PageVisibility#VISIBLE VISIBLE} to
     * {@link PageVisibility#HIDDEN HIDDEN} may take up to half a second longer
     * than on Chromium or Safari.</li>
     * <li>The {@link PageVisibility#VISIBLE_NOT_FOCUSED VISIBLE_NOT_FOCUSED}
     * distinction relies on {@code document.hasFocus()}, which is accurate in
     * all supported browsers but depends on the OS reporting focus changes
     * promptly — some window-manager configurations can delay it briefly.</li>
     * <li>Rapid focus/blur bursts are intentionally coalesced
     * ({@code debounce(100)}) so the signal settles once the sequence ends
     * instead of firing on each intermediate state.</li>
     * </ul>
     *
     * @return the read-only visibility signal
     */
    public Signal<PageVisibility> pageVisibilitySignal() {
        return pageVisibilityReadOnly;
    }

    /**
     * Sets the page visibility from a raw client-side value (e.g. from the
     * bootstrap parameters or from a {@code vaadin-page-visibility-change} DOM
     * event). {@code null} and unknown values are ignored — the latter is
     * logged at debug level so a forward-compatible client value does not
     * silently disappear.
     *
     * @param value
     *            the raw value, or {@code null}
     */
    void setPageVisibility(String value) {
        if (value == null) {
            return;
        }
        try {
            pageVisibilitySignal.set(PageVisibility.valueOf(value));
        } catch (IllegalArgumentException e) {
            LOGGER.debug("Unknown page visibility value from client: {}",
                    value);
        }
    }

    /**
     * Returns a read-only signal that tracks the browser's fullscreen state.
     * <p>
     * The signal distinguishes between {@link FullscreenState#FULLSCREEN
     * FULLSCREEN} (the page is currently in fullscreen),
     * {@link FullscreenState#NOT_FULLSCREEN NOT_FULLSCREEN} (fullscreen is
     * supported but the page is not in it), {@link FullscreenState#UNSUPPORTED
     * UNSUPPORTED} (the browser does not support fullscreen or the document is
     * not permitted to enter it), and {@link FullscreenState#UNKNOWN UNKNOWN}
     * (the initial value, replaced with a real one before any user code
     * observes the signal).
     * <p>
     * The signal value is seeded from the initial client bootstrap, so user
     * code always sees a real value. Subscribe with
     * {@code Signal.effect(owner, ...)} to react to changes; call
     * {@code fullscreenSignal().peek()} for a snapshot outside a reactive
     * context, and {@code .get()} inside one. Use {@link #requestFullscreen()},
     * {@link Component#requestFullscreen()}, or {@link #exitFullscreen()} to
     * change the state.
     * <p>
     * Note that browsers require transient user activation (e.g. a button
     * click) to enter fullscreen mode, so the signal will not transition to
     * {@link FullscreenState#FULLSCREEN FULLSCREEN} in response to a request
     * from a server push or view constructor.
     *
     * <h2>Example: bind a toggle button</h2>
     *
     * <pre>
     * Button toggle = new Button();
     * Page page = UI.getCurrent().getPage();
     * Signal&lt;FullscreenState&gt; state = page.fullscreenSignal();
     *
     * toggle.bindText(state.map(s -&gt; switch (s) {
     * case FULLSCREEN -&gt; "Exit fullscreen";
     * case NOT_FULLSCREEN -&gt; "Enter fullscreen";
     * default -&gt; "Fullscreen unavailable";
     * }));
     * toggle.bindEnabled(state.map(s -&gt; s == FullscreenState.FULLSCREEN
     *         || s == FullscreenState.NOT_FULLSCREEN));
     * toggle.addClickListener(e -&gt; {
     *     if (state.peek() == FullscreenState.FULLSCREEN) {
     *         page.exitFullscreen();
     *     } else {
     *         page.requestFullscreen();
     *     }
     * });
     * </pre>
     *
     * @return the read-only fullscreen signal
     */
    public Signal<FullscreenState> fullscreenSignal() {
        return fullscreenSignalReadOnly;
    }

    /**
     * Requests that the browser display the entire page in fullscreen mode.
     * <p>
     * This calls {@code document.documentElement.requestFullscreen()} on the
     * browser. Themes and overlay components (such as Notification and ComboBox
     * popups) work correctly in this mode. Use
     * {@link Component#requestFullscreen()} to fullscreen a single component
     * within the page.
     * <p>
     * Note that browsers require transient user activation (e.g. a button
     * click) to enter fullscreen mode. Calling this method from a server push
     * or view constructor will not work — the returned session will end up in
     * {@link FullscreenSessionState#REJECTED REJECTED}. The fullscreen state
     * can be observed via {@link #fullscreenSignal()}; calls made while the
     * state is {@link FullscreenState#UNSUPPORTED UNSUPPORTED} resolve to
     * {@link FullscreenSessionState#REJECTED REJECTED} immediately.
     * <p>
     * If a session is already active when this method is called, it is moved to
     * {@link FullscreenSessionState#EXITED_BY_CODE EXITED_BY_CODE} before the
     * new session is returned.
     *
     * <h2>Example: react to lifecycle transitions</h2>
     *
     * <pre>
     * FullscreenSession session = ui.getPage().requestFullscreen();
     * Signal.effect(this, () -&gt; {
     *     switch (session.stateSignal().get()) {
     *     case ACTIVE -&gt; statusLabel.setText("Now fullscreen");
     *     case REJECTED -&gt; statusLabel.setText("Could not enter fullscreen: "
     *             + session.error().orElse("unknown"));
     *     case EXITED_BY_USER -&gt; statusLabel.setText("You exited fullscreen");
     *     case EXITED_BY_CODE -&gt; statusLabel.setText("Fullscreen ended");
     *     default -&gt; {
     *     }
     *     }
     * });
     * </pre>
     *
     * @return a session handle for the request, never {@code null}
     * @see Component#requestFullscreen()
     * @see #exitFullscreen()
     * @see #fullscreenSignal()
     * @see <a href=
     *      "https://developer.mozilla.org/en-US/docs/Web/API/Fullscreen_API">MDN
     *      Fullscreen API</a>
     */
    public FullscreenSession requestFullscreen() {
        return startFullscreenSession(null,
                "return window.Vaadin.Flow.fullscreen.requestPageFullscreen()");
    }

    /**
     * Requests that the browser display the given component in fullscreen mode.
     * The component is moved into a wrapper element and the rest of the view is
     * hidden so Vaadin theming and overlay components keep working; see
     * {@link Component#requestFullscreen()} for the rationale.
     * <p>
     * This is the same operation as calling
     * {@link Component#requestFullscreen()} directly; the static-style entry
     * point is offered for code that already holds a {@link Page} reference and
     * would rather not reach back through the component to start a fullscreen
     * session.
     *
     * @param component
     *            the component to fullscreen, not {@code null}
     * @return a session handle for the request, never {@code null}
     * @throws NullPointerException
     *             if {@code component} is {@code null}
     * @throws IllegalStateException
     *             if the component is not attached to this page's UI
     * @see Component#requestFullscreen()
     * @see #requestFullscreen()
     * @see #exitFullscreen()
     */
    public FullscreenSession requestFullscreen(Component component) {
        Objects.requireNonNull(component, "component must not be null");
        UI componentUi = component.getUI()
                .orElseThrow(() -> new IllegalStateException(
                        "Component must be attached to the UI to request fullscreen"));
        if (componentUi != ui) {
            throw new IllegalStateException(
                    "Component is attached to a different UI than this page");
        }
        return startFullscreenSession(component,
                "return window.Vaadin.Flow.fullscreen.requestComponentFullscreen($0, $1)",
                component.getElement(), ui.getInternals().getWrapperElement());
    }

    /**
     * Exits fullscreen mode if the page is currently in fullscreen, otherwise a
     * no-op.
     * <p>
     * If a component was previously fullscreened via
     * {@link Component#requestFullscreen()}, it is automatically restored to
     * its original position in the DOM. The active session, if any, transitions
     * to {@link FullscreenSessionState#EXITED_BY_CODE EXITED_BY_CODE}.
     *
     * @see #requestFullscreen()
     * @see Component#requestFullscreen()
     */
    public void exitFullscreen() {
        expectProgrammaticFullscreenExit = true;
        executeJs("window.Vaadin.Flow.fullscreen.exitFullscreen()");
    }

    /**
     * Starts a new fullscreen session by running the given JavaScript
     * expression on the client and wiring its outcome (a {@code Promise<{ ok,
     * error? }>}) to the session's state signal.
     * <p>
     * If a previous session is still active or pending it is transitioned to
     * {@link FullscreenSessionState#EXITED_BY_CODE EXITED_BY_CODE} so that only
     * one session is observable at a time.
     */
    FullscreenSession startFullscreenSession(@Nullable Component owner,
            String requestExpression, Object... parameters) {
        if (currentFullscreenSession != null
                && !currentFullscreenSession.isTerminal()) {
            currentFullscreenSession.setExited(true);
        }
        FullscreenSession session = new FullscreenSession(this, owner);
        currentFullscreenSession = session;
        executeJs(requestExpression, parameters).then(JsonNode.class,
                result -> handleRequestOutcome(session, result),
                errorMessage -> handleRequestError(session, errorMessage));
        return session;
    }

    private void handleRequestOutcome(FullscreenSession session,
            JsonNode result) {
        if (session.isTerminal()) {
            return;
        }
        JsonNode okNode = result.get("ok");
        if (okNode != null && okNode.asBoolean()) {
            session.setActive();
            return;
        }
        JsonNode errorNode = result.get("error");
        String errorText = errorNode == null || errorNode.isNull() ? null
                : errorNode.asString();
        LOGGER.warn(
                "Fullscreen request was rejected by the browser{}. "
                        + "Most likely the call was not made from a "
                        + "user gesture (transient user activation).",
                errorText == null ? "" : ": " + errorText);
        session.setRejected(errorText);
    }

    private void handleRequestError(FullscreenSession session,
            String errorMessage) {
        if (session.isTerminal()) {
            return;
        }
        LOGGER.warn("Fullscreen request failed on the client: {}",
                errorMessage);
        session.setRejected(errorMessage);
    }

    /**
     * Sets the fullscreen state from a raw client-side value (e.g. from the
     * bootstrap parameters or from a {@code vaadin-fullscreen-change} DOM
     * event). {@code null} and unknown values are ignored — the latter is
     * logged at debug level so a forward-compatible client value does not
     * silently disappear.
     *
     * @param value
     *            the raw value, or {@code null}
     */
    void setFullscreenState(String value) {
        if (value == null) {
            return;
        }
        try {
            applyFullscreenState(FullscreenState.valueOf(value));
        } catch (IllegalArgumentException e) {
            LOGGER.debug("Unknown fullscreen state value from client: {}",
                    value);
        }
    }

    /**
     * Drives {@link #fullscreenSignal()} from test code without going through
     * the client bridge. Mirrors the effect of a
     * {@code vaadin-fullscreen-change} DOM event, including transitioning the
     * active {@link FullscreenSession} to its appropriate terminal state.
     * <p>
     * Intended for unit tests. Production code should not need to call this.
     *
     * @param newState
     *            the state to set, not {@code null}
     */
    public void simulateFullscreenChange(FullscreenState newState) {
        Objects.requireNonNull(newState, "newState must not be null");
        applyFullscreenState(newState);
    }

    private void applyFullscreenState(FullscreenState newState) {
        FullscreenState previous = fullscreenSignal.peek();
        fullscreenSignal.set(newState);
        if (previous == FullscreenState.FULLSCREEN
                && newState != FullscreenState.FULLSCREEN
                && currentFullscreenSession != null
                && !currentFullscreenSession.isTerminal()) {
            boolean programmatic = expectProgrammaticFullscreenExit;
            currentFullscreenSession.setExited(programmatic);
        }
        expectProgrammaticFullscreenExit = false;
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
     * <p>
     * "_blank" as {@code windowName} causes the URL to always be opened in a
     * new window or tab (depends on the browser and browser settings).
     * <p>
     * "_top" and "_parent" as {@code windowName} works as specified by the HTML
     * standard.
     * <p>
     * Any other {@code windowName} will open the URL in a window with that
     * name, either by opening a new window/tab in the browser or by replacing
     * the contents of an existing window with that name.
     *
     * @param url
     *            the URL to open.
     * @param windowName
     *            the name of the window.
     */
    public void open(String url, String windowName) {
        // The vaadin-redirect-pending event might be useful to block other
        // client side
        // reload/redirection triggered by other components, for example Vite.
        executeJs(
                "window.dispatchEvent(new CustomEvent('vaadin-redirect-pending', {detail: {url: $0}})); "
                        + "if ($1 == '_self') this.stopApplication(); window.open($0, $1)",
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
     * Gets the extended client details, such as screen resolution and time zone
     * information.
     * <p>
     * Browser details are automatically collected and sent during UI
     * initialization, making them immediately available. In normal operation,
     * this method returns complete details right after the UI is created.
     * <p>
     * If details are not yet available, this method returns a placeholder
     * instance with default values (dimensions set to -1). If you need to fetch
     * the actual values in such cases, use
     * {@link ExtendedClientDetails#refresh(SerializableConsumer)} to explicitly
     * retrieve updated values from the browser.
     * <p>
     * To refresh the cached values with updated data from the browser at any
     * time, use {@link ExtendedClientDetails#refresh(SerializableConsumer)}.
     *
     * @return the extended client details (never {@code null})
     */
    public ExtendedClientDetails getExtendedClientDetails() {
        return ui.getInternals().getExtendedClientDetails();
    }

    /**
     * Obtain extended client side details, such as time screen and time zone
     * information, via callback. If already obtained, the callback is called
     * directly. Otherwise, a client-side roundtrip will be carried out.
     *
     * @param receiver
     *            the callback to which the details are provided
     * @deprecated Use {@link #getExtendedClientDetails()} to get the cached
     *             details, or
     *             {@link ExtendedClientDetails#refresh(SerializableConsumer)}
     *             to refresh the cached values.
     */
    @Deprecated
    public void retrieveExtendedClientDetails(
            ExtendedClientDetailsReceiver receiver) {
        ExtendedClientDetails details = getExtendedClientDetails();
        if (details.getScreenWidth() != -1) {
            // Already fetched and complete, call receiver immediately
            receiver.receiveDetails(details);
        } else {
            // Placeholder with default values, trigger refresh
            details.refresh(receiver::receiveDetails);
        }
    }

    /**
     * Retrieves the current url from the browser. The URL is fetched from the
     * browser in another request asynchronously and passed to the callback. The
     * URL is the full URL that the user sees in the browser (including hash #)
     * and works even when client side routing is used or there is a reverse
     * proxy between the client and the server.
     * <p>
     * In case you need more control over the execution you can use
     * {@link #executeJs(String, Object...)} by passing
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
     * {@link #executeJs(String, Object...)} by passing
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
