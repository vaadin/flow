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
package com.vaadin.spring;

import static com.vaadin.spring.VaadinServletConfiguration.VAADIN_URL_MAPINGS;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Spring boot auto-configuration class for Flow.
 *
 * @author Vaadin Ltd
 *
 */
@Configuration
@AutoConfigureBefore(WebMvcAutoConfiguration.class)
@ConditionalOnClass(ServletContextInitializer.class)
@EnableConfigurationProperties(VaadinConfigurationProperties.class)
@Import(VaadinServletConfiguration.class)
public class SpringBootAutoConfiguration {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private VaadinConfigurationProperties configurationProperties;

    /**
     * Creates a {@link ServletContextInitializer} instance.
     *
     * @return a custom ServletContextInitializer instance
     */
    @Bean
    public ServletContextInitializer contextInitializer() {
        return new VaadinServletContextInitializer(context);
    }

    /**
     * Creates a {@link ServletRegistrationBean} instance with Spring aware
     * Vaadin servlet.
     *
     * @return a custom ServletRegistrationBean instance
     */
    @Bean
    public ServletRegistrationBean<SpringServlet> servletRegistrationBean() {
        String[] mappings = new String[VAADIN_URL_MAPINGS.length + 1];
        String mapping = configurationProperties.getUrlMapping();
        if (RootMappedCondition.isRootMapping(mapping)) {
            mappings[0] = VaadinServletConfiguration.VAADIN_SERVLET_MAPPING;
        } else {
            mappings[0] = mapping;
        }
        System.arraycopy(VAADIN_URL_MAPINGS, 0, mappings, 1,
                VAADIN_URL_MAPINGS.length);
        ServletRegistrationBean<SpringServlet> registration = new ServletRegistrationBean<>(
                new SpringServlet(context), mappings);
        registration
                .setAsyncSupported(configurationProperties.isAsyncSupported());
        registration.setName(
                ClassUtils.getShortNameAsProperty(SpringServlet.class));
        return registration;
    }

    /**
     * Creates a {@link ServletRegistrationBean} instance for a dispatcher
     * servlet in case Vaadin servlet is mapped to the root.
     * <p>
     * This is needed for correct servlet path (and path info) values available
     * in Vaadin servlet because it works via forwarding controller which is not
     * properly mapped without this registration.
     *
     * @return a custom ServletRegistrationBean instance for dispatcher servlet
     */
    @Bean
    @Conditional(RootMappedCondition.class)
    public ServletRegistrationBean<DispatcherServlet> dispatcherServletRegistration() {
        DispatcherServlet servlet = context.getBean(DispatcherServlet.class);
        ServletRegistrationBean<DispatcherServlet> registration = new ServletRegistrationBean<>(
                servlet, "/*");
        registration.setName("dispatcher");
        return registration;
    }
}
