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
package com.vaadin.flow.migration;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.stream.Stream;

import com.vaadin.flow.server.frontend.scanner.ClassFinder;

/**
 * Abstract class which allows to find classes in the project classpath.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public abstract class ClassPathIntrospector implements Serializable {

    private final ClassFinder finder;

    /**
     * Creates a new instance of class.
     *
     * @param finder the ClassFinder instance to use for class searches.
     */
    protected ClassPathIntrospector(ClassFinder finder) {
        this.finder = finder;
    }

    /**
     * Create a new instance but reusing the instrospector.
     *
     * @param otherIntrospector
     *            the other instance.
     */
    public ClassPathIntrospector(ClassPathIntrospector otherIntrospector) {
        this.finder = otherIntrospector.finder;
    }

    /**
     * Returns a resource {@link URL} given a file name.
     *
     * @param name
     *            the name of the resource
     * @return the URL with the resource or null if not found
     */
    protected URL getResource(String name) {
        return finder.getResource(name);
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
        return finder.getAnnotatedClasses(annotationInProjectContext).stream();
    }

    /**
     * Gets the subtypes of the given {@code type}.
     *
     * @param type
     *            super type for subtype search
     * @return all subtypes of the given {@code type}
     */
    protected Stream<Class<?>> getSubtypes(Class<?> type) {
        return finder
            .getSubTypesOf(loadClassInProjectClassLoader(type.getName()))
            .stream();
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
    public <T> Class<T> loadClassInProjectClassLoader(String className) {
        try {
            return finder.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(String.format(
                    "Failed to load class '%s' in custom classloader",
                    className), e);
        }
    }

    /**
     * Get the value of the method {@code methodName} from the
     * {@code target} annotation.
     *
     * @param target
     *            the target annotation
     * @param methodName
     *            the method name to execute
     * @return the Value of the execution result
     */
    @SuppressWarnings("unchecked")
    protected <T> T invokeAnnotationMethod(Annotation target, String methodName) {
        return (T) doInvokeAnnotationMethod(target, methodName);
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
}
