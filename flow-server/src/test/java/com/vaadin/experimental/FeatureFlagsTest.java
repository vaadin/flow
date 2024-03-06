/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.experimental;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.server.MockVaadinContext;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

import static com.vaadin.experimental.FeatureFlags.PROPERTIES_FILENAME;

@NotThreadSafe
public class FeatureFlagsTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private VaadinContext context;
    private FeatureFlags featureFlags;
    private File propertiesDir;
    private ApplicationConfiguration configuration;
    private File featureFlagsFile;

    @Before
    public void before() throws IOException {
        propertiesDir = temporaryFolder.newFolder();

        context = new MockVaadinContext();
        configuration = Mockito.mock(ApplicationConfiguration.class);
        Mockito.when(configuration.isProductionMode()).thenReturn(false);

        context.setAttribute(ApplicationConfiguration.class, configuration);

        featureFlags = FeatureFlags.get(context);
        featureFlags.setPropertiesLocation(propertiesDir);

        mockResourcesLocation();

        featureFlagsFile = new File(propertiesDir, PROPERTIES_FILENAME);
        Files.deleteIfExists(featureFlagsFile.toPath());
    }

    @Test
    public void propertiesLoaded() throws IOException {
        Assert.assertFalse("Feature should be initially disabled",
                featureFlags.isEnabled(FeatureFlags.EXAMPLE));

        createFeatureFlagsFile(
                "com.vaadin.experimental.exampleFeatureFlag=true\n");

        // This is done automatically but from a static block which we cannot
        // mock
        featureFlags.loadProperties();

        Assert.assertTrue("Feature should have been enabled",
                featureFlags.isEnabled(FeatureFlags.EXAMPLE));
    }

    @Test
    public void setPropertiesLocation() throws Exception {
        // Set location and ensure flags are loaded from there
        createFeatureFlagsFile(
                "com.vaadin.experimental.exampleFeatureFlag=true\n");
        Assert.assertFalse("Feature should be initially disabled",
                featureFlags.isEnabled(FeatureFlags.EXAMPLE));
        featureFlags.setPropertiesLocation(propertiesDir);
        Assert.assertTrue("Feature should have been enabled",
                featureFlags.isEnabled(FeatureFlags.EXAMPLE));
    }

    @Test
    public void setPropertiesLocationWithNoFileDisablesFeatures()
            throws Exception {
        // given an enabled feature
        createFeatureFlagsFile(
                "com.vaadin.experimental.exampleFeatureFlag=true\n");
        featureFlags.setPropertiesLocation(propertiesDir);

        // when a directory containing no vaadin-featureflags.properties is set
        File emptyFolder = Files.createTempDirectory("test-folder").toFile();
        emptyFolder.deleteOnExit();
        featureFlags.setPropertiesLocation(emptyFolder);

        // then the feature should be disabled
        Assert.assertFalse("Feature should have been disabled",
                featureFlags.isEnabled(FeatureFlags.EXAMPLE));
    }

    @Test
    public void enableDisableFeature() throws IOException {
        createFeatureFlagsFile(
                "com.vaadin.experimental.exampleFeatureFlag=false\n");
        featureFlags.loadProperties();
        Assert.assertFalse(
                "Feature should be disabled after reading the properties",
                featureFlags.isEnabled(FeatureFlags.EXAMPLE));
        featureFlags.setEnabled(FeatureFlags.EXAMPLE.getId(), true);
        Assert.assertTrue("Feature should have been enabled",
                featureFlags.isEnabled(FeatureFlags.EXAMPLE));
        Assert.assertEquals(
                "# Example feature. Will be removed once the first real feature flag is added\ncom.vaadin.experimental.exampleFeatureFlag=true\n",
                FileUtils.readFileToString(featureFlagsFile,
                        StandardCharsets.UTF_8));

        featureFlags.setEnabled(FeatureFlags.EXAMPLE.getId(), false);
        Assert.assertFalse("Feature should have been disabled",
                featureFlags.isEnabled(FeatureFlags.EXAMPLE));
        Assert.assertEquals(
                "Feature flags file should be empty when no features are enabled",
                "", FileUtils.readFileToString(featureFlagsFile,
                        StandardCharsets.UTF_8));
    }

    @Test(expected = IllegalStateException.class)
    public void setEnabledOnlyInDevelopmentMode() throws IOException {
        Mockito.when(configuration.isProductionMode()).thenReturn(true);

        createFeatureFlagsFile(
                "com.vaadin.experimental.exampleFeatureFlag=true\n");
        ApplicationConfiguration conf = ApplicationConfiguration
                .get(VaadinService.getCurrent().getContext());
        Mockito.when(conf.isProductionMode()).thenReturn(true);
        featureFlags.setEnabled(FeatureFlags.EXAMPLE.getId(), true);
    }

    @Test
    public void disabledFeatureFlagsNotMarkedInStatsWhenLoading()
            throws IOException {
        UsageStatistics.resetEntries();
        createFeatureFlagsFile("");
        featureFlags.loadProperties();
        Assert.assertFalse(
                hasUsageStatsEntry("flow/featureflags/exampleFeatureFlag"));
    }

    @Test
    public void enabledFeatureFlagsMarkedInStatsWhenLoading()
            throws IOException {
        createFeatureFlagsFile(
                "com.vaadin.experimental.exampleFeatureFlag=true\n");
        featureFlags.loadProperties();
        Assert.assertTrue(
                hasUsageStatsEntry("flow/featureflags/exampleFeatureFlag"));
    }

    @Test
    public void disabledFeatureFlagsNotMarkedInStatsWhenToggled()
            throws IOException {
        createFeatureFlagsFile(
                "com.vaadin.experimental.exampleFeatureFlag=true\n");
        UsageStatistics.resetEntries();
        featureFlags.setEnabled(FeatureFlags.EXAMPLE.getId(), false);
        Assert.assertFalse(
                hasUsageStatsEntry("flow/featureflags/exampleFeatureFlag"));
    }

    @Test
    public void enabledFeatureFlagsMarkedInStatsWhenToggled()
            throws IOException {
        createFeatureFlagsFile(
                "com.vaadin.experimental.exampleFeatureFlag=false\n");
        UsageStatistics.resetEntries();
        featureFlags.setEnabled(FeatureFlags.EXAMPLE.getId(), true);
        Assert.assertTrue(
                hasUsageStatsEntry("flow/featureflags/exampleFeatureFlag"));
    }

    @Test
    public void featureFlagShouldBeOverridableWithSystemProperty()
            throws IOException {
        var feature = "exampleFeatureFlag";
        var propertyName = FeatureFlags.SYSTEM_PROPERTY_PREFIX + feature;
        var previousValue = System.getProperty(propertyName);

        try {
            System.setProperty(propertyName, "true");
            String fileContents = String
                    .format("com.vaadin.experimental.%s=false\n", feature);
            createFeatureFlagsFile(fileContents);
            featureFlags.loadProperties();
            Assert.assertTrue(featureFlags.isEnabled(FeatureFlags.EXAMPLE));
            Assert.assertEquals(
                    "Feature flags file should not be overwritten by system property value",
                    fileContents, FileUtils.readFileToString(featureFlagsFile,
                            StandardCharsets.UTF_8));
        } finally {
            if (previousValue == null) {
                System.clearProperty(propertyName);
            } else {
                System.setProperty(propertyName, previousValue);
            }
        }
    }

    @Test
    public void featureFlagLoadedByResourceProviderShouldBeOverridableWithSystemProperty()
            throws IOException {
        var feature = "exampleFeatureFlag";
        var propertyName = FeatureFlags.SYSTEM_PROPERTY_PREFIX + feature;
        var previousValue = System.getProperty(propertyName);

        File flagsFile = new File(propertiesDir,
                "another-" + PROPERTIES_FILENAME);

        ResourceProvider resourceProvider = Mockito
                .mock(ResourceProvider.class);
        Mockito.when(
                resourceProvider.getApplicationResource(PROPERTIES_FILENAME))
                .thenReturn(flagsFile.toURI().toURL());
        Lookup lookup = context.getAttribute(Lookup.class);
        Mockito.when(lookup.lookup(ResourceProvider.class))
                .thenReturn(resourceProvider);

        try {
            String fileContents = String
                    .format("com.vaadin.experimental.%s=false\n", feature);
            FileUtils.write(flagsFile, fileContents, StandardCharsets.UTF_8);

            System.setProperty(propertyName, "true");
            featureFlags.loadProperties();
            Assert.assertTrue(featureFlags.isEnabled(FeatureFlags.EXAMPLE));
            Assert.assertFalse(
                    "Setting feature flag by system properties should not create feature flag file",
                    featureFlagsFile.exists());
        } finally {
            if (previousValue == null) {
                System.clearProperty(propertyName);
            } else {
                System.setProperty(propertyName, previousValue);
            }
        }
    }

    @Test
    public void noFeatureFlagFile_systemPropertyProvided_featureEnabled()
            throws IOException {
        var feature = "exampleFeatureFlag";
        var propertyName = FeatureFlags.SYSTEM_PROPERTY_PREFIX + feature;
        var previousValue = System.getProperty(propertyName);

        try {
            System.setProperty(propertyName, "true");
            featureFlags.loadProperties();
            Assert.assertTrue(
                    "Feature set with system property should be enabled",
                    featureFlags.isEnabled(FeatureFlags.EXAMPLE));
            Assert.assertFalse(
                    "Setting feature flag by system properties should not create feature flag file",
                    featureFlagsFile.exists());
        } finally {
            if (previousValue == null) {
                System.clearProperty(propertyName);
            } else {
                System.setProperty(propertyName, previousValue);
            }
        }
    }

    @Test
    public void noFeatureFlagFile_noSystemPropertyProvided_allFeatureDisabled()
            throws IOException {
        var feature = "exampleFeatureFlag";
        var propertyName = FeatureFlags.SYSTEM_PROPERTY_PREFIX + feature;
        var previousValue = System.getProperty(propertyName);

        try {
            if (previousValue != null) {
                System.clearProperty(propertyName);
            }
            featureFlags.loadProperties();
            Assert.assertFalse(
                    "Feature not set with system property should be disabled by default",
                    featureFlags.isEnabled(FeatureFlags.EXAMPLE));
        } finally {
            if (previousValue != null) {
                System.setProperty(propertyName, previousValue);
            }
        }
    }

    private boolean hasUsageStatsEntry(String name) {
        return UsageStatistics.getEntries()
                .filter(entry -> entry.getName().equals(name)).findFirst()
                .isPresent();
    }

    private void mockResourcesLocation() {
        VaadinService service = Mockito.mock(VaadinService.class);
        VaadinService.setCurrent(service);
        VaadinContext vaadinContext = Mockito.mock(VaadinContext.class);
        Mockito.when(service.getContext()).thenReturn(vaadinContext);

        ApplicationConfiguration applicationConfiguration = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(vaadinContext.getAttribute(
                Mockito.eq(ApplicationConfiguration.class), Mockito.any()))
                .thenReturn(applicationConfiguration);
        Mockito.when(applicationConfiguration.getJavaResourceFolder())
                .thenReturn(propertiesDir);

    }

    private void createFeatureFlagsFile(String data) throws IOException {
        FileUtils.write(featureFlagsFile, data, StandardCharsets.UTF_8);
    }
}
