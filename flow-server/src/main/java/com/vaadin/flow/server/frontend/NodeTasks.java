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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;

import com.vaadin.flow.server.Command;

import static com.vaadin.flow.server.frontend.FrontendUtils.FLOW_IMPORTS_FILE;
import static com.vaadin.flow.server.frontend.FrontendUtils.MAIN_JS_PARAM;
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

        private ClassFinder classFinder;

        private File npmFolder;

        private File nodeModulesPath;

        private File frontendDirectory;

        private File generatedFrontendDirectory;

        private File generatedFlowImports;

        private boolean convertHtml;

        private File webpackOutputDirectory;

        private String webpackTemplate;

        private boolean enablePackagesUpdate;

        private boolean enableImportsUpdate;

        private boolean runNpmInstall;

        private boolean generateEmbeddableWebComponents;

        /**
         * Create a builder instance.
         *
         * @param classFinder
         *            a class finder
         */
        public Builder(ClassFinder classFinder) {
            this(classFinder, new File(getBaseDir(), "frontend"),
                    new File(getBaseDir(), "target/frontend"),
                    Paths.get(getBaseDir()).resolve("target")
                            .resolve(System.getProperty(MAIN_JS_PARAM,
                                    FLOW_IMPORTS_FILE))
                            .toFile(),
                    new File(getBaseDir()),
                    new File(getBaseDir(), "node_modules"), true);
        }

        /**
         * Create a builder instance.
         *
         * @param classFinder
         *            a class finder
         * @param frontendDirectory
         *            a directory with project's frontend files
         * @param generatedFrontendDirectory
         *            a directory with project's generated frontend files
         * @param generatedFlowImports
         *            name of the JS file to update with the imports
         * @param npmFolder
         *            folder with the `package.json` file
         * @param nodeModulesPath
         *            the path to the {@literal node_modules} directory of the
         *            project
         * @param convertHtml
         *            <code>true</code> to enable polymer-2 annotated classes to
         *            be considered. Default is <code>true</code>.
         */
        public Builder(ClassFinder classFinder, File frontendDirectory,
                       File generatedFrontendDirectory,
                File generatedFlowImports, File npmFolder, File nodeModulesPath,
                boolean convertHtml) {
            this.classFinder = classFinder;
            this.frontendDirectory = frontendDirectory;
            this.generatedFrontendDirectory = generatedFrontendDirectory;
            this.generatedFlowImports = generatedFlowImports;
            this.npmFolder = npmFolder;
            this.nodeModulesPath = nodeModulesPath;
            this.convertHtml = convertHtml;
            this.webpackOutputDirectory = new File("./src/main/webapp");
            this.webpackTemplate = FrontendUtils.WEBPACK_CONFIG;
            this.enablePackagesUpdate = true;
            this.enableImportsUpdate = true;
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

        public Builder withEmbeddableWebComponents(boolean generateEmbeddableWebComponents) {
            this.generateEmbeddableWebComponents =
                    generateEmbeddableWebComponents;
            return this;
        }
    }

    private final Collection<Command> commands = new ArrayList<>();

    private NodeTasks(Builder builder) {

        ClassFinder classFinder = new ClassFinder.CachedClassFinder(
                builder.classFinder);

        FrontendDependencies frontendDependencies = new FrontendDependencies(
                classFinder, builder.generateEmbeddableWebComponents);

        if (builder.enablePackagesUpdate) {
            NodeUpdatePackages packageUpdater = new NodeUpdatePackages(classFinder,
                    frontendDependencies, builder.npmFolder,
                    builder.nodeModulesPath, builder.convertHtml);
            commands.add(packageUpdater);

            if (builder.runNpmInstall) {
                commands.add(new NodeNpmInstall(packageUpdater));
            }

            commands.add(new WebpackUpdater(builder.npmFolder,
                    builder.webpackOutputDirectory, builder.webpackTemplate,
                    builder.generatedFlowImports));
        }

        if (builder.enableImportsUpdate) {
            commands.add(new NodeUpdateImports(classFinder,
                    frontendDependencies, builder.frontendDirectory,
                    builder.generatedFrontendDirectory,
                    builder.generatedFlowImports, builder.npmFolder,
                    builder.nodeModulesPath,
                    builder.convertHtml));
        }

    }

    @Override
    public void execute() {
        commands.forEach(Command::execute);
    }

}
