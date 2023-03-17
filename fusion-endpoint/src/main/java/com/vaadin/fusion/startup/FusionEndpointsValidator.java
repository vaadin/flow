/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.startup;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;
import java.io.Serializable;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.ClassFinder.DefaultClassFinder;
import com.vaadin.flow.server.startup.ClassLoaderAwareServletContainerInitializer;
import com.vaadin.fusion.Endpoint;

/**
 * Validation class that is run during servlet container initialization which
 * checks that application is running with the appropriate spring dependencies
 * when there are {@link Endpoint} annotations.
 *
 * @since 3.0
 */
@HandlesTypes({ Endpoint.class })
public class FusionEndpointsValidator
        implements ClassLoaderAwareServletContainerInitializer, Serializable {

    private String classToCheck = "org.springframework.boot.autoconfigure.jackson.JacksonProperties";

    @Override
    public void process(Set<Class<?>> classSet, ServletContext servletContext)
            throws ServletException {

        if (classSet == null) {
            // This case happens when initializing in a CDI environment.
            //
            // We cannot check anything here to give a message.
            // Continue with the initialization, java will throw
            // the proper exception if application tries to use
            // an endpoint and dependencies are not added to the project.
            return;
        }

        ClassFinder finder = new DefaultClassFinder(classSet);
        Set<Class<?>> endpoints = finder.getAnnotatedClasses(Endpoint.class);
        if (!endpoints.isEmpty()) {
            try {
                finder.loadClass(classToCheck);
            } catch (ClassNotFoundException e) {
                throw new ServletException(
                        "ERROR: Vaadin endpoints only work for Spring "
                                + "enabled projects.\n"
                                + "This is not a spring application but there are Vaadin endpoints in these classes: "
                                + endpoints.stream().map(Class::getName)
                                        .collect(
                                                Collectors.joining("\n    - ")),
                        e);
            }
        }
    }

    void setClassToCheck(String className) {
        classToCheck = className;
    }
}
