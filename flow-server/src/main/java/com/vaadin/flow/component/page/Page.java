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

import java.io.Serializable;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.internal.UIInternals.JavaScriptInvocation;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.JsonCodec;
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

    /**
     * Callback method for canceling executable javascript set with
     * {@link Page#executeJavaScript(String, Serializable...)}.
     */
    @FunctionalInterface
    public interface ExecutionCanceler extends Serializable {
        /**
         * Cancel the javascript execution, if it was not yet sent to the
         * browser for execution.
         *
         * @return <code>true</code> if the execution was be canceled,
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
     * Adds the given HTML import to the page and ensures that it is loaded
     * successfully.
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
     */
    public void addHtmlImport(String url) {
        addHtmlImport(url, LoadMode.EAGER);
    }

    /**
     * Adds the given HTML import to the page and ensures that it is loaded
     * successfully.
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
     */
    public void addHtmlImport(String url, LoadMode loadMode) {
        addDependency(new Dependency(Type.HTML_IMPORT, url, loadMode));
    }

    private void addDependency(Dependency dependency) {
        assert dependency != null;
        ui.getInternals().getDependencyList().add(dependency);
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
     */
    public ExecutionCanceler executeJavaScript(String expression,
            Serializable... parameters) {
        /*
         * To ensure attached elements are actually attached, the parameters
         * won't be serialized until the phase the UIDL message is created. To
         * give the user immediate feedback if using a parameter type that can't
         * be serialized, we do a dry run at this point.
         */
        for (Object argument : parameters) {
            // Throws IAE for unsupported types
            JsonCodec.encodeWithTypeInfo(argument);
        }

        JavaScriptInvocation invocation = new JavaScriptInvocation(expression,
                parameters);

        return ui.getInternals().addJavaScriptInvocation(invocation);
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
        executeJavaScript("window.location.reload();");
    }

    /**
     * Adds a new {@link BrowserWindowResizeListener} to this UI. The listener
     * will be notified whenever the browser window within which this UI resides
     * is resized.
     *
     * @param resizeListener
     *            the listener to add
     * @return a registration object for removing the listener
     *
     * @see BrowserWindowResizeListener#browserWindowResized(BrowserWindowResizeEvent)
     * @see Registration
     */
    public Registration addBrowserWindowResizeListener(
            BrowserWindowResizeListener resizeListener) {
        // TODO
        return null;
    }
}
