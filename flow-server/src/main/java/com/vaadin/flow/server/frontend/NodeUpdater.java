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

import static com.vaadin.flow.server.Constants.RESOURCES_FRONTEND_DEFAULT;
import static com.vaadin.flow.shared.ApplicationConstants.FRONTEND_PROTOCOL_PREFIX;

/**
 * Base abstract class for frontend updaters that needs to be run when in
 * dev-mode or from the flow maven plugin.
 */
public abstract class NodeUpdater implements Serializable {
    /**
     * NPM package name that will be used for the javascript files present in
     * jar resources that will to be copied to the npm folder so as they are
     * accessible to webpack.
     */
    public static final String FLOW_PACKAGE = "@vaadin/flow-frontend/";

    /**
     * An analogue of {@link com.vaadin.flow.shared.ApplicationConstants#FRONTEND_PROTOCOL_PREFIX} for webpack.
     * This value is used for pointing at the project's resources located in the frontend directory.
     */
    protected static final String WEBPACK_PREFIX_ALIAS = "Frontend/";

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
     *            the path to the {@literal node_modules} directory of the project
     * @param convertHtml
     *            true to enable polymer-2 annotated classes to be considered
     */
    protected NodeUpdater(ClassFinder finder, File npmFolder, File nodeModulesPath, boolean convertHtml) {
        this.frontDeps = new FrontendDependencies(finder);
        this.finder = finder;
        this.npmFolder = npmFolder;
        this.nodeModulesPath = nodeModulesPath;
        this.convertHtml = convertHtml;
    }

    /**
     * Execute the update process.
     */
    public abstract void execute();

    public File getFlowPackage() {
        return new File(nodeModulesPath, FLOW_PACKAGE);
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

    String toValidBrowserImport(String s) {
        // add `./` prefix to names starting with letters
        return Character.isAlphabetic(s.charAt(0)) ? "./" + s : s;
    }

    String resolveInFlowFrontendDirectory(String importPath) {
        if (importPath.startsWith("@")) {
            return importPath;
        }
        String pathWithNoProtocols = importPath.replace(FRONTEND_PROTOCOL_PREFIX, "");

        if (flowModules.contains(pathWithNoProtocols) || getResourceUrl(pathWithNoProtocols) != null) {
          flowModules.add(pathWithNoProtocols);
          return FLOW_PACKAGE + pathWithNoProtocols;
        }
        return toValidBrowserImport(pathWithNoProtocols);
    }

    String toValidBrowserImport(String s) {
        // add `Frontend/` prefix to names starting with letters
        if (Character.isAlphabetic(s.charAt(0))) {
            return WEBPACK_PREFIX_ALIAS + s.substring(1);
        } else if (s.startsWith("./")) {
            return WEBPACK_PREFIX_ALIAS + s.substring(2);
        }
        return s;
    }

    void installFlowModules() throws IOException {
        for (String resource : flowModules) {
            URL source = getResourceUrl(resource);
            File destination = new File(getFlowPackage(), resource);
            FileUtils.forceMkdir(destination.getParentFile());
            FileUtils.copyURLToFile(source, destination);
        }
        if (log().isInfoEnabled()) {
            log().info("Installed {} {} modules", flowModules.size(), FLOW_PACKAGE);
        }
    }

    private URL getResourceUrl(String resource) {
        resource = RESOURCES_FRONTEND_DEFAULT + "/" + resource.replaceFirst(FLOW_PACKAGE, "");
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

    Logger log() {
        // Using short prefix so as npm output is more readable
        return LoggerFactory.getLogger("dev-updater");
    }
}
