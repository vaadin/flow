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
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.server.Command;

import static com.vaadin.flow.server.Constants.RESOURCES_FRONTEND_DEFAULT;
import static com.vaadin.flow.server.frontend.FrontendUtils.FLOW_NPM_PACKAGE_NAME;
import static com.vaadin.flow.shared.ApplicationConstants.FRONTEND_PROTOCOL_PREFIX;

/**
 * Base abstract class for frontend updaters that needs to be run when in
 * dev-mode or from the flow maven plugin.
 */

public abstract class NodeUpdater implements Command {
    /**
     * Relative paths of generated should be prefixed with this value, so
     * they can be correctly separated from {projectDir}/frontend files.
     */
    static final String GENERATED_PREFIX = "GENERATED/";
    static final String VALUE = "value";

    /**
     * Folder with the <code>package.json</code> file.
     */
    protected final File npmFolder;

    /**
     * The path to the {@literal node_modules} directory of the project.
     */
    protected final File nodeModulesPath;

    /**
     * Enable or disable legacy components annotated only with
     * {@link HtmlImport}.
     */
    protected final boolean convertHtml;

    /**
     * The {@link FrontendDependencies} object representing the application
     * dependencies.
     */
    protected final FrontendDependencies frontDeps;

    private final ClassFinder finder;

    private final Set<String> flowModules = new HashSet<>();

    /**
     * Constructor.
     *
     * @param finder
     *            a reusable class finder
     * @param npmFolder
     *            folder with the `package.json` file
     * @param nodeModulesPath
     *            the path to the {@literal node_modules} directory of the
     *            project
     * @param convertHtml
     *            true to enable polymer-2 annotated classes to be considered
     */
    protected NodeUpdater(ClassFinder finder, File npmFolder, File nodeModulesPath, boolean convertHtml) {
        this(finder, null, npmFolder, nodeModulesPath, convertHtml);
    }

    /**
     * Constructor.
     *
     * @param finder
     *            a reusable class finder
     * @param frontendDependencies
     *            a reusable frontend dependencies
     * @param npmFolder
     *            folder with the `package.json` file
     * @param nodeModulesPath
     *            the path to the {@literal node_modules} directory of the
     *            project
     * @param convertHtml
     *            true to enable polymer-2 annotated classes to be considered
     */
    protected NodeUpdater(ClassFinder finder, FrontendDependencies frontendDependencies, File npmFolder, File nodeModulesPath, boolean convertHtml) {
        this.frontDeps = frontendDependencies == null
                ? new FrontendDependencies(finder)
                : frontendDependencies;
        this.finder = finder;
        this.npmFolder = npmFolder;
        this.nodeModulesPath = nodeModulesPath;
        this.convertHtml = convertHtml;
    }

    Set<String> getHtmlImportJsModules(Set<String> htmlImports) {
        return htmlImports.stream().map(this::htmlImportToJsModule).filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    Set<String> getHtmlImportNpmPackages(Set<String> htmlImports) {
        return htmlImports.stream().map(this::htmlImportToNpmPackage).filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    Set<String> getJavascriptJsModules(Set<String> javascripts) {
        return javascripts.stream().map(this::resolveInFlowFrontendDirectory)
                .collect(Collectors.toSet());
    }

    Set<String> getTargetFrontendModules(File directory, Set<String> excludes) {
        if (!directory.exists()) {
            return Collections.emptySet();
        }

        final Function<String, String> unixPath = str -> str.replace("\\", "/");

        final URI baseDir = directory.toURI();

        return FileUtils.listFiles(directory, new String[]{"js"}, true)
                .stream()
                .filter(file -> {
                    String path = unixPath.apply(file.getPath());
                    return excludes.stream().noneMatch(postfix ->
                            path.endsWith(unixPath.apply(postfix)));
                })
                .map(file -> GENERATED_PREFIX + unixPath.apply(baseDir.relativize(file.toURI()).getPath()))
                .collect(Collectors.toSet());
    }

    private String resolveInFlowFrontendDirectory(String importPath) {
        if (importPath.startsWith("@")) {
            return importPath;
        }
        String pathWithNoProtocols = importPath.replace(FRONTEND_PROTOCOL_PREFIX, "");

        if (flowModules.contains(pathWithNoProtocols) || getResourceUrl(pathWithNoProtocols) != null) {
          flowModules.add(pathWithNoProtocols);
          return FLOW_NPM_PACKAGE_NAME + pathWithNoProtocols;
        }
        return pathWithNoProtocols;
    }

    private URL getResourceUrl(String resource) {
        resource = RESOURCES_FRONTEND_DEFAULT + "/" + resource.replaceFirst(FLOW_NPM_PACKAGE_NAME, "");
        URL url = finder.getResource(resource);
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

    static Logger log() {
        // Using short prefix so as npm output is more readable
        return LoggerFactory.getLogger("dev-updater");
    }
}
