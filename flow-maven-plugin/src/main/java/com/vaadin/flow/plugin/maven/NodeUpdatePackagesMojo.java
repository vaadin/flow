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

import org.apache.maven.model.Build;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.vaadin.flow.server.frontend.AnnotationValuesExtractor;
import com.vaadin.flow.server.frontend.NodeUpdatePackages;
import com.vaadin.flow.server.frontend.NodeUpdater;

/**
 * Goal that updates <code>package.json</code> file with @NpmPackage annotations
 * defined in the classpath, and that creates <code>webpack.config.js</code> if
 * does not exist yet.
 */
@Mojo(name = "update-npm-dependencies", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class NodeUpdatePackagesMojo extends NodeUpdateAbstractMojo {

    /**
     * Copy the `webapp.config.js` from the specified URL if missing. Default is
     * the template provided by this plugin. Leave it blank to disable the
     * feature.
     */
    @Parameter(defaultValue = NodeUpdatePackages.WEBPACK_CONFIG)
    private String webpackTemplate;

    @Override
    protected NodeUpdater getUpdater() {
        if (updater == null) {
            AnnotationValuesExtractor extractor = new AnnotationValuesExtractor(getClassFinder(project));
            updater = new NodeUpdatePackages(extractor,
                    getWebpackOutputDirectory(), webpackTemplate, npmFolder,
                    nodeModulesPath, convertHtml);
        }
        return updater;
    }

    private File getWebpackOutputDirectory() {
        Build buildInformation = project.getBuild();
        switch (project.getPackaging()) {
        case "jar": {
            return new File(buildInformation.getOutputDirectory(),
                    "META-INF/resources");
        }
        case "war": {
            return new File(buildInformation.getDirectory(),
                    buildInformation.getFinalName());
        }
        default:
            throw new IllegalStateException(String.format(
                    "Unsupported packaging '%s'", project.getPackaging()));
        }
    }
}
