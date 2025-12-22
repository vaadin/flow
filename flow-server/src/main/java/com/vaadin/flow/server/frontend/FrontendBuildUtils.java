/*
 * Copyright 2000-2025 Vaadin Ltd.
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
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.experimental.CoreFeatureFlagProvider;
import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.internal.FrontendVersion;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.StringUtil;
import com.vaadin.flow.internal.hilla.EndpointRequestUtil;
import com.vaadin.flow.server.AbstractConfiguration;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.Platform;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;

import static com.vaadin.flow.server.Constants.COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT;
import static com.vaadin.flow.server.Constants.RESOURCES_FRONTEND_DEFAULT;

public class FrontendBuildUtils {

    /**
     * Is the React module available in the classpath.
     *
     * @param options
     *            the build options
     * @return true if the React module is available, false otherwise
     */
    public static boolean isReactModuleAvailable(Options options) {
        try {
            options.getClassFinder().loadClass(
                    "com.vaadin.flow.component.react.ReactAdapterComponent");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Checks if integration with Tailwind CSS framework is enabled.
     *
     * @param options
     *            the build options
     * @return true if Tailwind CSS integration is enabled, false otherwise
     */
    public static boolean isTailwindCssEnabled(Options options) {
        return options.getFeatureFlags()
                .isEnabled(CoreFeatureFlagProvider.TAILWIND_CSS);
    }

    /**
     * Compares current platform version with the one last recorded as installed
     * in node_modules/.vaadin/vaadin_version. In case there was no existing
     * platform version recorder and node_modules exists, then platform is
     * considered as staying on the same version.
     *
     * @param finder
     *            project execution class finder
     * @param npmFolder
     *            npm root folder
     * @param nodeModules
     *            node_modules folder
     * @param buildDirectory
     *            project build directory, to find dev-bundle folder
     * @return {@code true} if the version has changed, {@code false} if not
     * @throws IOException
     *             when file reading fails
     */
    protected static boolean isPlatformMajorVersionUpdated(ClassFinder finder,
            File npmFolder, File nodeModules, File buildDirectory)
            throws IOException {
        // if no record of current version is present, version is not
        // considered updated
        Optional<String> platformVersion = getVaadinVersion(finder);
        if (platformVersion.isPresent()) {
            JsonNode vaadinJsonContents = getBundleVaadinContent(
                    buildDirectory);
            if (!vaadinJsonContents.has(NodeUpdater.VAADIN_VERSION)
                    && nodeModules.exists()) {
                // Check for vaadin version from installed node_modules
                vaadinJsonContents = getVaadinJsonContents(npmFolder);
            }
            // If no record of previous version, version is considered same
            if (!vaadinJsonContents.has(NodeUpdater.VAADIN_VERSION)) {
                return false;
            }
            FrontendVersion jsonVersion = new FrontendVersion(vaadinJsonContents
                    .get(NodeUpdater.VAADIN_VERSION).asString());
            FrontendVersion platformsVersion = new FrontendVersion(
                    platformVersion.get());
            return jsonVersion.getMajorVersion() != platformsVersion
                    .getMajorVersion();
        }
        return false;
    }

    private static JsonNode getBundleVaadinContent(File buildDirectory)
            throws IOException {
        JsonNode vaadinJsonContents;
        File vaadinJsonFile = new File(
                new File(buildDirectory, Constants.DEV_BUNDLE_LOCATION),
                TaskRunDevBundleBuild.VAADIN_JSON);
        if (!vaadinJsonFile.exists()) {
            return JacksonUtils.createObjectNode();
        }
        String fileContent = Files.readString(vaadinJsonFile.toPath(),
                StandardCharsets.UTF_8);
        vaadinJsonContents = JacksonUtils.readTree(fileContent);
        return vaadinJsonContents;
    }

    /**
     * Compares current platform version with the one last recorded as installed
     * in node_modules/.vaadin/vaadin_version. In case there was no existing
     * platform version recorder and node_modules exists, then platform is
     * considered updated.
     *
     * @param finder
     *            project execution class finder
     * @param npmFolder
     *            npm root folder
     * @param nodeModules
     *            node_modules folder
     * @return {@code true} if the version has changed, {@code false} if not
     * @throws IOException
     *             when file reading fails
     */
    protected static boolean isPlatformVersionUpdated(ClassFinder finder,
            File npmFolder, File nodeModules) throws IOException {
        // if no record of current version is present, version is not
        // considered updated
        Optional<String> platformVersion = getVaadinVersion(finder);
        if (platformVersion.isPresent() && nodeModules.exists()) {
            JsonNode vaadinJsonContents = getVaadinJsonContents(npmFolder);
            // If no record of previous version, version is considered updated
            if (!vaadinJsonContents.has(NodeUpdater.VAADIN_VERSION)) {
                return true;
            }
            return !Objects.equals(vaadinJsonContents
                    .get(NodeUpdater.VAADIN_VERSION).asString(),
                    platformVersion.get());
        }
        return false;
    }

    protected static Optional<String> getVaadinVersion(ClassFinder finder) {
        URL coreVersionsResource = finder
                .getResource(Constants.VAADIN_CORE_VERSIONS_JSON);

        if (coreVersionsResource == null) {
            return Optional.empty();
        }
        try (InputStream vaadinVersionsStream = coreVersionsResource
                .openStream()) {
            final JsonNode versionsJson = JacksonUtils
                    .readTree(StringUtil.toUTF8String(vaadinVersionsStream));
            if (versionsJson.has("platform")) {
                return Optional.of(versionsJson.get("platform").asString());
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(Platform.class)
                    .error("Unable to determine version information", e);
        }

        return Optional.empty();
    }

    static File getVaadinJsonFile(File npmFolder) {
        return new File(new File(npmFolder, FrontendUtils.NODE_MODULES),
                NodeUpdater.VAADIN_JSON);
    }

    static ObjectNode getVaadinJsonContents(File npmFolder) throws IOException {
        File vaadinJsonFile = getVaadinJsonFile(npmFolder);
        if (vaadinJsonFile.exists()) {
            String fileContent = Files.readString(vaadinJsonFile.toPath(),
                    StandardCharsets.UTF_8);
            return JacksonUtils.readTree(fileContent);
        } else {
            return JacksonUtils.createObjectNode();
        }
    }

    /**
     * Get resource from JAR package.
     *
     * @param jarImport
     *            jar file to get (no resource folder should be added)
     * @param finder
     *            the class finder to use for locating the resource
     * @return resource as String or {@code null} if not found
     */
    public static String getJarResourceString(String jarImport,
            ClassFinder finder) {
        URL resource = finder
                .getResource(RESOURCES_FRONTEND_DEFAULT + "/" + jarImport);
        if (resource == null) {
            resource = finder.getResource(
                    COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT + "/" + jarImport);
        }

        if (resource == null) {
            return null;
        }
        try (InputStream frontendContent = resource.openStream()) {
            return FrontendUtils.streamToString(frontendContent);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks if Hilla is available and Hilla views are used in the project
     * based on what is in routes.ts or routes.tsx file.
     * {@link FrontendUtils#getProjectFrontendDir(AbstractConfiguration)} can be
     * used to get the frontend directory. Given class finder is used to check
     * the presence of Hilla in a classpath.
     *
     * @param frontendDirectory
     *            the frontend directory
     * @param classFinder
     *            class finder to check the presence of Hilla endpoint class
     * @return {@code true} if Hilla is available and Hilla views are used,
     *         {@code false} otherwise
     */
    public static boolean isHillaUsed(File frontendDirectory,
            ClassFinder classFinder) {
        return isHillaAvailable(classFinder)
                && FrontendUtils.isHillaViewsUsed(frontendDirectory);
    }

    /**
     * Checks if Hilla is available using the given class finder.
     *
     * @param classFinder
     *            class finder to check the presence of Hilla endpoint class
     * @return true if Hilla is available, false otherwise
     */
    static boolean isHillaAvailable(ClassFinder classFinder) {
        try {
            classFinder.loadClass(EndpointRequestUtil.HILLA_ENDPOINT_CLASS);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
