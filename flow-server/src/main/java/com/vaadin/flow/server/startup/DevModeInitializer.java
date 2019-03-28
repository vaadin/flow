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

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.DevModeHandler;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.startup.ServletDeployer.StubServletConfig;
import com.vaadin.flow.theme.Theme;

/**
 * Servlet initializer starting node dependency and import updaters as well as
 * webpack-dev-mode server
 */
@HandlesTypes({ NpmPackage.class, JsModule.class, Theme.class, HtmlImport.class })
public class DevModeInitializer implements ServletContainerInitializer, Serializable {

    @Override
    public void onStartup(Set<Class<?>> classSet, ServletContext context) throws ServletException {
        Collection<? extends ServletRegistration> registrations = context.getServletRegistrations().values();

        if (registrations.size() == 0) {
            return;
        }

        DeploymentConfiguration configuration = StubServletConfig.createDeploymentConfiguration(context,
                registrations.iterator().next(), VaadinServlet.class);

        DevModeHandler.start(configuration);
    }
}
