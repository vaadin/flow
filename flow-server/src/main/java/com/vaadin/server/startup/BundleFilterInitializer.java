package com.vaadin.server.startup;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;

import com.vaadin.function.DeploymentConfiguration;
import com.vaadin.server.ServiceInitEvent;
import com.vaadin.server.VaadinServiceInitListener;
import com.vaadin.server.VaadinServletService;
import com.vaadin.shared.ApplicationConstants;
import com.vaadin.shared.VaadinUriResolver;

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
                event.addDependencyFilter(
                        new BundleDependencyFilter(importsInBundles));
            }
        }
    }

    private Map<String, Set<String>> readBundleDependencies(
            ServiceInitEvent event, VaadinUriResolver es6ContextPathResolver) {
        ServletContext servletContext = ((VaadinServletService) event
                .getSource()).getServlet().getServletContext();

        String es6Base = es6ContextPathResolver
                .resolveVaadinUri(ApplicationConstants.FRONTEND_PROTOCOL_PREFIX) + '/';
        String bundleManifestContextPath = es6Base + FLOW_BUNDLE_MANIFEST;
        try (InputStream bundleManifestStream = servletContext
                .getResourceAsStream(bundleManifestContextPath)) {
            if (bundleManifestStream == null) {
                getLogger().config(() -> String.format(
                        "Bundling disabled: Flow bundle manifest '%s' was not found in servlet context",
                        bundleManifestContextPath));
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
                    if (servletContext.getResource(es6ContextPathResolver
                            .resolveVaadinUri(es6Base + bundlePath)) == null) {
                        throw new IllegalArgumentException(String.format(
                                "Failed to find bundle at context path '%s', specified in manifest '%s'. Remove file reference from the manifest to disable bundle usage or add the bundle to the context path specified.",
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
        return Logger.getLogger(BundleFilterInitializer.class.getName());
    }
}
