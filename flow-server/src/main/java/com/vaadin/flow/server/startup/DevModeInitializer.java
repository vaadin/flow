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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.DevModeHandler;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.frontend.ClassFinder.DefaultClassFinder;
import com.vaadin.flow.server.frontend.NodeTasks;
import com.vaadin.flow.server.frontend.NodeTasks.Builder;
import com.vaadin.flow.server.startup.ServletDeployer.StubServletConfig;

import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_DEVMODE_WEBPACK_RUNNING_PORT;
import static com.vaadin.flow.server.frontend.FrontendUtils.WEBPACK_CONFIG;

/**
 * Servlet initializer starting node updaters as well as the webpack-dev-mode
 * server.
 */
@HandlesTypes({ Route.class, NpmPackage.class, WebComponentExporter.class })
public class DevModeInitializer
        implements ServletContainerInitializer, Serializable {

    /**
     * The classes that were visited when determining which frontend resources
     * are actually used.
     */
    public static class VisitedClasses implements Serializable {
        private Set<String> visitedClassNames;

        /**
         * Creates a new instance based on a set of class names.
         *
         * @param visitedClassNames
         *            the set of visited class names, not <code>null</code>
         */
        public VisitedClasses(Set<String> visitedClassNames) {
            assert visitedClassNames != null;
            this.visitedClassNames = visitedClassNames;
        }

        /**
         * Checks whether all dependency annotations of the provided class have
         * been visited.
         *
         * @param dependencyClass
         *            the class to check
         * @return <code>true</code> if all dependencies of the class have been
         *         visited, <code>false</code> otherwise
         */
        public boolean allDependenciesVisited(Class<?> dependencyClass) {
            if (visitedClassNames.contains(dependencyClass.getName())) {
                return true;
            }

            /*
             * Not being visited is only a problem if the class has own
             * dependency annotations or the parent class has problems.
             */
            if (dependencyClass
                    .getDeclaredAnnotationsByType(JsModule.class).length != 0) {
                return false;
            }

            Class<?> superclass = dependencyClass.getSuperclass();
            if (superclass == null) {
                return true;
            } else {
                return allDependenciesVisited(superclass);
            }
        }
    }

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

        if (config.isProductionMode() || config.isBowerMode()) {
            return;
        }

        Builder builder = new NodeTasks.Builder(new DefaultClassFinder(classes));

        int runningPort = Integer.parseInt(config.getStringProperty(
                SERVLET_PARAMETER_DEVMODE_WEBPACK_RUNNING_PORT, "0"));
        // User can run its own webpack server and provide port
        if (runningPort == 0) {
            for (File f : Arrays.asList(
                    new File(builder.npmFolder, PACKAGE_JSON),
                    new File(builder.generatedFolder, PACKAGE_JSON),
                    new File(builder.npmFolder, WEBPACK_CONFIG)
                    )) {
                if (!f.canRead()) {
                    log().warn("Skiping DEV MODE because cannot find '{}' file.", f.getPath());
                    return;
                }
            }

            Set<String> visitedClassNames = new HashSet<>();
            builder.enablePackagesUpdate(true)
                    .enableImportsUpdate(true)
                    .runNpmInstall(true)
                    .withEmbeddableWebComponents(true)
                    .collectVisitedClasses(visitedClassNames)
                    .build().execute();

            context.setAttribute(VisitedClasses.class.getName(),
                    new VisitedClasses(visitedClassNames));
        }

        DevModeHandler.start(config, builder.npmFolder);
    }

    private Logger log() {
        return LoggerFactory.getLogger(getClass());
    }
}
