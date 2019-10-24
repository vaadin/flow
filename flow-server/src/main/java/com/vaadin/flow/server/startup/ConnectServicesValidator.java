/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.server.startup;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;

import java.io.Serializable;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.flow.server.connect.VaadinService;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.ClassFinder.DefaultClassFinder;

/**
 * Validation class that is run during servlet container initialization which
 * checks that application is running with the appropriate spring dependencies
 * when there are {@link VaadinService} annotations.
 *
 * @since 3.0
 */
@HandlesTypes({ VaadinService.class })
public class ConnectServicesValidator implements ServletContainerInitializer, Serializable {

    private String classToCheck = "org.springframework.boot.autoconfigure.jackson.JacksonProperties";

    @Override
    public void onStartup(Set<Class<?>> classSet, ServletContext servletContext)
            throws ServletException {

        if (classSet == null) {
            // This case happens when initializing in a CDI environment.
            //
            // We cannot check anything here to give a message.
            // Continue with the initialization, java will throw
            // the proper exception if application tries to use
            // a service and dependencies are not added to the project.
            return;
        }

        ClassFinder finder = new DefaultClassFinder(classSet);
        Set<Class<?>> services = finder.getAnnotatedClasses(VaadinService.class);
        if (!services.isEmpty()) {
            try {
                finder.loadClass(classToCheck);
            } catch (ClassNotFoundException e) {
                throw new ServletException(
                        "ERROR: Vaadin Connect Services only work for Spring enabled projects.\n"
                                + "This is not a spring application but there are connect services in these classes: "
                                + services.stream()
                                        .map(clazz -> clazz.getName())
                                        .collect(Collectors
                                                .joining("\n    - ")), e);
            }
        }
    }

    void setClassToCheck(String className) {
        classToCheck = className;
    }
}
