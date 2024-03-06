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

/**
 * A container for CssImport information when scanning the class path. It
 * overrides equals and hashCode in order to use HashSet to eliminate
 * duplicates.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 2.0
 */
public final class CssData implements Serializable {
    String value;
    String id;
    String include;
    String themefor;

    /**
     * The value getter.
     *
     * @return value
     */
    public String getValue() {
        return value;
    }

    /**
     * The id getter.
     *
     * @return id
     */
    public String getId() {
        return id;
    }

    /**
     * The include getter.
     *
     * @return include
     */
    public String getInclude() {
        return include;
    }

    /**
     * The themefor getter.
     *
     * @return themefor
     */
    public String getThemefor() {
        return themefor;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof CssData)) {
            return false;
        }
        CssData that = (CssData) other;
        return Objects.equals(value, that.value) && Objects.equals(id, that.id)
                && Objects.equals(include, that.include)
                && Objects.equals(themefor, that.themefor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, id, include, themefor);
    }

    @Override
    public String toString() {
        return "value: " + value + (id != null ? " id:" + id : "")
                + (include != null ? " include:" + include : "")
                + (themefor != null ? " themefor:" + themefor : "");
    }
}
