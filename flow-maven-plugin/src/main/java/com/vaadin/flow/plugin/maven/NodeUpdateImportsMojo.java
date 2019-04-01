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
package com.vaadin.flow.plugin.maven;

import java.io.File;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.vaadin.flow.server.frontend.AnnotationValuesExtractor;
import com.vaadin.flow.server.frontend.NodeUpdateImports;
import com.vaadin.flow.server.frontend.NodeUpdater;

/**
 * Goal that updates main.js file with @JsModule, @HtmlImport and @Theme
 * annotations defined in the classpath.
 */
@Mojo(name = "update-imports", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class NodeUpdateImportsMojo extends NodeUpdateAbstractMojo {
    /**
     * A Flow JavaScript file with all project's imports to update.
     */
    @Parameter(defaultValue = "${project.basedir}/" + NodeUpdateImports.MAIN_JS)
    private File jsFile;

    @Override
    protected NodeUpdater getUpdater() {
        if (updater == null) {
            AnnotationValuesExtractor extractor = new AnnotationValuesExtractor(getClassFinder(project));
            updater = new NodeUpdateImports(extractor, jsFile, npmFolder, nodeModulesPath, convertHtml);
        }
        return updater;
    }
}
