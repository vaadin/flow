/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.test;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
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
@ComponentScan(excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = ProfileEnableInitializer.class))
@Import(DummyOAuth2Server.class)
public class TestConfiguration extends WebMvcConfigurationSupport {

}
