/*
 * Copyright 2000-2019 Vaadin Ltd.
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependencies;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;

import elemental.json.Json;
import elemental.json.JsonObject;
import static com.vaadin.flow.server.Constants.COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT;
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
 *
 * @since 2.0
 */
public abstract class NodeUpdater implements FallibleCommand {
    /**
     * Relative paths of generated should be prefixed with this value, so they
     * can be correctly separated from {projectDir}/frontend files.
     */
    static final String GENERATED_PREFIX = "GENERATED/";

    static final String DEPENDENCIES = "dependencies";
    static final String VAADIN_DEP_KEY = "vaadin";
    static final String DEV_DEPENDENCIES = "devDependencies";

    private static final String DEP_LICENSE_KEY = "license";
    private static final String DEP_LICENSE_DEFAULT = "UNLICENSED";
    private static final String DEP_NAME_KEY = "name";
    private static final String DEP_NAME_DEFAULT = "no-name";
    public static final Logger LOGGER = LoggerFactory.getLogger("dev-updater");

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
     * The {@link FrontendDependencies} object representing the application
     * dependencies.
     */
    protected final FrontendDependenciesScanner frontDeps;

    private final ClassFinder finder;

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
     */
    protected NodeUpdater(ClassFinder finder,
            FrontendDependenciesScanner frontendDependencies, File npmFolder,
            File generatedPath) {
        this.frontDeps = frontendDependencies;
        this.finder = finder;
        this.npmFolder = npmFolder;
        this.nodeModulesFolder = new File(npmFolder, NODE_MODULES);
        this.generatedFolder = generatedPath;
    }

    static Set<String> getGeneratedModules(File directory,
            Set<String> excludes) {
        if (!directory.exists()) {
            return Collections.emptySet();
        }

        final Function<String, String> unixPath = str -> str.replace("\\", "/");

        final URI baseDir = directory.toURI();

        return FileUtils.listFiles(directory, new String[] { "js" }, true)
                .stream().filter(file -> {
                    String path = unixPath.apply(file.getPath());
                    if (path.contains("/node_modules/")) {
                        return false;
                    }
                    return excludes.stream().noneMatch(
                            postfix -> path.endsWith(unixPath.apply(postfix)));
                })
                .map(file -> GENERATED_PREFIX + unixPath
                        .apply(baseDir.relativize(file.toURI()).getPath()))
                .collect(Collectors.toSet());
    }

    String resolveResource(String importPath, boolean isJsModule) {
        String resolved = importPath;
        if (!importPath.startsWith("@")) {

            if (importPath.startsWith(FRONTEND_PROTOCOL_PREFIX)) {
                resolved = importPath.replaceFirst(FRONTEND_PROTOCOL_PREFIX,
                        "./");
                if (isJsModule) {
                    // Remove this when all flow components annotated with
                    // @JsModule have the './' prefix instead of 'frontend://'
                    log().warn(
                            "Do not use the '{}' protocol in '@JsModule', changing '{}' to '{}', please update your component.",
                            FRONTEND_PROTOCOL_PREFIX, importPath, resolved);
                }
            }

            // We only should check here those paths starting with './' when all
            // flow components
            // have the './' prefix
            String resource = resolved.replaceFirst("^\\./+", "");
            if (hasMetaInfResource(resource)) {
                if (!resolved.startsWith("./")) {
                    log().warn(
                            "Use the './' prefix for files in JAR files: '{}', please update your component.",
                            importPath);
                }
                resolved = FLOW_NPM_PACKAGE_NAME + resource;
            }
        }
        return resolved;
    }

    private boolean hasMetaInfResource(String resource) {
        return finder.getResource(
                RESOURCES_FRONTEND_DEFAULT + "/" + resource) != null
                || finder.getResource(COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT
                        + "/" + resource) != null;
    }

    JsonObject getPackageJson() throws IOException {
        JsonObject packageJson = getJsonFileContent(
                new File(npmFolder, PACKAGE_JSON));
        if (packageJson == null) {
            packageJson = Json.createObject();
            packageJson.put(DEP_NAME_KEY, DEP_NAME_DEFAULT);
            packageJson.put(DEP_LICENSE_KEY, DEP_LICENSE_DEFAULT);
            packageJson.put(VAADIN_DEP_KEY, createVaadinPackagesJson());
        }
        return packageJson;
    }

    static JsonObject getJsonFileContent(File packageFile) throws IOException {
        JsonObject jsonContent = null;
        if (packageFile.exists()) {
            String fileContent = FileUtils.readFileToString(packageFile,
                    UTF_8.name());
            jsonContent = Json.parse(fileContent);
        }
        return jsonContent;
    }

    static JsonObject createVaadinPackagesJson() {
        JsonObject vaadinPackages = Json.createObject();
        vaadinPackages.put(DEPENDENCIES, Json.createObject());
        vaadinPackages.put(DEV_DEPENDENCIES, Json.createObject());

        // Add default dependencies
        JsonObject dependencies = vaadinPackages.getObject(DEPENDENCIES);
        getDefaultDependencies().forEach((key, value) -> dependencies.put(key, value));

        // Add default developmentDependencies
        JsonObject devDependencies = vaadinPackages.getObject(DEV_DEPENDENCIES);
        getDefaultDevDependencies().forEach((key, value) -> devDependencies.put(key, value));

        return vaadinPackages;
    }

    static Map<String, String> getDefaultDependencies() {
        Map<String, String> defaults = new HashMap<>();

        defaults.put("@polymer/polymer", "3.2.0");
        defaults.put("@webcomponents/webcomponentsjs", "^2.2.10");

        return defaults;
    }

    static Map<String, String> getDefaultDevDependencies() {
        Map<String, String> defaults = new HashMap<>();

        defaults.put("webpack", "4.30.0");
        defaults.put("webpack-cli", "3.3.0");
        defaults.put("webpack-dev-server", "3.3.0");
        defaults.put("webpack-babel-multi-target-plugin", "2.3.1");
        defaults.put("copy-webpack-plugin", "5.0.3");
        defaults.put("compression-webpack-plugin", "3.0.0");
        defaults.put("webpack-merge", "4.2.1");
        defaults.put("raw-loader", "3.0.0");

        return defaults;
    }

    /**
     * Update default dependencies with the framework default polymer version.
     *
     * @param packageJson
     *         package.json object to update
     * @return true if items were added or removed from the json
     */
    boolean updateDefaultDependencies(JsonObject packageJson) {
        return updateDefaultDependencies(packageJson, null);
    }

    /**
     * Updates default dependencies and development dependencies to
     * package.json.
     *
     * @param packageJson
     *         package.json json object to update with dependencies
     * @param polymerVersion
     *         user set polymer dependency, {@code null} for framework default
     * @return true if items were added or removed from the {@code packageJson}
     */
    boolean updateDefaultDependencies(JsonObject packageJson,
            String polymerVersion) {
        int added = 0;

        added += addDependency(packageJson, DEPENDENCIES, "@polymer/polymer",
                polymerVersion);
        added += addDependency(packageJson, DEPENDENCIES,
                "@webcomponents/webcomponentsjs", "^2.2.10");

        added += addDependency(packageJson, DEV_DEPENDENCIES, "webpack",
                "4.30.0");
        added += addDependency(packageJson, DEV_DEPENDENCIES, "webpack-cli",
                "3.3.0");
        added += addDependency(packageJson, DEV_DEPENDENCIES,
                "webpack-dev-server", "3.3.0");
        added += addDependency(packageJson, DEV_DEPENDENCIES,
                "webpack-babel-multi-target-plugin", "2.3.1");
        added += addDependency(packageJson, DEV_DEPENDENCIES,
                "copy-webpack-plugin", "5.0.3");
        added += addDependency(packageJson, DEV_DEPENDENCIES,
                "compression-webpack-plugin", "3.0.0");
        added += addDependency(packageJson, DEV_DEPENDENCIES, "webpack-merge",
                "4.2.1");
        added += addDependency(packageJson, DEV_DEPENDENCIES, "raw-loader",
                "3.0.0");
        if(added > 0) {
            log().info("Added {} dependencies to main package.json", added);
        }
        return added > 0;
    }

    int addDependency(JsonObject json, String key, String pkg, String version) {
        Objects.requireNonNull(json, "Json object need to be given");
        Objects.requireNonNull(key, "Json sub object needs to be give.");
        Objects.requireNonNull(pkg, "dependency package needs to be defined");

        JsonObject vaadinDeps = json.getObject(VAADIN_DEP_KEY);
        if (!json.hasKey(key)) {
            json.put(key, Json.createObject());
        }
        json = json.get(key);
        vaadinDeps = vaadinDeps.getObject(key);

        if (vaadinDeps.hasKey(pkg)) {
            if (version == null) {
                version = vaadinDeps.getString(pkg);
            }
            return handleExistingVaadinDep(json, pkg, version, vaadinDeps);
        } else {
            vaadinDeps.put(pkg, version);
            if (!json.hasKey(pkg) || new FrontendVersion(version)
                    .isNewerThan(toVersion(json, pkg))) {
                json.put(pkg, version);
                log().debug("Added \"{}\": \"{}\" line.", pkg, version);
                return 1;
            }
        }
        return 0;
    }

    private int handleExistingVaadinDep(JsonObject json, String pkg,
            String version, JsonObject vaadinDeps) {
        boolean added = false;
        FrontendVersion newVersion = new FrontendVersion(version);
        FrontendVersion vaadinVersion = toVersion(vaadinDeps, pkg);
        if (json.hasKey(pkg)) {
            FrontendVersion packageVersion = toVersion(json, pkg);
            // Vaadin and package.json versions are the same, but dependency updates (can be up or down)
            if (vaadinVersion.equals(packageVersion) && !vaadinVersion
                    .equals(newVersion)) {
                json.put(pkg, version);
                added = true;
                // if vaadin and package not the same, but new version is newer update package version.
            } else if (newVersion.isNewerThan(packageVersion)) {
                json.put(pkg, version);
                added = true;
            }
        } else {
            json.put(pkg, version);
            added = true;
        }
        // always update vaadin version to the latest set version
        vaadinDeps.put(pkg, version);

        if(added) {
            log().debug("Added \"{}\": \"{}\" line.", pkg, version);
        }
        return added ? 1 : 0;
    }

    private static FrontendVersion toVersion(JsonObject json, String key) {
        return new FrontendVersion(json.getString(key));
    }

    String writePackageFile(JsonObject packageJson) throws IOException {
        return writePackageFile(packageJson, new File(npmFolder, PACKAGE_JSON));
    }

    String writePackageFile(JsonObject json, File packageFile)
            throws IOException {
        log().info("Updated npm {}.", packageFile.getAbsolutePath());
        FileUtils.forceMkdirParent(packageFile);
        String content = stringify(json, 2) + "\n";
        FileUtils.writeStringToFile(packageFile, content, UTF_8.name());
        return content;
    }

    Logger log() {
        // Using short prefix so as npm output is more readable
        return LoggerFactory.getLogger("dev-updater");
    }
}
