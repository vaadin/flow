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
package com.vaadin.flow.router;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.HasStyle;

/**
 * A set of predefined {@link HighlightAction}s.
 *
 * @since 1.0
 */
public final class HighlightActions {

    private HighlightActions() {
    }

    /**
     * An action which toggles {@code className} class on the target based on
     * its highlight state.
     *
     * @param <C>
     *            the target type
     * @param className
     *            the class name to toggle
     * @return the highlight action
     */
    public static <C extends HasStyle> HighlightAction<C> toggleClassName(
            String className) {
        return (component, highlight) -> component.getClassNames()
                .set(className, highlight);
    }

    /**
     * An action which toggles {@code theme} on the target based on its
     * highlight state.
     *
     * @param <C>
     *            the target type
     * @param theme
     *            the theme to toggle
     * @return the highlight action
     */
    public static <C extends HasElement> HighlightAction<C> toggleTheme(
            String theme) {
        return (component, highlight) -> component.getElement().getThemeList()
                .set(theme, highlight);
    }

    /**
     * An action which toggles the target's {@code attribute} based on its
     * highlight state.
     *
     * @param <C>
     *            the target type
     * @param attribute
     *            the attribute to toggle
     * @return the highlight action
     */
    public static <C extends HasElement> HighlightAction<C> toggleAttribute(
            String attribute) {
        return (component, highlight) -> component.getElement()
                .setAttribute(attribute, highlight);
    }

    /**
     * An action which does nothing, regardless of the highlight state.
     *
     * @param <C>
     *            the target type
     * @return the highlight action
     */
    public static <C extends HasElement> HighlightAction<C> none() {
        return (component, highlight) -> {
            // Do nothing.
        };
    }
}
