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
package com.vaadin.flow.server.frontend.scanner;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mockito.Mockito;

import com.vaadin.flow.server.PWA;
import com.vaadin.flow.server.PwaConfiguration;

public class FullScannerPwaTest extends AbstractScannerPwaTest {
    private ClassFinder finder = Mockito.mock(ClassFinder.class);

    protected PwaConfiguration getPwaConfiguration(Class<?>... classes)
            throws Exception {
        // use this fake/mock class for the loaded class to check that annotated
        // classes are requested for the loaded class and not for the
        // annotationType
        Class clazz = Object.class;

        Mockito.doReturn(clazz).when(finder).loadClass(PWA.class.getName());

        Mockito.doReturn(getPwaAnnotatedClasses(classes)).when(finder)
                .getAnnotatedClasses(clazz);

        FullDependenciesScanner fullDependenciesScanner = new FullDependenciesScanner(
                finder, (type, annotation) -> findPwaAnnotations(type), null,
                true);
        return fullDependenciesScanner.getPwaConfiguration();
    }

    private Set<Class<?>> getPwaAnnotatedClasses(Class<?>[] classes) {
        Set<Class<?>> result = new HashSet<>();
        for (Class<?> clazz : classes) {
            if (clazz.getAnnotationsByType(PWA.class).length > 0) {
                result.add(clazz);
            }
        }

        return result;
    }

    private List<? extends Annotation> findPwaAnnotations(Class<?> type) {
        return Arrays.asList(type.getAnnotationsByType(PWA.class));
    }
}
