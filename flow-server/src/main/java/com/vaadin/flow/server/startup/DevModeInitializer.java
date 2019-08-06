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
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.HandlesTypes;
import javax.servlet.annotation.WebListener;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.DevModeHandler;
import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.UIInitListener;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.frontend.NodeTasks;
import com.vaadin.flow.server.frontend.NodeTasks.Builder;
import com.vaadin.flow.server.frontend.scanner.ClassFinder.DefaultClassFinder;
import com.vaadin.flow.server.startup.ServletDeployer.StubServletConfig;

import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.Constants.RESOURCES_FRONTEND_DEFAULT;
import static com.vaadin.flow.server.frontend.FrontendUtils.WEBPACK_GENERATED;

/**
 * Servlet initializer starting node updaters as well as the webpack-dev-mode
 * server.
 */
@HandlesTypes({ Route.class, NpmPackage.class, NpmPackage.Container.class,
        WebComponentExporter.class, UIInitListener.class })
@WebListener
public class DevModeInitializer implements ServletContainerInitializer,
        Serializable, ServletContextListener {

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

        /**
         * Ensures that all {@code clazz} dependencies are visited.
         *
         * @see #allDependenciesVisited(Class)
         *
         * @param clazz
         *            the class to check
         */
        public void ensureAllDependenciesVisited(Class<?> clazz) {
            if (!allDependenciesVisited(clazz)) {
                DevModeInitializer.log().warn(
                        "Frontend dependencies have not been analyzed for {}."
                                + " To make the component's frontend dependencies work, you must ensure the component class is directly referenced through an application entry point such as a class annotated with @Route.",
                        clazz.getName());
            }
        }
    }

    private static final Pattern JAR_FILE_REGEX = Pattern
            .compile(".*file:(.+\\.jar).*");

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

        initDevModeHandler(classes, context, config);
    }

    /**
     * Initialize the devmode server if not in production mode or compatibility
     * mode.
     *
     * @param classes
     *            classes to check for npm- and js modules
     * @param context
     *            servlet context we are running in
     * @param config
     *            deployment configuration
     *
     * @throws ServletException
     *             if dev mode can't be initialized
     */
    public static void initDevModeHandler(Set<Class<?>> classes,
            ServletContext context, DeploymentConfiguration config)
            throws ServletException {
        if (config.isProductionMode()) {
            log().debug("Skipping DEV MODE because PRODUCTION MODE is set.");
            return;
        }
        if (config.isCompatibilityMode()) {
            log().debug("Skipping DEV MODE because BOWER MODE is set.");
            return;
        }
        if (!config.enableDevServer()) {
            log().debug(
                    "Skipping DEV MODE because dev server shouldn't be enabled.");
            return;
        }

        String baseDir = config.getStringProperty(FrontendUtils.PROJECT_BASEDIR,
                System.getProperty("user.dir", "."));

        Set<File> jarFiles = getJarFilesFromClassloader();

        Builder builder = new NodeTasks.Builder(new DefaultClassFinder(classes),
                new File(baseDir));

        log().info("Starting dev-mode updaters in {} folder.",
                builder.npmFolder);

        if (!builder.generatedFolder.exists()) {
            try {
                FileUtils.forceMkdir(builder.generatedFolder);
            } catch (IOException e) {
                throw new UncheckedIOException(
                        String.format("Failed to create directory '%s'",
                                builder.generatedFolder),
                        e);
            }
        }

        File generatedPackages = new File(builder.generatedFolder,
                PACKAGE_JSON);

        // If we are missing the generated webpack configuration then generate
        // webpack configurations
        if (!new File(builder.npmFolder, WEBPACK_GENERATED).exists()) {
            builder.withWebpack(builder.npmFolder, FrontendUtils.WEBPACK_CONFIG,
                    FrontendUtils.WEBPACK_GENERATED);
        }

        // If we are missing either the base or generated package json files
        // generate those
        if (!new File(builder.npmFolder, PACKAGE_JSON).exists()
                || !generatedPackages.exists()) {
            builder.createMissingPackageJson(true);
        }

        Set<String> visitedClassNames = new HashSet<>();
        try {
            builder.enablePackagesUpdate(true).copyResources(jarFiles)
                    .copyLocalResources(new File(baseDir,
                            Constants.LOCAL_FRONTEND_RESOURCES_PATH))
                    .enableImportsUpdate(true).runNpmInstall(true)
                    .withEmbeddableWebComponents(true)
                    .collectVisitedClasses(visitedClassNames).build().execute();
        } catch (ExecutionFailedException exception) {
            log().debug(
                    "Could not initializer dev mode handler. One of the node tasks failed",
                    exception);
            throw new ServletException(exception);
        }

        VaadinContext vaadinContext = new VaadinServletContext(context);
        vaadinContext.setAttribute(new VisitedClasses(visitedClassNames));

        try {
            DevModeHandler.start(config, builder.npmFolder);
        } catch (IllegalStateException exception) {
            // wrap an ISE which can be caused by inability to find tools like
            // node, npm into a servlet exception
            throw new ServletException(exception);
        }
    }

    private static Logger log() {
        return LoggerFactory.getLogger(DevModeInitializer.class);
    }

    @Override
    public void contextInitialized(ServletContextEvent ctx) {
        // No need to do anything on init
    }

    @Override
    public void contextDestroyed(ServletContextEvent ctx) {
        DevModeHandler handler = DevModeHandler.getDevModeHandler();
        if (handler != null && !handler.reuseDevServer()) {
            handler.stop();
        }
    }

    /*
     * This method returns all jar files having a specific folder. We don't use
     * URLClassLoader because will fail in Java 9+
     */
    private static Set<File> getJarFilesFromClassloader() {
        Set<File> jarFiles = new HashSet<>();
        try {
            Enumeration<URL> en = DevModeInitializer.class.getClassLoader()
                    .getResources(RESOURCES_FRONTEND_DEFAULT);
            while (en.hasMoreElements()) {
                URL url = en.nextElement();
                Matcher matcher = JAR_FILE_REGEX.matcher(url.getPath());
                if (matcher.find()) {
                    jarFiles.add(new File(matcher.group(1)));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return jarFiles;
    }
}
