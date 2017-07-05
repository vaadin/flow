/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.template.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * 
 * @author Vaadin Ltd
 */
public class ModelConverterProvider implements
        Function<PropertyFilter, Optional<ModelConverter<?, ?>>> {

    public static final ModelConverterProvider EMPTY_PROVIDER = new ModelConverterProvider();

    private final Map<String, Class<? extends ModelConverter<?, ?>>> converters;

    private ModelConverterProvider() {
        converters = new HashMap<>();
    }

    public ModelConverterProvider(ModelConverterProvider converterProvider,
            Map<String, Class<? extends ModelConverter<?, ?>>> converters,
            PropertyFilter propertyFilter) {
        this.converters = new HashMap<>();
        this.converters.putAll(converterProvider.converters);
        converters.entrySet()
                .forEach(entry -> this.converters.put(
                        propertyFilter.getPrefix() + entry.getKey()
                                + (entry.getKey().isEmpty() ? "" : "."),
                        entry.getValue()));
    }

    @Override
    public Optional<ModelConverter<?, ?>> apply(PropertyFilter propertyFilter) {
        if (!converters.containsKey(propertyFilter.getPrefix())) {
            return Optional.empty();
        }
        Class<? extends ModelConverter<?, ?>> converterClass = converters
                .get(propertyFilter.getPrefix());
        try {
            return Optional.of(converterClass.newInstance());
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(
                    "ModelConverter " + converterClass.getSimpleName()
                            + " does not implement an accessible default constructor.");
        }
    }
}
