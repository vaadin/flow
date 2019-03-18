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
