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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import com.vaadin.flow.plugin.common.FlowPluginFileUtils;

import static com.vaadin.flow.shared.ApplicationConstants.FRONTEND_PROTOCOL_PREFIX;

/**
 * Base class for properties and methods related to npm support.
 */
public abstract class AbstractNpmMojo extends AbstractMojo {
    static final String FLOW_PACKAGE = "@vaadin/flow-frontend/";

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    /**
     * The folder where `package.json` file is located. Default is current dir.
     */
    @Parameter(defaultValue = "${project.basedir}")
    protected File npmFolder;

    @Parameter(defaultValue = "${npmFolder}/node_modules/" + FLOW_PACKAGE)
    protected File flowPackageDirectory;

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

    protected Set<String> getHtmlImportJsModules(Set<String> htmlImports) {
        return htmlImports.stream().map(this::getHtmlImportJsModule)
                .filter(Objects::nonNull).collect(Collectors.toSet());
    }

    protected Set<String> getHtmlImportNpmPackages(Set<String> htmlImports) {
        return htmlImports.stream().map(this::getHtmlImportNpmPackage)
                .filter(Objects::nonNull).collect(Collectors.toSet());
    }

    protected String resolveInFlowFrontendDirectory(String importPath) {
        if (importPath.startsWith("@")) {
            return importPath;
        }
        String pathWithNoProtocols = importPath
                .replace(FRONTEND_PROTOCOL_PREFIX, "");
        return String.format("%s%s",
                new File(flowPackageDirectory, pathWithNoProtocols).isFile()
                        ? FLOW_PACKAGE
                        : "./",
                pathWithNoProtocols);
    }

    private String getHtmlImportJsModule(String htmlImport) {
        String module = resolveInFlowFrontendDirectory(htmlImport // @formatter:off
        .replaceFirst("^.*bower_components/(vaadin-[^/]*/.*)\\.html$", "@vaadin/$1.js")
        .replaceFirst("^.*bower_components/((iron|paper)-[^/]*/.*)\\.html$", "@polymer/$1.js")
        .replaceFirst("\\.html$", ".js")
        ); // @formatter:on
        return Objects.equals(module, htmlImport) ? null : module;
    }

    private String getHtmlImportNpmPackage(String htmlImport) {
        String module = resolveInFlowFrontendDirectory(htmlImport // @formatter:off
        .replaceFirst("^.*bower_components/(vaadin-[^/]*)/.*\\.html$", "@vaadin/$1")
        .replaceFirst("^.*bower_components/((iron|paper)-[^/]*)/.*\\.html$", "@polymer/$1")
        .replaceFirst("\\.html$", ".js")
        ); // @formatter:on
        return Objects.equals(module, htmlImport) ? null : module;
    }
}
