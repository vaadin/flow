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
import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.DevModeHandler;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.frontend.AnnotationValuesExtractor;
import com.vaadin.flow.server.frontend.ClassPathIntrospector.ClassFinder;
import com.vaadin.flow.server.frontend.ClassPathIntrospector.DefaultClassFinder;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.frontend.NodeUpdateImports;
import com.vaadin.flow.server.frontend.NodeUpdatePackages;
import com.vaadin.flow.server.startup.ServletDeployer.StubServletConfig;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.Theme;

import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_DEVMODE_SKIP_UPDATE_IMPORTS;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_DEVMODE_SKIP_UPDATE_NPM;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_DEVMODE_WEBPACK_RUNNING_PORT;

/**
 * Servlet initializer starting node updaters as well as the webpack-dev-mode
 * server.
 */
@HandlesTypes({
    NpmPackage.class, JsModule.class,
    HtmlImport.class, JavaScript.class,
    Theme.class, AbstractTheme.class,
    Route.class, Component.class })
public class DevModeInitializer implements ServletContainerInitializer, Serializable {

    @Override
    public void onStartup(Set<Class<?>> classes, ServletContext context) throws ServletException {
        Collection<? extends ServletRegistration> registrations = context.getServletRegistrations().values();

        if (registrations.isEmpty()) {
            return;
        }

        DeploymentConfiguration config = StubServletConfig.createDeploymentConfiguration(context,
                registrations.iterator().next(), VaadinServlet.class);

        if (config.isProductionMode() || config.isBowerMode()) {
            return;
        }

        // Our working dir is not in a proper dev project
        // (need to check package.json and webpack.config.js in the future)
        if (!new File(FrontendUtils.getBaseDir(), "src").isDirectory()
                // We don't run in dev project, but user might have webpack-dev-server
                // running in the background
                && config.getStringProperty(SERVLET_PARAMETER_DEVMODE_WEBPACK_RUNNING_PORT, null) == null) {
            return;
        }

        ClassFinder finder = new DefaultClassFinder(classes);

        AnnotationValuesExtractor extractor = new AnnotationValuesExtractor(finder);
        try {
            if (!config.getBooleanProperty(
                    SERVLET_PARAMETER_DEVMODE_SKIP_UPDATE_NPM, false)) {
                new NodeUpdatePackages(finder).execute();
            }

            if (!config.getBooleanProperty(
                    SERVLET_PARAMETER_DEVMODE_SKIP_UPDATE_IMPORTS, false)) {
                new NodeUpdateImports(extractor).execute();
            }

            DevModeHandler.start(config);
        } catch (Exception e) {
            log().warn(
                    "Failed to start a dev mode, hot reload is disabled. Continuing to start the application.",
                    e);
        }
    }

    private Logger log() {
        return LoggerFactory.getLogger(getClass());
    }
 }
