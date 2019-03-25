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

import static com.vaadin.flow.server.Constants.NON_WEB_JAR_RESOURCE_PATH;
import static com.vaadin.flow.shared.ApplicationConstants.FRONTEND_PROTOCOL_PREFIX;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.dependency.HtmlImport;

/**
 * Base interface for methods for updating node_js files.
 */
public abstract class NodeUpdater implements Serializable {

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

    AnnotationValuesExtractor annotationValuesExtractor;

    private Set<String> flowModules = new HashSet<>();

    public abstract void execute();

    public File getFlowPackage() {
        return new File(npmFolder, flowPackagePath);
    }

    protected Set<String> getHtmlImportJsModules(Set<String> htmlImports) {
        return htmlImports.stream().map(this::htmlImportToJsModule).filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    protected Set<String> getHtmlImportNpmPackages(Set<String> htmlImports) {
        return htmlImports.stream().map(this::htmlImportToNpmPackage).filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    protected String resolveInFlowFrontendDirectory(String importPath) {
        if (importPath.startsWith("@")) {
            return importPath;
        }
        String pathWithNoProtocols = importPath.replace(FRONTEND_PROTOCOL_PREFIX, "");

        if (flowModules.contains(pathWithNoProtocols) || getResourceUrl(pathWithNoProtocols) != null) {
          flowModules.add(pathWithNoProtocols);
          return FLOW_PACKAGE + pathWithNoProtocols;
        }
        return "./" + pathWithNoProtocols;
    }

    protected void installFlowModules() throws IOException {
        for (String resource : flowModules) {
            URL source = getResourceUrl(resource);
            File destination = new File(getFlowPackage(), resource);
            FileUtils.forceMkdir(destination.getParentFile());
            FileUtils.copyURLToFile(source, destination);
        }
        log().info("Installed " + flowModules.size() + " " + FLOW_PACKAGE + " modules.");
    }

    private URL getResourceUrl(String resource) {
      URL url = annotationValuesExtractor.projectClassLoader.getResource(
          NON_WEB_JAR_RESOURCE_PATH + "/" + resource.replaceFirst(FLOW_PACKAGE, ""));
      return url != null && url.getPath().contains(".jar!") ? url : null;
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
