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
package com.vaadin.flow.spring;

import jakarta.servlet.MultipartConfigElement;

import java.util.HashMap;
import java.util.Map;

import org.atmosphere.cpr.ApplicationConfig;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.communication.JSR356WebsocketInitializer;
import com.vaadin.flow.spring.springnative.ClientCallableAotProcessor;
import com.vaadin.flow.spring.springnative.VaadinBeanFactoryInitializationAotProcessor;

/**
 * Spring boot auto-configuration class for Flow.
 *
 * @author Vaadin Ltd
 *
 * @since 10.0
 */
@AutoConfiguration(before = WebMvcAutoConfiguration.class)
@ConditionalOnClass(ServletContextInitializer.class)
@EnableConfigurationProperties(VaadinConfigurationProperties.class)
public class SpringBootAutoConfiguration {

    // By name, as @ConditionalOnMissingClass only accepts class names, so
    // that both conditions visibly check the same class
    private static final String SERVER_ENDPOINT_EXPORTER = "org.springframework.web.socket.server.standard.ServerEndpointExporter";

    @Autowired
    private WebApplicationContext context;

    /**
     * Creates the auto configuration. Spring instantiates it.
     */
    public SpringBootAutoConfiguration() {
        // Default constructor
    }

    @Bean
    static VaadinBeanFactoryInitializationAotProcessor flowBeanFactoryInitializationAotProcessor() {
        return new VaadinBeanFactoryInitializationAotProcessor();
    }

    @Bean
    static ClientCallableAotProcessor flowClientCallableFactoryInitializationAotProcessor() {
        return new ClientCallableAotProcessor();
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
     *            multipart configuration, if available
     * @param configurationProperties
     *            the vaadin configuration properties
     * @return a custom ServletRegistrationBean instance
     * @since 23.0.1
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

    /**
     * Configures a servlet registration for the given Vaadin servlet instance,
     * so that an application providing its own servlet bean gets the same url
     * mapping, push mapping and multipart setup as the default one.
     *
     * @param multipartConfig
     *            multipart configuration, if available
     * @param configurationProperties
     *            the vaadin configuration properties
     * @param servletInstance
     *            the servlet to register
     * @return the configured registration bean
     * @since 24.5.1
     */
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

        registration
                .setMultipartConfig(new MultipartConfigElement((String) null));
        return registration;
    }

    /**
     * Deploys JSR-356 websocket endpoints when Atmosphere is available. Only
     * active when the application has Spring WebSocket on the classpath, e.g.
     * through <code>spring-boot-starter-websocket</code>.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = SERVER_ENDPOINT_EXPORTER)
    static class WebsocketConfiguration {

        @Bean
        ServerEndpointExporter websocketEndpointDeployer() {
            return new VaadinWebsocketEndpointExporter();
        }
    }

    /**
     * Warns when push is available but its websocket endpoints can not be
     * deployed, because push then silently falls back to long polling.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingClass(SERVER_ENDPOINT_EXPORTER)
    static class MissingWebsocketConfiguration {

        MissingWebsocketConfiguration() {
            if (JSR356WebsocketInitializer.isAtmosphereAvailable()) {
                LoggerFactory.getLogger(SpringBootAutoConfiguration.class)
                        .warn("Spring WebSocket is not on the classpath, so "
                                + "push can not use websockets in an embedded "
                                + "server. Add spring-boot-starter-websocket "
                                + "to the application to enable them.");
            }
        }
    }

}
