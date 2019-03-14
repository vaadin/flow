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
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.ThemeDefinition;

/**
 * Collects annotation values from all classes or jars specified.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class AnnotationValuesExtractor extends ClassPathIntrospector {

    public static final String LUMO = "com.vaadin.flow.theme.lumo.Lumo";

    /**
     * Prepares the class to extract annotations from the project classes
     * specified.
     *
     * @param projectClassesLocations
     *            urls to project class locations (directories, jars etc.)
     */
    public AnnotationValuesExtractor(URL... projectClassesLocations) {
        super(projectClassesLocations);
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


    /**
     * Extracts annotation values from the annotations of the given class. Each
     * annotation value is retrieved by calling a method by name specified.
     *
     * @param clazz
     *            the annotated class
     * @param annotationClass
     *            the annotation
     * @param valueGetterMethodName
     *            the getter name used for values
     * @return a set of all values when the annotation is repeatable
     */
    public Set<String> getClassAnnotationValues(Class<?> clazz, Class<? extends Annotation> annotationClass,
            String valueGetterMethodName) {
        Class<? extends Annotation> annotationInProjectContext = loadClassInProjectClassLoader(
                annotationClass.getName());

        return Arrays.asList(clazz.getAnnotationsByType(annotationInProjectContext)).stream()
                .map(annotation -> (String) invokeAnnotationMethod(annotation, valueGetterMethodName))
                .collect(Collectors.toSet());
    }

    /**
     * Get theme definitions for all theme annotated classes.
     * @param log
     *
     * @return a map of {@link ThemeDefinition}
     */
    public Map<ThemeDefinition, Class<?>> getThemeDefinitions() {
        Map<ThemeDefinition, Class<?>> map = new LinkedHashMap<>();

        Class<? extends Annotation> annotationInProjectContext = loadClassInProjectClassLoader(Theme.class.getName());

        getAnnotatedClasses(annotationInProjectContext)
                .forEach(entry -> Arrays.stream(entry.getAnnotations()).forEach(annotation -> {
                    if (annotation.annotationType().getCanonicalName().equals(Theme.class.getCanonicalName())) {
                        map.put(new ThemeDefinition(invokeAnnotationMethod(annotation, "value"),
                                invokeAnnotationMethod(annotation, "variant")), entry);
                    }
                }));

        if (map.size() == 0) {
            try {
                Class<? extends AbstractTheme> lumo = loadClassInProjectClassLoader(LUMO);
                map.put(new ThemeDefinition(lumo, ""), null);
            } catch (IllegalStateException ignore) { //NOSONAR
            }
        }

        return map;
    }

    /**
     * Get all classes annotated with a specific annotation and return a map
     * with class as key and a list with the annotation values found per class.
     * Note that we use a set because the annotation might be repeatable.
     *
     * @param annotationClass
     *            the annotation class
     * @param valueGetterMethodName
     *            method names to call to get data from
     * @return a map of classes and their annotation values
     */
    public Map<Class<?>, Set<String>> getAnnotatedClasses(Class<? extends Annotation> annotationClass,
            String valueGetterMethodName) {

        Class<? extends Annotation> annotationInProjectContext = loadClassInProjectClassLoader(
                annotationClass.getName());

        Stream<Class<?>> classes = getAnnotatedClasses(annotationInProjectContext);

        return classes.collect(Collectors.toMap(entry -> entry,
                entry -> getClassAnnotationValues(entry, annotationClass, valueGetterMethodName)));
    }

}
