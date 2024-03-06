/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend.scanner;

import java.io.Serializable;
import java.util.Objects;

import com.vaadin.flow.theme.AbstractTheme;

/**
 * A container for Theme information when scanning the class path. It overrides
 * equals and hashCode in order to use HashSet to eliminate duplicates.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 2.0
 */
final class ThemeData implements Serializable {
    String themeClass;
    String variant = "";
    String themeName = "";
    boolean notheme;

    ThemeData(String themeClass, String variant, String themeName) {
        if (themeClass.equals(AbstractTheme.class.getName())) {
            this.themeClass = FullDependenciesScanner.LUMO;
        } else {
            this.themeClass = themeClass;
        }
        this.variant = variant;
        this.themeName = themeName;
    }

    ThemeData() {
    }

    String getThemeClass() {
        return themeClass;
    }

    String getVariant() {
        return variant;
    }

    public String getThemeName() {
        return themeName;
    }

    boolean isNotheme() {
        return notheme;
    }

    static ThemeData createNoTheme() {
        ThemeData data = new ThemeData();
        data.notheme = true;
        return data;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof ThemeData)) {
            return false;
        }
        ThemeData that = (ThemeData) other;
        return notheme == that.notheme
                && Objects.equals(themeClass, that.themeClass)
                && Objects.equals(themeName, that.themeName);
    }

    @Override
    public int hashCode() {
        // We might need to add variant when we wanted to fail in the
        // case of same theme class with different variant, which was
        // right in v13
        return Objects.hash(themeClass, notheme, themeName);
    }

    @Override
    public String toString() {
        return " notheme: " + notheme + "\n themeClass:" + themeClass
                + "\n variant: " + variant + "\n themeName: " + themeName;
    }
}
