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
package com.vaadin.flow.shared.ui;

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.StyleSheet;

/**
 * Determines a way that dependency is loaded. This mode can be specified when
 * importing dependency either via annotations:
 * <ul>
 * <li>{@link JavaScript},
 * <li>{@link StyleSheet},
 * <li>{@link HtmlImport}
 * </ul>
 * or via {@link com.vaadin.flow.component.page.Page} methods:
 * <ul>
 * <li>{@link com.vaadin.flow.component.page.Page#addJavaScript(String, LoadMode)}
 * <li>{@link com.vaadin.flow.component.page.Page#addStyleSheet(String, LoadMode)}
 * <li>{@link com.vaadin.flow.component.page.Page#addHtmlImport(String, LoadMode)}
 * </ul>
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
public enum LoadMode {
    /*
     * Implementation note: the order of the enums is from "more eager" to
     * laziest. This is due to being able to easily using the more eager load
     * mode when duplicate dependencies are loaded with different load modes.
     * (#3677)
     */
    /**
     * Forced the dependency to be inlined in the body of the html page,
     * removing the requirement to have additional roundtrips to fetch the
     * script.
     * <p>
     * It is guaranteed that all {@link LoadMode#INLINE} dependencies are loaded
     * before any {@link LoadMode#LAZY} dependency.
     */
    INLINE,
    /**
     * Forces the dependency being loaded before the initial page load. This
     * mode is suitable for situation when the application cannot start without
     * the dependency being loaded first.
     * <p>
     * It is guaranteed that all {@link LoadMode#EAGER} dependencies are loaded
     * before any {@link LoadMode#LAZY} dependency.
     */
    EAGER,
    /**
     * Allows the dependency being loaded independent of the initial page load.
     * This mode is suitable for situation when the application have no strict
     * requirements on dependency load time.
     * <p>
     * It is guaranteed that all {@link LoadMode#EAGER} dependencies are loaded
     * before any {@link LoadMode#LAZY} dependency.
     */
    LAZY

}
