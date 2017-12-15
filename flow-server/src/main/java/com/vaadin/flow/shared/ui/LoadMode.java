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
package com.vaadin.flow.shared.ui;

import com.vaadin.ui.common.HtmlImport;
import com.vaadin.ui.common.JavaScript;
import com.vaadin.ui.common.StyleSheet;

/**
 * Class that determines a way that dependency is loaded. This mode can be
 * specified when importing dependency either via annotations:
 * <ul>
 * <li>{@link JavaScript},
 * <li>{@link StyleSheet},
 * <li>{@link HtmlImport}
 * </ul>
 * or via {@link com.vaadin.ui.Page} methods:
 * <ul>
 * <li>{@link com.vaadin.ui.Page#addJavaScript(String, LoadMode)}
 * <li>{@link com.vaadin.ui.Page#addStyleSheet(String, LoadMode)}
 * <li>{@link com.vaadin.ui.Page#addHtmlImport(String, LoadMode)}
 * </ul>
 *
 * @author Vaadin Ltd.
 */
public enum LoadMode {
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
    LAZY,

    /**
     * Forced the dependency to be inlined in the body of the html page,
     * removing the requirement to have additional roundtrips to fetch the script.
     * <p>
     * It is guaranteed that all {@link LoadMode#INLINE} dependencies are loaded
     * before any {@link LoadMode#LAZY} dependency.
     */
    INLINE
}
