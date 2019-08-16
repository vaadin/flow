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

package com.vaadin.flow.plugin.common;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.migration.ClassPathIntrospector;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;

/**
 * Collects annotation values from all classes or jars specified.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class AnnotationValuesExtractor extends ClassPathIntrospector {
    /**
     * Prepares the class to extract annotations with the class finder
     * specified.
     *
     * @param finder
     *            the specific ClassFinder to use
     */
    public AnnotationValuesExtractor(ClassFinder finder) {
        super(finder);
    }

    /**
     * Extracts annotation values from the annotations. Each annotation value is
     * retrieved by calling a method by name specified.
     *
     * @param annotationToValueGetterMethodName
     *            annotations and method names to call to get data from
     * @return map with same keys and all unique values from the corresponding
     *         annotations
     * @throws IllegalArgumentException
     *             if annotation does not have the method specified
     * @throws IllegalStateException
     *             if annotation cannot be loaded for specified project classes
     *             or annotation method call threw an exception
     */
    public Map<Class<? extends Annotation>, Set<String>> extractAnnotationValues(
            Map<Class<? extends Annotation>, String> annotationToValueGetterMethodName) {
        return annotationToValueGetterMethodName.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> getProjectAnnotationValues(entry.getKey(),
                                entry.getValue())));
    }

    private Set<String> getProjectAnnotationValues(
            Class<? extends Annotation> annotationClass,
            String valueGetterMethodName) {
        Class<? extends Annotation> annotationInProjectContext = loadClassInProjectClassLoader(
                annotationClass.getName());
        Stream<Class<?>> concat = getAnnotatedClasses(
                annotationInProjectContext);
        return concat
                .map(type -> type.getAnnotationsByType(annotationInProjectContext))
                .flatMap(Stream::of)
                .map(annotation -> (String)invokeAnnotationMethod(annotation,
                        valueGetterMethodName))
                .collect(Collectors.toSet());
    }
}
