package com.vaadin.flow.server.frontend.scanner;

import java.io.Serializable;
import java.util.Objects;

/**
 * A container for Theme information when scanning the class path. It
 * overrides equals and hashCode in order to use HashSet to eliminate
 * duplicates.
 */
final class ThemeData implements Serializable {
    String name;
    String variant = "";
    boolean notheme;

    String getName() {
        return name;
    }

    String getVariant() {
        return variant;
    }

    boolean isNotheme() {
        return notheme;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof ThemeData)) {
            return false;
        }
        ThemeData that = (ThemeData) other;
        return notheme == that.notheme && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        // We might need to add variant when we wanted to fail in the
        // case of same theme class with different variant, which was
        // right in v13
        return Objects.hash(name, notheme);
    }

    @Override
    public String toString() {
        return " notheme: " + notheme + "\n name:" + name + "\n variant: "
                + variant;
    }
}