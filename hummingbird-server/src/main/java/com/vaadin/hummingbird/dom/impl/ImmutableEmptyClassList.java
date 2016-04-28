package com.vaadin.hummingbird.dom.impl;

import java.util.AbstractSet;
import java.util.Collections;
import java.util.Iterator;

import com.vaadin.hummingbird.dom.ClassList;

/**
 * A class list implementation which is empty and immutable.
 *
 * @author Vaadin Ltd
 */
public class ImmutableEmptyClassList extends AbstractSet<String>
        implements ClassList {

    private static final String CANT_MODIFY_MESSAGE = ImmutableEmptyStyle.CANT_MODIFY_MESSAGE;

    @Override
    public boolean add(String e) {
        throw new UnsupportedOperationException(CANT_MODIFY_MESSAGE);
    }

    @Override
    public boolean set(String className, boolean set) {
        throw new UnsupportedOperationException(CANT_MODIFY_MESSAGE);
    }

    @Override
    public Iterator<String> iterator() {
        return Collections.<String> emptySet().iterator();
    }

    @Override
    public int size() {
        return 0;
    }
}