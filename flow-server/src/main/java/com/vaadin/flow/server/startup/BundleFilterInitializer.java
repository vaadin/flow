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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.ServletContextUriResolver;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.VaadinServletService;
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

    private final ServletContextUriResolver servletContextUriResolver = new ServletContextUriResolver();

    @Override
    public void serviceInit(ServiceInitEvent event) {
        DeploymentConfiguration deploymentConfiguration = event.getSource()
                .getDeploymentConfiguration();
        if (deploymentConfiguration.isProductionMode()) {
            Map<String, Set<String>> importsInBundles = readBundleDependencies(
                    event);
            if (!importsInBundles.isEmpty()) {
                if (importsInBundles.values().stream()
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
    }

    private Map<String, Set<String>> readBundleDependencies(
            ServiceInitEvent event) {
        VaadinServletService service = ((VaadinServletService) event
                .getSource());
        String es6FrontendPrefix = event.getSource()
                .getDeploymentConfiguration().getEs6FrontendPrefix();
        String manifestResourceContextPath = resolveContextPath(
                FLOW_BUNDLE_MANIFEST, es6FrontendPrefix);
        try (InputStream bundleManifestStream = service
                .getResourceAsStream(manifestResourceContextPath)) {
            if (bundleManifestStream == null) {
                getLogger().info(
                        "Bundling disabled: Flow bundle manifest '{}' was not found in servlet context",
                        manifestResourceContextPath);
                return Collections.emptyMap();
            }

            JsonObject bundlesToUrlsContained = Json.parse(IOUtils
                    .toString(bundleManifestStream, StandardCharsets.UTF_8));
            Map<String, Set<String>> importToBundle = new HashMap<>();
            for (String bundleFrontendPath : bundlesToUrlsContained.keys()) {
                String bundleContextPath = resolveContextPath(
                        bundleFrontendPath, es6FrontendPrefix);
                if (service.getResource(bundleContextPath) == null) {
                    throw new IllegalArgumentException(String.format(
                            "Failed to find bundle at context path '%s', specified in manifest '%s'. "
                                    + "Remove file reference from the manifest to disable bundle usage or add the bundle to the context path specified.",
                            bundleContextPath, manifestResourceContextPath));
                }

                JsonArray bundledFiles = bundlesToUrlsContained
                        .getArray(bundleFrontendPath);
                for (int i = 0; i < bundledFiles.length(); i++) {
                    String bundledFile = bundledFiles.getString(i);
                    importToBundle
                            .computeIfAbsent(bundledFile,
                                    key -> new HashSet<>())
                            .add(bundleFrontendPath);
                }
            }
            return importToBundle;
        } catch (IOException e) {
            throw new UncheckedIOException(String.format(
                    "Failed to read bundle manifest file at context path '%s'",
                    manifestResourceContextPath), e);
        }
    }

    private String resolveContextPath(String pathInFrontendFolder,
            String es6FrontendPrefix) {
        return servletContextUriResolver
                .resolveVaadinUri(ApplicationConstants.FRONTEND_PROTOCOL_PREFIX
                        + pathInFrontendFolder, es6FrontendPrefix);
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(BundleFilterInitializer.class.getName());
    }
}
