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
package com.vaadin.flow.spring.i18n;

import java.util.List;
import java.util.Locale;
import java.util.Properties;

import javax.servlet.ServletException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit4.SpringRunner;

import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.spring.instantiator.SpringInstantiatorTest;

@RunWith(SpringRunner.class)
@Import(I18NProviderInstantiationTest.I18NTestConfig.class)
public class I18NProviderInstantiationTest {

    @Autowired
    private ApplicationContext context;

    @Configuration
    @ComponentScan
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

    public static class DefaultI18NTestProvider extends I18NTestProvider {

    }

    @Test
    public void getI18NProvider_i18nProviderIsABean_i18nProviderIsAvailable()
            throws ServletException {
        Instantiator instantiator = getInstantiator(context);

        Assert.assertNotNull(instantiator.getI18NProvider());
        Assert.assertEquals(DefaultI18NTestProvider.class,
                instantiator.getI18NProvider().getClass());
    }

    public static Instantiator getInstantiator(ApplicationContext context)
            throws ServletException {
        Properties properties = new Properties();
        properties.put(Constants.I18N_PROVIDER,
                DefaultI18NTestProvider.class.getName());
        return SpringInstantiatorTest.getService(context, properties)
                .getInstantiator();
    }
}
