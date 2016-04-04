/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.namespace;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Iterator;

/**
 * Provides access to a {@link ListNamespace} contents as a set.
 */
public abstract class ListNamespaceSetView<T> extends AbstractSet<T>
        implements Serializable {

    private ListNamespace<T> namespace;

    public ListNamespaceSetView(ListNamespace<T> namespace) {
        this.namespace = namespace;
    }

    @Override
    public int size() {
        return namespace.size();
    }

    @Override
    public void clear() {
        namespace.clear();
    }

    @Override
    public boolean add(T o) {
        validate(o);
        if (contains(o)) {
            return false;
        }

        namespace.add(size(), o);
        return true;
    }

    @Override
    public boolean remove(Object o) {
        validate(o);
        // Uses iterator() which supports proper remove()
        return super.remove(o);
    }

    protected abstract void validate(Object o);

    public boolean set(T o, boolean set) {
        if (set) {
            return add(o);
        } else {
            return remove(o);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean contains(Object o) {
        validate(o);

        return namespace.indexOf((T) o) != -1;
    }

    @Override
    public Iterator<T> iterator() {
        return namespace.iterator();
    }

}