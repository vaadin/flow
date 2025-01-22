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
package com.vaadin.flow.spring;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import com.vaadin.flow.i18n.DefaultI18NProvider;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.server.auth.DefaultMenuAccessControl;
import com.vaadin.flow.server.auth.MenuAccessControl;
import com.vaadin.flow.server.startup.ApplicationConfigurationFactory;
import com.vaadin.flow.spring.SpringLookupInitializer.SpringApplicationContextInit;
import com.vaadin.flow.spring.i18n.DefaultI18NProviderFactory;
import com.vaadin.flow.spring.security.SpringMenuAccessControl;

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

    /**
     * Creates default {@link MenuAccessControl}. This is created only if
     * there's no {@link MenuAccessControl} bean declared.
     *
     * @return default menu access control
     */
    @Bean
    @ConditionalOnMissingBean(value = MenuAccessControl.class)
    @ConditionalOnMissingClass("org.springframework.security.core.context.SecurityContextHolder")
    public MenuAccessControl vaadinMenuAccessControl() {
        return new DefaultMenuAccessControl();
    }

    /**
     * Creates default {@link MenuAccessControl}. This is created only if
     * there's no {@link MenuAccessControl} bean declared.
     *
     * @return default menu access control
     */
    @Bean
    @ConditionalOnMissingBean(value = MenuAccessControl.class)
    @ConditionalOnClass(name = "org.springframework.security.core.context.SecurityContextHolder")
    public MenuAccessControl springSecurityVaadinMenuAccessControl() {
        return new SpringMenuAccessControl();
    }

}
