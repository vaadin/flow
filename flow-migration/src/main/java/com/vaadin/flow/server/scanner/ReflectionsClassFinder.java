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
package com.vaadin.flow.server.scanner;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.AnnotatedElement;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.HashSet;
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
        reflections = new Reflections(
                new ConfigurationBuilder().addClassLoader(classLoader)
                        .setExpandSuperTypes(false).addUrls(urls));
    }

    @Override
    public Set<Class<?>> getAnnotatedClasses(
            Class<? extends Annotation> clazz) {
        Set<Class<?>> classes = new HashSet<>();
        classes.addAll(reflections.getTypesAnnotatedWith(clazz, true));
        classes.addAll(getAnnotatedByRepeatedAnnotation(clazz));
        return classes;

    }

    private Set<Class<?>> getAnnotatedByRepeatedAnnotation(
            AnnotatedElement annotationClass) {
        Repeatable repeatableAnnotation = annotationClass
                .getAnnotation(Repeatable.class);
        if (repeatableAnnotation != null) {
            return reflections.getTypesAnnotatedWith(
                    repeatableAnnotation.value(), true);
        }
        return Collections.emptySet();
    }

    @Override
    public URL getResource(String name) {
        return classLoader.getResource(name);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Class<T> loadClass(String name)
            throws ClassNotFoundException {
        return (Class<T>) classLoader.loadClass(name);
    }

    @Override
    public <T> Set<Class<? extends T>> getSubTypesOf(Class<T> type) {
        return reflections.getSubTypesOf(type);
    }
}
