/*
 * Copyright 2000-2017 Vaadin Ltd.
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
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.WebBrowser;
import com.vaadin.flow.shared.ApplicationConstants;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

/**
 * An initializer for a bundle filter.
 *
 * @author Vaadin Ltd.
 */
public class BundleFilterInitializer implements VaadinServiceInitListener {

    private static final String FLOW_BUNDLE_MANIFEST = "vaadin-flow-bundle-manifest.json";

    @Override
    public void serviceInit(ServiceInitEvent event) {
        DeploymentConfiguration deploymentConfiguration = event.getSource()
                .getDeploymentConfiguration();
        if (!deploymentConfiguration.useCompiledFrontendResources()) {
            return;
        }

        Map<String, Set<String>> importsInBundles = readBundleDependencies(
                event.getSource());
        if (!importsInBundles.isEmpty()) {
            if (!importsInBundles
                    .containsKey(BundleDependencyFilter.MAIN_BUNDLE_URL)
                    && importsInBundles.values().stream()
                            .noneMatch(importSet -> importSet.contains(
                                    BundleDependencyFilter.MAIN_BUNDLE_URL))) {
                throw new IllegalArgumentException(String.format(
                        "Attempted to initialize BundleDependencyFilter with an "
                                + "import to bundle mapping which does not contain the main bundle %s",
                        BundleDependencyFilter.MAIN_BUNDLE_URL));
            }
            event.addDependencyFilter(
                    new BundleDependencyFilter(importsInBundles));
        }
    }

    private Map<String, Set<String>> readBundleDependencies(
            VaadinService service) {
        WebBrowser es6Browser = FakeEs6Browser.get();
        String manifestResource = ApplicationConstants.FRONTEND_PROTOCOL_PREFIX
                + FLOW_BUNDLE_MANIFEST;
        try (InputStream bundleManifestStream = service
                .getResourceAsStream(manifestResource, es6Browser, null)) {
            if (bundleManifestStream == null) {
                throw new IllegalArgumentException(String.format(
                        "Failed to find the bundle manifest file '%s' in the servlet context."
                        + " If you are running a dev-mode servlet container in maven e.g. `jetty:run` change it to `jetty:run-exploded`."
                        + " If you are not compiling frontend resources, include the 'flow-maven-plugin' in your build script."
                        + " Otherwise, you can skip this error either by disabling production mode, or by setting the servlet parameter '%s=true'.",
                        manifestResource, Constants.USE_ORIGINAL_FRONTEND_RESOURCES));
            }

            JsonObject bundlesToUrlsContained = Json.parse(IOUtils
                    .toString(bundleManifestStream, StandardCharsets.UTF_8));
            Map<String, Set<String>> importToBundle = new HashMap<>();
            for (String bundlePath : bundlesToUrlsContained.keys()) {
                if (!service.isResourceAvailable(
                        ApplicationConstants.FRONTEND_PROTOCOL_PREFIX
                                + bundlePath,
                        es6Browser, null)) {
                    throw new IllegalArgumentException(String.format(
                            "Failed to find bundle at context path '%s', specified in manifest '%s'. "
                                    + "Remove file reference from the manifest to disable bundle usage or add the bundle to the context path specified.",
                            bundlePath, manifestResource));
                }
                JsonArray bundledFiles = bundlesToUrlsContained
                        .getArray(bundlePath);
                for (int i = 0; i < bundledFiles.length(); i++) {
                    String bundledFile = bundledFiles.getString(i);
                    importToBundle.computeIfAbsent(bundledFile,
                            key -> new HashSet<>()).add(bundlePath);
                }
            }
            return importToBundle;
        } catch (IOException e) {
            throw new UncheckedIOException(String.format(
                    "Failed to read bundle manifest file at context path '%s'",
                    manifestResource), e);
        }
    }
}
