/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.HandlesTypes;
import jakarta.servlet.annotation.WebListener;

import java.io.Serializable;
import java.util.Set;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Tag;
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
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.LoadDependenciesOnStartup;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.server.UIInitListener;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.frontend.TypeScriptBootstrapModifier;
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
        Template.class, LoadDependenciesOnStartup.class,
        TypeScriptBootstrapModifier.class, Component.class, Layout.class })
@WebListener
public class DevModeStartupListener
        implements VaadinServletContextStartupInitializer, Serializable,
        ServletContextListener {

    private DevModeHandlerManager devModeHandlerManager;

    @Override
    public void initialize(Set<Class<?>> classes, VaadinContext context)
            throws VaadinInitializerException {
        lookupDevModeHandlerManager(context).initDevModeHandler(classes,
                context);
        classes.stream().filter(Component.class::isAssignableFrom)
                .forEach(clazz -> {
                    Tag tag = clazz.getAnnotation(Tag.class);
                    if (tag != null) {
                        ComponentUtil.registerComponentClass(tag.value(),
                                (Class<? extends Component>) clazz);
                    }
                });
    }

    @Override
    public void contextInitialized(ServletContextEvent ctx) {
        // Keep a reference to the dev mode handler manager to stop it on
        // context destroy event, since lookup in that phase could fail, for
        // example if the DI container behind lookup has been already disposed
        devModeHandlerManager = lookupDevModeHandlerManager(
                new VaadinServletContext(ctx.getServletContext()));
    }

    @Override
    public void contextDestroyed(ServletContextEvent ctx) {
        if (devModeHandlerManager == null) {
            // devModeHandlerManager should never be null here.
            // if it happens try to lookup to ensure dev server is stopped
            // but do not propagate potential failures to the container, since
            // the situation error cannot be handled in any way.
            try {
                devModeHandlerManager = lookupDevModeHandlerManager(
                        new VaadinServletContext(ctx.getServletContext()));
            } catch (Exception exception) {
                LoggerFactory.getLogger(DevModeStartupListener.class).debug(
                        "Cannot obtain DevModeHandlerManager instance during ServletContext destroy event. "
                                + "Potential cause could be DI container behind Lookup being already disposed.",
                        exception);
            }
        }
        if (devModeHandlerManager != null) {
            devModeHandlerManager.stopDevModeHandler();
        }
        devModeHandlerManager = null;
    }

    private DevModeHandlerManager lookupDevModeHandlerManager(
            VaadinContext context) {
        Lookup lookup = context.getAttribute(Lookup.class);
        if (lookup == null) {
            LoggerFactory.getLogger(DevModeStartupListener.class).debug(
                    "Cannot obtain a Lookup instance from VaadinContext.");
            return null;
        }
        return lookup.lookup(DevModeHandlerManager.class);
    }
}
