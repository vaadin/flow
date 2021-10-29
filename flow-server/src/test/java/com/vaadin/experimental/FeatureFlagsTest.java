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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.server.MockVaadinContext;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

public class FeatureFlagsTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private VaadinContext context;
    private FeatureFlags featureFlags;
    private File propertiesDir;

    @Before
    public void before() throws IOException {
        propertiesDir = temporaryFolder.newFolder();

        context = new MockVaadinContext();
        featureFlags = FeatureFlags.get(context);
        featureFlags.setPropertiesLocation(propertiesDir);
        setProductionMode(false);

        mockResourcesLocation();
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
                FileUtils.readFileToString(
                        new File(propertiesDir,
                                FeatureFlags.PROPERTIES_FILENAME),
                        StandardCharsets.UTF_8));

        featureFlags.setEnabled(FeatureFlags.EXAMPLE.getId(), false);
        Assert.assertFalse("Feature should have been disabled",
                featureFlags.isEnabled(FeatureFlags.EXAMPLE));
        Assert.assertEquals(
                "Feature flags file should be empty when no features are enabled",
                "",
                FileUtils.readFileToString(
                        new File(propertiesDir,
                                FeatureFlags.PROPERTIES_FILENAME),
                        StandardCharsets.UTF_8));
    }

    @Test(expected = IllegalStateException.class)
    public void setEnabledOnlyInDevelopmentMode() throws IOException {
        setProductionMode(true);
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
        UsageStatistics.clearEntries();
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
        UsageStatistics.clearEntries();
        featureFlags.setEnabled(FeatureFlags.EXAMPLE.getId(), false);
        Assert.assertFalse(
                hasUsageStatsEntry("flow/featureflags/exampleFeatureFlag"));

    }

    @Test
    public void enabledFeatureFlagsMarkedInStatsWhenToggled()
            throws IOException {
        createFeatureFlagsFile(
                "com.vaadin.experimental.exampleFeatureFlag=false\n");
        UsageStatistics.clearEntries();
        featureFlags.setEnabled(FeatureFlags.EXAMPLE.getId(), true);
        Assert.assertTrue(
                hasUsageStatsEntry("flow/featureflags/exampleFeatureFlag"));

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
        FileUtils.write(
                new File(propertiesDir, FeatureFlags.PROPERTIES_FILENAME), data,
                StandardCharsets.UTF_8);
    }

    private void setProductionMode(boolean productionMode) {
        ApplicationConfiguration appConfig = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(appConfig.isProductionMode()).thenReturn(productionMode);

        Lookup lookup = context.getAttribute(Lookup.class);
        Mockito.when(lookup.lookup(ApplicationConfiguration.class))
                .thenReturn(appConfig);
    }
}
