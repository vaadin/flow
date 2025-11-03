/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.data.converter;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.testutil.ClassFinder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DefaultConverterFactoryTest {

    private final DefaultConverterFactory factory = DefaultConverterFactory.INSTANCE;

    @Test
    public void newInstance_unknownConversion_converterNotFound() {
        assertThatConversionIsNotSupported(Integer.class, Float.class);
        assertThatConversionIsNotSupported(String.class, ZonedDateTime.class);
        assertThatConversionIsNotSupported(String.class, Timestamp.class);
    }

    @Test
    public void newInstance_knownConversion_converterCreated()
            throws IOException {
        Map<Class<? extends Converter<?, ?>>, List<Class<?>>> converters = new ConverterClassFinder()
                .knownConverters();
        Assert.assertFalse(
                "Expecting standard converters to exist, but none found",
                converters.isEmpty());
        converters.forEach(
                (converterType, types) -> assertThatConversionIsSupported(
                        types.get(0), types.get(1), converterType));
    }

    @Test
    public void newInstance_knownConversionPrimitiveTypes_converterCreated()
            throws IOException {
        Map<Class<? extends Converter<?, ?>>, List<Class<?>>> converters = new ConverterClassFinder()
                .knownConverters();
        converters.replaceAll((converterType, genericTypes) -> genericTypes
                .stream().map(this::toPrimitiveTypeIfExist)
                .collect(Collectors.toList()));
        Assert.assertFalse(
                "Expecting standard converters to exist, but none found",
                converters.isEmpty());
        converters.forEach(
                (converterType, types) -> assertThatConversionIsSupported(
                        types.get(0), types.get(1), converterType));
    }

    @Test
    public void newInstance_nullArguments_invocationFails() {
        Assert.assertThrows("Expecting null presentationType not allowed",
                IllegalArgumentException.class,
                () -> factory.newInstance(null, String.class));
        Assert.assertThrows("Expecting null modelType not allowed",
                IllegalArgumentException.class,
                () -> factory.newInstance(String.class, null));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void assertThatConversionIsSupported(Class presentationType,
            Class modelType, Class<? extends Converter> expectedConverter) {
        Optional<Converter> maybeConverter = factory
                .newInstance(presentationType, modelType);
        assertTrue(
                "Expected conversion (" + presentationType + " -> " + modelType
                        + ") to be handled in "
                        + DefaultConverterFactory.class.getName() + " by "
                        + expectedConverter.getName() + ", but was not",
                maybeConverter.isPresent());
        Converter instance = maybeConverter.get();
        assertEquals(
                "Expecting converter (" + presentationType + " -> " + modelType
                        + ") to be of type " + expectedConverter.getName()
                        + ", but was " + instance.getClass().getName(),
                expectedConverter, instance.getClass());
    }

    private <P, M> void assertThatConversionIsNotSupported(
            Class<P> presentationType, Class<M> modelType) {
        Assert.assertFalse(
                "Converter (" + presentationType + " -> " + modelType
                        + ") should not be supported",
                factory.newInstance(presentationType, modelType).isPresent());
    }

    private Class<?> toPrimitiveTypeIfExist(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type.equals(Boolean.class)) {
            type = Boolean.TYPE;
        } else if (type.equals(Integer.class)) {
            type = Integer.TYPE;
        } else if (type.equals(Float.class)) {
            type = Float.TYPE;
        } else if (type.equals(Double.class)) {
            type = Double.TYPE;
        } else if (type.equals(Byte.class)) {
            type = Byte.TYPE;
        } else if (type.equals(Character.class)) {
            type = Character.TYPE;
        } else if (type.equals(Short.class)) {
            type = Short.TYPE;
        } else if (type.equals(Long.class)) {
            type = Long.TYPE;
        }

        return type;
    }

    // Helper to get all concrete Converter implementation in
    // com.vaadin.flow.data.converters package
    private static class ConverterClassFinder extends ClassFinder {

        @Override
        protected Stream<String> getBasePackages() {
            return Stream.of(Converter.class.getPackage().getName());
        }

        Map<Class<? extends Converter<?, ?>>, List<Class<?>>> knownConverters()
                throws IOException {

            List<String> rawClasspathEntries = getRawClasspathEntries();

            List<String> classes = new ArrayList<>();
            for (String location : rawClasspathEntries) {
                if (!isTestClassPath(location)) {
                    classes.addAll(findServerClasses(location,
                            Collections.emptyList()));
                }
            }

            Map<Class<? extends Converter<?, ?>>, List<Class<?>>> result = new HashMap<>();
            for (String className : classes) {
                try {
                    Class<?> clazz = Class.forName(className);
                    // Accept only public top level concrete Converter
                    // implementations
                    if (Converter.class.isAssignableFrom(clazz)) {
                        List<Class<?>> types = ReflectTools
                                .getGenericInterfaceTypes(clazz,
                                        Converter.class);
                        if (types.stream().allMatch(Objects::nonNull)
                                && Modifier.isPublic(clazz.getModifiers())
                                && !Modifier.isAbstract(clazz.getModifiers())
                                && !clazz.isSynthetic() && !clazz.isInterface()
                                && !clazz.isAnonymousClass()
                                && !clazz.isMemberClass()
                                && !clazz.isLocalClass()) {
                            result.put((Class<? extends Converter<?, ?>>) clazz,
                                    types);
                        }
                    }
                } catch (ClassNotFoundException ex) {
                    // ignore
                }
            }
            return result;
        }
    }

}
