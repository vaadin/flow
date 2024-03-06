/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.dom.impl;

import java.util.stream.Stream;

import com.vaadin.flow.dom.Style;

/**
 * A style implementation which is empty and immutable.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ImmutableEmptyStyle implements Style {

    static final String CANT_MODIFY_MESSAGE = "This instance is immutable";

    @Override
    public String get(String name) {
        return null;
    }

    @Override
    public Style set(String name, String value) {
        throw new UnsupportedOperationException(CANT_MODIFY_MESSAGE);
    }

    @Override
    public Style remove(String name) {
        throw new UnsupportedOperationException(CANT_MODIFY_MESSAGE);
    }

    @Override
    public Style clear() {
        throw new UnsupportedOperationException(CANT_MODIFY_MESSAGE);
    }

    @Override
    public boolean has(String name) {
        return false;
    }

    @Override
    public Stream<String> getNames() {
        return Stream.empty();
    }
}
