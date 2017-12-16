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
package com.vaadin.flow.templatemodel;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Class for providing instances of {@link ModelConverter}s for given property
 * filters.
 * 
 * @author Vaadin Ltd
 */
public class ModelConverterProvider implements
        Function<PropertyFilter, Optional<ModelConverter<?, ?>>> {

    /**
     * A ModelConverterProvider that always provides an empty Optional.
     */
    public static final ModelConverterProvider EMPTY_PROVIDER = new ModelConverterProvider();

    private final Map<String, Class<? extends ModelConverter<?, ?>>> converters;

    private final String pathPrefix;

    private ModelConverterProvider() {
        converters = new HashMap<>();
        pathPrefix = "";
    }

    /**
     * Composes a new ModelConverterProvider from the given
     * ModelConverterProvider, map of converters and PropertyFilter.
     * 
     * @param converterProvider
     *            the model converter provider to compose this with
     * @param converters
     *            map of converter paths to their corresponding class
     * @param propertyFilter
     *            the property filter to use
     */
    public ModelConverterProvider(ModelConverterProvider converterProvider,
            Map<String, Class<? extends ModelConverter<?, ?>>> converters,
            PropertyFilter propertyFilter) {
        this.converters = new HashMap<>();
        this.converters.putAll(converterProvider.converters);
        pathPrefix = propertyFilter.getPrefix();
        converters.forEach(this::putConverter);
    }

    private void putConverter(String path,
            Class<? extends ModelConverter<?, ?>> converterClass) {
        StringBuilder pathStringBuilder = new StringBuilder(pathPrefix);
        if (!path.isEmpty()) {
            pathStringBuilder.append(path);
            pathStringBuilder.append('.');
        }
        converters.put(pathStringBuilder.toString(), converterClass);
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
        } catch (InstantiationException | IllegalAccessException exception) {
            throw new InvalidTemplateModelException(
                    "ModelConverter '" + converterClass.getSimpleName()
                            + "' does not implement an accessible default constructor.",
                    exception);
        }
    }
}
