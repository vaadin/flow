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

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.frontend.ClassPathIntrospector.ClassFinder;
import com.vaadin.flow.theme.Theme;

import static com.vaadin.flow.server.frontend.FrontendUtils.getBaseDir;

/**
 * An updater that it's run when the servlet context is initialised in dev-mode
 * or when flow-maven-plugin goals are run in order to update
 * <code>package.json</code> by visiting {@link NpmPackage} annotations found in
 * the classpath. It also updates <code>main.js</code> and
 * <code>node_module/@vaadin/flow-frontend</code> contents by visiting all
 * classes with {@link JsModule} {@link HtmlImport} and {@link Theme}
 * annotations.
 */
public class NodeExecutor implements Command {

    /**
     * Build a <code>NodeExecutor</code> instance.
     */
    public static class Builder implements Serializable {

        private ClassFinder classFinder;

        private File npmFolder;

        private File nodeModulesPath;

        private File jsFile;

        private boolean convertHtml;

        private File webpackOutputDirectory;

        private String webpackTemplate;

        private boolean enablePackagesUpdate;

        private boolean enableImportsUpdate;

        /**
         * Create a builder instance.
         * 
         * @param classFinder
         *            a class finder
         */
        public Builder(ClassFinder classFinder) {
            this(classFinder,
                    new File(getBaseDir(),
                            System.getProperty(NodeUpdateImports.MAIN_JS_PARAM,
                                    NodeUpdateImports.MAIN_JS)),
                    new File(getBaseDir()),
                    new File(getBaseDir(), "node_modules"), true);
        }

        /**
         * Create a builder instance.
         *
         * @param classFinder
         *            a class finder
         * @param jsFile
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
        public Builder(ClassFinder classFinder, File jsFile, File npmFolder,
                File nodeModulesPath, boolean convertHtml) {
            this.classFinder = classFinder;
            this.jsFile = jsFile;
            this.npmFolder = npmFolder;
            this.nodeModulesPath = nodeModulesPath;
            this.convertHtml = convertHtml;
            this.webpackOutputDirectory = new File("./src/main/webapp");
            this.webpackTemplate = WebpackUpdater.WEBPACK_CONFIG;
            this.enablePackagesUpdate = true;
            this.enableImportsUpdate = true;
        }

        /**
         * Creates a <code>NodeExecutor</code> using this configuration.
         * 
         * @return a <code>NodeExecutor</code> instance
         */
        public NodeExecutor build() {
            return new NodeExecutor(this);
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
        public Builder setWebpack(File webpackOutputDirectory,
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
        public Builder setEnablePackagesUpdate(boolean enablePackagesUpdate) {
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
        public Builder setEnableImportsUpdate(boolean enableImportsUpdate) {
            this.enableImportsUpdate = enableImportsUpdate;
            return this;
        }

    }

    private Collection<Command> commands = new ArrayList<>();

    private NodeExecutor() {
    }

    private NodeExecutor(Builder builder) {

        AnnotationValuesExtractor extractor = new AnnotationValuesCache(
                builder.classFinder);

        if (builder.enablePackagesUpdate) {
            commands.add(new NodeUpdatePackages(extractor,
                    builder.npmFolder, builder.nodeModulesPath,
                    builder.convertHtml));
            commands.add(new WebpackUpdater(builder.npmFolder,
                    builder.webpackOutputDirectory, builder.webpackTemplate));
        }
        
        if (builder.enableImportsUpdate) {
            commands.add(new NodeUpdateImports(extractor,
                    builder.jsFile, builder.npmFolder, builder.nodeModulesPath,
                    builder.convertHtml));
        }
    }

    @Override
    public void execute() {
        commands.forEach(Command::execute);
    }

}
