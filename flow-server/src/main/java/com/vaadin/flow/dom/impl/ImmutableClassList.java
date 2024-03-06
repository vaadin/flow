/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.dom.impl;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import com.vaadin.flow.dom.ClassList;

/**
 * Immutable class list implementation.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ImmutableClassList extends AbstractSet<String>
        implements ClassList, Serializable {

    private static final String CANT_MODIFY_MESSAGE = ImmutableEmptyStyle.CANT_MODIFY_MESSAGE;

    private final Collection<String> values;

    /**
     * Creates a new immutable class list with the given values.
     *
     * @param values
     *            the values of the class list
     */
    public ImmutableClassList(Collection<String> values) {
        this.values = Collections.unmodifiableList(new ArrayList<>(values));
    }

    @Override
    public boolean add(String e) {
        throw new UnsupportedOperationException(CANT_MODIFY_MESSAGE);
    }

    @Override
    public Iterator<String> iterator() {
        return values.iterator();
    }

    @Override
    public int size() {
        return values.size();
    }
}
