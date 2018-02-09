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
import java.util.Set;
import java.util.stream.Stream;

import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;

/**
 * Abstract class which allows to find classes in the project classpath.
 *
 * @author Vaadin Ltd
 *
 */
public abstract class ClassPathIntrospector {

    private final ClassLoader projectClassLoader;
    private final Reflections reflections;

    /**
     * Creates a new instance of class path introspector using the
     * {@code projectClassesLocations}.
     *
     * @param projectClassesLocations
     *            urls to project class locations (directories, jars etc.)
     */
    protected ClassPathIntrospector(URL... projectClassesLocations) {
        projectClassLoader = new URLClassLoader(projectClassesLocations, null);
        reflections = new Reflections(
                new ConfigurationBuilder().addClassLoader(projectClassLoader)
                        .addUrls(projectClassesLocations));
    }

    /**
     * Creates a new instance of class path introspector using the
     * {@code otherIntrpespector}'s reflection tools.
     *
     * @param otherIntrospector
     *            the introspector whose reflection tools will be reused
     */
    protected ClassPathIntrospector(ClassPathIntrospector otherIntrospector) {
        projectClassLoader = otherIntrospector.projectClassLoader;
        reflections = otherIntrospector.reflections;
    }

    /**
     * Gets the classes annotated with the given
     * {@code annotationInProjectContext} annotation.
     *
     * @param annotationInProjectContext
     *            annotation class
     * @return all classes annotated with the given annotation
     */
    protected Stream<Class<?>> getAnnotatedClasses(
            Class<? extends Annotation> annotationInProjectContext) {
        Set<Class<?>> annotatedBySingleAnnotation = reflections
                .getTypesAnnotatedWith(annotationInProjectContext, true);
        Set<Class<?>> annotatedByRepeatedAnnotation = getAnnotatedByRepeatedAnnotation(
                annotationInProjectContext);
        return Stream.concat(annotatedBySingleAnnotation.stream(),
                annotatedByRepeatedAnnotation.stream());
    }

    /**
     * Returns the annotation class by its FQN.
     * <p>
     * Note that the resulting class is loaded by project classloader ( not the
     * maven plugin classloader).
     *
     * @param className
     *            the FQN of the class to load
     * @param <T>
     *            the class to be loaded
     *
     * @return the class with the given {@code className} loaded by the project
     *         class loader
     */
    @SuppressWarnings("unchecked")
    protected <T> Class<T> loadClassInProjectClassLoader(String className) {
        try {
            return (Class<T>) projectClassLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(String.format(
                    "Failed to load class '%s' in custom classloader",
                    className), e);
        }
    }

    /**
     * Get the String value of the method {@code methodName} from the
     * {@code target} annotation.
     *
     * @param target
     *            the target annotation
     * @param methodName
     *            the method name to execute
     * @return the String representation of the execution result
     */
    protected String invokeAnnotationMethod(Annotation target,
            String methodName) {
        return String.valueOf(doInvokeAnnotationMethod(target, methodName));
    }

    /**
     * Get the value of the method {@code methodName} from the {@code target}
     * annotation.
     *
     * @param target
     *            the target annotation
     * @param methodName
     *            the method name to execute
     * @return the result of method {@code methodName} invocation on the
     *         {@code target}
     */
    protected Object doInvokeAnnotationMethod(Annotation target,
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
}
