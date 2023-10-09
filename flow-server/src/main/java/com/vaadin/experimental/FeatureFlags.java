/*
 * Copyright 2000-2023 Vaadin Ltd.
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
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

/**
 * Tracks available feature flags and their status.
 *
 * Enabled feature flags are stored in
 * <code>vaadin-featureflags.properties</code> inside the resources folder
 * (<code>src/main/resources</code>).
 */
public class FeatureFlags implements Serializable {

    public static final String PROPERTIES_FILENAME = "vaadin-featureflags.properties";
    public static final String SYSTEM_PROPERTY_PREFIX = "vaadin.";

    public static final Feature EXAMPLE = new Feature(
            "Example feature. Will be removed once the first real feature flag is added",
            "exampleFeatureFlag", "https://github.com/vaadin/flow/pull/12004",
            false,
            "com.vaadin.flow.server.frontend.NodeTestComponents$ExampleExperimentalComponent");
    public static final Feature COLLABORATION_ENGINE_BACKEND = new Feature(
            "Collaboration Kit backend for clustering support",
            "collaborationEngineBackend",
            "https://github.com/vaadin/platform/issues/1988", true, null);

    public static final Feature WEB_PUSH = new Feature(
            "Server side WebPush API", "webPush",
            "https://github.com/vaadin/docs/pull/2442/files", true,
            "com.vaadin.flow.server.webpush.WebPush");

    public static final Feature FORM_FILLER_ADDON = new Feature(
            "Form Filler Add-on", "formFillerAddon",
            "https://github.com/vaadin/form-filler-addon", true,
            "com.vaadin.flow.ai.formfiller.FormFiller");

    private List<Feature> features = new ArrayList<>();

    File propertiesFolder = null;

    private final Lookup lookup;

    private ApplicationConfiguration configuration;

    /**
     * Generate FeatureFlags with given lookup data.
     *
     * @param lookup
     *            lookup to use
     */
    public FeatureFlags(Lookup lookup) {
        this.lookup = lookup;
        features.add(new Feature(EXAMPLE));
        features.add(new Feature(COLLABORATION_ENGINE_BACKEND));
        features.add(new Feature(WEB_PUSH));
        features.add(new Feature(FORM_FILLER_ADDON));
        loadProperties();
    }

    /**
     * FeatureFlags wrapper class for storing the FeatureFlags object.
     */
    protected static class FeatureFlagsWrapper implements Serializable {
        private final FeatureFlags featureFlags;

        /**
         * Create a feature flags wrapper.
         *
         * @param featureFlags
         *            featureFlags to wrap
         */
        public FeatureFlagsWrapper(FeatureFlags featureFlags) {
            this.featureFlags = featureFlags;
        }

        /**
         * Get the featureFlags.
         *
         * @return wrapped FeatureFlags
         */
        public FeatureFlags getFeatureFlags() {
            return featureFlags;
        }
    }

    /**
     * Gets the FeatureFlags for the given Vaadin context. If the Vaadin context
     * has no FeatureFlags, a new instance is created and assigned to the
     * context.
     *
     * @param context
     *            the vaadin context for which to get FeatureFlags from, not
     *            <code>null</code>
     * @return a feature flags instance for the given context, not
     *         <code>null</code>
     */
    public static FeatureFlags get(final VaadinContext context) {
        assert context != null;

        FeatureFlagsWrapper attribute = context
                .getAttribute(FeatureFlagsWrapper.class, () -> {
                    final FeatureFlags featureFlags = new FeatureFlags(
                            context.getAttribute(Lookup.class));
                    featureFlags.configuration = ApplicationConfiguration
                            .get(context);
                    featureFlags.loadProperties();
                    return new FeatureFlagsWrapper(featureFlags);
                });
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

    /**
     * Read the feature flag properties files and updates the enable property of
     * each feature object.
     */
    public void loadProperties() {
        final ResourceProvider resourceProvider = lookup
                .lookup(ResourceProvider.class);
        if (resourceProvider != null) {
            final URL applicationResource = resourceProvider
                    .getApplicationResource(PROPERTIES_FILENAME);
            if (applicationResource != null) {
                getLogger().debug("Properties loaded from classpath.");
                try (InputStream propertiesStream = applicationResource
                        .openStream()) {
                    loadProperties(propertiesStream);
                    return;
                } catch (IOException e) {
                    throw new UncheckedIOException(
                            "Failed to read properties file from classpath", e);
                }
            }
        }

        File featureFlagFile = getFeatureFlagFile();
        if (featureFlagFile == null || !featureFlagFile.exists()) {
            // Disable all features if no file exists
            for (Feature f : features) {
                f.setEnabled(
                        Boolean.getBoolean(SYSTEM_PROPERTY_PREFIX + f.getId()));
            }
        } else {
            try (FileInputStream propertiesStream = new FileInputStream(
                    featureFlagFile)) {
                getLogger().debug("Loading properties from file '{}'",
                        featureFlagFile);
                loadProperties(propertiesStream);
            } catch (IOException e) {
                throw new UncheckedIOException(
                        "Failed to read properties file from filesystem", e);
            }
        }
    }

    void loadProperties(InputStream propertiesStream) {
        try {
            Properties props = new Properties();

            if (propertiesStream != null) {
                props.load(propertiesStream);
            }
            for (Feature f : features) {
                // Allow users to override a feature flag with a system property
                String propertyValue = System.getProperty(
                        SYSTEM_PROPERTY_PREFIX + f.getId(),
                        props.getProperty(getPropertyName(f.getId())));

                f.setEnabled(Boolean.parseBoolean(propertyValue));
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
            if (configuration == null) {
                return null;
            }
            propertiesFolder = configuration.getJavaResourceFolder();

        }
        return new File(propertiesFolder, PROPERTIES_FILENAME);
    }

    private boolean isDevelopmentMode() {
        return configuration != null && !configuration.isProductionMode();
    }

    public String getEnableHelperMessage(Feature feature) {
        return feature.getTitle()
                + " is not enabled. Enable it in the debug window or by adding "
                + getPropertyName(feature.getId())
                + "=true to src/main/resources/" + PROPERTIES_FILENAME;
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(FeatureFlags.class);
    }
}
