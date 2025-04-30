/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependencies;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;

import elemental.json.Json;
import elemental.json.JsonException;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

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
 * <p>
 * For internal use only. May be renamed or removed in a future release.
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
    static final String HASH_KEY = "hash";
    static final String DEV_DEPENDENCIES = "devDependencies";

    private static final String DEP_LICENSE_KEY = "license";
    private static final String DEP_LICENSE_DEFAULT = "UNLICENSED";
    private static final String DEP_NAME_KEY = "name";
    private static final String DEP_NAME_DEFAULT = "no-name";
    protected static final String POLYMER_VERSION = "3.2.0";

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

    private File getPackageJsonFile() {
        return new File(npmFolder, PACKAGE_JSON);
    }

    /**
     * Gets the platform pinned versions that are not overridden by the user in
     * package.json.
     *
     * @return json object with the dependencies or {@code null}
     * @throws IOException
     *             when versions file could not be read
     */
    JsonObject getPlatformPinnedDependencies() throws IOException {
        URL coreVersionsResource = finder
                .getResource(Constants.VAADIN_CORE_VERSIONS_JSON);
        if (coreVersionsResource == null) {
            log().info(
                    "Couldn't find {} file to pin dependency versions for core components."
                            + " Transitive dependencies won't be pinned for npm/pnpm.",
                    Constants.VAADIN_CORE_VERSIONS_JSON);
            return null;
        }

        JsonObject versionsJson = getFilteredVersionsFromResource(
                coreVersionsResource, Constants.VAADIN_CORE_VERSIONS_JSON);

        URL vaadinVersionsResource = finder
                .getResource(Constants.VAADIN_VERSIONS_JSON);
        if (vaadinVersionsResource == null) {
            // vaadin is not on the classpath, only vaadin-core is present.
            return versionsJson;
        }

        JsonObject vaadinVersionsJson = getFilteredVersionsFromResource(
                vaadinVersionsResource, Constants.VAADIN_VERSIONS_JSON);
        for (String key : vaadinVersionsJson.keys()) {
            versionsJson.put(key, vaadinVersionsJson.getString(key));
        }

        return versionsJson;
    }

    private JsonObject getFilteredVersionsFromResource(URL versionsResource,
            String versionsOrigin) throws IOException {
        JsonObject versionsJson;
        try (InputStream content = versionsResource.openStream()) {
            VersionsJsonConverter convert = new VersionsJsonConverter(Json
                    .parse(IOUtils.toString(content, StandardCharsets.UTF_8)));
            versionsJson = convert.getConvertedJson();
            versionsJson = new VersionsJsonFilter(getPackageJson(),
                    DEPENDENCIES)
                    .getFilteredVersions(versionsJson, versionsOrigin);
        }
        return versionsJson;
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
        JsonObject packageJson = getJsonFileContent(getPackageJsonFile());
        if (packageJson == null) {
            packageJson = Json.createObject();
            packageJson.put(DEP_NAME_KEY, DEP_NAME_DEFAULT);
            packageJson.put(DEP_LICENSE_KEY, DEP_LICENSE_DEFAULT);
        }

        addDefaultObjects(packageJson);
        addVaadinDefaultsToJson(packageJson);
        return packageJson;
    }

    private void addDefaultObjects(JsonObject json) {
        computeIfAbsent(json, DEPENDENCIES, Json::createObject);
        computeIfAbsent(json, DEV_DEPENDENCIES, Json::createObject);
    }

    static JsonObject getJsonFileContent(File packageFile) throws IOException {
        JsonObject jsonContent = null;
        if (packageFile.exists()) {
            String fileContent = FileUtils.readFileToString(packageFile,
                    UTF_8.name());
            try {
                jsonContent = Json.parse(fileContent);
            } catch (JsonException e) {
                throw new JsonException(String
                        .format("Cannot parse package file '%s'", packageFile));
            }
        }
        return jsonContent;
    }

    static void addVaadinDefaultsToJson(JsonObject json) {
        JsonObject vaadinPackages = computeIfAbsent(json, VAADIN_DEP_KEY,
                Json::createObject);

        computeIfAbsent(vaadinPackages, DEPENDENCIES, () -> {
            final JsonObject dependencies = Json.createObject();
            getDefaultDependencies().forEach(dependencies::put);
            return dependencies;
        });
        computeIfAbsent(vaadinPackages, DEV_DEPENDENCIES, () -> {
            final JsonObject devDependencies = Json.createObject();
            getDefaultDevDependencies().forEach(devDependencies::put);
            return devDependencies;
        });
        computeIfAbsent(vaadinPackages, HASH_KEY, () -> Json.create(""));
    }

    private static <T extends JsonValue> T computeIfAbsent(
            JsonObject jsonObject, String key, Supplier<T> valueSupplier) {
        T result = jsonObject.get(key);
        if (result == null) {
            result = valueSupplier.get();
            jsonObject.put(key, result);
        }
        return result;
    }

    static Map<String, String> getDefaultDependencies() {
        Map<String, String> defaults = new HashMap<>();

        defaults.put("@polymer/polymer", POLYMER_VERSION);
        defaults.put("@webcomponents/webcomponentsjs", "^2.2.10");
        defaults.put("lit-element", "2.5.1");
        defaults.put("lit-html", "1.4.1");

        // Constructable style sheets is only implemented for chrome,
        // polyfill needed for FireFox et.al. at the moment
        defaults.put("construct-style-sheets-polyfill", "3.0.4");
        return defaults;
    }

    static Map<String, String> getDefaultDevDependencies() {
        Map<String, String> defaults = new HashMap<>();

        defaults.put("webpack", "5.99.6");
        defaults.put("webpack-cli", "6.0.1");
        defaults.put("webpack-dev-server", "5.2.1");
        // Defining loader until a resolution exists to issue
        // https://github.com/DanielSchaffer/webpack-babel-multi-target-plugin/issues/94
        defaults.put("babel-loader", "10.0.0");
        defaults.put("@babel/preset-env", "7.27.1");
        // Defining html-webpack-plugin due to same issue as babel-loader
        defaults.put("html-webpack-plugin", "5.6.3");
        defaults.put("copy-webpack-plugin", "13.0.0");
        defaults.put("compression-webpack-plugin", "11.1.0");
        defaults.put("extra-watch-webpack-plugin", "1.0.3");
        defaults.put("webpack-merge", "6.0.1");
        defaults.put("css-loader", "7.1.2");

        defaults.put("typescript", "4.9.5");
        defaults.put("ts-loader", "9.5.2");
        defaults.put("lit-css-loader", "3.0.1");
        defaults.put("loader-utils", "3.3.1");

        // Forcing chokidar version for now until new babel version is available
        // check out https://github.com/babel/babel/issues/11488
        defaults.put("chokidar", "^3.5.0");
        defaults.put("mkdirp", "3.0.1");

        defaults.put("@types/node", "18.14.0");
        return defaults;
    }

    /**
     * Updates default dependencies and development dependencies to
     * package.json.
     *
     * @param packageJson
     *            package.json json object to update with dependencies
     * @return true if items were added or removed from the {@code packageJson}
     */
    boolean updateDefaultDependencies(JsonObject packageJson) {
        int added = 0;

        for (Map.Entry<String, String> entry : getDefaultDependencies()
                .entrySet()) {
            added += addDependency(packageJson, DEPENDENCIES, entry.getKey(),
                    entry.getValue());
        }
        for (Map.Entry<String, String> entry : getDefaultDevDependencies()
                .entrySet()) {
            added += addDependency(packageJson, DEV_DEPENDENCIES,
                    entry.getKey(), entry.getValue());
        }

        if (added > 0) {
            log().info("Added {} default dependencies to main package.json",
                    added);
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
        FrontendVersion vaadinVersion = toVersion(vaadinDeps, pkg);
        if (json.hasKey(pkg)) {
            try {
                FrontendVersion packageVersion = toVersion(json, pkg);
                FrontendVersion newVersion = new FrontendVersion(version);
                // Vaadin and package.json versions are the same, but dependency
                // updates (can be up or down)
                if (vaadinVersion.isEqualTo(packageVersion)
                        && !vaadinVersion.isEqualTo(newVersion)) {
                    json.put(pkg, version);
                    added = true;
                    // if vaadin and package not the same, but new version is
                    // newer
                    // update package version.
                } else if (newVersion.isNewerThan(packageVersion)) {
                    json.put(pkg, version);
                    added = true;
                }
            } catch (NumberFormatException e) { // NOSONAR
                /*
                 * If the current version is not parseable, it can refer to a
                 * file and we should leave it alone
                 */
            }
        } else {
            json.put(pkg, version);
            added = true;
        }
        // always update vaadin version to the latest set version
        vaadinDeps.put(pkg, version);

        if (added) {
            log().debug("Added \"{}\": \"{}\" line.", pkg, version);
        } else {
            // we made a change to the package json vaadin defaults
            // even if we didn't add to the dependencies.
            added = !vaadinVersion.isEqualTo(new FrontendVersion(version));
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
        log().debug("Updated npm {}.", packageFile.getAbsolutePath());
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
