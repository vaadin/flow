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

import com.vaadin.flow.function.SerializableComparator;

/**
 * Stores the component's specific sorting.
 * 
 * @param <T>
 *            component's items type
 */
public class ComponentSorting<T> implements Serializable {

    private final SerializableComparator<T> sortComparator;

    /**
     * Create a new instance of component's sorting.
     * 
     * @param sortComparator
     *            the sorting to be bound to the component.
     */
    public ComponentSorting(SerializableComparator<T> sortComparator) {
        this.sortComparator = sortComparator;
    }

    /**
     * Gets the component's sorting.
     * 
     * @return component's sorting.
     */
    public SerializableComparator<T> getSortComparator() {
        return sortComparator;
    }
}
