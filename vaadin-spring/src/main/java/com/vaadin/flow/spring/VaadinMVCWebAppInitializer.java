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
package com.vaadin.flow.spring;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration.Dynamic;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.atmosphere.cpr.ApplicationConfig;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import com.vaadin.flow.server.InitParameters;

/**
 * Abstract Vaadin Spring MVC {@link WebApplicationInitializer}.
 * <p>
 * Extend this class in your Spring MVC application and provide your
 * configuration classes via the {@link #getConfigurationClasses()} method.
 *
 * @author Vaadin Ltd
 *
 */
public abstract class VaadinMVCWebAppInitializer
        implements WebApplicationInitializer {

    @Override
    public void onStartup(ServletContext servletContext)
            throws ServletException {
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(servletContext);
        registerConfiguration(context);
        servletContext.addListener(new ContextLoaderListener(context));
        context.refresh();
        Environment env = context.getBean(Environment.class);
        String mapping = RootMappedCondition.getUrlMapping(env);
        if (mapping == null) {
            mapping = "/*";
        }
        String pushRegistrationPath;

        boolean rootMapping = RootMappedCondition.isRootMapping(mapping);
        Dynamic registration = servletContext.addServlet(
                ClassUtils.getShortNameAsProperty(SpringServlet.class),
                new SpringServlet(context, rootMapping));
        Map<String, String> initParameters = new HashMap<>();
        boolean pushServletMappingEnabled = isPushServletMappingEnabled(
                context.getEnvironment());
        if (rootMapping) {
            Dynamic dispatcherRegistration = servletContext
                    .addServlet("dispatcher", new DispatcherServlet(context));
            dispatcherRegistration.addMapping("/*");

            if (pushServletMappingEnabled) {
                initParameters.put(InitParameters.SERVLET_PARAMETER_PUSH_URL,
                        makeContextRelative(mapping.replace("*", "")));
            }

            mapping = VaadinServletConfiguration.VAADIN_SERVLET_PUSH_MAPPING;
            pushRegistrationPath = "";
        } else {
            pushRegistrationPath = mapping.replace("/*", "");
        }
        registration.addMapping(mapping);

        /*
         * Tell Atmosphere which servlet to use for the push endpoint. Servlet
         * mappings are returned as a Set from at least Tomcat so even if
         * Atmosphere always picks the first, it might end up using /VAADIN/*
         * and websockets will fail.
         */

        if (pushServletMappingEnabled) {
            initParameters.put(ApplicationConfig.JSR356_MAPPING_PATH,
                    mapping.replace("/*", ""));
        } else {
            initParameters.put(ApplicationConfig.JSR356_MAPPING_PATH,
                    pushRegistrationPath);
        }

        registration.setInitParameters(initParameters);

        registration.setAsyncSupported(
                Boolean.TRUE.toString().equals(env.getProperty(
                        "vaadin.asyncSupported", Boolean.TRUE.toString())));
    }

    static String makeContextRelative(String url) {
        // / -> context://
        // foo -> context://foo
        // /foo -> context://foo
        if (url.startsWith("/")) {
            url = url.substring(1);
        }
        return "context://" + url;
    }

    /**
     * Registers application configuration classes.
     * <p>
     * Uses developer defined configuration classes via the
     * {@link #getConfigurationClasses()} method. Also register Vaadin
     * configuration from the add-on.
     * <p>
     * Override this method if you want to register configuration classes in a
     * totally different way or just provide implementation for
     * {@link #getConfigurationClasses()} method.
     *
     * @see #getConfigurationClasses()
     *
     * @param context
     *            web application context, not {@code null}
     */
    protected void registerConfiguration(
            AnnotationConfigWebApplicationContext context) {
        Stream<Class<? extends Object>> configs = Stream.concat(
                Stream.of(VaadinScopesConfig.class,
                        VaadinServletConfiguration.class,
                        VaadinApplicationConfiguration.class),
                getConfigurationClasses().stream());
        context.register(configs.toArray(Class<?>[]::new));
    }

    /**
     * Gets the application configuration classes.
     *
     * @return a collection of configuration classes
     */
    protected abstract Collection<Class<?>> getConfigurationClasses();

    /**
     * Gets the url mapping in a way compatible with both plain Spring and
     * Spring Boot.
     *
     * @param environment
     *            the application environment
     * @return the url mapping or null if none is defined
     */
    protected boolean isPushServletMappingEnabled(Environment environment) {
        if (SpringUtil.isSpringBoot()) {
            try {
                return (boolean) Class.forName(
                        "com.vaadin.flow.spring.VaadinConfigurationProperties")
                        .getMethod("isPushServletMappingEnabled",
                                Environment.class)
                        .invoke(null, environment);
            } catch (IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException
                    | SecurityException | ClassNotFoundException e) {
                LoggerFactory.getLogger(VaadinMVCWebAppInitializer.class).error(
                        "Unable to find isPushMappingEnabled from properties",
                        e);
                return false;
            }
        } else {
            return Boolean.parseBoolean(
                    environment.getProperty("vaadin.pushServletMapping"));
        }
    }

}
