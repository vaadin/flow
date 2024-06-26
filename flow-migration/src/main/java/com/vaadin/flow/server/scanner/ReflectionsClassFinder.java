/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.scanner;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.AnnotatedElement;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;

import com.vaadin.flow.server.frontend.scanner.ClassFinder;

/**
 * A class finder using org.reflections.
 *
 * @since 2.0
 */
public class ReflectionsClassFinder implements ClassFinder {
    private final transient ClassLoader classLoader;

    private final transient Reflections reflections;

    /**
     * Constructor.
     *
     * @param urls
     *            the list of urls for finding classes.
     */
    public ReflectionsClassFinder(URL... urls) {
        classLoader = new URLClassLoader(urls, null); // NOSONAR
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder()
                .addClassLoader(classLoader).setExpandSuperTypes(false)
                .addUrls(urls);

        configurationBuilder
                .setInputsFilter(resourceName -> resourceName.endsWith(".class")
                        && !resourceName.endsWith("module-info.class"));
        reflections = new Reflections(configurationBuilder);
    }

    @Override
    public Set<Class<?>> getAnnotatedClasses(
            Class<? extends Annotation> clazz) {
        Set<Class<?>> classes = new LinkedHashSet<>();
        classes.addAll(reflections.getTypesAnnotatedWith(clazz, true));
        classes.addAll(getAnnotatedByRepeatedAnnotation(clazz));
        return classes;

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

    @Override
    public URL getResource(String name) {
        return classLoader.getResource(name);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Class<T> loadClass(String name) throws ClassNotFoundException {
        return (Class<T>) classLoader.loadClass(name);
    }

    @Override
    public <T> Set<Class<? extends T>> getSubTypesOf(Class<T> type) {
        return reflections.getSubTypesOf(type);
    }
}
