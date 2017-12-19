package com.vaadin.server.startup;

import java.util.ArrayList;
import java.util.HashSet;
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
    public BundleDependencyFilter(
            Map<String, Set<String>> importContainedInBundles) {
        this.importContainedInBundles = Objects.requireNonNull(
                importContainedInBundles,
                "Import to bundle mapping cannot be null");
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
                    getLogger().warn(
                            "Dependency '{}' is contained in multiple fragments: '{}', this may lead to performance degradation",
                            dependency, bundleUrls);
                }
                bundleUrlsToInclude.addAll(bundleUrls);
            } else {
                dependenciesWithBundles.add(dependency);
            }
        }

        dependenciesWithBundles.add(0, createBundleDependency(MAIN_BUNDLE_URL));
        bundleUrlsToInclude.stream()
                .filter(bundleUrl -> !MAIN_BUNDLE_URL.equals(bundleUrl))
                .map(BundleDependencyFilter::createBundleDependency)
                .forEach(dependenciesWithBundles::add);

        return dependenciesWithBundles;
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
