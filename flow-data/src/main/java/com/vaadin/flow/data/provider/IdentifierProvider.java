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

import com.vaadin.flow.function.ValueProvider;

/**
 * A callback interface that is used to provide the identifier of an item.
 *
 * @param <T>
 *            the type of the item
 * @since
 */
@FunctionalInterface
public interface IdentifierProvider<T> extends ValueProvider<T, Object> {
    /**
     * Returns an identifier provider that always returns its input argument.
     *
     * @param <T>
     *            the type of the input and output objects to the function
     * @return a function that always returns its input argument
     */
    static <T> IdentifierProvider<T> identity() {
        return t -> t;
    }
}
