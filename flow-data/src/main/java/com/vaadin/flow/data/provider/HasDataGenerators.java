/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
}
