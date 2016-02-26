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

import com.vaadin.hummingbird.JsonCodec;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.namespace.DependencyListNamespace;
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

    private UI ui;

    /**
     * Creates a page instance for the given UI.
     *
     * @param ui
     *            the UI that this page instance is connected to
     */
    public Page(UI ui) {
        this.ui = ui;
    }

    /**
     * Adds the given style sheet to the page and ensures that it is loaded
     * successfully.
     * <p>
     * The URL is passed through the translation mechanism before loading, so
     * custom protocols such as "vaadin://" can be used.
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
     * The URL is passed through the translation mechanism before loading, so
     * custom protocols such as "vaadin://" can be used.
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
     */
    public void executeJavaScript(String expression, Object... parameters) {
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

        ui.getFrameworkData().addJavaScriptInvocation(new JavaScriptInvocation(
                expression, Arrays.asList(parameters)));
    }
}
