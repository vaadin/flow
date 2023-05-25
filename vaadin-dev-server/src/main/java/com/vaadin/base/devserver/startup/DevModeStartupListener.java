/*
 * Copyright 2000-2023 Vaadin Ltd.
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

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.HandlesTypes;
import jakarta.servlet.annotation.WebListener;

import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.WebComponentExporterFactory;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.internal.DevModeHandlerManager;
import com.vaadin.flow.internal.Template;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.LoadDependenciesOnStartup;
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
 * Trigger for running dev mode initialization when running in a compatible
 * servlet environment.
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
        HasErrorParameter.class, PWA.class, AppShellConfigurator.class,
        Template.class, LoadDependenciesOnStartup.class })
@WebListener
public class DevModeStartupListener
        implements VaadinServletContextStartupInitializer, Serializable,
        ServletContextListener {

    @Override
    public void initialize(Set<Class<?>> classes, VaadinContext context)
            throws VaadinInitializerException {
        Lookup lookup = context.getAttribute(Lookup.class);
        DevModeHandlerManager devModeHandlerManager = lookup
                .lookup(DevModeHandlerManager.class);
        devModeHandlerManager.initDevModeHandler(classes, context);

    }

    @Override
    public void contextInitialized(ServletContextEvent ctx) {
        // No need to do anything on init
    }

    @Override
    public void contextDestroyed(ServletContextEvent ctx) {
        VaadinServletContext context = new VaadinServletContext(
                ctx.getServletContext());
        Lookup lookup = context.getAttribute(Lookup.class);
        DevModeHandlerManager devModeHandlerManager = lookup
                .lookup(DevModeHandlerManager.class);
        if (devModeHandlerManager != null) {
            devModeHandlerManager.stopDevModeHandler();
        }
    }

}
