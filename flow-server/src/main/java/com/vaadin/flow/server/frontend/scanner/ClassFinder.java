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
package com.vaadin.flow.server.frontend.scanner;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.googlecode.gentyref.GenericTypeReflector;

/**
 * Interface for annotated and subclass class searches.
 *
 * @since 2.0
 */
public interface ClassFinder extends Serializable {

    /**
     * Implementation that searches for annotated classes or subclasses in a
     * list of classes.
     */
    class DefaultClassFinder implements ClassFinder {
        private final Set<Class<?>> classes;

        private final transient ClassLoader classLoader;

        /**
         * It uses current classloader for getting resources or loading classes.
         *
         * @param classes
         *            The classes.
         */
        public DefaultClassFinder(Set<Class<?>> classes) {
            this.classes = classes;
            // take classLoader from the first class in the set, unless empty
            classLoader = classes.isEmpty() ? getClass().getClassLoader()
                    : classes.iterator().next().getClassLoader();
        }

        /**
         * ClassFinder using a specified <code>ClassLoader</code> to load
         * classes and a list of classes where to look for annotations or
         * subclasses.
         *
         * @param classLoader
         *            classloader for getting resources or loading classes.
         * @param classes
         *            classes where to look for annotations or subclasses.
         */
        public DefaultClassFinder(ClassLoader classLoader,
                Class<?>... classes) {
            this.classLoader = classLoader;
            this.classes = new HashSet<>();
            for (Class<?> clazz : classes) {
                this.classes.add(clazz);
            }
        }

        @Override
        public Set<Class<?>> getAnnotatedClasses(
                Class<? extends Annotation> annotation) {
            return classes.stream().filter(
                    cl -> cl.getAnnotationsByType(annotation).length > 0)
                    .collect(Collectors.toSet());
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
        @SuppressWarnings("unchecked")
        public <T> Set<Class<? extends T>> getSubTypesOf(Class<T> type) {
            return this.classes.stream()
                    .filter(cl -> GenericTypeReflector.isSuperType(type, cl)
                            && !type.equals(cl))
                    .map(cl -> (Class<T>) cl).collect(Collectors.toSet());
        }
    }

    /**
     * Implementation that proxy and cache a real <code>ClassFinder</code>.
     */
    class CachedClassFinder implements ClassFinder {

        private ClassFinder classFinder;

        private Map<Class<? extends Annotation>, Set<Class<?>>> annotatedClassesMapCache = new HashMap<>();

        /**
         * It uses specified classFinder and caches scanned annotation.
         *
         * @param classFinder
         *            A real classFinder.
         */
        public CachedClassFinder(ClassFinder classFinder) {
            this.classFinder = classFinder;
        }

        @Override
        public Set<Class<?>> getAnnotatedClasses(
                Class<? extends Annotation> annotation) {
            return annotatedClassesMapCache.computeIfAbsent(annotation,
                    key -> classFinder.getAnnotatedClasses(key));
        }

        @Override
        public URL getResource(String name) {
            return classFinder.getResource(name);
        }

        @Override
        public <T> Class<T> loadClass(String name)
                throws ClassNotFoundException {
            return classFinder.loadClass(name);
        }

        @Override
        public <T> Set<Class<? extends T>> getSubTypesOf(Class<T> type) {
            return classFinder.getSubTypesOf(type);
        }
    }

    /**
     * Get annotated classes in the classloader.
     *
     * @param clazz
     *            the annotation
     * @return a set with all classes that are annotated
     */
    Set<Class<?>> getAnnotatedClasses(Class<? extends Annotation> clazz);

    /**
     * Get annotated classes in the classloader.
     *
     * @param className
     *            the annotation class name
     * @return a set with all classes that are annotated
     * @throws ClassNotFoundException
     *             when the class not found
     */
    default Set<Class<?>> getAnnotatedClasses(String className)
            throws ClassNotFoundException {
        return getAnnotatedClasses(loadClass(className));
    }

    /**
     * Get a resource from the classpath.
     *
     * @param name
     *            class literal
     * @return the resource
     */
    URL getResource(String name);

    /**
     * Load a class in the classloader.
     *
     * @param name
     *            the class literal
     * @return the class
     * @throws ClassNotFoundException
     *             when the class is not in the classpath
     */
    <T> Class<T> loadClass(String name) throws ClassNotFoundException;

    /**
     * Gets all subtypes in hierarchy of a given type.
     *
     * @param type
     *            the type to search for the subtypes for
     * @param <T>
     *            the class of the type
     * @return set of the subtypes of the given class
     */
    <T> Set<Class<? extends T>> getSubTypesOf(final Class<T> type);

    /**
     * Gets all subtypes in hierarchy of a given type, using FQN string.
     *
     * @param name
     *            Fully qualified name of the type to search subtypes of
     * @param <T>
     *            the class of the type
     * @return set of the subtypes of the given class
     * @throws ClassNotFoundException
     *             when the parent class is not in the classpath
     */
    default <T> Set<Class<? extends T>> getSubTypesOf(final String name)
            throws ClassNotFoundException {
        Class<T> parent = loadClass(name);
        return getSubTypesOf(parent);
    }
}
