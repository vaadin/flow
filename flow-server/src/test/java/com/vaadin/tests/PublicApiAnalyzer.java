/*
 * Copyright 2000-2019 Vaadin Ltd.
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
