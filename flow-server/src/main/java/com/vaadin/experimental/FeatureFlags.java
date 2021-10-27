/*
 * Copyright 2000-2021 Vaadin Ltd.
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
package com.vaadin.experimental;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks available feature flags and their status.
 * 
 * Enabled feature flags are stored in
 * <code>vaadin-featureflags.properties</code> inside the resources folder
 * (<code>src/main/resources</code>).
 */
public class FeatureFlags implements Serializable {

    public static final String PROPERTIES_FILENAME = "vaadin-featureflags.properties";

    public static final Feature EXAMPLE = new Feature(
            "Example feature. Will be removed once the first real feature flag is added",
            "exampleFeatureFlag", "https://github.com/vaadin/flow/pull/12004",
            false);
    public static final Feature VITE = new Feature(
            "Use Vite for faster front-end builds", "viteForFrontendBuild",
            "https://github.com/vaadin/platform/issues/2448", true);

    private List<Feature> features = new ArrayList<>();

    File propertiesFolder = null;

    private final VaadinContext context;

    protected FeatureFlags(VaadinContext context) {
        this.context = context;
        features.add(new Feature(EXAMPLE));
        features.add(new Feature(VITE));
        loadProperties();
    }

    /**
     * FeatureFlags wrapper class for storing the FeatureFlags object.
     */
    protected static class FeatureFlagsWrapper implements Serializable {
        private final FeatureFlags feature;

        /**
         * Create a application route registry wrapper.
         *
         * @param registry
         *            application route registry to wrap
         */
        public FeatureFlagsWrapper(FeatureFlags registry) {
            this.feature = registry;
        }

        /**
         * Get the application route registry.
         *
         * @return wrapped application route registry
         */
        public FeatureFlags getFeatureFlags() {
            return feature;
        }
    }

    /**
     * Gets the route registry for the given Vaadin context. If the Vaadin
     * context has no route registry, a new instance is created and assigned to
     * the context.
     *
     * @param context
     *            the vaadin context for which to get a route registry, not
     *            <code>null</code>
     * @return a registry instance for the given context, not <code>null</code>
     */
    public static FeatureFlags getInstance(VaadinContext context) {
        assert context != null;

        FeatureFlagsWrapper attribute;
        synchronized (context) {
            attribute = context.getAttribute(FeatureFlagsWrapper.class);

            if (attribute == null) {
                attribute = new FeatureFlagsWrapper(new FeatureFlags(context));
                context.setAttribute(attribute);
            }
        }

        return attribute.getFeatureFlags();
    }

    /**
     * Set by the Maven / Gradle plugin when running through that so the feature
     * flags will be correctly detected.
     */
    public void setPropertiesLocation(File propertiesFolder) {
        this.propertiesFolder = propertiesFolder;
        loadProperties();
    }

    void loadProperties() {
        final Lookup lookup = context.getAttribute(Lookup.class);
        final URL applicationResource = lookup.lookup(ResourceProvider.class)
                .getApplicationResource(PROPERTIES_FILENAME);
        if (applicationResource != null) {
            getLogger().debug("Properties loaded from classpath.");
            try {
                loadProperties(applicationResource.openStream());
            } catch (IOException e) {
                getLogger().error("Unable to read properties from classpath",
                        e);
            }
            return;
        }

        File featureFlagFile = getFeatureFlagFile();
        if (featureFlagFile == null || !featureFlagFile.exists()) {
            return;
        }

        try (FileInputStream propertiesStream = new FileInputStream(
                featureFlagFile)) {
            getLogger().debug("Loading properties from file '{}'",
                    featureFlagFile);
            loadProperties(propertiesStream);
        } catch (IOException e) {
            getLogger().error("Unable to read properties from file", e);
        }
    }

    void loadProperties(InputStream propertiesStream) {
        try {
            Properties props = new Properties();

            if (propertiesStream != null) {
                props.load(propertiesStream);
            }
            for (Feature f : features) {
                boolean enabled = "true".equals(
                        props.getProperty(getPropertyName(f.getId()), "false"));
                f.setEnabled(enabled);
            }

        } catch (IOException e) {
            getLogger().error("Unable to read feature flags", e);
        }
    }

    private void saveProperties() {
        File featureFlagFile = getFeatureFlagFile();
        if (featureFlagFile == null) {
            throw new IllegalStateException(
                    "Unable to determine feature flag file location");
        }
        StringBuilder properties = new StringBuilder();
        for (Feature feature : features) {
            if (!feature.isEnabled()) {
                continue;
            }
            properties.append("# ").append(feature.getTitle()).append("\n");
            properties.append(getPropertyName(feature.getId()))
                    .append("=true\n");
        }
        if (!featureFlagFile.getParentFile().exists()) {
            featureFlagFile.getParentFile().mkdirs(); // NOSONAR
        }
        try {
            FileUtils.write(featureFlagFile, properties.toString(),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            getLogger().error("Unable to store feature flags", e);
        }
    }

    /**
     * Get a list of all available features and their status.
     * 
     * @return a list of all features
     */
    public List<Feature> getFeatures() {
        return features;
    }

    /**
     * Checks if the given feature is enabled.
     * 
     * @param feature
     *            the feature to check
     * @return <code>true</code> if enabled, <code>false</code> otherwise
     */
    public boolean isEnabled(Feature feature) {
        return getFeature(feature.getId())
                .orElseThrow(
                        () -> new UnknownFeatureException(feature.getTitle()))
                .isEnabled();
    }

    /**
     * Checks if the given feature is enabled.
     * 
     * @param featureId
     *            the feature to check
     * @return <code>true</code> if enabled, <code>false</code> otherwise
     */
    public boolean isEnabled(String featureId) {
        return getFeature(featureId).map(Feature::isEnabled).orElse(false);
    }

    private Optional<Feature> getFeature(String featureId) {
        return features.stream()
                .filter(feature -> feature.getId().equals(featureId))
                .findFirst();
    }

    private String getPropertyName(String featureId) {
        return "com.vaadin.experimental." + featureId;
    }

    /**
     * Enables or disables the given feature.
     * 
     * @param featureId
     *            the feature id
     * @param enabled
     *            <code>true</code> to enable, <code>false</code> to disable
     */
    public void setEnabled(String featureId, boolean enabled) {
        if (!isDevelopmentMode()) {
            throw new IllegalStateException(
                    "Feature flags can only be toggled when in development mode");
        }
        Optional<Feature> maybeFeature = getFeature(featureId);
        if (!maybeFeature.isPresent()) {
            throw new IllegalArgumentException("Unknown feature " + featureId);
        }
        Feature feature = maybeFeature.get();
        if (feature.isEnabled() == enabled) {
            return;
        }

        maybeFeature.get().setEnabled(enabled);
        // Update the feature flag file
        saveProperties();
        getLogger().info("Set feature {} to {}", featureId, enabled);
    }

    private File getFeatureFlagFile() {
        if (propertiesFolder == null) {
            ApplicationConfiguration config = getApplicationConfiguration();
            if (config == null) {
                return null;
            }
            propertiesFolder = config.getJavaResourceFolder();

        }
        return new File(propertiesFolder, PROPERTIES_FILENAME);
    }

    private boolean isDevelopmentMode() {
        ApplicationConfiguration config = getApplicationConfiguration();
        return config != null && !config.isProductionMode();
    }

    private ApplicationConfiguration getApplicationConfiguration() {
        return ApplicationConfiguration.get(context);
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(FeatureFlags.class);
    }
}
