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
package com.vaadin.flow.spring.test;

import org.springdoc.webmvc.ui.SwaggerWebMvcConfigurer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

/**
 * Test configuration which holds the package: all UI classes in the dependent
 * module are inside this package.
 *
 * @author Vaadin Ltd
 *
 */
@Configuration
@EnableWebSecurity
@ComponentScan(basePackages = { "org.springdoc",
        "com.vaadin.flow.spring.test" }, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = ProfileEnableInitializer.class))
@Import({ DummyOAuth2Server.class,
        org.springdoc.core.SpringDocConfiguration.class,
        org.springdoc.webmvc.core.SpringDocWebMvcConfiguration.class,
        org.springdoc.webmvc.ui.SwaggerConfig.class,
        org.springdoc.core.SwaggerUiConfigProperties.class,
        org.springdoc.core.SwaggerUiOAuthProperties.class,
        org.springdoc.core.SpringDocConfigProperties.class,
        org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration.class })
@PropertySource("classpath:spring.properties")
public class TestConfiguration extends WebMvcConfigurationSupport {
    @Autowired
    private SwaggerWebMvcConfigurer swaggerWebMvcConfigurer;

    @Override
    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
        super.addResourceHandlers(registry);
        swaggerWebMvcConfigurer.addResourceHandlers(registry);

    }
}
