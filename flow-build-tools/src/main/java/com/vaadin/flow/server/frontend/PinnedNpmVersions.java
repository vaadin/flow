/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.internal.FrontendVersion;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.JsonDecodingException;
import com.vaadin.flow.internal.ResourceFolderUtil;
import com.vaadin.flow.internal.StringUtil;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.Platform;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;

import static com.vaadin.flow.server.frontend.VersionsJsonConverter.JS_VERSION;
import static com.vaadin.flow.server.frontend.VersionsJsonConverter.NPM_NAME;
import static com.vaadin.flow.server.frontend.VersionsJsonConverter.NPM_VERSION;
import static com.vaadin.flow.server.frontend.VersionsJsonConverter.VAADIN_CORE_NPM_PACKAGE;

/**
 * The npm packages whose versions are pinned and the versions to pin them to,
 * collected from all the {@value Constants#PINNED_NPM_VERSIONS_FOLDER} folders
 * on the classpath.
 * <p>
 * Each jar pinning npm package versions, such as the platform itself and the
 * component integrations, ships one or more json files in that folder. All of
 * them are read and merged into one set of packages. A package defined in
 * several files is expected to have the same version everywhere; if it does
 * not, the newest version wins and a warning is logged.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
class PinnedNpmVersions {

    private static final String JSON_SUFFIX = ".json";

    private static final String PLATFORM = "platform";

    /**
     * The npm packages only the platform declares, which tell a versions file
     * of the platform from one of any other jar.
     */
    private static final Set<String> PLATFORM_PACKAGES = Set
            .of(VAADIN_CORE_NPM_PACKAGE, "@vaadin/react-components");

    /**
     * A versions file and the location it was read from.
     */
    private record VersionsFile(String origin, JsonNode content) {
    }

    private final List<VersionsFile> files;

    private final ClassLoader classLoader;

    /**
     * Reads all versions files visible to the given class finder.
     *
     * @param finder
     *            the class finder to look up the versions files with
     * @throws IOException
     *             if the versions folders cannot be looked up
     */
    PinnedNpmVersions(ClassFinder finder) throws IOException {
        classLoader = finder.getClassLoader();
        // Sorted by origin for a deterministic order and to skip the
        // duplicates a class loader hierarchy may report for the same folder
        Map<String, VersionsFile> versionsFiles = new TreeMap<>();
        for (URL folder : finder
                .getResources(Constants.PINNED_NPM_VERSIONS_FOLDER)) {
            readVersionsFiles(folder, versionsFiles);
        }
        files = List.copyOf(versionsFiles.values());
    }

    /**
     * Reads the versions files from one folder on the classpath.
     * <p>
     * A folder that cannot be listed is skipped as a whole, with a warning:
     * there is no telling which of its files were missed, so taking the ones
     * that were read would pin an unknown part of what the folder declares. The
     * versions files of the other folders still pin what they define.
     * <p>
     * A file that is there but cannot be read is a different matter, as it is
     * known exactly what is missing, and it is skipped on its own.
     */
    private static void readVersionsFiles(URL folder,
            Map<String, VersionsFile> versionsFiles) {
        Map<String, VersionsFile> filesInFolder = new TreeMap<>();
        try {
            ResourceFolderUtil.visitFiles(folder, file -> {
                if (file.getName().endsWith(JSON_SUFFIX)) {
                    readVersionsFile(file)
                            .ifPresent(versionsFile -> filesInFolder
                                    .put(versionsFile.origin(), versionsFile));
                }
            });
        } catch (IOException e) {
            log().warn("Unable to read pinned npm versions from '{}'."
                    + " Dependencies defined there won't be pinned for npm/pnpm/bun.",
                    folder, e);
            return;
        }
        versionsFiles.putAll(filesInFolder);
    }

    /**
     * Reads one versions file, skipping it if it cannot be read or is not the
     * json the versions files are.
     * <p>
     * A versions file comes from whichever jar ships it, so one that is broken
     * is not a reason to stop pinning the packages the other files declare.
     */
    private static Optional<VersionsFile> readVersionsFile(
            ResourceFolderUtil.FolderFile file) {
        try (InputStream content = file.open()) {
            VersionsFile versionsFile = new VersionsFile(file.getLocation(),
                    JacksonUtils.readTree(StringUtil.toUTF8String(content)));
            warnAboutPackagesWithoutVersion(versionsFile);
            return Optional.of(versionsFile);
        } catch (IOException | JsonDecodingException | ClassCastException e) {
            log().warn("Unable to read the pinned npm versions of '{}'."
                    + " The packages it defines won't be pinned for npm/pnpm/bun.",
                    file.getLocation(), e);
            return Optional.empty();
        }
    }

    /**
     * Warns about the packages a versions file gives no version for.
     */
    private static void warnAboutPackagesWithoutVersion(
            VersionsFile versionsFile) {
        Set<String> packages = new TreeSet<>();
        collectPackagesWithoutVersion(versionsFile.content(), packages);
        if (!packages.isEmpty()) {
            log().warn(
                    "The npm packages {} of '{}' have no 'npmVersion' or 'jsVersion',"
                            + " so their versions are not pinned. Report it to whoever ships the file.",
                    packages, versionsFile.origin());
        }
    }

    private static void collectPackagesWithoutVersion(JsonNode obj,
            Set<String> packages) {
        for (String key : JacksonUtils.getKeys(obj)) {
            JsonNode value = obj.get(key);
            if (!(value instanceof ObjectNode)) {
                continue;
            }
            if (value.has(NPM_NAME)) {
                if (!value.has(NPM_VERSION) && !value.has(JS_VERSION)) {
                    packages.add(value.get(NPM_NAME).asString());
                }
            } else {
                collectPackagesWithoutVersion(value, packages);
            }
        }
    }

    /**
     * Checks whether any versions file was found.
     *
     * @return {@code true} if no versions file is available
     */
    boolean isEmpty() {
        return files.isEmpty();
    }

    /**
     * Gets the Vaadin version, i.e. the version of the platform the versions
     * files come from, as declared in their {@code platform} field.
     * <p>
     * Only the files of the platform itself, the ones declaring one of the npm
     * packages the platform ships under its own name, say what the version is;
     * what any other file declares is ignored, so that a jar that is not the
     * platform cannot set it. Where no file declares one of those packages, the
     * version of the Vaadin on the classpath is used rather than anything the
     * files say.
     * <p>
     * The files that count are expected to declare the same version; if they do
     * not, the newest one wins and a warning is logged, as for the packages.
     *
     * @return the Vaadin version, or empty if neither a versions file of the
     *         platform nor the classpath tells it
     */
    Optional<String> getVaadinVersion() {
        List<VersionsFile> declaringFiles = files.stream()
                .filter(this::declaresVaadinVersion).toList();
        List<VersionsFile> platformFiles = declaringFiles.stream()
                .filter(file -> declaresPlatformPackage(file.content()))
                .toList();
        if (platformFiles.isEmpty()) {
            // No file of the platform to read it from, and what another file
            // says about it is not to be trusted
            declaringFiles.forEach(file -> log().debug(
                    "Ignoring the Vaadin version '{}' of {}, as the file declares none of the packages of the platform {}.",
                    file.content().get(PLATFORM).asString(), file.origin(),
                    PLATFORM_PACKAGES));
            return Platform.getVaadinVersion(classLoader);
        }
        Optional<String> vaadinVersion = getVaadinVersion(platformFiles);
        declaringFiles.stream().filter(file -> !platformFiles.contains(file)
                && !vaadinVersion.orElseThrow()
                        .equals(file.content().get(PLATFORM).asString()))
                .forEach(file -> log().warn(
                        "Ignoring the Vaadin version '{}' of {}, as the file declares none of the packages of the platform {}. Using '{}'.",
                        file.content().get(PLATFORM).asString(), file.origin(),
                        PLATFORM_PACKAGES, vaadinVersion.orElseThrow()));
        return vaadinVersion;
    }

    private boolean declaresVaadinVersion(VersionsFile file) {
        JsonNode platform = file.content().get(PLATFORM);
        return platform != null && platform.isString();
    }

    /**
     * Checks whether the versions file is one of the platform itself, which is
     * a file declaring one of the npm packages the platform ships under its own
     * name: the core file declares the React components, and the commercial one
     * the package of the platform.
     */
    private static boolean declaresPlatformPackage(JsonNode obj) {
        for (String key : JacksonUtils.getKeys(obj)) {
            JsonNode value = obj.get(key);
            if (!(value instanceof ObjectNode)) {
                continue;
            }
            if (value.has(NPM_NAME)) {
                if (PLATFORM_PACKAGES
                        .contains(value.get(NPM_NAME).asString())) {
                    return true;
                }
            } else if (declaresPlatformPackage(value)) {
                return true;
            }
        }
        return false;
    }

    private Optional<String> getVaadinVersion(
            List<VersionsFile> versionsFiles) {
        String vaadinVersion = null;
        String origin = null;
        for (VersionsFile file : versionsFiles) {
            String version = file.content().get(PLATFORM).asString();
            if (vaadinVersion == null) {
                vaadinVersion = version;
                origin = file.origin();
            } else if (!vaadinVersion.equals(version)) {
                boolean newer = isNewerThan(version, vaadinVersion);
                log().warn(
                        "Conflicting Vaadin versions found: '{}' in {} and '{}' in {}. Using '{}'.",
                        vaadinVersion, origin, version, file.origin(),
                        newer ? version : vaadinVersion);
                if (newer) {
                    vaadinVersion = version;
                    origin = file.origin();
                }
            }
        }
        return Optional.ofNullable(vaadinVersion);
    }

    /**
     * Gets the npm packages and versions to pin, excluding the packages that
     * are not used in the current mode, the ones excluded by another package
     * and the ones the given filter drops.
     * <p>
     * Each versions file is filtered on its own, before the files are merged,
     * so that the filter reports the file a version came from, and so that a
     * version it drops falls back to the one another file declares.
     *
     * @param reactEnabled
     *            whether React is enabled
     * @param excludeWebComponents
     *            whether to exclude web component npm packages
     * @param filter
     *            the filter that keeps the versions the user has not changed
     * @return the npm package names and versions to pin
     */
    ObjectNode getDependencies(boolean reactEnabled,
            boolean excludeWebComponents, VersionsJsonFilter filter) {
        ObjectNode dependencies = merge(file -> filter.getFilteredVersions(
                new VersionsJsonConverter(file.content(), reactEnabled,
                        excludeWebComponents).getConvertedJson(),
                file.origin()));
        // A package a file excludes is excluded from all of them, unlike a
        // package that is only left out of that file because of the mode it
        // applies to, which says nothing about the other files
        getDeclaredExclusions(reactEnabled, excludeWebComponents)
                .forEach(dependencies::remove);
        return dependencies;
    }

    /**
     * Gets the npm packages that the versions files exclude.
     *
     * @param reactEnabled
     *            whether React is enabled
     * @param excludeWebComponents
     *            whether to exclude web component npm packages
     * @return the excluded npm package names
     */
    Set<String> getExclusions(boolean reactEnabled,
            boolean excludeWebComponents) {
        Set<String> exclusions = new TreeSet<>();
        files.forEach(file -> exclusions
                .addAll(new VersionsJsonConverter(file.content(), reactEnabled,
                        excludeWebComponents).getExclusions()));
        return exclusions;
    }

    /**
     * Gets the npm packages the versions files themselves exclude, leaving out
     * the ones a file only leaves out because of the mode they apply to.
     *
     * @param reactEnabled
     *            whether React is enabled
     * @param excludeWebComponents
     *            whether to exclude web component npm packages
     * @return the excluded npm package names
     */
    private Set<String> getDeclaredExclusions(boolean reactEnabled,
            boolean excludeWebComponents) {
        Set<String> exclusions = new TreeSet<>();
        files.forEach(file -> exclusions
                .addAll(new VersionsJsonConverter(file.content(), reactEnabled,
                        excludeWebComponents).getDeclaredExclusions()));
        return exclusions;
    }

    /**
     * Gets all npm packages and versions the versions files declare, regardless
     * of the mode they apply to, to use in overrides so that the versions get
     * locked even when the packages are transitive.
     *
     * @return all npm package names and versions
     */
    ObjectNode getAllDependencies() {
        return merge(file -> {
            ObjectNode dependencies = JacksonUtils.createObjectNode();
            collectDependencies(file.content(), dependencies);
            return dependencies;
        });
    }

    private static void collectDependencies(JsonNode obj,
            ObjectNode collection) {
        for (String key : JacksonUtils.getKeys(obj)) {
            JsonNode value = obj.get(key);
            if (!(value instanceof ObjectNode)) {
                continue;
            }
            if (value.has(NPM_NAME)) {
                String npmName = value.get(NPM_NAME).asString();
                if (Objects.equals(npmName, VAADIN_CORE_NPM_PACKAGE)) {
                    // The package of the platform itself is not pinned, but
                    // the packages declared next to it still are
                    continue;
                }
                String version;
                if (value.has(NPM_VERSION)) {
                    version = value.get(NPM_VERSION).asString();
                } else if (value.has(JS_VERSION)) {
                    version = value.get(JS_VERSION).asString();
                } else {
                    log().debug(
                            "dependency '{}' has no 'npmVersion'/'jsVersion'.",
                            npmName);
                    continue;
                }
                collection.put(npmName, version);
            } else {
                collectDependencies(value, collection);
            }
        }
    }

    /**
     * Combines the packages each versions file declares into one object, using
     * the newest version of a package declared in several files.
     */
    private ObjectNode merge(
            Function<VersionsFile, ObjectNode> dependenciesExtractor) {
        ObjectNode merged = JacksonUtils.createObjectNode();
        Map<String, String> origins = new HashMap<>();
        for (VersionsFile file : files) {
            ObjectNode dependencies = dependenciesExtractor.apply(file);
            for (String npmPackage : JacksonUtils.getKeys(dependencies)) {
                String version = dependencies.get(npmPackage).asString();
                if (merged.has(npmPackage) && !acceptsVersion(merged, origins,
                        npmPackage, version, file.origin())) {
                    continue;
                }
                merged.put(npmPackage, version);
                origins.put(npmPackage, file.origin());
            }
        }
        return merged;
    }

    /**
     * Resolves a package declared in several versions files, keeping the newest
     * version and warning about the conflict.
     *
     * @return {@code true} if the new version should replace the current one
     */
    private boolean acceptsVersion(ObjectNode merged,
            Map<String, String> origins, String npmPackage, String version,
            String origin) {
        String currentVersion = merged.get(npmPackage).asString();
        if (currentVersion.equals(version)) {
            return false;
        }
        boolean newer = isNewerThan(version, currentVersion);
        log().warn(
                "Multiple versions defined for npm package '{}': '{}' in {} and '{}' in {}. Using '{}'.",
                npmPackage, currentVersion, origins.get(npmPackage), version,
                origin, newer ? version : currentVersion);
        return newer;
    }

    private static boolean isNewerThan(String version, String otherVersion) {
        FrontendVersion frontendVersion = parseVersion(version);
        if (frontendVersion == null) {
            // A version that cannot be compared does not replace one that is
            // already there
            return false;
        }
        FrontendVersion otherFrontendVersion = parseVersion(otherVersion);
        // A version that can be compared replaces one that cannot
        return otherFrontendVersion == null
                || frontendVersion.isNewerThan(otherFrontendVersion);
    }

    private static FrontendVersion parseVersion(String version) {
        try {
            return new FrontendVersion(version);
        } catch (NumberFormatException e) {
            log().debug("Unable to read '{}' as a version", version, e);
            return null;
        }
    }

    private static Logger log() {
        return LoggerFactory.getLogger(PinnedNpmVersions.class);
    }
}
