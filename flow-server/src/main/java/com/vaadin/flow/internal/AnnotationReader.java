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
package com.vaadin.flow.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.StyleSheet;

/**
 * Helper class for reading annotation data.
 *
 * @since 1.0
 */
public class AnnotationReader {

    private AnnotationReader() {
        // Utility class with only static methods
    }

    /**
     * Finds all {@link StyleSheet} annotations on the given {@link Component}
     * class, its super classes and implemented interfaces.
     *
     * @param componentClass
     *            the component class to search for the annotation
     * @return a list the style sheet annotations found
     * @see #getAnnotationFor(Class, Class) for what order the annotations are
     *      in the list
     */
    public static List<StyleSheet> getStyleSheetAnnotations(
            Class<? extends Component> componentClass) {
        return getAnnotationsFor(componentClass, StyleSheet.class);
    }

    /**
     * Finds all {@link CssImport} annotations on the given {@link Component}
     * class, its super classes and implemented interfaces.
     *
     * @param componentClass
     *            the component class to search for the annotation
     * @return a list the CssImport annotations found
     * @see #getAnnotationFor(Class, Class) for what order the annotations are
     *      in the list
     */
    public static List<CssImport> getCssImportAnnotations(
            Class<? extends Component> componentClass) {
        return getAnnotationsFor(componentClass, CssImport.class);
    }

    /**
     * Finds all {@link JavaScript} annotations on the given {@link Component}
     * class, its super classes and implemented interfaces.
     *
     * @param componentClass
     *            the component class to search for the annotation
     * @return a list the JavaScript annotations found
     * @see #getAnnotationFor(Class, Class) for what order the annotations are
     *      in the list
     */
    public static List<JavaScript> getJavaScriptAnnotations(
            Class<? extends Component> componentClass) {
        return getAnnotationsFor(componentClass, JavaScript.class);
    }

    /**
     * Finds all {@link JsModule} annotation on the given {@link Component}
     * class, its super classes and implenented interfaces.
     *
     * @param componentClass
     *         the component class to search for the annotation
     * @return a list the JavaScript annotations found
     */
    public static List<JsModule> getJsModuleAnnotations(
            Class<? extends Component> componentClass) {
        return getAnnotationsFor(componentClass, JsModule.class);
    }

    /**
     * Finds all {@link HtmlImport} annotations on the given {@link Component}
     * class, its super classes and implemented interfaces.
     *
     * @param componentClass
     *            the component class to search for the annotation
     * @return a list the html import annotations found
     * @see #getAnnotationFor(Class, Class) for what order the annotations are
     *      in the list
     */
    public static List<HtmlImport> getHtmlImportAnnotations(
            Class<? extends Component> componentClass) {
        return getAnnotationsFor(componentClass, HtmlImport.class);
    }

    /**
     * Helper to get an annotation for a field.
     *
     * @param <T>
     *            the annotation type
     * @param field
     *            the field to check
     * @param annotationType
     *            the annotation type to look for
     * @return an <code>Optional</code> annotation of the given type, or an
     *         empty Optional if the field does not have the given annotation
     */
    public static <T extends Annotation> Optional<T> getAnnotationFor(
            Field field, Class<T> annotationType) {
        T annotation = field.getAnnotation(annotationType);
        return Optional.ofNullable(annotation);
    }

    /**
     * Helper to get an annotation for a class. If the annotation is not present
     * on the target class, its super classes and implemented interfaces are
     * also searched for the annotation.
     *
     * @param <T>
     *            the annotation type
     * @param clazz
     *            the class from which the annotation should be found
     * @param annotationType
     *            the annotation type to look for
     * @return an <code>Optional</code> annotation of the given type
     */
    public static <T extends Annotation> Optional<T> getAnnotationFor(
            Class<?> clazz, Class<T> annotationType) {
        // Find from the class hierarchy
        Class<?> currentType = clazz;
        T annotation;
        while (currentType != null && currentType != Object.class) {
            annotation = currentType.getAnnotation(annotationType);
            if (annotation != null) {
                return Optional.of(annotation);
            } else {
                currentType = currentType.getSuperclass();
            }
        }

        // Find from an implemented interface
        for (Class<?> iface : clazz.getInterfaces()) {
            annotation = iface.getAnnotation(annotationType);
            if (annotation != null) {
                return Optional.of(annotation);
            }
        }

        return Optional.empty();
    }

    /**
     * Gets a value from an annotation for a class. If the annotation is not
     * present on the target class, its super classes and implemented interfaces
     * are also searched for the annotation.
     *
     * @param <A>
     *            the annotation type
     * @param <T>
     *            the annotation value type
     * @param clazz
     *            the class from which the annotation should be found
     * @param annotationType
     *            the annotation type to look for
     * @param valueExtractor
     *            the function for extracting the value from the annotation if
     *            an annotation is present
     * @return an <code>Optional</code> annotation value
     */
    public static <A extends Annotation, T> Optional<T> getAnnotationValueFor(
            Class<?> clazz, Class<A> annotationType,
            Function<A, T> valueExtractor) {
        return getAnnotationFor(clazz, annotationType).map(valueExtractor);
    }

    /**
     * Helper to get annotations for a class by searching recursively the class
     * and all its super classes and implemented interfaces and their parent
     * interfaces.
     * <p>
     * The annotations in the list are ordered top-down according to the class
     * hierarchy. For each hierarchy level, the annotations from interfaces
     * implemented at that level are on the list before the annotations of the
     * class itself.
     * <p>
     * NOTE: the list may contain annotations with the same values.
     *
     * @param <T>
     *            the annotation type
     * @param clazz
     *            the class from which the annotation should be found
     * @param annotationType
     *            the annotation type to look for
     * @return a list containing all the annotations found
     */
    public static <T extends Annotation> List<T> getAnnotationsFor(
            Class<?> clazz, Class<T> annotationType) {
        if (clazz == null || clazz == Object.class) {
            return Collections.emptyList();
        }

        List<T> annotations = new ArrayList<>();
        // find from super classes
        annotations.addAll(
                getAnnotationsFor(clazz.getSuperclass(), annotationType));

        // find from any implemented interfaces
        for (Class<?> iface : clazz.getInterfaces()) {
            annotations.addAll(getAnnotationsFor(iface, annotationType));
        }

        // find from this class
        for (T annotation : clazz.getAnnotationsByType(annotationType)) {
            if (annotation != null) {
                annotations.add(annotation);
            }
        }

        return annotations;
    }

    /**
     * Gets values from annotations for a class by searching recursively the
     * class and all its super classes and implemented interfaces and their
     * parent interfaces.
     * <p>
     * The values in the stream are ordered top-down according to the class
     * hierarchy. For each hierarchy level, the values from annotations of
     * interfaces implemented at that level are on the list before the values
     * from annotations of the class itself.
     * <p>
     * NOTE: the stream may contain duplicates
     *
     * @param <A>
     *            the annotation type
     * @param <T>
     *            the annotation value type
     * @param clazz
     *            the class from which the annotation should be found
     * @param annotationType
     *            the annotation type to look for
     * @param valueExtractor
     *            the function for extracting the value from any found
     *            annotation
     * @return a list containing all the annotations found
     */
    public static <A extends Annotation, T> Stream<T> getAnnotationValuesFor(
            Class<?> clazz, Class<A> annotationType,
            Function<A, T> valueExtractor) {
        return getAnnotationsFor(clazz, annotationType).stream()
                .map(valueExtractor);
    }
}
