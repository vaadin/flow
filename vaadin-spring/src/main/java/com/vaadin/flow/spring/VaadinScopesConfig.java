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

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.vaadin.flow.spring.scopes.VaadinRouteScope;
import com.vaadin.flow.spring.scopes.VaadinSessionScope;
import com.vaadin.flow.spring.scopes.VaadinUIScope;

/**
 * Vaadin scopes configuration.
 *
 * @author Vaadin Ltd
 *
 */
@Configuration
public class VaadinScopesConfig {

    /**
     * Creates a Vaadin session scope.
     *
     * @return the Vaadin session scope
     */
    @Bean
    public static BeanFactoryPostProcessor vaadinSessionScope() {
        return new VaadinSessionScope();
    }

    /**
     * Creates a Vaadin UI scope.
     *
     * @return the Vaadin UI scope
     */
    @Bean
    public static BeanFactoryPostProcessor vaadinUIScope() {
        return new VaadinUIScope();
    }

    /**
     * Creates a Vaadin route scope.
     *
     * @return the Vaadin route scope
     */
    @Bean
    public static BeanFactoryPostProcessor vaadinRouteScope() {
        return new VaadinRouteScope();
    }
}
