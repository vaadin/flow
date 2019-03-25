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

import static com.vaadin.flow.plugin.production.ProductionModeCopyStep.NON_WEB_JAR_RESOURCE_PATH;
import static com.vaadin.flow.shared.ApplicationConstants.FRONTEND_PROTOCOL_PREFIX;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.plugin.common.AnnotationValuesExtractor;

/**
 * Base interface for methods for updating node_js files.
 */
public abstract class NodeUpdater {

    public static final String FLOW_PACKAGE = "@vaadin/flow-frontend/";

    /**
     * Folder with the <code>package.json</code> file
     */
     String npmFolder;

    /**
     * The relative path to the Flow package. Always relative to
     * {@link FrontendUpdater#npmFolder()}.
     */
    String flowPackagePath;

    /**
     * Enable or disable legacy components annotated only with
     * {@link HtmlImport}.
     */
    boolean convertHtml;

    URL[] projectClassPathUrls;

    AnnotationValuesExtractor annotationValuesExtractor;

    public abstract void execute();

    public File getFlowPackage() {
        return new File(npmFolder, flowPackagePath);
    }

    Set<String> getHtmlImportJsModules(Set<String> htmlImports) {
        return htmlImports.stream().map(this::htmlImportToJsModule).filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    Set<String> getHtmlImportNpmPackages(Set<String> htmlImports) {
        return htmlImports.stream().map(this::htmlImportToNpmPackage).filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    String resolveInFlowFrontendDirectory(String importPath) {
        if (importPath.startsWith("@")) {
            return importPath;
        }
        String pathWithNoProtocols = importPath.replace(FRONTEND_PROTOCOL_PREFIX, "");
        URL url = resourceUrlInJars(pathWithNoProtocols);
        return String.format("%s%s", url != null ? FLOW_PACKAGE : "./", pathWithNoProtocols);
    }

    private URL resourceUrlInJars(String resource) {
        URL url = annotationValuesExtractor.projectClassLoader.getResource(
                NON_WEB_JAR_RESOURCE_PATH + "/" + resource.replaceFirst(FLOW_PACKAGE, ""));

        if (url != null && url.getPath().contains(".jar!")) {
            installFileToNode(url);
            return url;
        }
        return null;
    }

    private void installFileToNode(URL source) {
        try {
            String name = source.getPath().replaceFirst(".*" + NON_WEB_JAR_RESOURCE_PATH, "");
            File destination = new File(getFlowPackage(), name);
            FileUtils.forceMkdir(destination.getParentFile());
            FileUtils.copyURLToFile(source, destination);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String htmlImportToJsModule(String htmlImport) {
        String module = resolveInFlowFrontendDirectory( // @formatter:off
        htmlImport
          .replaceFirst("^.*bower_components/(vaadin-[^/]*/.*)\\.html$", "@vaadin/$1.js")
          .replaceFirst("^.*bower_components/((iron|paper)-[^/]*/.*)\\.html$", "@polymer/$1.js")
          .replaceFirst("\\.html$", ".js")
      ); // @formatter:on
        return Objects.equals(module, htmlImport) ? null : module;
    }

    private String htmlImportToNpmPackage(String htmlImport) {
        String module = resolveInFlowFrontendDirectory( // @formatter:off
        htmlImport
          .replaceFirst("^.*bower_components/(vaadin-[^/]*)/.*\\.html$", "@vaadin/$1")
          .replaceFirst("^.*bower_components/((iron|paper)-[^/]*)/.*\\.html$", "@polymer/$1")
          .replaceFirst("\\.html$", ".js")
      ); // @formatter:on
        return Objects.equals(module, htmlImport) ? null : module;
    }

    Logger log() {
        return LoggerFactory.getLogger("c.v.f.s.d" + this.getClass().getSimpleName());
    }
}
