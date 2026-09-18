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
import java.util.Optional;
import java.util.function.Consumer;

import org.atmosphere.cpr.ApplicationConfig;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.spring.bootstrap.VaadinAsyncInitFutureRegistry;
import com.vaadin.flow.spring.bootstrap.VaadinDefaultAsyncInitFutureRegistry;
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

    private static final Logger LOG = LoggerFactory
            .getLogger(SpringBootAutoConfiguration.class);

    @Autowired
    private WebApplicationContext context;

    @Bean
    static VaadinBeanFactoryInitializationAotProcessor flowBeanFactoryInitializationAotProcessor() {
        return new VaadinBeanFactoryInitializationAotProcessor();
    }

    @Bean
    static ClientCallableAotProcessor flowClientCallableFactoryInitializationAotProcessor() {
        return new ClientCallableAotProcessor();
    }

    @Bean
    @ConditionalOnProperty(name = "vaadin.bootstrap.async")
    @ConditionalOnMissingBean
    public VaadinAsyncInitFutureRegistry vaadinWaitForAsyncInitContainer() {
        return new VaadinDefaultAsyncInitFutureRegistry();
    }

    /**
     * Creates a {@link ServletContextInitializer} instance.
     *
     * @return a custom ServletContextInitializer instance
     */
    @Bean
    @ConditionalOnMissingBean
    public VaadinServletContextInitializer contextInitializer(
            Map<String, AsyncTaskExecutor> taskExecutors,
            Optional<VaadinAsyncInitFutureRegistry> optVaadinAsyncInitFutureRegistry) {
        Consumer<Runnable> contextInitializedExecutor = optVaadinAsyncInitFutureRegistry
                .<Consumer<Runnable>> map(reg -> {
                    final AsyncTaskExecutor asyncTaskExecutor = determineBootstrapExecutor(
                            taskExecutors);
                    if (asyncTaskExecutor == null) {
                        return null;
                    }
                    LOG.debug("Will use async bootstrap using {}",
                            asyncTaskExecutor);
                    return asyncTaskExecutor::submit;
                }).orElseGet(() -> Runnable::run);
        return new VaadinServletContextInitializer(context,
                contextInitializedExecutor);
    }

    // NOTE: Based on JpaBaseConfiguration
    protected @Nullable AsyncTaskExecutor determineBootstrapExecutor(
            Map<String, AsyncTaskExecutor> taskExecutors) {
        return taskExecutors.size() == 1
                ? taskExecutors.values().iterator().next()
                : taskExecutors.get(
                        TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME);
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
            VaadinConfigurationProperties configurationProperties,
            Optional<VaadinAsyncInitFutureRegistry> optVaadinAsyncInitFutureRegistry) {
        boolean rootMapping = RootMappedCondition
                .isRootMapping(configurationProperties.getUrlMapping());
        return configureServletRegistrationBean(multipartConfig,
                configurationProperties, new SpringServlet(context, rootMapping,
                        optVaadinAsyncInitFutureRegistry));
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

        registration
                .setMultipartConfig(new MultipartConfigElement((String) null));
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
