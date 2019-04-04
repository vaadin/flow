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

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.DevModeHandler;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.frontend.AnnotationValuesExtractor;
import com.vaadin.flow.server.frontend.ClassPathIntrospector.DefaultClassFinder;
import com.vaadin.flow.server.frontend.NodeUpdateImports;
import com.vaadin.flow.server.frontend.NodeUpdatePackages;
import com.vaadin.flow.server.startup.ServletDeployer.StubServletConfig;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.Theme;

import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_DEVMODE_SKIP_UPDATE_IMPORTS;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_DEVMODE_SKIP_UPDATE_NPM;

/**
 * Servlet initializer starting node updaters as well as the webpack-dev-mode
 * server.
 */
@HandlesTypes({
    NpmPackage.class, JsModule.class,
    HtmlImport.class, JavaScript.class,
    Theme.class, AbstractTheme.class,
    Component.class })
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

        AnnotationValuesExtractor extractor = new AnnotationValuesExtractor(new DefaultClassFinder(classes));

        if (!config.getBooleanProperty(SERVLET_PARAMETER_DEVMODE_SKIP_UPDATE_NPM, false)) {
            new NodeUpdatePackages(extractor).execute();
        }

        if (!config.getBooleanProperty(SERVLET_PARAMETER_DEVMODE_SKIP_UPDATE_IMPORTS, false)) {
            new NodeUpdateImports(extractor).execute();
        }

        DevModeHandler.start(config);
    }
}
