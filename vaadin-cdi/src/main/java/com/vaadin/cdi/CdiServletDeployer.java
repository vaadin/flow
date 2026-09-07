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
package com.vaadin.cdi;

import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.annotation.HandlesTypes;
import jakarta.servlet.annotation.WebListener;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.startup.ServletDeployer;

/**
 * Container initializer that automatically registers a CDI Vaadin servlet.
 *
 * The servlet is only registered if all of the following conditions apply:
 * <ul>
 * <li>At least one class annotated with {@link Route @Route} is found on the
 * classpath
 * <li>No servlet is registered for <code>/*</code>
 * <li>No Vaadin servlet is registered
 * </ul>
 * <p>
 * Vaadin ships with {@link ServletDeployer}, which is a {@link WebListener} to
 * register the default {@link VaadinServlet}. We can't override it from java,
 * but since this class is a container initializer, we run first. After
 * registration of the CDI Vaadin Servlet, the original deployer won't register
 * the default one.
 * <p>
 * The rest of this class is copied from {@link ServletDeployer}.
 */
@HandlesTypes(Route.class)
public class CdiServletDeployer implements ServletContainerInitializer {

    @Override
    public void onStartup(Set<Class<?>> classSet, ServletContext ctx) {
        if (classSet == null) {
            // ServletDeployer will log routes are missing.
            return;
        }

        ServletRegistration rootServlet = findRootServlet(ctx);
        if (rootServlet != null) {
            // ServletDeployer will log automatic servlet registration skipped.
            // Even if we registered successfully the CDI servlet.
            return;
        }

        ServletRegistration vaadinServlet = findVaadinServlet(ctx);
        if (vaadinServlet != null) {
            // ServletDeployer will log it.
            return;
        }

        String servletName = getClass().getName();
        ServletRegistration.Dynamic registration = ctx.addServlet(servletName,
                CdiVaadinServlet.class);
        if (registration == null) {
            // Not expected to ever happen
            // ServletDeployer have to fail too, and will log it
            return;
        }

        getLogger().info("Automatically deploying CDI Vaadin servlet to /*");

        registration.setAsyncSupported(true);
        registration.addMapping("/*");
        registration.setLoadOnStartup(1);
    }

    private static ServletRegistration findRootServlet(ServletContext context) {
        return context.getServletRegistrations().values().stream().filter(
                registration -> registration.getMappings().contains("/*"))
                .findAny().orElse(null);
    }

    private static ServletRegistration findVaadinServlet(
            ServletContext context) {
        return context.getServletRegistrations().values().stream()
                .filter(registration -> isVaadinServlet(
                        context.getClassLoader(), registration))
                .findAny().orElse(null);
    }

    private static boolean isVaadinServlet(ClassLoader classLoader,
            ServletRegistration registration) {
        String className = registration.getClassName();
        try {
            return VaadinServlet.class
                    .isAssignableFrom(classLoader.loadClass(className));
        } catch (ClassNotFoundException e) {
            getLogger().info("Assuming {} is not a Vaadin servlet", className,
                    e);
            return false;
        }
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(CdiServletDeployer.class);
    }

}
