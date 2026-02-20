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
package com.vaadin.flow.spring.i18n;

import jakarta.servlet.ServletException;

import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.spring.instantiator.SpringInstantiatorTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@Import(PrimaryI18NProviderInstantiationTest.I18NTestConfig.class)
class PrimaryI18NProviderInstantiationTest {

    @Autowired
    private ApplicationContext context;

    @Configuration
    @ComponentScan(useDefaultFilters = false, includeFilters = {
            @ComponentScan.Filter(classes = { I18NTestProvider.class,
                    I18NTestProvider1.class,
                    PrimaryI18NTestProvider.class }, type = FilterType.ASSIGNABLE_TYPE) })
    public static class I18NTestConfig {

    }

    @Component
    public static class I18NTestProvider implements I18NProvider {

        @Override
        public List<Locale> getProvidedLocales() {
            return null;
        }

        @Override
        public String getTranslation(String key, Locale locale,
                Object... params) {
            return null;
        }

    }

    @Component
    public static class I18NTestProvider1 extends I18NTestProvider {

    }

    @Primary
    @Component
    public static class PrimaryI18NTestProvider extends I18NTestProvider {

    }

    public static class DefaultI18NTestProvider extends I18NTestProvider {

    }

    @Test
    void getI18NProvider_usePrimaryBean() throws ServletException {
        Instantiator instantiator = SpringInstantiatorTest
                .getService(context, null).getInstantiator();

        assertNotNull(instantiator.getI18NProvider());
        assertEquals(PrimaryI18NTestProvider.class,
                instantiator.getI18NProvider().getClass());
    }

    @Test
    void getI18NProvider_givenDefaultI18NProvider_usePrimaryBean()
            throws ServletException {
        Instantiator instantiator = getInstantiator(context);

        assertNotNull(instantiator.getI18NProvider());
        assertEquals(PrimaryI18NTestProvider.class,
                instantiator.getI18NProvider().getClass());
    }

    public static Instantiator getInstantiator(ApplicationContext context)
            throws ServletException {
        Properties properties = new Properties();
        properties.put(InitParameters.I18N_PROVIDER,
                DefaultI18NTestProvider.class.getName());
        return SpringInstantiatorTest.getService(context, properties)
                .getInstantiator();
    }
}
