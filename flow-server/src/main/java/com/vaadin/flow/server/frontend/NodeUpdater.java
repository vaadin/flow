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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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
    private static final String DEV_DEPENDENCIES = "devDependencies";

    private static final String DEP_LICENSE_KEY = "license";
    private static final String DEP_LICENSE_DEFAULT = "UNLICENSED";
    private static final String DEP_NAME_KEY = "name";
    private static final String DEP_NAME_DEFAULT = "no-name";
    protected static final String DEP_NAME_FLOW_DEPS = "@vaadin/flow-deps";
    private static final String DEP_VERSION_KEY = "version";
    private static final String DEP_VERSION_DEFAULT = "1.0.0";

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

    List<String> resolveModules(Collection<String> modules,
            boolean isJsModule) {
        return modules.stream()
                .map(module -> resolveResource(module, isJsModule))
                .collect(Collectors.toList());
    }

    protected String resolveResource(String importPath, boolean isJsModule) {
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

    JsonObject getMainPackageJson() throws IOException {
        return getPackageJson(new File(npmFolder, PACKAGE_JSON));
    }

    JsonObject getAppPackageJson() throws IOException {
        return getPackageJson(new File(generatedFolder, PACKAGE_JSON));
    }

    JsonObject getPackageJson(File packageFile) throws IOException {
        JsonObject packageJson = null;
        if (packageFile.exists()) {
            String fileContent = FileUtils.readFileToString(packageFile,
                    UTF_8.name());
            packageJson = Json.parse(fileContent);
        }
        return packageJson;
    }

    boolean updateMainDefaultDependencies(JsonObject packageJson,
            String polymerVersion) {
        boolean added = false;
        added = addDependency(packageJson, null, DEP_NAME_KEY, DEP_NAME_DEFAULT)
                || added;
        added = addDependency(packageJson, null, DEP_LICENSE_KEY,
                DEP_LICENSE_DEFAULT) || added;

        String polymerDepVersion = polymerVersion;
        if (polymerDepVersion == null) {
            polymerDepVersion = "3.2.0";
        }

        added = addDependency(packageJson, DEPENDENCIES, "@polymer/polymer",
                polymerDepVersion) || added;
        added = addDependency(packageJson, DEPENDENCIES,
                "@webcomponents/webcomponentsjs", "^2.2.10") || added;
        // dependency for the custom package.json placed in the generated
        // folder.
        try {
            String customPkg = "./" + npmFolder.getAbsoluteFile().toPath()
                    .relativize(generatedFolder.getAbsoluteFile().toPath())
                    .toString();
            added = addDependency(packageJson, DEPENDENCIES, DEP_NAME_FLOW_DEPS,
                    customPkg.replaceAll("\\\\", "/")) || added;
        } catch (IllegalArgumentException iae) {
            log().error("Exception in relativization of '%s' to '%s'",
                    npmFolder.getAbsoluteFile().toPath(),
                    generatedFolder.getAbsoluteFile().toPath());
            throw iae;
        }
        added = addDependency(packageJson, DEV_DEPENDENCIES, "webpack",
                "4.30.0") || added;
        added = addDependency(packageJson, DEV_DEPENDENCIES, "webpack-cli",
                "3.3.0") || added;
        added = addDependency(packageJson, DEV_DEPENDENCIES,
                "webpack-dev-server", "3.3.0") || added;
        added = addDependency(packageJson, DEV_DEPENDENCIES,
                "webpack-babel-multi-target-plugin", "2.1.0") || added;
        added = addDependency(packageJson, DEV_DEPENDENCIES,
                "copy-webpack-plugin", "5.0.3") || added;
        added = addDependency(packageJson, DEV_DEPENDENCIES,
                "compression-webpack-plugin", "3.0.0") || added;
        added = addDependency(packageJson, DEV_DEPENDENCIES, "webpack-merge",
                "4.2.1") || added;
        added = addDependency(packageJson, DEV_DEPENDENCIES, "raw-loader",
                "3.0.0") || added;
        return added;
    }

    void updateAppDefaultDependencies(JsonObject packageJson) {
        addDependency(packageJson, null, DEP_NAME_KEY, DEP_NAME_FLOW_DEPS);
        addDependency(packageJson, null, DEP_VERSION_KEY, DEP_VERSION_DEFAULT);
        addDependency(packageJson, null, DEP_LICENSE_KEY, DEP_LICENSE_DEFAULT);
    }

    boolean addDependency(JsonObject json, String key, String pkg,
            String vers) {
        if (key != null) {
            if (!json.hasKey(key)) {
                json.put(key, Json.createObject());
            }
            json = json.get(key);
        }
        if (!json.hasKey(pkg) || !json.getString(pkg).equals(vers)) {
            json.put(pkg, vers);
            log().info("Added \"{}\": \"{}\" line.", pkg, vers);
            return true;
        }
        return false;
    }

    String writeMainPackageFile(JsonObject packageJson) throws IOException {
        return writePackageFile(packageJson, new File(npmFolder, PACKAGE_JSON));
    }

    String writeAppPackageFile(JsonObject packageJson) throws IOException {
        return writePackageFile(packageJson,
                new File(generatedFolder, PACKAGE_JSON));
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
