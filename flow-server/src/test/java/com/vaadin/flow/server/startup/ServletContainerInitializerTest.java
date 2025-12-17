/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.server.startup;

import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.testutil.ClassFinder;

/**
 * Checks that any class which implements {@link ServletContainerInitializer}
 * implements {@link FixedServletContainerInitializer} instead and doesn't
 * override
 * {@link ServletContainerInitializer#onStartup(java.util.Set, jakarta.servlet.ServletContext)}
 */
public class ServletContainerInitializerTest extends ClassFinder {

    @Test
    public void servletContextHasNoLookup_deferredServletContextInitializersAttributeIsSet_processIsNotExecuted()
            throws ServletException {
        AtomicBoolean processIsExecuted = new AtomicBoolean();
        ClassLoaderAwareServletContainerInitializer initializer = new ClassLoaderAwareServletContainerInitializer() {

            @Override
            public void process(Set<Class<?>> set, ServletContext ctx)
                    throws ServletException {
                processIsExecuted.set(true);
            }

        };
        ServletContext context = Mockito.mock(ServletContext.class);
        initializer.onStartup(Collections.emptySet(), context);

        Mockito.verify(context).setAttribute(
                Mockito.eq(DeferredServletContextInitializers.class.getName()),
                Mockito.any());

        Assert.assertFalse(processIsExecuted.get());
    }

    @Test
    public void servletContextHasLookup_deferredServletContextInitializersAttributeIsNotSet_processIsExecuted()
            throws ServletException {
        AtomicBoolean processIsExecuted = new AtomicBoolean();
        ClassLoaderAwareServletContainerInitializer initializer = new ClassLoaderAwareServletContainerInitializer() {

            @Override
            public void process(Set<Class<?>> set, ServletContext ctx)
                    throws ServletException {
                processIsExecuted.set(true);
            }

        };
        ServletContext context = Mockito.mock(ServletContext.class);
        Mockito.when(context.getAttribute(Lookup.class.getName()))
                .thenReturn(Mockito.mock(Lookup.class));

        Mockito.when(context.getClassLoader())
                .thenReturn(initializer.getClass().getClassLoader());
        initializer.onStartup(Collections.emptySet(), context);

        Mockito.verify(context, Mockito.times(0)).setAttribute(
                Mockito.eq(DeferredServletContextInitializers.class.getName()),
                Mockito.any());

        Assert.assertTrue(processIsExecuted.get());
    }

    @Test
    public void anyServletContainerInitializerSubclassImplementsFixedServletContainerInitializer()
            throws IOException {
        List<String> rawClasspathEntries = getRawClasspathEntries();

        List<Pattern> excludes = getExcludedPatterns().map(Pattern::compile)
                .collect(Collectors.toList());

        List<String> classes = new ArrayList<>();
        for (String location : rawClasspathEntries) {
            if (!isTestClassPath(location)) {
                classes.addAll(findServerClasses(location, excludes));
            }
        }

        List<String> brokenInitializers = new ArrayList<>();
        for (String className : classes) {
            if (className
                    .equals(ClassLoaderAwareServletContainerInitializer.class
                            .getName())) {
                continue;
            }
            try {
                Class<?> clazz = Class.forName(className);
                // skip annotations and synthetic classes
                if (clazz.isAnnotation() || clazz.isSynthetic()) {
                    continue;
                }

                if (ServletContainerInitializer.class.isAssignableFrom(clazz)
                        && isBadSubType(clazz)) {
                    brokenInitializers.add(className);
                }
            } catch (ClassNotFoundException ignore) {
                // ignore
            }
        }
        Assert.assertTrue(
                brokenInitializers + " classes are subtypes of "
                        + ServletContainerInitializer.class
                        + " but either are not subtypes of "
                        + ClassLoaderAwareServletContainerInitializer.class
                        + " or override "
                        + ClassLoaderAwareServletContainerInitializer.class
                                .getName()
                        + ".onStartup method",
                brokenInitializers.isEmpty());

    }

    private Stream<String> getExcludedPatterns() {
        return Stream.of("com\\.vaadin\\.flow\\..*osgi\\..*",
                "com\\.vaadin\\.flow\\.server\\.startup\\.LookupInitializer\\$OsgiLookupImpl",
                // Shaded third-party libraries
                "com\\.vaadin\\.frontendtools\\.shaded\\..*");
    }

    private boolean isBadSubType(Class<?> clazz) {
        if (!ClassLoaderAwareServletContainerInitializer.class
                .isAssignableFrom(clazz)) {
            return true;
        }
        Method onStartUpMethod = Stream
                .of(ServletContainerInitializer.class.getDeclaredMethods())
                .filter(method -> !method.isSynthetic()).findFirst().get();
        return Stream.of(clazz.getDeclaredMethods())
                .anyMatch(method -> sameSignature(method, onStartUpMethod));
    }

    private boolean sameSignature(Method method1, Method method2) {
        return method1.getName().equals(method2.getName())
                && method1.getReturnType().equals(method2.getReturnType())
                && Arrays.asList(method1.getParameterTypes())
                        .equals(Arrays.asList(method2.getParameterTypes()));
    }
}
