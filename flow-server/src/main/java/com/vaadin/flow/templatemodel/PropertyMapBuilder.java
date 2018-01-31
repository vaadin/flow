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

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.internal.ReflectTools;

/**
 * Creates a property map builder that extracts all the properties' data from
 * the class given.
 *
 * @author Vaadin Ltd.
 */
class PropertyMapBuilder {
    private final Map<String, ModelType> properties;

    private static class PropertyData {
        private final String propertyName;
        private final Type propertyType;
        private final Class<?> declaringClass;
        private final Collection<Method> accessors = new ArrayList<>();

        private PropertyData(Method method) {
            propertyName = ReflectTools.getPropertyName(method);
            propertyType = ReflectTools.getPropertyType(method);
            declaringClass = method.getDeclaringClass();
            accessors.add(method);
        }

        private PropertyData merge(PropertyData newData) {
            assert Objects.equals(propertyName, newData.propertyName) : String
                    .format("This object is expected to be merged for objects with same 'propertyName' field, but got different ones: '%s' and '%s'",
                            propertyName, newData.propertyName);
            accessors.addAll(newData.accessors);
            return this;
        }

        private ModelType getModelPropertyType(PropertyFilter propertyFilter,
                PathLookup<ModelConverter<?, ?>> outerConverters) {
            PropertyFilter innerFilter = new PropertyFilter(propertyFilter,
                    propertyName, getExcludeFieldsFilter());
            PathLookup<ModelConverter<?, ?>> innerConverters = outerConverters
                    .compose(getModelConverters(), innerFilter.getPrefix());

            if (innerConverters.getItem(innerFilter.getPrefix()).isPresent()) {
                return BeanModelType.getConvertedModelType(propertyType,
                        innerFilter, propertyName, declaringClass,
                        innerConverters);
            } else {
                return BeanModelType.getModelType(propertyType, innerFilter,
                        propertyName, declaringClass, innerConverters);
            }
        }

        private Map<String, ModelConverter<?, ?>> getModelConverters() {
            return accessors.stream()
                    .map(method -> method.getAnnotationsByType(Convert.class))
                    .flatMap(Stream::of)
                    .collect(Collectors.toMap(Convert::path,
                            convert -> ReflectTools
                                    .createInstance(convert.value()),
                            (u, v) -> {
                                throw new InvalidTemplateModelException(
                                        "A template model method cannot have multiple "
                                                + "converters with the same path. Affected methods: "
                                                + accessors + ".");
                            }));
        }

        private Predicate<String> getExcludeFieldsFilter() {
            return accessors.stream()
                    .map(TemplateModelUtil::getFilterFromIncludeExclude)
                    .reduce(Predicate::and).orElse(fieldName -> true);
        }

        private String getPropertyName() {
            return propertyName;
        }
    }

    /**
     * Creates a property map builder that extracts all the properties' data
     * from the class given.
     *
     * @param javaType
     *            the java type of the bean to extract properties' data from
     * @param propertyFilter
     *            the filter that allows to skip some properties by their name
     * @param converterLookup
     *            the provided that allows converting model properties with
     *            special converters
     */
    PropertyMapBuilder(Class<?> javaType, PropertyFilter propertyFilter,
            PathLookup<ModelConverter<?, ?>> converterLookup) {
        assert javaType != null;
        assert propertyFilter != null;

        properties = Stream
                .concat(ReflectTools.getSetterMethods(javaType),
                        ReflectTools.getGetterMethods(javaType))
                .map(PropertyData::new)
                .filter(data -> propertyFilter.test(data.getPropertyName()))
                .collect(Collectors.toMap(PropertyData::getPropertyName,
                        Function.identity(), PropertyData::merge))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> entry.getValue().getModelPropertyType(
                                propertyFilter, converterLookup)));
    }

    /**
     * Get extracted properties.
     *
     * @return the extracted properties
     */
    Map<String, ModelType> getProperties() {
        return properties;
    }

}
