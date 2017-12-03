package com.vaadin.server.startup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

import com.vaadin.server.DependencyFilter;
import com.vaadin.shared.ApplicationConstants;
import com.vaadin.shared.ui.Dependency;
import com.vaadin.shared.ui.LoadMode;

/**
 * Filter for serving bundles instead of single dependencies basing on the
 * bundling information.
 *
 * @author Vaadin Ltd.
 */
public class BundleDependencyFilter implements DependencyFilter {
    private final Map<String, Set<String>> importContainedInBundles;

    /**
     * Creates a filter with the required information.
     *
     * @param importContainedInBundles
     *            a map that is used to look up the bundles that imports are
     *            contained in, not {@code null}
     */
    public BundleDependencyFilter(
            Map<String, Set<String>> importContainedInBundles) {
        this.importContainedInBundles = Objects
                .requireNonNull(importContainedInBundles);
    }

    @Override
    public List<Dependency> filter(List<Dependency> dependencies,
            FilterContext filterContext) {
        List<Dependency> dependenciesWithBundles = new ArrayList<>(
                dependencies.size());
        Set<String> bundleUrlsToInclude = new HashSet<>();

        for (Dependency dependency : dependencies) {
            Set<String> bundleUrls = importContainedInBundles
                    .get(clearFlowProtocols(dependency.getUrl()));
            if (bundleUrls != null) {
                if (bundleUrls.size() > 1) {
                    getLogger().warning(() -> String.format(
                            "Dependency '%s' is contained in multiple bundles: '%s', this may lead to performance degradation",
                            dependency, bundleUrls));
                }
                bundleUrlsToInclude.addAll(bundleUrls);
            } else {
                dependenciesWithBundles.add(dependency);
            }
        }

        bundleUrlsToInclude.stream()
                .map(bundleUrl -> new Dependency(Dependency.Type.HTML_IMPORT,
                        bundleUrl, LoadMode.EAGER))
                .forEach(dependenciesWithBundles::add);
        return dependenciesWithBundles;
    }

    private String clearFlowProtocols(String url) {
        return url.replace(ApplicationConstants.CONTEXT_PROTOCOL_PREFIX, "")
                .replace(ApplicationConstants.FRONTEND_PROTOCOL_PREFIX, "")
                .replace(ApplicationConstants.BASE_PROTOCOL_PREFIX, "");
    }

    private static Logger getLogger() {
        return Logger.getLogger(BundleDependencyFilter.class.getName());
    }
}
