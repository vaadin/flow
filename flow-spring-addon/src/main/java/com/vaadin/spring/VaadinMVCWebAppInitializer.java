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

import java.util.Collection;
import java.util.stream.Stream;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

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
        registerConfiguration(context);
        servletContext.addListener(new ContextLoaderListener(context));

        ServletRegistration.Dynamic registration = servletContext
                .addServlet("dispatcher", new SpringServlet(context));
        registration.setLoadOnStartup(1);
        registration.addMapping("/*");
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
                Stream.of(VaadinScopesConfig.class),
                getConfigurationClasses().stream());
        context.register(configs.toArray(Class<?>[]::new));
    }

    /**
     * Gets the application configuration classes.
     *
     * @return a collection of configuration classes
     */
    protected abstract Collection<Class<?>> getConfigurationClasses();

}
