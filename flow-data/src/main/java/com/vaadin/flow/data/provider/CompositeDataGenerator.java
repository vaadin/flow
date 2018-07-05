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

import java.util.HashSet;
import java.util.Set;

import com.vaadin.flow.shared.Registration;

import elemental.json.JsonObject;

/**
 * A {@link DataGenerator} that aggregates multiple DataGenerators and delegates
 * the data generation to them. It doesn't generate or destroy any data by its
 * own.
 * <p>
 * It is used by components that need to add and remove DataGenerators
 * dynamically, or that support multiple layers of data generation.
 * 
 * @author Vaadin Ltd
 * @since 1.0.
 *
 * @param <T>
 *            the data type of the underlying DataGenerators
 */
public class CompositeDataGenerator<T>
        implements DataGenerator<T>, HasDataGenerators<T> {

    private final Set<DataGenerator<T>> dataGenerators = new HashSet<>();

    @Override
    public void generateData(T item, JsonObject jsonObject) {
        dataGenerators
                .forEach(generator -> generator.generateData(item, jsonObject));
    }

    @Override
    public void destroyData(T item) {
        dataGenerators.forEach(generator -> generator.destroyData(item));
    }

    @Override
    public void destroyAllData() {
        dataGenerators.forEach(DataGenerator::destroyAllData);
    }

    @Override
    public void refreshData(T item) {
        dataGenerators.forEach(generator -> generator.refreshData(item));
    }

    @Override
    public Registration addDataGenerator(DataGenerator<T> generator) {
        assert generator != null : "generator should not be null";
        dataGenerators.add(generator);
        return () -> removeDataGenerator(generator);
    }

    /**
     * Removes the DataGenerator from the list, destroying its data.
     * 
     * @param generator
     *            the data generator to remove
     */
    @Override
    public void removeDataGenerator(DataGenerator<T> generator) {
        assert generator != null : "generator should not be null";
        generator.destroyAllData();
        dataGenerators.remove(generator);
    }

}
