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

import javax.servlet.ServletContext;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.flow.shared.VaadinUriResolver;

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
        if (deploymentConfiguration.isProductionMode()) {
            VaadinUriResolver es6ContextPathResolver = new VaadinUriResolver() {
                @Override
                protected String getContextRootUrl() {
                    return "/";
                }

                @Override
                protected String getFrontendRootUrl() {
                    return deploymentConfiguration.getEs6FrontendPrefix();
                }
            };

            Map<String, Set<String>> importsInBundles = readBundleDependencies(
                    event, es6ContextPathResolver);
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
            ServiceInitEvent event, VaadinUriResolver es6ContextPathResolver) {
        VaadinServletService servlet = ((VaadinServletService) event
                .getSource());

        String es6Base = es6ContextPathResolver.resolveVaadinUri(
                ApplicationConstants.FRONTEND_PROTOCOL_PREFIX);
        if(!es6Base.endsWith("/")) {
            es6Base += '/';
        }
        String bundleManifestContextPath = es6Base + FLOW_BUNDLE_MANIFEST;
        try (InputStream bundleManifestStream = servlet
                .getResourceAsStream(bundleManifestContextPath)) {
            if (bundleManifestStream == null) {
                getLogger().info(
                        "Bundling disabled: Flow bundle manifest '{}' was not found in servlet context",
                        bundleManifestContextPath);
                return Collections.emptyMap();
            }

            JsonObject bundlesToUrlsContained = Json.parse(IOUtils
                    .toString(bundleManifestStream, StandardCharsets.UTF_8));
            Map<String, Set<String>> importToBundle = new HashMap<>();
            for (String bundlePath : bundlesToUrlsContained.keys()) {
                JsonArray bundledFiles = bundlesToUrlsContained
                        .getArray(bundlePath);
                for (int i = 0; i < bundledFiles.length(); i++) {
                    String bundledFile = bundledFiles.getString(i);
                    if (servlet.getResource(es6ContextPathResolver
                            .resolveVaadinUri(es6Base + bundlePath)) == null) {
                        throw new IllegalArgumentException(String.format(
                                "Failed to find bundle at context path '%s', specified in manifest '%s'. "
                                        + "Remove file reference from the manifest to disable bundle usage or add the bundle to the context path specified.",
                                bundlePath, bundleManifestContextPath));
                    }
                    importToBundle.computeIfAbsent(bundledFile,
                            key -> new HashSet<>()).add(bundlePath);
                }
            }
            return importToBundle;
        } catch (IOException e) {
            throw new UncheckedIOException(String.format(
                    "Failed to read bundle manifest file at context path '%s'",
                    bundleManifestContextPath), e);
        }
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(BundleFilterInitializer.class.getName());
    }
}
