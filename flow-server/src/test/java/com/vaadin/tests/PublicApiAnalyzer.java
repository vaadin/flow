/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.tests;

import java.lang.reflect.Method;
import java.util.stream.Stream;

public class PublicApiAnalyzer {
    private PublicApiAnalyzer() {
        // Static methods only
    }

    public static Stream<Method> findNewPublicMethods(Class<?> type) {
        return Stream.of(type.getMethods())
                .filter(method -> method.getDeclaringClass() == type)
                .filter(method -> !isOverrideMethod(method));
    }

    private static boolean isOverrideMethod(Method method) {
        Class<?> declaringClass = method.getDeclaringClass();
        return Stream
                .concat(Stream.of(declaringClass.getSuperclass()),
                        Stream.of(declaringClass.getInterfaces()))
                .anyMatch(superType -> {
                    try {
                        superType.getMethod(method.getName(),
                                method.getParameterTypes());
                        return true;
                    } catch (NoSuchMethodException ignore) {
                        return false;
                    }
                });
    }
}
