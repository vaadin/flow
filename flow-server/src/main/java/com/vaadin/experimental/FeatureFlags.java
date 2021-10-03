package com.vaadin.experimental;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeatureFlags implements Serializable {

    /**
     * Set by the Maven / Gradle plugin when running through that so the feature
     * flags will be correctly detected.
     */
    public static void setPropertiesLocation(File featureProperties) {
        if (featureProperties != null && featureProperties.exists()) {
            try (FileInputStream stream = new FileInputStream(featureProperties)) {
                loadProperties(stream);
            } catch (IOException e) {
                getLogger().error("Unable to read properties from file " + featureProperties.getAbsolutePath(), e);
            }
        }
    }

    private static final String PROPERTIES_FILENAME = "featureflags.properties";
    public static final Feature VITE = new Feature("Use Vite for frontend build", "viteForFrontendBuild", 0, true);
    private static List<Feature> features = new ArrayList<>();
    static {
        features.add(VITE);
        loadProperties();
    }

    private static void loadProperties() {
        try (InputStream propertiesStream = FeatureFlags.class.getClassLoader()
                .getResourceAsStream(PROPERTIES_FILENAME)) {
            loadProperties(propertiesStream);
        } catch (IOException e) {
            getLogger().error("Unable to read properties using classloader", e);
        }
    }

    private static void loadProperties(InputStream propertiesStream) {
        try {
            Properties props = new Properties();

            if (propertiesStream != null) {
                props.load(propertiesStream);
            }
            for (Feature f : features) {
                if ("true".equals(props.getProperty(getPropertyName(f.getId()), "false"))) {
                    f.setEnabled(true);
                }
            }

        } catch (IOException e) {
            getLogger().error("Unable to read feature flags", e);
        }
    }

    private static void saveProperties() {
        File featureFlagFile = getFeatureFlagFile();
        String properties = "";
        for (Feature feature : features) {
            if (!feature.isEnabled()) {
                continue;
            }
            properties += "# " + feature.getTitle() + "\n";
            properties += getPropertyName(feature.getId()) + "=true\n";
        }
        try (FileWriter writer = new FileWriter(featureFlagFile)) {
            IOUtils.write(properties, writer);
        } catch (IOException e) {
            getLogger().error("Unable to store feature flags", e);
        }
    }

    public static List<Feature> getFeatures() {
        return features;
    }

    public static boolean isEnabled(Feature feature) {
        return feature.isEnabled();
    }

    public static boolean isEnabled(String featureId) {
        return getFeature(featureId).map(f -> f.isEnabled()).orElse(false);
    }

    private static Optional<Feature> getFeature(String featureId) {
        return features.stream().filter(feature -> feature.getId().equals(featureId)).findFirst();
    }

    private static String getPropertyName(String featureId) {
        return "com.vaadin.experimental." + featureId;
    }

    public static void setEnabled(String featureId, boolean enabled) {
        Optional<Feature> maybeFeature = getFeature(featureId);
        if (!maybeFeature.isPresent()) {
            throw new IllegalArgumentException("Unknown feature " + featureId);
        }
        Feature feature = maybeFeature.get();
        if (feature.isEnabled() == enabled) {
            return;
        }

        maybeFeature.get().setEnabled(enabled);
        // Update the feature flag file. This will cause a server restart.
        saveProperties();
        getLogger().info("Set feature " + featureId + " to " + enabled);
    }

    private static File getFeatureFlagFile() {
        ApplicationConfiguration config = ApplicationConfiguration.get(VaadinService.getCurrent().getContext());
        String javaFolder = config.getStringProperty(Constants.CONNECT_JAVA_SOURCE_FOLDER_TOKEN, "src/main/java");
        String resourceFolder = javaFolder.replace("/java", "/resources");
        return new File(resourceFolder, PROPERTIES_FILENAME);
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(FeatureFlags.class);
    }
}
