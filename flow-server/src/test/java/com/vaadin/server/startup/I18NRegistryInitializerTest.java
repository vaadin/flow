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
package com.vaadin.server.startup;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.lang.reflect.Field;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.mockito.internal.util.collections.Sets;

import com.vaadin.ui.i18n.I18NProvider;

/**
 * Test class for the I18NRegistry and I18NRegistryInitializer.
 */
public class I18NRegistryInitializerTest {

    private I18NRegistryInitializer registry;
    private ServletContext servletContext;

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Before
    public void init() throws NoSuchFieldException, IllegalAccessException {
        Field i18nRegistry = I18NRegistry.class
                .getDeclaredField("i18nProvider");
        i18nRegistry.setAccessible(true);
        i18nRegistry.set(I18NRegistry.getInstance(), new AtomicReference<>());

        registry = new I18NRegistryInitializer();
        servletContext = Mockito.mock(ServletContext.class);
    }

    public static class LangProvider implements I18NProvider {
        @Override
        public String getTranslation(String key, String... params) {
            return key;
        }

        @Override
        public String getTranslation(String key, Locale locale,
                String... params) {
            return key;
        }
    }

    public static class SecondLangProvider implements I18NProvider {
        @Override
        public String getTranslation(String key, String... params) {
            return key;
        }

        @Override
        public String getTranslation(String key, Locale locale,
                String... params) {
            return key;
        }
    }

    @Test
    public void onStartUp() throws ServletException {
        registry.onStartup(Sets.newSet(LangProvider.class), servletContext);

        Assert.assertEquals("Wrong provider was found from registry",
                LangProvider.class,
                I18NRegistry.getInstance().getProvider().getClass());
    }

    @Test
    public void onStartUp_no_exception_with_null_arguments() {
        try {
            registry.onStartup(null, servletContext);
        } catch (Exception e) {
            Assert.fail(
                    "I18NRegistryInitializer.onStartup should not throw with null arguments. "
                            + e.getMessage());
        }
    }

    @Test
    public void onStartUp_with_null_arguments_default_implementation_is_used()
            throws ServletException {
        expectedEx.expect(UnsupportedOperationException.class);
        expectedEx.expectMessage(
                "Implement an I18NProvider to get translation support.");

        registry.onStartup(null, servletContext);

        I18NRegistry.getInstance().getProvider().getTranslation("my_key");
    }

    @Test
    public void onStartUp_multiple_providers_throws() throws ServletException {
        expectedEx.expect(IllegalStateException.class);
        expectedEx.expectMessage("Only one I18NProvider should be defined.");

        registry.onStartup(
                Stream.of(LangProvider.class, SecondLangProvider.class)
                        .collect(Collectors.toSet()),
                servletContext);
    }
}
