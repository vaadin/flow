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
import java.net.URL;
import java.util.List;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.plugin.common.FlowPluginFileUtils;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.NodeUpdater;

/**
 * Common stuff for node update mojos.
 */
public abstract class NodeUpdateAbstractMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    /**
     * Enable or disable legacy components annotated only with
     * {@link HtmlImport}.
     */
    @Parameter(defaultValue = "true")
    protected boolean convertHtml;

    /**
     * The folder where `package.json` file is located. Default is current dir.
     */
    @Parameter(defaultValue = "${project.basedir}")
    protected File npmFolder;

    /**
     * The path to the {@literal node_modules} directory of the project.
     */
    @Parameter(defaultValue = "${project.basedir}/node_modules/")
    protected File nodeModulesPath;

    protected NodeUpdater updater;

    @Override
    public void execute() {
        // Do nothing when bower mode
        if (Boolean.getBoolean("vaadin." + Constants.SERVLET_PARAMETER_BOWER_MODE)) {
            String goal = this.getClass().equals(NodeUpdateImportsMojo.class) ? "update-imports"  : "update-npm-dependencies";
            getLog().info("Skipped '" + goal + "' goal because `vaadin.bowerMode` is set.");
            return;
        }
        getUpdater().execute();
    }

    protected abstract NodeUpdater getUpdater();

    static URL[] getProjectClassPathUrls(MavenProject project) {
        final List<String> runtimeClasspathElements;
        try {
            runtimeClasspathElements = project.getRuntimeClasspathElements();
        } catch (DependencyResolutionRequiredException e) {
            throw new IllegalStateException(String.format(
                    "Failed to retrieve runtime classpath elements from project '%s'",
                    project), e);
        }
        return runtimeClasspathElements.stream().map(File::new)
                .map(FlowPluginFileUtils::convertToUrl).toArray(URL[]::new);
    }
}
