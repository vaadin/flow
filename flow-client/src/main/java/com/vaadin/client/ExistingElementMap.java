/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.GWT;

import elemental.dom.Element;

/**
 * Mapping between a server-side node identifier which has been requested to
 * attach an existing client-side element.
 *
 * <p>
 * Under GWT this is a thin facade over the TypeScript implementation at
 * {@code src/main/frontend/internal/client/ExistingElementMap.ts}, reached
 * through {@link NativeExistingElementMap}. The JVM path keeps a parallel
 * {@link HashMap}-backed implementation so {@code ExistingElementMapTest} (and
 * any other JUnit code instantiating this class) keeps working.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ExistingElementMap {

    private final NativeExistingElementMap delegate;
    private final Map<Element, Integer> jvmElementToId;
    private final List<Element> jvmIdToElement;

    /**
     * Creates a new empty map.
     */
    public ExistingElementMap() {
        if (GWT.isScript()) {
            delegate = new NativeExistingElementMap();
            jvmElementToId = null;
            jvmIdToElement = null;
        } else {
            delegate = null;
            jvmElementToId = new HashMap<>();
            jvmIdToElement = new ArrayList<>();
        }
    }

    /**
     * Gets the element stored via {@link #add(int, Element)} for the given
     * {@code id}.
     */
    public Element getElement(int id) {
        if (delegate != null) {
            return delegate.getElement(id);
        }
        if (id < 0 || id >= jvmIdToElement.size()) {
            return null;
        }
        return jvmIdToElement.get(id);
    }

    /**
     * Gets the id stored via {@link #add(int, Element)} for the given
     * {@code element}.
     */
    public Integer getId(Element element) {
        if (delegate != null) {
            return delegate.getId(element);
        }
        return jvmElementToId.get(element);
    }

    /**
     * Removes the identifier and the associated element from the mapping.
     */
    public void remove(int id) {
        if (delegate != null) {
            delegate.remove(id);
            return;
        }
        if (id < 0 || id >= jvmIdToElement.size()) {
            return;
        }
        Element element = jvmIdToElement.get(id);
        if (element != null) {
            jvmIdToElement.set(id, null);
            jvmElementToId.remove(element);
        }
    }

    /**
     * Adds the {@code id} and the {@code element} to the mapping.
     */
    public void add(int id, Element element) {
        if (delegate != null) {
            delegate.add(id, element);
            return;
        }
        while (jvmIdToElement.size() <= id) {
            jvmIdToElement.add(null);
        }
        jvmIdToElement.set(id, element);
        jvmElementToId.put(element, id);
    }
}
