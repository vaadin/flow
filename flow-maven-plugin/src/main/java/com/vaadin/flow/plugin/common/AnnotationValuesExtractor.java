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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.Theme;

/**
 * Collects annotation values from all classes or jars specified.
 *
 * @author Vaadin Ltd.
 */
public class AnnotationValuesExtractor {
    private final ClassLoader projectClassLoader;
    private final Reflections reflections;

    /**
     * Prepares the class to extract annotations from the project classes
     * specified.
     *
     * @param projectClassesLocations
     *            urls to project class locations (directories, jars etc.)
     */
    public AnnotationValuesExtractor(URL... projectClassesLocations) {
        projectClassLoader = new URLClassLoader(projectClassesLocations, null);
        reflections = new Reflections(
                new ConfigurationBuilder().addClassLoader(projectClassLoader)
                        .addUrls(projectClassesLocations));
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

    public void collectThemedHtmlImports(
            BiConsumer<Class<? extends AbstractTheme>, Set<String>> htmlImportsUrlConsumer) {
        Class<? extends Annotation> annotationInProjectContext = loadClassInProjectClassLoader(
                HtmlImport.class.getName());
        Stream<Class<?>> annotatedClasses = getAnnotatedClasses(
                annotationInProjectContext);
        HashMap<Class<? extends AbstractTheme>, List<Class<?>>> themedClasses = annotatedClasses
                .collect(Collectors.toMap(clazz -> findTheme(clazz),
                        Collections::singletonList, this::mergeLists,
                        HashMap::new));
        if (themedClasses.size() > 1) {
            throw new IllegalStateException(
                    "Multiple themes are not supported, "
                            + themedClasses.entrySet().stream()
                                    .map(this::printThemeAnnotatedClasses)
                                    .collect(Collectors.joining(",\n")));
        }
        htmlImportsUrlConsumer.accept(themedClasses.keySet().iterator().next(),
                getProjectAnnotationValues(HtmlImport.class, "value"));
    }

    private Class<? extends AbstractTheme> findTheme(Class<?> component) {
        List<Class<? extends RouterLayout>> layouts = getTopParentLayouts(
                component);
        Set<Class<? extends AbstractTheme>> themes = layouts.stream()
                .map(this::getTheme).collect(Collectors.toSet());

        Class<? extends AbstractTheme> theme = getTheme(component);
        if (theme != null) {
            themes.add(theme);
        }
        if (themes.size() > 1) {
            Map<Class<? extends AbstractTheme>, List<Class<?>>> themedClasses = layouts
                    .stream()
                    .collect(Collectors.toMap(this::getTheme,
                            Collections::singletonList, this::mergeLists,
                            HashMap::new));
            if (theme != null) {
                themedClasses.computeIfAbsent(theme, value -> new ArrayList<>())
                        .add(component);
            }
            String themesList = themedClasses.entrySet().stream()
                    .map(this::printThemeAnnotatedClasses)
                    .collect(Collectors.joining(",\n"));
            throw new IllegalStateException(String.format(
                    "Class '%s' has several different themes used in @%s annotation: %s",
                    component.getName(), Theme.class.getSimpleName(),
                    themesList));
        }
        return themes.stream().findFirst().orElse(null);
    }

    @SuppressWarnings("unchecked")
    private Class<? extends AbstractTheme> getTheme(Class<?> component) {
        Annotation annotation = component.getAnnotation(
                loadClassInProjectClassLoader(Theme.class.getName()));
        if (annotation == null) {
            return null;
        }
        return (Class<? extends AbstractTheme>) doInvokeAnnotationMethod(
                annotation, "value");
    }

    private String printThemeAnnotatedClasses(
            Entry<Class<? extends AbstractTheme>, List<Class<?>>> entry) {
        StringBuilder builder;
        if (entry.getKey() == null) {
            builder = new StringBuilder("No theme ");
        } else {
            builder = new StringBuilder("Theme '");
            builder.append(entry.getKey()).append("'");
        }
        builder.append(" is discovered for classes: ");
        builder.append(entry.getValue().stream().map(clazz -> clazz.getName())
                .collect(Collectors.joining(", ")));
        return builder.toString();
    }

    private List<Class<?>> mergeLists(List<Class<?>> list1,
            List<Class<?>> list2) {
        if (list1 instanceof ArrayList<?>) {
            list1.addAll(list2);
            return list1;
        }
        ArrayList<Class<?>> list = new ArrayList<>();
        list.addAll(list1);
        list.addAll(list2);
        return list;
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
                .map(annotation -> invokeAnnotationMethod(annotation,
                        valueGetterMethodName))
                .collect(Collectors.toSet());
    }

    private Stream<Class<?>> getAnnotatedClasses(
            Class<? extends Annotation> annotationInProjectContext) {
        Set<Class<?>> annotatedBySingleAnnotation = reflections
                .getTypesAnnotatedWith(annotationInProjectContext, true);
        Set<Class<?>> annotatedByRepeatedAnnotation = getAnnotatedByRepeatedAnnotation(
                annotationInProjectContext);
        return Stream.concat(annotatedBySingleAnnotation.stream(),
                annotatedByRepeatedAnnotation.stream());
    }

    @SuppressWarnings("unchecked")
    private <T> Class<T> loadClassInProjectClassLoader(String className) {
        try {
            return (Class<T>) projectClassLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(String.format(
                    "Failed to load class '%s' in custom classloader",
                    className), e);
        }
    }

    private Set<Class<?>> getAnnotatedByRepeatedAnnotation(
            AnnotatedElement annotationClass) {
        Repeatable repeatableAnnotation = annotationClass
                .getAnnotation(Repeatable.class);
        if (repeatableAnnotation != null) {
            return reflections
                    .getTypesAnnotatedWith(repeatableAnnotation.value(), true);
        }
        return Collections.emptySet();
    }

    private String invokeAnnotationMethod(Annotation target,
            String methodName) {
        return String.valueOf(doInvokeAnnotationMethod(target, methodName));
    }

    private Object doInvokeAnnotationMethod(Annotation target,
            String methodName) {
        try {
            return target.getClass().getDeclaredMethod(methodName)
                    .invoke(target);
        } catch (IllegalAccessException e) {
            throw new UnsupportedOperationException(String.format(
                    "Failed to access method '%s' in annotation interface '%s', should not be happening due to JLS definition of annotation interface",
                    methodName, target), e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(String.format(
                    "Got an exception by invoking method '%s' from annotation '%s'",
                    methodName, target), e);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                    String.format("Annotation '%s' has no method named `%s",
                            target, methodName),
                    e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Class<? extends RouterLayout>> getTopParentLayouts(
            Class<?> component) {
        List<Class<? extends RouterLayout>> accumulator = new ArrayList<>();

        Class<? extends Annotation> parentLayoutAnnotation = loadClassInProjectClassLoader(
                ParentLayout.class.getName());
        Annotation parentLayout = component
                .getAnnotation(parentLayoutAnnotation);
        if (parentLayout != null) {
            Class<? extends RouterLayout> routerLayout = (Class<? extends RouterLayout>) doInvokeAnnotationMethod(
                    parentLayout, "value");
            accumulator.add(recuseToTopLayout(routerLayout));
        }

        Class<? extends Annotation> routeAnnotation = loadClassInProjectClassLoader(
                Route.class.getName());
        Annotation route = component.getAnnotation(routeAnnotation);
        if (route != null) {
            Class<? extends RouterLayout> routerLayout = (Class<? extends RouterLayout>) doInvokeAnnotationMethod(
                    route, "layout");
            Class<? extends RouterLayout> layout = recuseToTopLayout(
                    routerLayout);
            if (!UI.class.getName().equals(layout.getName())) {
                accumulator.add(layout);
            }
        }

        Class<? extends Annotation> routeAliasAnnotation = loadClassInProjectClassLoader(
                RouteAlias.class.getName());
        Stream.of(component.getAnnotationsByType(routeAliasAnnotation))
                .map(alias -> doInvokeAnnotationMethod(alias, "layout"))
                .map(clazz -> (Class<? extends RouterLayout>) clazz)
                .map(this::recuseToTopLayout)
                .filter(routeLayout -> !UI.class.getName()
                        .equals(routeLayout.getName()))
                .forEach(accumulator::add);
        return accumulator;
    }

    @SuppressWarnings("unchecked")
    private Class<? extends RouterLayout> recuseToTopLayout(
            Class<? extends RouterLayout> layout) {
        Class<? extends Annotation> parentLayoutClass = loadClassInProjectClassLoader(
                ParentLayout.class.getName());
        Annotation parentLayout = layout.getAnnotation(parentLayoutClass);

        if (parentLayout != null) {
            return recuseToTopLayout(
                    (Class<? extends RouterLayout>) doInvokeAnnotationMethod(
                            parentLayout, "value"));
        }
        return layout;
    }

}
