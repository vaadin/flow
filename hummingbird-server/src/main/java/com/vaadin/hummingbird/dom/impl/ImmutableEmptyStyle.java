package com.vaadin.hummingbird.dom.impl;

import java.util.stream.Stream;

import com.vaadin.hummingbird.dom.Style;

/**
 * A style implementation which is empty and immutable.
 *
 * @author Vaadin Ltd
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
