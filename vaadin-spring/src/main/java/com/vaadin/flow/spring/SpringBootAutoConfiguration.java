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

import jakarta.servlet.MultipartConfigElement;

import java.util.HashMap;
import java.util.Map;

import org.atmosphere.cpr.ApplicationConfig;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.spring.springnative.VaadinBeanFactoryInitializationAotProcessor;

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
@Import({ VaadinApplicationConfiguration.class,
        VaadinServletConfiguration.class })
public class SpringBootAutoConfiguration {

    @Autowired
    private WebApplicationContext context;

    @Bean
    static VaadinBeanFactoryInitializationAotProcessor flowBeanFactoryInitializationAotProcessor() {
        return new VaadinBeanFactoryInitializationAotProcessor();
    }

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
     * @param multipartConfig
     *                                multipart configuration, if available
     * @param configurationProperties
     *                                the vaadin configuration properties
     * @return a custom ServletRegistrationBean instance
     */
    @Bean
    @ConditionalOnMissingBean(value = SpringServlet.class, parameterizedContainer = ServletRegistrationBean.class)
    public ServletRegistrationBean<SpringServlet> servletRegistrationBean(
            ObjectProvider<MultipartConfigElement> multipartConfig,
            VaadinConfigurationProperties configurationProperties) {
        boolean rootMapping = RootMappedCondition
                .isRootMapping(configurationProperties.getUrlMapping());
        return configureServletRegistrationBean(multipartConfig,
                configurationProperties,
                new SpringServlet(context, rootMapping));
    }

    public static ServletRegistrationBean<SpringServlet> configureServletRegistrationBean(
            ObjectProvider<MultipartConfigElement> multipartConfig,
            VaadinConfigurationProperties configurationProperties,
            SpringServlet servletInstance) {

        String mapping = configurationProperties.getUrlMapping();
        boolean rootMapping = RootMappedCondition.isRootMapping(mapping);
        Map<String, String> initParameters = new HashMap<>();
        if (rootMapping) {
            mapping = VaadinServletConfiguration.VAADIN_SERVLET_MAPPING;
            initParameters.put(
                    VaadinServlet.INTERNAL_VAADIN_SERVLET_VITE_DEV_MODE_FRONTEND_PATH,
                    "");
        }

        String pushUrl = rootMapping ? "" : mapping.replace("/*", "");
        pushUrl += "/" + Constants.PUSH_MAPPING;

        initParameters.put(ApplicationConfig.JSR356_MAPPING_PATH, pushUrl);

        ServletRegistrationBean<SpringServlet> registration = new ServletRegistrationBean<>(
                servletInstance, mapping);
        registration.setInitParameters(initParameters);
        registration
                .setAsyncSupported(configurationProperties.isAsyncSupported());
        registration.setName(
                ClassUtils.getShortNameAsProperty(SpringServlet.class));
        // Setup multi part form processing for non root servlet mapping to be
        // able to process Hilla login out of the box
        if (!rootMapping) {
            multipartConfig.ifAvailable(registration::setMultipartConfig);
        }
        registration.setLoadOnStartup(
                configurationProperties.isLoadOnStartup() ? 1 : -1);

        registration.setMultipartConfig(new MultipartConfigElement((String) null));
        return registration;
    }

    /**
     * Deploys JSR-356 websocket endpoints when Atmosphere is available.
     *
     * @return the server endpoint exporter which does the actual work.
     */
    @Bean
    public ServerEndpointExporter websocketEndpointDeployer() {
        return new VaadinWebsocketEndpointExporter();
    }

}
