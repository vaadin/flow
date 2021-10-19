/*
 * Copyright 2000-2021 Vaadin Ltd.
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
package com.vaadin.base.devserver.startup;

import java.io.Serializable;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.HandlesTypes;
import javax.servlet.annotation.WebListener;

import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.WebComponentExporterFactory;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.internal.DevModeHandler;
import com.vaadin.flow.internal.DevModeHandlerManager;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.server.UIInitListener;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.startup.VaadinInitializerException;
import com.vaadin.flow.server.startup.VaadinServletContextStartupInitializer;
import com.vaadin.flow.theme.NoTheme;
import com.vaadin.flow.theme.Theme;

/**
 * Servlet initializer starting node updaters as well as the webpack-dev-mode
 * server.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 2.0
 */
@HandlesTypes({ Route.class, UIInitListener.class,
        VaadinServiceInitListener.class, WebComponentExporter.class,
        WebComponentExporterFactory.class, NpmPackage.class,
        NpmPackage.Container.class, JsModule.class, JsModule.Container.class,
        CssImport.class, CssImport.Container.class, JavaScript.class,
        JavaScript.Container.class, Theme.class, NoTheme.class,
        HasErrorParameter.class, PWA.class, AppShellConfigurator.class })
@WebListener
public class DevModeInitializer
        implements VaadinServletContextStartupInitializer, Serializable,
        ServletContextListener {

    @Override
    public void initialize(Set<Class<?>> classes, VaadinContext context)
            throws VaadinInitializerException {
        // DevModeHandler devModeHandler = initDevModeHandler(classes,
        // context);

        Lookup lookup = context.getAttribute(Lookup.class);
        DevModeHandlerManager devModeHandlerManager = lookup
                .lookup(DevModeHandlerManager.class);
        devModeHandlerManager.initDevModeHandler(classes, context);
    }

    /**
     * Shows whether {@link DevModeHandler} has been already started or not.
     *
     * @deprecated Use {@link #isDevModeAlreadyStarted(VaadinContext)} instead
     *             by wrapping {@link ServletContext} with
     *             {@link VaadinServletContext}.
     *
     * @param servletContext
     *            The servlet context, not <code>null</code>
     * @return <code>true</code> if {@link DevModeHandler} has already been
     *         started, <code>false</code> - otherwise
     */
    @Deprecated
    public static boolean isDevModeAlreadyStarted(
            ServletContext servletContext) {
        return isDevModeAlreadyStarted(
                new VaadinServletContext(servletContext));
    }

    /**
     * Shows whether {@link DevModeHandler} has been already started or not.
     *
     * @param context
     *            The {@link VaadinContext}, not <code>null</code>
     * @return <code>true</code> if {@link DevModeHandler} has already been
     *         started, <code>false</code> - otherwise
     * 
     * @deprecated Check {@link DevModeHandlerManager#getDevModeHandler()} for
     *             {@code null} instead.
     * 
     */
    @Deprecated
    public static boolean isDevModeAlreadyStarted(VaadinContext context) {
        assert context != null;
        Lookup lookup = context.getAttribute(Lookup.class);
        DevModeHandlerManager devModeHandlerManager = lookup
                .lookup(DevModeHandlerManager.class);

        return devModeHandlerManager.getDevModeHandler() != null;
    }

    @Override
    public void contextInitialized(ServletContextEvent ctx) {
        // No need to do anything on init
    }

    @Override
    public void contextDestroyed(ServletContextEvent ctx) {
        DevModeHandlerManager
                .getDevModeHandler(
                        new VaadinServletContext(ctx.getServletContext()))
                .ifPresent(DevModeHandler::stop);
    }

}
