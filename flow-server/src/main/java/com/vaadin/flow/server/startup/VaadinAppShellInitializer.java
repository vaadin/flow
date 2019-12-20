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
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.HandlesTypes;
import javax.servlet.annotation.WebListener;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.vaadin.flow.component.page.Inline;
import com.vaadin.flow.component.page.Viewport;
import com.vaadin.flow.component.page.BodySize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.page.Meta;
import com.vaadin.flow.component.page.VaadinAppShell;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.InvalidApplicationConfigurationException;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.startup.ServletDeployer.StubServletConfig;

/**
 * Servlet initializer visiting {@link VaadinAppShell} configuration.
 *
 * @since 3.0
 */
@HandlesTypes({ VaadinAppShell.class, Meta.class, Meta.Container.class,
        PWA.class, Inline.class, Inline.Container.class, Viewport.class, BodySize.class})
@WebListener
public class VaadinAppShellInitializer implements ServletContainerInitializer,
        Serializable {

    @Override
    public void onStartup(Set<Class<?>> classes, ServletContext context)
            throws ServletException {

        Collection<? extends ServletRegistration> registrations = context
                .getServletRegistrations().values();
        if (registrations.isEmpty()) {
            return;
        }

        DeploymentConfiguration config = StubServletConfig
                .createDeploymentConfiguration(context,
                        registrations.iterator().next(), VaadinServlet.class);

        init(classes, context, config);
    }

    /**
     * Initializes the {@link VaadinAppShellRegistry} for the application.
     *
     * @param classes
     *            a set of classes that matches the {@link HandlesTypes} set in
     *            this class.
     * @param context
     *            the servlet context.
     * @param config
     *            the vaadin configuration for the application.
     */
    @SuppressWarnings("unchecked")
    public static void init(Set<Class<?>> classes, ServletContext context,
            DeploymentConfiguration config) {

        if (!config.isClientSideMode()) {
            return;
        }

        VaadinAppShellRegistry registry = VaadinAppShellRegistry
                .getInstance(new VaadinServletContext(context));
        registry.reset();

        if (classes == null || classes.isEmpty()) {
            return;
        }

        List<String> offendingAnnotations = new ArrayList<>();

        classes.stream()
                // sort classes by putting VaadinAppShell in first position
                .sorted((a, b) -> registry.isShell(a) ? -1
                        : registry.isShell(b) ? 1 : 0)
                .forEach(clz -> {
                    if (registry.isShell(clz)) {
                        registry.setShell(
                                (Class<? extends VaadinAppShell>) clz);
                        getLogger().info(
                                "Using {} class for configuring `index.html` response",
                                clz.getName());
                    } else {
                        String error = registry.validateClass(clz);
                        if (error != null) {
                            offendingAnnotations.add(error);
                        }
                    }
                });

        if (!offendingAnnotations.isEmpty()) {
            if (registry.getShell() == null) {
                String message = String.format(
                        VaadinAppShellRegistry.ERROR_HEADER_NO_SHELL,
                        String.join("\n  ", offendingAnnotations));
                getLogger().error(message);
            } else {
                String message = String.format(
                        VaadinAppShellRegistry.ERROR_HEADER_OFFENDING,
                        registry.getShell(),
                        String.join("\n  ", offendingAnnotations));
                throw new InvalidApplicationConfigurationException(message);
            }
        }
    }

    /**
     * Return the set of valid annotations in a {@link VaadinAppShell} class.
     * This method is thought to be called from external plugins to decouple
     * them.
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public static List<Class<? extends Annotation>> getValidAnnotations() {
        HandlesTypes annotation = VaadinAppShellInitializer.class
                .getAnnotation(HandlesTypes.class);
        assert annotation != null;
        List<Class<? extends Annotation>> ret = new ArrayList<>();
        for (Class<?> clazz : annotation.value()) {
            if (clazz.isAnnotation()) {
                ret.add((Class<? extends Annotation>) clazz);
            }
        }
        return ret;
    }

    /**
     * Return the {@link VaadinAppShell} class. This method is thought to be
     * called from external plugins to decouple them.
     *
     * @return
     */
    public static List<Class<?>> getValidSupers() {
        return Collections.singletonList(VaadinAppShell.class);
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(VaadinAppShellInitializer.class);
    }
 }
