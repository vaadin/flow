/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring;

import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

public class SpringBootAutoConfigurationConditionalTest {

    @Test
    public void customServletRegistrationBean() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations
                        .of(SpringBootAutoConfiguration.class))
                .withUserConfiguration(
                        ServletRegistrationBeanConfiguration.class)
                .run(context -> assertThat(context)
                        .getBean(ServletRegistrationBean.class).isInstanceOf(
                                ServletRegistrationBeanConfiguration.MockServletRegistrationBean.class));
    }

    @Configuration(proxyBeanMethods = false)
    public static class ServletRegistrationBeanConfiguration {

        @Bean
        public ServletRegistrationBean<SpringServlet> servletRegistrationBean(
                final WebApplicationContext webApplicationContext) {
            return new MockServletRegistrationBean(webApplicationContext);
        }

        public static class MockServletRegistrationBean
                extends ServletRegistrationBean<SpringServlet> {

            public MockServletRegistrationBean(
                    final WebApplicationContext webApplicationContext) {
                super(new SpringServlet(webApplicationContext, false));
            }
        }

    }
}
