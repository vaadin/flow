/*
 * Copyright 2000-2024 Vaadin Ltd.
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
package com.vaadin.flow.spring;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import com.vaadin.flow.i18n.DefaultI18NProvider;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.router.internal.ClientRoutesProvider;
import com.vaadin.flow.server.startup.ApplicationConfigurationFactory;
import com.vaadin.flow.spring.SpringLookupInitializer.SpringApplicationContextInit;
import com.vaadin.flow.spring.i18n.DefaultI18NProviderFactory;

/**
 * Vaadin Application Spring configuration.
 * <p>
 * Registers a default {@link ApplicationConfigurationFactory} for Vaadin web
 * application if there is no developer provided factory available.
 *
 * @author Vaadin Ltd
 * @since
 *
 */
@Configuration
public class VaadinApplicationConfiguration {

    /**
     * Creates a default {@link ApplicationConfigurationFactory} bean in case
     * there is no developer provided bean.
     *
     * @return the default application configuration factory
     */
    @Bean
    @ConditionalOnMissingBean
    public ApplicationConfigurationFactory defaultApplicationConfigurationFactory() {
        return new SpringApplicationConfigurationFactory();
    }

    /**
     * Creates an application context initializer for lookup initializer
     * {@link SpringLookupInitializer}.
     *
     * @return an application context initializer
     */
    @Bean
    public ApplicationContextAware vaadinApplicationContextInitializer() {
        return new SpringApplicationContextInit();
    }

    /**
     * Creates default {@link I18NProvider}. This is created only if there's no
     * {@link I18NProvider} bean declared.
     *
     * @return default I18N provider
     */
    @Bean
    @ConditionalOnMissingBean(value = I18NProvider.class)
    @Conditional(DefaultI18NProviderFactory.class)
    public DefaultI18NProvider vaadinI18nProvider(
            @Value("${vaadin.i18n.location-pattern:"
                    + DefaultI18NProviderFactory.DEFAULT_LOCATION_PATTERN
                    + "}") String locationPattern) {
        return DefaultI18NProviderFactory.create(locationPattern);
    }

}
