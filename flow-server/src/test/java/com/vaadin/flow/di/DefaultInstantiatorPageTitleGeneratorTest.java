/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.di;

import net.jcip.annotations.NotThreadSafe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;

import com.vaadin.flow.router.PageTitleGenerator;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.VaadinService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@NotThreadSafe
class DefaultInstantiatorPageTitleGeneratorTest {
    private ClassLoader contextClassLoader;
    private ClassLoader classLoader;

    @BeforeEach
    void init() throws ClassNotFoundException {
        contextClassLoader = Thread.currentThread().getContextClassLoader();

        classLoader = Mockito.mock(ClassLoader.class);
        Mockito.when(classLoader.loadClass(Mockito.any()))
                .thenAnswer(AdditionalAnswers.delegatesTo(contextClassLoader));
        Thread.currentThread().setContextClassLoader(classLoader);
    }

    @AfterEach
    void destroy() {
        Thread.currentThread().setContextClassLoader(contextClassLoader);
    }

    @Test
    void getPageTitleGenerator_noProperty_returnsNull() {
        VaadinService service = Mockito.mock(VaadinService.class);
        DefaultInstantiator defaultInstantiator = new DefaultInstantiator(
                service);

        assertNull(defaultInstantiator.getPageTitleGenerator());
    }

    @Test
    void getPageTitleGenerator_customGenerator_isCreated()
            throws ClassNotFoundException {
        String customGeneratorClassName = "com.vaadin.flow.di.CustomPageTitleGenerator";

        VaadinService service = Mockito.mock(VaadinService.class);
        DefaultInstantiator defaultInstantiator = new DefaultInstantiator(
                service) {
            @Override
            protected String getInitProperty(String propertyName) {
                return customGeneratorClassName;
            }
        };

        PageTitleGenerator generator = defaultInstantiator
                .getPageTitleGenerator();
        assertInstanceOf(CustomPageTitleGenerator.class, generator);
        Mockito.verify(classLoader).loadClass(customGeneratorClassName);
    }

    @Test
    void getPageTitleGenerator_invalidType_throwsException() {
        VaadinService service = Mockito.mock(VaadinService.class);
        DefaultInstantiator defaultInstantiator = new DefaultInstantiator(
                service) {
            @Override
            protected String getInitProperty(String propertyName) {
                return "com.vaadin.flow.di.InvalidPageTitleGenerator";
            }
        };

        String errorMessage = assertThrows(IllegalStateException.class,
                () -> defaultInstantiator.getPageTitleGenerator()).getMessage();
        assertEquals("Page title generator class property '"
                + InitParameters.PAGE_TITLE_GENERATOR
                + "' is set to 'com.vaadin.flow.di.InvalidPageTitleGenerator' but it's not a "
                + PageTitleGenerator.class.getSimpleName() + " implementation",
                errorMessage);
    }
}
