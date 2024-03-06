/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.provider;

import java.util.LinkedHashSet;
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

    final Set<DataGenerator<T>> dataGenerators = new LinkedHashSet<>();

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
    private void removeDataGenerator(DataGenerator<T> generator) {
        generator.destroyAllData();
        dataGenerators.remove(generator);
    }
}
