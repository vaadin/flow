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
package com.vaadin.flow.router;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.HasStyle;

/**
 * A set of predefined {@link HighlightAction}s.
 */
public final class HighlightActions {

    /**
     * An action which toggles {@code className} class on the target based on
     * its highlight state.
     *
     * @param className
     *            the class name to toggle
     * @return the highlight action
     */
    public static <C extends HasStyle> HighlightAction<C> toggleClassName(
            String className) {
        return (link, highligh) -> {
            if (highligh) {
                link.addClassName(className);
            } else {
                link.removeClassName(className);
            }
        };
    }

    /**
     * An action which toggles {@code theme} on the target based on its
     * highlight state.
     *
     * @param theme
     *            the theme to toggle
     * @return the highlight action
     */
    public static <C extends HasElement> HighlightAction<C> toggleTheme(
            String theme) {
        return (link, highligh) -> {
            if (highligh) {
                link.getElement().getThemeList().add(theme);
            } else {
                link.getElement().getThemeList().remove(theme);
            }
        };
    }

    /**
     * An action which toggles the target's {@code attribute} based on its
     * highlight state.
     *
     * @param attribute
     *            the attribute to toggle
     * @return the highlight action
     */
    public static <C extends HasElement> HighlightAction<C> toggleAttribute(
            String attribute) {
        return (link, highlight) -> link.getElement().setAttribute(attribute,
                highlight);
    }

    /**
     * An action which does nothing, regardless of the highlight state.
     *
     * @return the highlight action
     */
    public static <C extends HasElement> HighlightAction<C> none() {
        return (link, highligh) -> {
            // Do nothing.
        };
    }

    private HighlightActions() {
    }
}
