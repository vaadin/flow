/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.base.devserver.startup;

import java.io.Serializable;
import java.util.Set;

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
        HasErrorParameter.class, PWA.class, AppShellConfigurator.class })
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
