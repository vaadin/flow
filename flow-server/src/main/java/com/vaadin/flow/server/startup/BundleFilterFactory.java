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
package com.vaadin.flow.server.startup;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.WebBrowser;
import com.vaadin.flow.shared.ApplicationConstants;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

/**
 * Factory for bundle filters.
 *
 * @author Vaadin Ltd
 * @since 1.3
 */
public class BundleFilterFactory implements Serializable {
    static final String MAIN_BUNDLE_NAME_PREFIX = "vaadin-flow-bundle";
    static final String FLOW_BUNDLE_MANIFEST = ApplicationConstants.FRONTEND_PROTOCOL_PREFIX
            + "vaadin-flow-bundle-manifest.json";

    /**
     * Creates bundle filters for the given service.
     *
     * @param service
     *            the Vaadin service for which to create filters, not
     *            <code>null</code>
     * @return a stream of bundle filters, or an empty stream if no bundle
     *         filter should be used
     */
    public Stream<BundleDependencyFilter> createFilters(VaadinService service) {
        if (!service.getDeploymentConfiguration().isCompatibilityMode()
                || !service.getDeploymentConfiguration().useCompiledFrontendResources()) {
            return Stream.empty();
        }

        return Stream
                .of(createBundleFilterForBrowser(FakeBrowser.getEs6(), service),
                        createBundleFilterForBrowser(FakeBrowser.getEs5(),
                                service))
                .flatMap(maybeFilter -> maybeFilter.map(Stream::of)
                        .orElseGet(Stream::empty));
    }

    private Optional<BundleDependencyFilter> createBundleFilterForBrowser(
            WebBrowser browser, VaadinService service) {
        return readBundleManifest(browser, service)
                .flatMap(bundleData -> createDependencyFilter(browser,
                        bundleData, service));
    }

    private Optional<JsonObject> readBundleManifest(WebBrowser browser,
            VaadinService service) {
        try (InputStream bundleManifestStream = service
                .getResourceAsStream(FLOW_BUNDLE_MANIFEST, browser, null)) {
            if (bundleManifestStream == null) {
                throw new IllegalArgumentException(String.format(
                        "Failed to find the bundle manifest file '%s' in the servlet context for '%s' browsers."
                                + " If you are running a dev-mode servlet container in maven e.g. `jetty:run` change it to `jetty:run-exploded`."
                                + " If you are not compiling frontend resources, include the 'vaadin-maven-plugin' in your build script."
                                + " Otherwise, you can skip this error either by disabling production mode, or by setting the servlet parameter '%s=true'.",
                        FLOW_BUNDLE_MANIFEST,
                        browser.isEs6Supported() ? "ES6" : "ES5",
                        Constants.USE_ORIGINAL_FRONTEND_RESOURCES));
            }
            return Optional.of(Json.parse(IOUtils.toString(bundleManifestStream,
                    StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new UncheckedIOException(String.format(
                    "Failed to read bundle manifest file at context path '%s'",
                    FLOW_BUNDLE_MANIFEST), e);
        }
    }

    private Optional<BundleDependencyFilter> createDependencyFilter(
            WebBrowser browser, JsonObject bundlesToUrlsContained,
            VaadinService service) {
        Map<String, Set<String>> importToBundle = new HashMap<>();
        String mainBundle = null;

        for (String bundlePath : bundlesToUrlsContained.keys()) {
            String frontendBundlePath = ApplicationConstants.FRONTEND_PROTOCOL_PREFIX
                    + bundlePath;
            if (!service.isResourceAvailable(frontendBundlePath, browser,
                    null)) {
                throw new IllegalArgumentException(String.format(
                        "Failed to find bundle '%s', specified in manifest '%s'. Remove file reference from the manifest to disable bundle usage or add the bundle to the path specified.",
                        frontendBundlePath, FLOW_BUNDLE_MANIFEST));
            }

            if (bundlePath.startsWith(MAIN_BUNDLE_NAME_PREFIX)) {
                if (mainBundle == null) {
                    mainBundle = bundlePath;
                } else {
                    throw new IllegalArgumentException(String.format(
                            "Flow bundle manifest '%s' contains multiple bundle files with name that starts with '%s'. This prefix is reserved for Flow purposes and you should not use it to name your fragments.",
                            FLOW_BUNDLE_MANIFEST, MAIN_BUNDLE_NAME_PREFIX));
                }
            }

            JsonArray bundledFiles = bundlesToUrlsContained
                    .getArray(bundlePath);
            for (int i = 0; i < bundledFiles.length(); i++) {
                importToBundle.computeIfAbsent(bundledFiles.getString(i),
                        key -> new HashSet<>()).add(bundlePath);
            }
        }

        if (importToBundle.isEmpty()) {
            getLogger().info(
                    "Bundling disabled: Flow bundle manifest '{}' contains no bundle data",
                    FLOW_BUNDLE_MANIFEST);
            return Optional.empty();
        } else {
            if (mainBundle == null) {
                throw new IllegalArgumentException(String.format(
                        "Flow bundle manifest '%s' contains no main bundle: the single file prefixed with '%s' and having common code for all the fragments",
                        FLOW_BUNDLE_MANIFEST, MAIN_BUNDLE_NAME_PREFIX));
            }
            return Optional.of(new BundleDependencyFilter(browser, mainBundle,
                    importToBundle));
        }
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(BundleFilterFactory.class.getName());
    }
}
