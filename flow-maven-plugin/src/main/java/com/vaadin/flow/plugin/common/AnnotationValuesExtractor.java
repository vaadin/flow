/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
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
                .map(type -> type
                        .getAnnotationsByType(annotationInProjectContext))
                .flatMap(Stream::of)
                .map(annotation -> (String) invokeAnnotationMethod(annotation,
                        valueGetterMethodName))
                .collect(Collectors.toSet());
    }
}
