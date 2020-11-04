/*
 * Copyright 2000-2020 Vaadin Ltd.
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

package com.vaadin.flow.data.provider;

import java.io.Serializable;

import com.vaadin.flow.function.SerializablePredicate;

/**
 * Stores the component's specific in-memory filter.
 * 
 * @param <T>
 *            component's items type
 */
public class ComponentInMemoryFilter<T> implements Serializable {

    private final SerializablePredicate<T> filter;

    /**
     * Create a new instance of component's in-memory filter.
     * 
     * @param filter
     *            in-memory filter to be bound to the component.
     */
    public ComponentInMemoryFilter(SerializablePredicate<T> filter) {
        this.filter = filter;
    }

    /**
     * Gets the component's in-memory filter.
     * 
     * @return component's in-memory filter.
     */
    public SerializablePredicate<T> getFilter() {
        return filter;
    }
}
