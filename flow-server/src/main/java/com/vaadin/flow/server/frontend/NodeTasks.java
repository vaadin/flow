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

package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import com.vaadin.flow.server.Command;

import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_FRONTEND_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_GENERATED_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.IMPORTS_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.PARAM_FRONTEND_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.PARAM_GENERATED_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.getBaseDir;

/**
 * An executor that it's run when the servlet context is initialised in dev-mode
 * or when flow-maven-plugin goals are run. It can chain a set of task to run.
 */
public class NodeTasks implements Command {

    /**
     * Build a <code>NodeExecutor</code> instance.
     */
    public static class Builder implements Serializable {

        private final ClassFinder classFinder;

        private final File frontendDirectory;

        private File webpackOutputDirectory;

        private String webpackTemplate;

        private boolean enablePackagesUpdate;

        private boolean createMissingPackageJson;

        private boolean enableImportsUpdate;

        private boolean runNpmInstall;

        private boolean generateEmbeddableWebComponents;

        private Set<String> visitedClasses;

        /**
         * Directory for for npm and folders and files.
         */
        public final File npmFolder;

        /**
         * Directory where generated files are written.
         */
        public final File generatedFolder;

        /**
         * Create a builder instance, with everything set as default.
         *
         * @param classFinder
         *            a class finder
         */
        public Builder(ClassFinder classFinder) {
            this(classFinder, new File(getBaseDir()));
        }

        /**
         * Create a builder instance given an specific npm folder.
         *
         * @param classFinder
         *            a class finder
         * @param npmFolder
         *            folder with the `package.json` file
         */
        public Builder(ClassFinder classFinder, File npmFolder) {
            this(classFinder, npmFolder, new File(npmFolder, System
                    .getProperty(PARAM_GENERATED_DIR, DEFAULT_GENERATED_DIR)));
        }

        /**
         * Create a builder instance with custom npmFolder and generatedPath
         *
         * @param classFinder
         *            a class finder
         * @param npmFolder
         *            folder with the `package.json` file
         * @param generatedPath
         *            folder where flow generated files will be placed.
         */
        public Builder(ClassFinder classFinder, File npmFolder,
                File generatedPath) {
            this(classFinder, npmFolder, generatedPath,
                    new File(npmFolder, System.getProperty(PARAM_FRONTEND_DIR,
                            DEFAULT_FRONTEND_DIR)));
        }

        /**
         * Create a builder instance with all parameters.
         *
         * @param classFinder
         *            a class finder
         * @param npmFolder
         *            folder with the `package.json` file
         * @param generatedPath
         *            folder where flow generated files will be placed.
         * @param frontendDirectory
         *            a directory with project's frontend files
         */
        public Builder(ClassFinder classFinder, File npmFolder,
                File generatedPath, File frontendDirectory) {
            this.classFinder = classFinder;
            this.npmFolder = npmFolder;
            this.generateEmbeddableWebComponents = true;
            this.generatedFolder = generatedPath.isAbsolute() ? generatedPath
                    : new File(npmFolder, generatedPath.getPath());
            this.frontendDirectory = frontendDirectory.isAbsolute()
                    ? frontendDirectory
                    : new File(npmFolder, frontendDirectory.getPath());
        }

        /**
         * Creates a <code>NodeExecutor</code> using this configuration.
         *
         * @return a <code>NodeExecutor</code> instance
         */
        public NodeTasks build() {
            return new NodeTasks(this);
        }

        /**
         * Sets the webpack related properties.
         *
         * @param webpackOutputDirectory
         *            the directory to set for webpack to output its build
         *            results.
         * @param webpackTemplate
         *            name of the webpack resource to be used as template when
         *            creating the <code>webpack.config.js</code> file.
         * @return this builder
         */
        public Builder withWebpack(File webpackOutputDirectory,
                String webpackTemplate) {
            this.webpackOutputDirectory = webpackOutputDirectory;
            this.webpackTemplate = webpackTemplate;
            return this;
        }

        /**
         * Sets whether to enable packages and webpack file updates. Default is
         * <code>true</code>.
         *
         * @param enablePackagesUpdate
         *            <code>true</code> to enable packages and webpack update,
         *            otherwise <code>false</code>
         * @return this builder
         */
        public Builder enablePackagesUpdate(boolean enablePackagesUpdate) {
            this.enablePackagesUpdate = enablePackagesUpdate;
            return this;
        }

        /**
         * Sets whether to enable imports file update. Default is
         * <code>true</code>.
         *
         * @param enableImportsUpdate
         *            <code>true</code> to enable imports file update, otherwise
         *            <code>false</code>
         * @return this builder
         */
        public Builder enableImportsUpdate(boolean enableImportsUpdate) {
            this.enableImportsUpdate = enableImportsUpdate;
            return this;
        }

        /**
         * Sets whether run <code>npm install</code> after updating
         * dependencies.
         *
         * @param runNpmInstall
         *            run npm install. Default is <code>true</code>
         * @return the builder
         */
        public Builder runNpmInstall(boolean runNpmInstall) {
            this.runNpmInstall = runNpmInstall;
            return this;
        }

        /**
         * Sets whether to collect and package
         * {@link com.vaadin.flow.component.WebComponentExporter} dependencies.
         *
         * @param generateEmbeddableWebComponents
         *            collect dependencies. Default is {@code true}
         * @return the builder
         */
        public Builder withEmbeddableWebComponents(
                boolean generateEmbeddableWebComponents) {
            this.generateEmbeddableWebComponents = generateEmbeddableWebComponents;
            return this;
        }

        /**
         * Sets whether to create the package file if missing.
         *
         * @param create
         *            create the package
         * @return the builder
         */
        public Builder createMissingPackageJson(boolean create) {
            this.createMissingPackageJson = create;
            return this;
        }

        /**
         * Sets a set to which the names of classes visited when finding
         * dependencies will be collected.
         *
         * @param visitedClasses
         *            a set to collect class name to, or <code>null</code> to
         *            not collect visited classes
         * @return the builder, for chaining
         */
        public Builder collectVisitedClasses(Set<String> visitedClasses) {
            this.visitedClasses = visitedClasses;
            return this;
        }
    }

    private final Collection<Command> commands = new ArrayList<>();

    private NodeTasks(Builder builder) {

        ClassFinder classFinder = null;
        FrontendDependencies frontendDependencies = null;

        if (builder.enablePackagesUpdate || builder.enableImportsUpdate) {
            classFinder = new ClassFinder.CachedClassFinder(
                    builder.classFinder);

            if (builder.generateEmbeddableWebComponents) {
                FrontendWebComponentGenerator generator =
                        new FrontendWebComponentGenerator(classFinder);
                generator.generateWebComponents(builder.generatedFolder);
            }

            frontendDependencies = new FrontendDependencies(classFinder,
                    builder.generateEmbeddableWebComponents);
        }

        if (builder.createMissingPackageJson) {
            TaskCreatePackageJson packageCreator = new TaskCreatePackageJson(
                    builder.npmFolder, builder.generatedFolder);
            commands.add(packageCreator);
        }

        if (builder.enablePackagesUpdate) {
            TaskUpdatePackages packageUpdater = new TaskUpdatePackages(
                    classFinder, frontendDependencies, builder.npmFolder,
                    builder.generatedFolder);
            commands.add(packageUpdater);

            if (builder.runNpmInstall) {
                commands.add(new TaskRunNpmInstall(packageUpdater));
            }
        }

        if (builder.webpackTemplate != null
                && !builder.webpackTemplate.isEmpty()) {
            commands.add(new TaskUpdateWebpack(builder.npmFolder,
                    builder.webpackOutputDirectory, builder.webpackTemplate,
                    new File(builder.generatedFolder, IMPORTS_NAME)));
        }

        if (builder.enableImportsUpdate) {
            commands.add(new TaskUpdateImports(classFinder,
                    frontendDependencies, builder.npmFolder,
                    builder.generatedFolder, builder.frontendDirectory));

            if (builder.visitedClasses != null) {
                builder.visitedClasses
                        .addAll(frontendDependencies.getClasses());
            }
        }
    }

    @Override
    public void execute() {
        commands.forEach(Command::execute);
    }

}
