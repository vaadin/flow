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
package com.vaadin.flow.component;

import java.util.Arrays;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ThemeList;

/**
 * Represents {@link Component} which has theme attribute.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public interface HasTheme extends HasElement {

    /**
     * Adds a theme name to this component.
     *
     * @param themeName
     *            the theme name to add, not <code>null</code>
     */
    default void addThemeName(String themeName) {
        getThemeNames().add(themeName);
    }

    /**
     * Removes a theme name from this component.
     *
     * @param themeName
     *            the theme name to remove, not <code>null</code>
     * @return <code>true</code> if the theme name was removed,
     *         <code>false</code> if the theme list didn't contain the theme
     *         name
     */
    default boolean removeThemeName(String themeName) {
        return getThemeNames().remove(themeName);
    }

    /**
     * Sets the theme names of this component. This method overwrites any
     * previous set theme names.
     *
     * @param themeName
     *            a space-separated string of theme names to set, or empty
     *            string to remove all theme names
     */
    default void setThemeName(String themeName) {
        getElement().setAttribute("theme", themeName);
    }

    /**
     * Gets the theme names for this component.
     *
     * @return a space-separated string of theme names, empty string if there
     *         are no theme names or <code>null</code> if attribute (theme) is
     *         not set at all
     */
    default String getThemeName() {
        return getElement().getAttribute("theme");
    }

    /**
     * Gets the set of theme names used for this element. The returned set can
     * be modified to add or remove theme names. The contents of the set is also
     * reflected in the value of the <code>theme</code> attribute.
     *
     * @see Element#getThemeList()
     *
     * @return a list of theme names, never <code>null</code>
     */
    default ThemeList getThemeNames() {
        return getElement().getThemeList();
    }

    /**
     * Sets or removes the given theme name for this component.
     *
     * @param themeName
     *            the theme name to set or remove, not <code>null</code>
     * @param set
     *            <code>true</code> to set the theme name, <code>false</code> to
     *            remove it
     */
    default void setThemeName(String themeName, boolean set) {
        getThemeNames().set(themeName, set);
    }

    /**
     * Checks if the component has the given theme name.
     *
     * @param themeName
     *            the theme name to check for
     * @return <code>true</code> if the component has the given theme name,
     *         <code>false</code> otherwise
     */
    default boolean hasThemeName(String themeName) {
        return getThemeNames().contains(themeName);
    }

    /**
     * Adds one or more theme names to this component. Multiple theme names can
     * be specified by using multiple parameters.
     *
     * @param themeNames
     *            the theme name or theme names to be added to the component
     */
    default void addThemeNames(String... themeNames) {
        getThemeNames().addAll(Arrays.asList(themeNames));
    }

    /**
     * Removes one or more theme names from component. Multiple theme names can
     * be specified by using multiple parameters.
     *
     * @param themeNames
     *            the theme name or theme names to be removed from the component
     */
    default void removeThemeNames(String... themeNames) {
        getThemeNames().removeAll(Arrays.asList(themeNames));
    }
}
