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

package com.vaadin.flow.plugin.common;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;

/**
 * Collects annotation values from all classes or jars specified.
 *
 * @author Vaadin Ltd.
 */
public class AnnotationValuesExtractor {
    private final ClassLoader projectClassLoader;
    private final Reflections reflections;

    /**
     * Prepares the class to extract annotations from the project classes specified.
     *
     * @param projectClassesLocations urls to project class locations (directories, jars etc.)
     */
    public AnnotationValuesExtractor(URL... projectClassesLocations) {
        projectClassLoader = new URLClassLoader(projectClassesLocations, null);
        reflections = new Reflections(
                new ConfigurationBuilder()
                        .addClassLoader(projectClassLoader)
                        .addUrls(projectClassesLocations)
        );
    }

    /**
     * Extracts annotation values from the annotations.
     * Each annotation value is retrieved by calling a method by name specified.
     *
     * @param annotationToValueGetterMethodName annotations and method names to call to get data from
     * @return map with same keys and all unique values from the corresponding annotations
     * @throws IllegalArgumentException if annotation does not have the method specified
     * @throws IllegalStateException    if annotation cannot be loaded for specified project classes or annotation method call threw an exception
     */
    public Map<Class<? extends Annotation>, Set<String>> extractAnnotationValues(Map<Class<? extends Annotation>, String> annotationToValueGetterMethodName) {
        return annotationToValueGetterMethodName.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> getProjectAnnotationValues(entry.getKey(), entry.getValue())
        ));
    }

    private Set<String> getProjectAnnotationValues(Class<? extends Annotation> annotationClass, String valueGetterMethodName) {
        Class<? extends Annotation> annotationInProjectContext = loadClassInProjectClassLoader(annotationClass.getName());
        Set<Class<?>> annotatedBySingleAnnotation = reflections.getTypesAnnotatedWith(annotationInProjectContext, true);
        Set<Class<?>> annotatedByRepeatedAnnotation = getAnnotatedByRepeatedAnnotation(annotationInProjectContext);
        return Stream.concat(annotatedBySingleAnnotation.stream(), annotatedByRepeatedAnnotation.stream())
                .map(type -> type.getAnnotationsByType(annotationInProjectContext))
                .flatMap(Stream::of)
                .map(annotation -> invokeAnnotationMethod(annotation, valueGetterMethodName))
                .collect(Collectors.toSet());
    }

    @SuppressWarnings("unchecked")
    private <T> Class<T> loadClassInProjectClassLoader(String className) {
        try {
            return (Class<T>) projectClassLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(String.format("Failed to load class '%s' in custom classloader", className), e);
        }
    }

    private Set<Class<?>> getAnnotatedByRepeatedAnnotation(AnnotatedElement annotationClass) {
        Repeatable repeatableAnnotation = annotationClass.getAnnotation(Repeatable.class);
        if (repeatableAnnotation != null) {
            return reflections.getTypesAnnotatedWith(repeatableAnnotation.value(), true);
        }
        return Collections.emptySet();
    }

    private String invokeAnnotationMethod(Annotation target, String methodName) {
        try {
            return String.valueOf(target.getClass().getDeclaredMethod(methodName).invoke(target));
        } catch (IllegalAccessException e) {
            throw new UnsupportedOperationException(String.format("Failed to access method '%s' in annotation interface '%s', should not be happening due to JLS definition of annotation interface", methodName, target), e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(String.format("Got an exception by invoking method '%s' from annotation '%s'", methodName, target), e);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(String.format("Annotation '%s' has no method named `%s", target, methodName), e);
        }
    }
}
