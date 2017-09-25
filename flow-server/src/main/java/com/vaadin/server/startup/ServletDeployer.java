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
package com.vaadin.server.startup;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.WebListener;

import com.vaadin.router.Route;
import com.vaadin.server.Constants;
import com.vaadin.server.VaadinServlet;

/**
 * Context listener that automatically registers a Vaadin servlet. The servlet
 * is only registered if all of the following conditions apply:
 * <ul>
 * <li>At least one class annotated with {@link Route @Route} is found on the
 * classpath
 * <li>No servlet is registered for <code>/*</code>
 * <li>No Vaadin servlet is registered
 * </ul>
 *
 * @author Vaadin Ltd
 */
@WebListener
public class ServletDeployer implements ServletContextListener {

    private static final String SKIPPING_AUTOMATIC_SERVLET_REGISTRATION_BECAUSE = "Skipping automatic servlet registration because ";

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        if (!RouteRegistry.getInstance(sce.getServletContext())
                .hasNavigationTargets()) {
            getLogger()
                    .config(() -> SKIPPING_AUTOMATIC_SERVLET_REGISTRATION_BECAUSE
                            + "there are no navigation targets annotated with @Route");
            return;
        }

        ServletContext servletContext = sce.getServletContext();

        ServletRegistration rootServlet = findRootServlet(servletContext);
        if (rootServlet != null) {
            getLogger()
                    .config(() -> SKIPPING_AUTOMATIC_SERVLET_REGISTRATION_BECAUSE
                            + "there is already a /* servlet with the name "
                            + rootServlet.getName());
            return;
        }

        ServletRegistration vaadinServlet = findVaadinServlet(servletContext);
        if (vaadinServlet != null) {
            getLogger()
                    .config(() -> SKIPPING_AUTOMATIC_SERVLET_REGISTRATION_BECAUSE
                            + "there is already a Vaadin servlet with the name "
                            + vaadinServlet.getName());
            return;
        }

        String servletName = getClass().getName();
        ServletRegistration.Dynamic registration = servletContext
                .addServlet(servletName, VaadinServlet.class);
        if (registration == null) {
            // Not expected to ever happen
            getLogger()
                    .config(() -> SKIPPING_AUTOMATIC_SERVLET_REGISTRATION_BECAUSE
                            + "there is already a servlet with the name "
                            + servletName);
            return;
        }

        getLogger().info("Automatically deploying Vaadin servlet to /*");

        registration.setInitParameter(
                Constants.SERVLET_PARAMETER_USING_NEW_ROUTING, "true");
        registration.setAsyncSupported(true);
        registration.addMapping("/*");
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
            getLogger().log(Level.CONFIG,
                    "Assuming " + className + " is not a Vaadin servlet", e);
            return false;
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Nothing to do
    }

    private static Logger getLogger() {
        return Logger.getLogger(ServletDeployer.class.getName());
    }
}
