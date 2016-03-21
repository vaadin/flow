/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.ui;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Optional;

import com.vaadin.annotations.Title;
import com.vaadin.hummingbird.JsonCodec;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.namespace.DependencyListNamespace;
import com.vaadin.hummingbird.router.PageTitleGenerator;
import com.vaadin.hummingbird.router.View;
import com.vaadin.ui.Dependency.Type;
import com.vaadin.ui.FrameworkData.JavaScriptInvocation;

/**
 * Represents the web page open in the browser, containing the UI it is
 * connected to.
 *
 * @author Vaadin
 * @since
 */
public class Page implements Serializable {

    /**
     * Callback method for canceling executable javascript set with
     * {@link Page#executeJavaScript(String, Object...)}.
     */
    @FunctionalInterface
    public interface ExecutionCanceler extends Serializable {
        /**
         * Cancel the javascript execution, if it was not yet sent to the
         * browser for execution.
         *
         * @return <code>true</code> if the execution was be canceled,
         *         <code>false</code>if not
         */
        boolean cancelExecution();
    }

    private final UI ui;
    private final History history;

    private ExecutionCanceler pendingTitleUpdate;

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
     * <p>
     * <code>null</code> will cancel any pending title update not yet sent to
     * browser.
     *
     * @param title
     *            the page title to set
     */
    public void setTitle(String title) {
        if (pendingTitleUpdate != null) {
            pendingTitleUpdate.cancelExecution();
        }

        if (title != null) {
            pendingTitleUpdate = executeJavaScript("document.title = $0",
                    title);

            ui.getFrameworkData().setTitle(title);
        } else {
            pendingTitleUpdate = null;
        }
    }

    /**
     * Gets the optional page title that has been set using:
     * <ul>
     * <li>{@link Page#setTitle(String)}</li>
     * <li>{@link PageTitleGenerator}</li>
     * <li>
     * {@link View#getTitle(com.vaadin.hummingbird.router.LocationChangeEvent)
     * View.getTitle(LocationChangeEvent)}</li>
     * <li>{@link Title @Title} annotation in a {@link View}</li>
     * <li>{@link Title @Title} annotation in a {@link UI}</li>
     * </ul>
     * <b>NOTE</b>: The value returned by this method is the title set on the
     * server side. Changes made directly in the browser are not taken into
     * account.
     *
     * @return optional page title
     */
    public Optional<String> getTitle() {
        return Optional.ofNullable(ui.getFrameworkData().getTitle());
    }

    /**
     * Adds the given style sheet to the page and ensures that it is loaded
     * successfully.
     * <p>
     * Relative URLs are interpreted as relative to the service (servlet) path.
     * You can prefix the URL with {@literal context://} to make it relative to
     * the context path or use an absolute URL to refer to files outside the
     * service (servlet) path.
     *
     * @param url
     *            the URL to load the style sheet from, not <code>null</code>
     */
    public void addStyleSheet(String url) {
        addDependency(new Dependency(Type.STYLESHEET, url));
    }

    /**
     * Adds the given JavaScript to the page and ensures that it is loaded
     * successfully.
     * <p>
     * Relative URLs are interpreted as relative to the service (servlet) path.
     * You can prefix the URL with {@literal context://} to make it relative to
     * the context path or use an absolute URL to refer to files outside the
     * service (servlet) path.
     *
     * @param url
     *            the URL to load the JavaScript from, not <code>null</code>
     */
    public void addJavaScript(String url) {
        addDependency(new Dependency(Type.JAVASCRIPT, url));
    }

    /**
     * Adds the given dependency to the page and ensures that it is loaded
     * successfully.
     *
     * Relative URLs are interpreted as relative to the service (servlet) path.
     * You can prefix the URL with {@literal context://} to make it relative to
     * the context path or use an absolute URL to refer to files outside the
     * service (servlet) path.
     *
     * @param dependency
     *            the dependency to load
     */
    private void addDependency(Dependency dependency) {
        assert dependency != null;

        DependencyListNamespace namespace = ui.getFrameworkData().getStateTree()
                .getRootNode().getNamespace(DependencyListNamespace.class);

        namespace.add(dependency);
    }

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
     *
     * @param expression
     *            the JavaScript expression to invoke
     * @param parameters
     *            parameters to pass to the expression
     * @return a callback for canceling the execution if not yet sent to browser
     */
    public ExecutionCanceler executeJavaScript(String expression,
            Object... parameters) {
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
                Arrays.asList(parameters));
        ui.getFrameworkData().addJavaScriptInvocation(invocation);

        return () -> ui.getFrameworkData().getPendingJavaScriptInvocations()
                .remove(invocation);
    }

    /**
     * Gets a representation of <code>window.history</code> for this page.
     *
     * @return the history representation
     */
    public History getHistory() {
        return history;
    }
}
