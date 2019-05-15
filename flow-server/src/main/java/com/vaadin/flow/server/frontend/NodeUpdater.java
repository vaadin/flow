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
import com.vaadin.flow.server.Constants;

import elemental.json.Json;
import elemental.json.JsonObject;

import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.Constants.RESOURCES_FRONTEND_DEFAULT;
import static com.vaadin.flow.server.frontend.FrontendUtils.FLOW_NPM_PACKAGE_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.NODE_MODULES;
import static com.vaadin.flow.shared.ApplicationConstants.FRONTEND_PROTOCOL_PREFIX;
import static elemental.json.impl.JsonUtil.stringify;
import static java.nio.charset.StandardCharsets.UTF_8;
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

    static final String DEPENDENCIES = "dependencies";
    private static final String DEV_DEPENDENCIES = "devDependencies";

    private static final String DEP_LICENSE_KEY = "license";
    private static final String DEP_LICENSE_DEFAULT = "UNLICENSED";
    private static final String DEP_NAME_KEY = "name";
    private static final String DEP_NAME_DEFAULT = "no-name";
    private static final String DEP_NAME_FLOW_DEPS = "@vaadin/flow-deps";

    /**
     * Base directory for {@link Constants#PACKAGE_JSON},
     * {@link FrontendUtils#WEBPACK_CONFIG}, {@link FrontendUtils#NODE_MODULES}.
     */
    protected final File npmFolder;

    /**
     * The path to the {@link FrontendUtils#NODE_MODULES} directory.
     */
    protected final File nodeModulesFolder;


    /**
     * Base directory for flow generated files.
     */
    protected final File generatedFolder;

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

    boolean modified;

    /**
     * Constructor.
     *
     * @param finder
     *            a reusable class finder
     * @param frontendDependencies
     *            a reusable frontend dependencies
     * @param npmFolder
     *            folder with the `package.json` file
     * @param generatedPath
     *            folder where flow generated files will be placed.
     * @param convertHtml
     *            true to enable polymer-2 annotated classes to be considered
     */
    protected NodeUpdater(ClassFinder finder, FrontendDependencies frontendDependencies, File npmFolder,
            File generatedPath, boolean convertHtml) {
        this.frontDeps = finder != null && frontendDependencies == null
                ? new FrontendDependencies(finder)
                : frontendDependencies;
        this.finder = finder;
        this.npmFolder = npmFolder;
        this.nodeModulesFolder = new File(npmFolder, NODE_MODULES);
        this.generatedFolder = generatedPath;
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

    Set<String> getGeneratedModules(File directory, Set<String> excludes) {
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
        return finder.getResource(resource);
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

    JsonObject getMainPackageJson() throws IOException {
        return getPackageJson(new File(npmFolder, PACKAGE_JSON));
    }

    JsonObject getAppPackageJson() throws IOException {
        return getPackageJson(new File(generatedFolder, PACKAGE_JSON));
    }

    JsonObject getPackageJson(File packageFile) throws IOException {
        JsonObject packageJson = null;
        if (packageFile.exists()) {
            String fileContent = FileUtils.readFileToString(packageFile, UTF_8.name());
            packageJson = Json.parse(fileContent);
        }
        return packageJson;
    }

    boolean updateMainDefaultDependencies(JsonObject packageJson) {
        boolean modified = false;
        modified = addDependency(packageJson, null, DEP_NAME_KEY, DEP_NAME_DEFAULT) || modified;
        modified = addDependency(packageJson, null, DEP_LICENSE_KEY, DEP_LICENSE_DEFAULT) || modified;


        modified = addDependency(packageJson, DEPENDENCIES, "@polymer/polymer", "^3.1.0") || modified;
        modified = addDependency(packageJson, DEPENDENCIES, "@webcomponents/webcomponentsjs", "^2.2.10") || modified;
        // dependency for the custom package.json placed in the generated folder.
        String customPkg = "./" + npmFolder.getAbsoluteFile().toPath()
                .relativize(generatedFolder.toPath()).toString();
        modified = addDependency(packageJson, DEPENDENCIES, DEP_NAME_FLOW_DEPS, customPkg) || modified;

        modified = addDependency(packageJson, DEV_DEPENDENCIES, "webpack", "^4.30.0") || modified;
        modified = addDependency(packageJson, DEV_DEPENDENCIES, "webpack-cli", "^3.3.0") || modified;
        modified = addDependency(packageJson, DEV_DEPENDENCIES, "webpack-dev-server", "^3.3.0") || modified;
        modified = addDependency(packageJson, DEV_DEPENDENCIES, "webpack-babel-multi-target-plugin", "^2.1.0") || modified;
        modified = addDependency(packageJson, DEV_DEPENDENCIES, "copy-webpack-plugin", "^5.0.3") || modified;
        return modified;
    }

    void updateAppDefaultDependencies(JsonObject packageJson) {
        addDependency(packageJson, null, DEP_NAME_KEY, DEP_NAME_FLOW_DEPS);
        addDependency(packageJson, null, DEP_LICENSE_KEY, DEP_LICENSE_DEFAULT);
    }

    boolean addDependency(JsonObject json, String key, String pkg, String vers) {
        if (key != null) {
            if (!json.hasKey(key)) {
                json.put(key, Json.createObject());
            }
            json = json.get(key);
        }
        if (!json.hasKey(pkg) || !json.getString(pkg).equals(vers)) {
            json.put(pkg, vers);
            log().info("Added {}@{} dependency.", pkg, vers);
            return true;
        }
        return false;
    }

    void writeMainPackageFile(JsonObject packageJson) throws IOException {
        writePackageFile(packageJson, new File(npmFolder, PACKAGE_JSON));
    }

    void writeAppPackageFile(JsonObject packageJson) throws IOException {
        writePackageFile(packageJson, new File(generatedFolder, PACKAGE_JSON));
    }

    void writePackageFile(JsonObject json, File packageFile) throws IOException {
        log().info("Updating npm {} file ...", packageFile.getAbsolutePath());
        packageFile.getParentFile().mkdirs();
        FileUtils.writeStringToFile(packageFile, stringify(json, 2) + "\n", UTF_8.name());
    }

    static Logger log() {
        // Using short prefix so as npm output is more readable
        return LoggerFactory.getLogger("dev-updater");
    }
}
