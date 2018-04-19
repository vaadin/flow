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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.DependencyFilter;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.flow.shared.ui.Dependency;
import com.vaadin.flow.shared.ui.LoadMode;

/**
 * Filter for serving bundles instead of single dependencies basing on the
 * bundling information.
 *
 * @author Vaadin Ltd.
 */
public class BundleDependencyFilter implements DependencyFilter {

    /**
     * Reference to the main bundle file containing all dependencies not split
     * into fragments.
     */
    static final String MAIN_BUNDLE_URL = "vaadin-flow-bundle.html";

    private final Map<String, Set<String>> importContainedInBundles;

    /**
     * Creates a filter with the required information.
     *
     * @param importContainedInBundles
     *            a map that is used to look up the bundles that imports are
     *            contained in, not {@code null}
     */
    public BundleDependencyFilter(Map<String, Set<String>> importContainedInBundles) {
        this.importContainedInBundles = Objects.requireNonNull(importContainedInBundles,
                "Import to bundle mapping cannot be null");
    }

    @Override
    public List<Dependency> filter(List<Dependency> dependencies, FilterContext filterContext) {
        String mainFragment = resolveFragment(MAIN_BUNDLE_URL);
        mainFragment = mainFragment != null ? mainFragment : MAIN_BUNDLE_URL;

        LinkedHashSet<Dependency> bundleDependencies = new LinkedHashSet<>();
        for (Dependency dependency : dependencies) {
            String fragment = resolveFragment(dependency.getUrl());
            if (fragment != null) {
                if (bundleDependencies.isEmpty()) {
                    bundleDependencies.add(createBundleDependency(mainFragment));
                }
                bundleDependencies.add(createBundleDependency(fragment));
            } else {
                bundleDependencies.add(dependency);
            }
        }
        return new ArrayList<>(bundleDependencies);
    }

    private String resolveFragment(String url) {
        Set<String> bundleUrls = importContainedInBundles.get(clearFlowProtocols(url));
        if (bundleUrls != null) {
            if (bundleUrls.size() > 1) {
                getLogger().warn(
                        "Dependency '{}' is contained in multiple fragments: '{}', this may lead to performance degradation",
                        url, bundleUrls);
            }
            return bundleUrls.iterator().next();
        }
        return null;
    }

    private String clearFlowProtocols(String url) {
        return url.replace(ApplicationConstants.CONTEXT_PROTOCOL_PREFIX, "")
                .replace(ApplicationConstants.FRONTEND_PROTOCOL_PREFIX, "")
                .replace(ApplicationConstants.BASE_PROTOCOL_PREFIX, "");
    }

    private static Dependency createBundleDependency(String bundleUrl) {
        return new Dependency(Dependency.Type.HTML_IMPORT, bundleUrl, LoadMode.EAGER);
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(BundleDependencyFilter.class.getName());
    }
}
