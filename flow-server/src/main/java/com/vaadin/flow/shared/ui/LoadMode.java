/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.shared.ui;

import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.StyleSheet;

/**
 * Determines a way that dependency is loaded. This mode can be specified when
 * importing dependency either via annotations:
 * <ul>
 * <li>{@link JavaScript},
 * <li>{@link StyleSheet}
 * </ul>
 * or via {@link com.vaadin.flow.component.page.Page} methods:
 * <ul>
 * <li>{@link com.vaadin.flow.component.page.Page#addJavaScript(String, LoadMode)}
 * <li>{@link com.vaadin.flow.component.page.Page#addStyleSheet(String, LoadMode)}
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
