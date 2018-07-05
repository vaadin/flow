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
package com.vaadin.flow.function;

import java.io.Serializable;

/**
 * Like {@link SerializableBiConsumer}, but with three arguments.
 *
 * @param <T>
 *            the type of the first argument to the operation
 * @param <U>
 *            the type of the second argument to the operation
 * @param <V>
 *            the type of the third argument to the operation
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@FunctionalInterface
public interface SerializableTriConsumer<T, U, V> extends Serializable {
    /**
     * Performs the action.
     *
     * @param t
     *            the first argument
     * @param u
     *            the second argument
     * @param v
     *            the third argument
     */
    void accept(T t, U u, V v);
}
