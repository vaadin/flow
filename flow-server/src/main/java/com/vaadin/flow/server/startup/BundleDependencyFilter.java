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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.DependencyFilter;
import com.vaadin.flow.server.WebBrowser;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.flow.shared.ui.Dependency;
import com.vaadin.flow.shared.ui.LoadMode;

/**
 * Filter for serving bundles instead of single dependencies basing on the
 * bundling information.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class BundleDependencyFilter implements DependencyFilter {
    private final String mainBundlePath;
    private final Map<String, Set<String>> importContainedInBundles;
    private boolean isES6;

    /**
     * Creates a filter with the required information.
     *
     * @param browser
     *            Type of browser handled by this filter
     * @param mainBundlePath
     *            path to the main bundle that contains common code of the app,
     *            not {@code null}
     * @param importContainedInBundles
     *            a map that is used to look up the bundles that imports are
     *            contained in, not {@code null}
     */
    public BundleDependencyFilter(WebBrowser browser, String mainBundlePath,
            Map<String, Set<String>> importContainedInBundles) {
        this.mainBundlePath = Objects.requireNonNull(mainBundlePath,
                "Main bundle name cannot be null");
        this.importContainedInBundles = Objects.requireNonNull(
                importContainedInBundles,
                "Import to bundle mapping cannot be null");
        this.isES6 = browser.isEs6Supported();
    }

    @Override
    public List<Dependency> filter(List<Dependency> dependencies,
            FilterContext filterContext) {
        if (filterContext.getBrowser().isEs6Supported() != this.isES6) {
            return dependencies;
        }

        Collection<Dependency> fragments = new LinkedHashSet<>(
                dependencies.size());
        Collection<Dependency> notFragments = new LinkedHashSet<>(
                dependencies.size());

        for (Dependency dependency : dependencies) {
            Set<String> bundleUrls = importContainedInBundles
                    .get(clearFlowProtocols(dependency.getUrl()));
            if (bundleUrls != null) {
                if (bundleUrls.size() > 1) {
                    getLogger().warn(
                            "Dependency '{}' is contained in multiple fragments: '{}', this may lead to performance degradation",
                            dependency, bundleUrls);
                }
                bundleUrls.stream()
                        .map(BundleDependencyFilter::createBundleDependency)
                        .forEach(fragments::add);
            } else {
                notFragments.add(dependency);
            }
        }

        List<Dependency> fragmentsFirst = new ArrayList<>();
        if (!fragments.isEmpty()) {
            Dependency mainBundle = createBundleDependency(mainBundlePath);
            fragments.remove(mainBundle);
            fragmentsFirst.add(mainBundle);
            fragmentsFirst.addAll(fragments);
        }
        fragmentsFirst.addAll(notFragments);
        return fragmentsFirst;
    }

    private String clearFlowProtocols(String url) {
        return url.replace(ApplicationConstants.CONTEXT_PROTOCOL_PREFIX, "")
                .replace(ApplicationConstants.FRONTEND_PROTOCOL_PREFIX, "")
                .replace(ApplicationConstants.BASE_PROTOCOL_PREFIX, "");
    }

    private static Dependency createBundleDependency(String bundleUrl) {
        return new Dependency(Dependency.Type.HTML_IMPORT, bundleUrl,
                LoadMode.EAGER);
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(BundleDependencyFilter.class.getName());
    }
}
