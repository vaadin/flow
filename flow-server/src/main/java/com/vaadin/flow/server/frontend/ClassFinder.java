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
package com.vaadin.flow.server.frontend;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Interface for annotated class searches.
 */
public interface ClassFinder extends Serializable {

    /**
     * Implementation that searchs for annotated classes in a list of classes.
     */
    class DefaultClassFinder implements ClassFinder {
        private final Set<Class<?>> annotatedClasses;

        private final transient ClassLoader classLoader;

        /**
         * It uses current classloader for getting resources or loading classes.
         *
         * @param annotatedClasses The annotated classes.
         */
        public DefaultClassFinder(Set<Class<?>> annotatedClasses) {
            this.annotatedClasses = annotatedClasses;
            this.classLoader = getClass().getClassLoader();
        }

        /**
         * ClassFinder using a specified <code>ClassLoader</code> to load
         * classes and a list of classes where to look for annotations.
         *
         * @param classLoader
         *            classloader for getting resources or loading classes.
         * @param annotatedClasses
         *            classes where to look for annotations.
         */
        public DefaultClassFinder(ClassLoader classLoader,
                Class<?>... annotatedClasses) {
            this.classLoader = classLoader;
            this.annotatedClasses = new HashSet<>();
            for (Class<?> clazz : annotatedClasses) {
                this.annotatedClasses.add(clazz);
            }
        }

        @Override
        public Set<Class<?>> getAnnotatedClasses(
                Class<? extends Annotation> annotation) {
            return annotatedClasses.stream().filter(
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
        public <T> Set<Class<? extends T>> getSubTypesOf(Class<T> type) {
            throw new IllegalStateException("Unimplemented");
        }
    }

    /**
     * Get annotated classes in the classloader.
     *
     * @param clazz the annotation
     * @return a set with all classes that are annotated
     */
    Set<Class<?>> getAnnotatedClasses(Class<? extends Annotation> clazz);

    /**
     * Get a resource from the classpath.
     *
     * @param name class literal
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
     * @param type the type to search for the subtypes for
     * @param <T> the class of the type
     * @return set of the subtypes of the given class
     */
    <T> Set<Class<? extends T>> getSubTypesOf(final Class<T> type);

    /**
     * Get the value of the the method {@code methodName} from the
     * {@code instance} with the given {@code arguments}.
     *
     * @param instance
     *            instance with the method to invoke
     * @param methodName
     *            the method name
     * @param arguments
     *            a variable list with the arguments for the method invocation
     * @return the object resulting from the method invocation
     */
    @SuppressWarnings("unchecked")
    default <T> T invoke(Object instance, String methodName, Object... arguments) {
        try {
            for (Method m : instance.getClass().getMethods()) {
                if (m.getName().equals(methodName)) {
                    return (T) m.invoke(instance, arguments);
                }
            }
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException e) {
            throw new IllegalArgumentException(e);
        }
        return null;
    }
}
