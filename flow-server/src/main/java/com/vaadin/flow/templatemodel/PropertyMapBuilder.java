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
package com.vaadin.flow.templatemodel;

import java.lang.annotation.Annotation;
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
import com.vaadin.flow.templatemodel.BeanModelType.BeanModelTypeProperty;

/**
 * Creates a property map builder that extracts all the properties' data from
 * the class given.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
class PropertyMapBuilder {
    private final Map<String, BeanModelTypeProperty> properties;

    private static class PropertyData {
        private final String propertyName;
        private final Type propertyType;
        private final Class<?> declaringClass;
        private final Collection<Method> accessors = new ArrayList<>();

        private boolean hasGetter;

        private PropertyData(Method method, boolean isGetter) {
            propertyName = ReflectTools.getPropertyName(method);
            propertyType = ReflectTools.getPropertyType(method);
            declaringClass = method.getDeclaringClass();
            accessors.add(method);
            hasGetter = isGetter;
        }

        private PropertyData merge(PropertyData newData) {
            assert Objects.equals(propertyName, newData.propertyName) : String
                    .format("This object is expected to be merged for objects with same 'propertyName' field, but got different ones: '%s' and '%s'",
                            propertyName, newData.propertyName);
            accessors.addAll(newData.accessors);
            if (!hasGetter) {
                hasGetter = newData.hasGetter;
            }
            return this;
        }

        private BeanModelTypeProperty buildProperty(
                PropertyFilter propertyFilter,
                PathLookup<ModelEncoder<?, ?>> outerConverters,
                PathLookup<ClientUpdateMode> outerClientModes) {

            PropertyFilter innerFilter = new PropertyFilter(propertyFilter,
                    propertyName, getExcludeFieldsFilter());
            String prefix = innerFilter.getPrefix();

            PathLookup<ModelEncoder<?, ?>> innerConverters = outerConverters
                    .compose(getModelConverters(), prefix);
            PathLookup<ClientUpdateMode> innerUpdateModes = outerClientModes
                    .compose(getClientUpdateModes(), prefix);

            return new BeanModelTypeProperty(
                    createModelType(innerFilter, innerConverters,
                            innerUpdateModes),
                    innerUpdateModes.getItem(prefix).orElse(null), hasGetter);

        }

        private ModelType createModelType(PropertyFilter innerFilter,
                PathLookup<ModelEncoder<?, ?>> innerConverters,
                PathLookup<ClientUpdateMode> innerUpdateModes) {
            if (innerConverters.getItem(innerFilter.getPrefix()).isPresent()) {
                return BeanModelType.getConvertedModelType(propertyType,
                        innerFilter, propertyName, declaringClass,
                        innerConverters, innerUpdateModes);
            } else {
                return BeanModelType.getModelType(propertyType, innerFilter,
                        propertyName, declaringClass, innerConverters,
                        innerUpdateModes);
            }
        }

        private Map<String, ModelEncoder<?, ?>> getModelConverters() {
            return collectAnnotationsByPath(Encode.class, Encode::path,
                    convert -> ReflectTools.createInstance(convert.value()),
                    "converters");
        }

        private Map<String, ClientUpdateMode> getClientUpdateModes() {
            return collectAnnotationsByPath(AllowClientUpdates.class,
                    AllowClientUpdates::path, AllowClientUpdates::value,
                    "client update modes");
        }

        private <T, A extends Annotation> Map<String, T> collectAnnotationsByPath(
                Class<A> annotationType, Function<A, String> pathExtractor,
                Function<A, T> valueExtractor, String conflictMessageToken) {
            return accessors.stream()
                    .map(method -> method.getAnnotationsByType(annotationType))
                    .flatMap(Stream::of).collect(Collectors.toMap(pathExtractor,
                            valueExtractor, (u, v) -> {
                                throw new InvalidTemplateModelException(
                                        "A template model method cannot have multiple "
                                                + conflictMessageToken
                                                + " with the same path. Affected methods: "
                                                + u + ", " + v + ".");
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
            PathLookup<ModelEncoder<?, ?>> converterLookup,
            PathLookup<ClientUpdateMode> updateModeLookup) {
        assert javaType != null;
        assert propertyFilter != null;

        properties = Stream.concat(
                getPropertiesData(ReflectTools.getGetterMethods(javaType), true,
                        propertyFilter),
                getPropertiesData(ReflectTools.getSetterMethods(javaType),
                        false, propertyFilter))
                .collect(Collectors.toMap(PropertyData::getPropertyName,
                        Function.identity(), PropertyData::merge))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> entry.getValue().buildProperty(propertyFilter,
                                converterLookup, updateModeLookup)));
    }

    /**
     * Get extracted properties.
     *
     * @return the extracted properties
     */
    Map<String, BeanModelTypeProperty> getProperties() {
        return properties;
    }

    private Stream<PropertyData> getPropertiesData(Stream<Method> methods,
            boolean getterMethods, PropertyFilter propertyFilter) {
        return methods.map(getter -> new PropertyData(getter, getterMethods))
                .filter(data -> propertyFilter.test(data.getPropertyName()));
    }

}
