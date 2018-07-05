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
package com.vaadin.flow.theme;

import java.io.Serializable;
import java.util.Objects;

import com.vaadin.flow.component.UI;

/**
 * Holds all the settings needed to properly set a Theme in the application.
 * 
 * @author Vaadin Ltd
 * @since 1.0.
 * @see Theme
 * @see UI#getThemeFor(Class, String)
 *
 */
public class ThemeDefinition implements Serializable {

    private final Class<? extends AbstractTheme> theme;
    private final String variant;

    /**
     * Creates a definition with the given them class and variant.
     * 
     * @param theme
     *            the theme class, not <code>null</code>
     * @param variant
     *            the variant of the theme, not <code>null</code>
     */
    public ThemeDefinition(Class<? extends AbstractTheme> theme,
            String variant) {

        Objects.requireNonNull(theme);
        Objects.requireNonNull(variant);

        this.theme = theme;
        this.variant = variant;
    }

    /**
     * Helper constructor that extracts the needed information from a Theme
     * annotation.
     * 
     * @param themeAnnotation
     *            the annotation to get the definition from
     */
    public ThemeDefinition(Theme themeAnnotation) {
        this(themeAnnotation.value(), themeAnnotation.variant());
    }

    /**
     * Gets the theme class.
     * 
     * @return the theme class
     */
    public Class<? extends AbstractTheme> getTheme() {
        return theme;
    }

    /**
     * Gets the variant of the theme.
     * 
     * @return the variant
     */
    public String getVariant() {
        return variant;
    }

}
