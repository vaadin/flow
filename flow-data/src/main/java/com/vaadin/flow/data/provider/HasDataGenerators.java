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
package com.vaadin.flow.data.provider;

import java.io.Serializable;

import com.vaadin.flow.shared.Registration;

/**
 * Defines the contract of adding and removing multiple {@link DataGenerator}s
 * to a given object.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 *
 * @param <T>
 *            the data type of the {@link DataGenerator}s.
 */
public interface HasDataGenerators<T> extends Serializable {

    /**
     * Adds the given data generator. If the generator was already added, does
     * nothing.
     *
     * @param generator
     *            the data generator to add
     * @return a registration that can be used to remove the data generator
     */
    Registration addDataGenerator(DataGenerator<T> generator);

    /**
     * Removes the given data generator.
     *
     * @deprecated Use the registration returned from
     *             {@link #addDataGenerator(DataGenerator)} instead.
     *
     * @param generator
     *            the data generator to remove
     */
    @Deprecated
    void removeDataGenerator(DataGenerator<T> generator);

}
